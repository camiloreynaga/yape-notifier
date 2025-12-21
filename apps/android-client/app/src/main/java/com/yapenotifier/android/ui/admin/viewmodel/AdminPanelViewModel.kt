package com.yapenotifier.android.ui.admin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.model.Notification
import com.yapenotifier.android.data.model.PaginatedResponse
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class AdminPanelUiState(
    val notifications: List<Notification> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = false,
    val currentPage: Int = 1,
    val total: Int = 0
)

class AdminPanelViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService: ApiService = RetrofitClient.createApiService(application)

    private val _uiState = MutableLiveData<AdminPanelUiState>(AdminPanelUiState())
    val uiState: LiveData<AdminPanelUiState> = _uiState

    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery

    private var currentFilters = mutableMapOf<String, Any?>()

    init {
        loadNotifications()
    }

    fun loadNotifications(refresh: Boolean = false) {
        if (_uiState.value?.loading == true) return

        viewModelScope.launch {
            try {
                val currentState = _uiState.value ?: AdminPanelUiState()
                val page = if (refresh) 1 else currentState.currentPage

                _uiState.value = currentState.copy(loading = true, error = null)

                val response = apiService.getNotifications(
                    deviceId = currentFilters["device_id"] as? Long,
                    sourceApp = currentFilters["source_app"] as? String,
                    packageName = currentFilters["package_name"] as? String,
                    appInstanceId = currentFilters["app_instance_id"] as? Long,
                    startDate = currentFilters["start_date"] as? String,
                    endDate = currentFilters["end_date"] as? String,
                    status = currentFilters["status"] as? String,
                    excludeDuplicates = currentFilters["exclude_duplicates"] as? Boolean,
                    perPage = 50,
                    page = page
                )

                if (response.isSuccessful) {
                    val paginatedResponse = response.body()
                    if (paginatedResponse != null) {
                        val newNotifications = if (refresh) {
                            paginatedResponse.data
                        } else {
                            currentState.notifications + paginatedResponse.data
                        }

                        _uiState.value = AdminPanelUiState(
                            notifications = newNotifications,
                            loading = false,
                            hasMore = paginatedResponse.currentPage < paginatedResponse.lastPage,
                            currentPage = paginatedResponse.currentPage,
                            total = paginatedResponse.total
                        )
                    } else {
                        _uiState.value = currentState.copy(loading = false, error = "No se pudieron cargar las notificaciones")
                    }
                } else {
                    _uiState.value = currentState.copy(loading = false, error = "Error ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(
                    loading = false,
                    error = e.message ?: "Error de conexión"
                )
            }
        }
    }

    fun loadMore() {
        val currentState = _uiState.value ?: return
        if (!currentState.loading && currentState.hasMore) {
            _uiState.value = currentState.copy(currentPage = currentState.currentPage + 1)
            loadNotifications()
        }
    }

    fun refresh() {
        _uiState.value = AdminPanelUiState()
        loadNotifications(refresh = true)
    }

    fun setFilter(key: String, value: Any?) {
        if (value == null) {
            currentFilters.remove(key)
        } else {
            currentFilters[key] = value
        }
        refresh()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        // Filter locally for now, can be improved with backend search
        filterBySearchQuery(query)
    }

    private fun filterBySearchQuery(query: String) {
        val currentState = _uiState.value ?: return
        if (query.isEmpty()) {
            // Reload from server if query is cleared
            refresh()
            return
        }

        val filtered = currentState.notifications.filter { notification ->
            notification.title.contains(query, ignoreCase = true) ||
            notification.body.contains(query, ignoreCase = true) ||
            notification.payerName?.contains(query, ignoreCase = true) == true ||
            notification.amount?.toString()?.contains(query, ignoreCase = true) == true ||
            notification.device?.name?.contains(query, ignoreCase = true) == true
        }

        _uiState.value = currentState.copy(notifications = filtered)
    }

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.updateNotificationStatus(
                    notificationId,
                    mapOf("status" to "validated")
                )
                if (response.isSuccessful) {
                    // Update local state
                    val currentState = _uiState.value ?: return@launch
                    val updatedNotifications = currentState.notifications.map { notification ->
                        if (notification.id == notificationId) {
                            notification.copy(status = "validated")
                        } else {
                            notification
                        }
                    }
                    _uiState.value = currentState.copy(notifications = updatedNotifications)
                }
            } catch (e: Exception) {
                // Handle error silently or show toast
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val currentState = _uiState.value ?: return@launch
            val unreadNotifications = currentState.notifications.filter { it.status == "pending" }
            
            unreadNotifications.forEach { notification ->
                try {
                    apiService.updateNotificationStatus(
                        notification.id,
                        mapOf("status" to "validated")
                    )
                } catch (e: Exception) {
                    // Continue with next notification
                }
            }

            // Update local state
            val updatedNotifications = currentState.notifications.map { notification ->
                if (notification.status == "pending") {
                    notification.copy(status = "validated")
                } else {
                    notification
                }
            }
            _uiState.value = currentState.copy(notifications = updatedNotifications)
        }
    }

    fun getTodayDateFilter(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}

