package com.yapenotifier.android.ui.admin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.model.Notification
import com.yapenotifier.android.data.model.PaginatedResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
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

sealed class PollingState {
    object Idle : PollingState()
    object Active : PollingState()
    object Paused : PollingState()
    data class Error(val message: String) : PollingState()
}

@HiltViewModel
class AdminPanelViewModel @Inject constructor(
    application: Application,
    private val apiService: ApiService
) : AndroidViewModel(application) {

    private val _uiState = MutableLiveData<AdminPanelUiState>(AdminPanelUiState())
    val uiState: LiveData<AdminPanelUiState> = _uiState

    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _pollingState = MutableLiveData<PollingState>(PollingState.Idle)
    val pollingState: LiveData<PollingState> = _pollingState

    private var currentFilters = mutableMapOf<String, Any?>()

    // Estados de polling
    private var pollingJob: Job? = null
    private var isPollingActive = false
    private var isUserTyping = false
    private var consecutiveErrors = 0
    private val maxConsecutiveErrors = 3

    init {
        Timber.d("AdminPanelViewModel init - cargando notificaciones iniciales")
        loadNotifications()
    }

    fun loadNotifications(refresh: Boolean = false) {
        viewModelScope.launch {
            loadNotificationsInternal(refresh, silent = false)
        }
    }

    /**
     * Carga notificaciones con opción de modo silencioso (sin mostrar loading)
     */
    private suspend fun loadNotificationsInternal(refresh: Boolean = false, silent: Boolean = false): Boolean {
        if (_uiState.value?.loading == true && !silent) return false

        return try {
            val currentState = _uiState.value ?: AdminPanelUiState()
            val page = if (refresh) 1 else currentState.currentPage

            if (!silent) {
                _uiState.value = currentState.copy(loading = true, error = null)
            }

            Timber.d("AdminPanelViewModel: Cargando notificaciones - page=$page, filters=$currentFilters")
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

            Timber.d("AdminPanelViewModel: Respuesta recibida - isSuccessful=${response.isSuccessful}, code=${response.code()}")
            if (response.isSuccessful) {
                val paginatedResponse = response.body()
                Timber.d("AdminPanelViewModel: paginatedResponse=${paginatedResponse != null}, data count=${paginatedResponse?.data?.size ?: 0}, total=${paginatedResponse?.total ?: 0}")
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
                    true
                } else {
                    if (!silent) {
                        _uiState.value = currentState.copy(loading = false, error = "No se pudieron cargar las notificaciones")
                    }
                    false
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.e("AdminPanelViewModel: Error en respuesta - code=${response.code()}, errorBody=$errorBody")
                if (!silent) {
                    _uiState.value = currentState.copy(loading = false, error = "Error ${response.code()}: ${errorBody ?: "Sin detalles"}")
                }
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "AdminPanelViewModel: Excepción al cargar notificaciones")
            if (!silent) {
                _uiState.value = _uiState.value?.copy(
                    loading = false,
                    error = e.message ?: "Error de conexión"
                )
            }
            false
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

    /**
     * Marca que el usuario está escribiendo (pausa polling)
     */
    fun setUserTyping(typing: Boolean) {
        isUserTyping = typing
        if (typing) {
            pausePolling()
        } else {
            resumePolling()
        }
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

    /**
     * Inicia polling inteligente con manejo de errores y optimización de batería
     */
    fun startPolling() {
        if (isPollingActive) return

        isPollingActive = true
        consecutiveErrors = 0
        _pollingState.value = PollingState.Active

        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var pollInterval = 15000L // 15 segundos por defecto

            while (isActive && isPollingActive) {
                try {
                    // Verificar condiciones antes de hacer polling
                    if (!shouldPoll()) {
                        delay(5000) // Esperar 5 segundos antes de verificar de nuevo
                        continue
                    }

                    // Hacer polling
                    val success = loadNotificationsInternal(refresh = true, silent = true)

                    if (success) {
                        consecutiveErrors = 0
                        pollInterval = 15000L // Resetear intervalo a 15s si fue exitoso
                    } else {
                        consecutiveErrors++
                        // Aumentar intervalo exponencialmente en caso de errores
                        pollInterval = minOf(pollInterval * 2, 120000L) // Máximo 2 minutos

                        if (consecutiveErrors >= maxConsecutiveErrors) {
                            _pollingState.value = PollingState.Error("Error de conexión. Reintentando...")
                            // Esperar más tiempo antes de reintentar
                            delay(30000)
                            consecutiveErrors = 0 // Resetear después de esperar
                        }
                    }

                    delay(pollInterval)
                } catch (e: Exception) {
                    Timber.e(e, "Error en polling")
                    consecutiveErrors++
                    pollInterval = minOf(pollInterval * 2, 120000L)
                    delay(pollInterval)
                }
            }
        }
    }

    /**
     * Detiene el polling
     */
    fun stopPolling() {
        isPollingActive = false
        pollingJob?.cancel()
        pollingJob = null
        _pollingState.value = PollingState.Idle
    }

    /**
     * Pausa temporalmente el polling (ej: cuando usuario está escribiendo)
     */
    fun pausePolling() {
        if (isPollingActive) {
            _pollingState.value = PollingState.Paused
        }
    }

    /**
     * Reanuda el polling después de una pausa
     */
    fun resumePolling() {
        if (isPollingActive && _pollingState.value is PollingState.Paused) {
            _pollingState.value = PollingState.Active
        }
    }

    /**
     * Verifica si se debe hacer polling
     */
    private fun shouldPoll(): Boolean {
        // No hacer polling si:
        // 1. App no está en foreground
        if (!isAppInForeground()) return false

        // 2. Usuario está escribiendo (evitar interrupciones)
        if (isUserTyping) return false

        // 3. Ya hay una carga en progreso
        if (_uiState.value?.loading == true) return false

        return true
    }

    /**
     * Verifica si la app está en foreground
     */
    private fun isAppInForeground(): Boolean {
        return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(
            androidx.lifecycle.Lifecycle.State.STARTED
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }

    companion object {
        private const val TAG = "AdminPanelViewModel"
    }
}

