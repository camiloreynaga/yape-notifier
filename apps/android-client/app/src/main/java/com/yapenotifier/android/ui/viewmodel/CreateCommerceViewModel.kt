package com.yapenotifier.android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.model.Commerce
import com.yapenotifier.android.data.repository.CommerceRepository
import kotlinx.coroutines.launch

class CreateCommerceViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.createApiService(application)
    private val commerceRepository = CommerceRepository(apiService)

    sealed class CreateCommerceState {
        object Idle : CreateCommerceState()
        object Loading : CreateCommerceState()
        data class Success(val commerce: Commerce) : CreateCommerceState()
        data class Error(val message: String) : CreateCommerceState()
    }

    private val _createCommerceState = MutableLiveData<CreateCommerceState>(CreateCommerceState.Idle)
    val createCommerceState: LiveData<CreateCommerceState> = _createCommerceState

    fun createCommerce(name: String, country: String, currency: String) {
        _createCommerceState.value = CreateCommerceState.Loading
        viewModelScope.launch {
            try {
                val response = commerceRepository.createCommerce(name, country, currency)
                if (response.isSuccessful) {
                    response.body()?.let {
                        _createCommerceState.postValue(CreateCommerceState.Success(it.commerce))
                    } ?: run {
                        _createCommerceState.postValue(CreateCommerceState.Error("Empty response body"))
                    }
                } else {
                    _createCommerceState.postValue(CreateCommerceState.Error(response.errorBody()?.string() ?: "Unknown error"))
                }
            } catch (e: Exception) {
                _createCommerceState.postValue(CreateCommerceState.Error(e.message ?: "Unknown error"))
            }
        }
    }
}
