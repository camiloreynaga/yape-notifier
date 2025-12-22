package com.yapenotifier.android.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.model.MonitorPackage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class MonitoredAppsUiState(
    val packages: List<MonitorPackage> = emptyList(),
    val filteredPackages: List<MonitorPackage> = emptyList(),
    val searchQuery: String = "",
    val filterType: FilterType = FilterType.ALL,
    val selectedPackages: Set<String> = emptySet(),
    val loading: Boolean = false,
    val error: String? = null,
    val saving: Boolean = false,
    val saveError: String? = null,
    val lastUpdated: String? = null
)

enum class FilterType {
    ALL, MONITORED, NOT_MONITORED
}

class MonitoredAppsSelectionViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = RetrofitClient.createApiService(application)
    private val preferencesManager = PreferencesManager(application)

    private val _uiState = MutableLiveData<MonitoredAppsUiState>(MonitoredAppsUiState())
    val uiState: LiveData<MonitoredAppsUiState> = _uiState

    init {
        loadMonitorPackages()
    }

    fun loadMonitorPackages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(loading = true, error = null)
            
            try {
                val response = apiService.getMonitorPackages(activeOnly = false)
                
                if (response.isSuccessful) {
                    val packages = response.body()?.packages ?: emptyList()
                    val selectedPackages = packages.filter { it.isActive }.map { it.packageName }.toSet()
                    
                    _uiState.value = _uiState.value?.copy(
                        packages = packages,
                        selectedPackages = selectedPackages,
                        loading = false,
                        lastUpdated = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    )
                    applyFilters()
                } else {
                    val errorMessage = "Error al cargar apps: ${response.code()}"
                    _uiState.value = _uiState.value?.copy(
                        loading = false,
                        error = errorMessage
                    )
                }
            } catch (e: Exception) {
                Log.e("MonitoredAppsViewModel", "Error loading monitor packages", e)
                _uiState.value = _uiState.value?.copy(
                    loading = false,
                    error = "Error de conexión: ${e.message}"
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value?.copy(searchQuery = query)
        applyFilters()
    }

    fun setFilterType(filterType: FilterType) {
        _uiState.value = _uiState.value?.copy(filterType = filterType)
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value ?: return
        val packages = state.packages
        
        // Apply search filter
        val searchFiltered = if (state.searchQuery.isBlank()) {
            packages
        } else {
            val query = state.searchQuery.lowercase()
            packages.filter {
                it.packageName.lowercase().contains(query) ||
                it.appName?.lowercase()?.contains(query) == true ||
                it.description?.lowercase()?.contains(query) == true
            }
        }
        
        // Apply type filter
        val typeFiltered = when (state.filterType) {
            FilterType.ALL -> searchFiltered
            FilterType.MONITORED -> searchFiltered.filter { it.isActive }
            FilterType.NOT_MONITORED -> searchFiltered.filter { !it.isActive }
        }
        
        _uiState.value = state.copy(filteredPackages = typeFiltered)
    }

    fun togglePackageStatus(packageId: Long) {
        viewModelScope.launch {
            val currentState = _uiState.value ?: return@launch
            val packageToToggle = currentState.packages.find { it.id == packageId } ?: return@launch
            val newStatus = !packageToToggle.isActive
            
            _uiState.value = currentState.copy(saving = true, saveError = null)
            
            try {
                val response = apiService.toggleMonitorPackageStatus(
                    packageId,
                    mapOf("is_active" to newStatus)
                )
                
                if (response.isSuccessful) {
                    val updatedPackage = response.body()
                    if (updatedPackage != null) {
                        // Update the package in the list
                        val updatedPackages = currentState.packages.map {
                            if (it.id == packageId) updatedPackage else it
                        }
                        
                        val updatedSelected = if (newStatus) {
                            currentState.selectedPackages + updatedPackage.packageName
                        } else {
                            currentState.selectedPackages - updatedPackage.packageName
                        }
                        
                        _uiState.value = currentState.copy(
                            packages = updatedPackages,
                            selectedPackages = updatedSelected,
                            saving = false,
                            lastUpdated = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        )
                        applyFilters()
                        
                        // Save to local preferences
                        preferencesManager.saveSelectedMonitoredPackages(updatedSelected)
                    } else {
                        _uiState.value = currentState.copy(
                            saving = false,
                            saveError = "Respuesta inválida del servidor"
                        )
                    }
                } else {
                    _uiState.value = currentState.copy(
                        saving = false,
                        saveError = "Error al actualizar: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                Log.e("MonitoredAppsViewModel", "Error toggling package status", e)
                _uiState.value = currentState.copy(
                    saving = false,
                    saveError = "Error de conexión: ${e.message}"
                )
            }
        }
    }

    fun getMonitoredCount(): Int {
        return _uiState.value?.selectedPackages?.size ?: 0
    }

    fun refresh() {
        loadMonitorPackages()
    }
}

