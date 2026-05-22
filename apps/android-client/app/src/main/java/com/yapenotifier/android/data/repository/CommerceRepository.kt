package com.yapenotifier.android.data.repository

import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.model.CommerceCheckResponse
import com.yapenotifier.android.data.model.CommerceResponse
import com.yapenotifier.android.data.model.CreateCommerceRequest
import retrofit2.Response

class CommerceRepository(private val apiService: ApiService) {

    suspend fun createCommerce(name: String, country: String, currency: String): Response<CommerceResponse> {
        val request = CreateCommerceRequest(name, country, currency)
        return apiService.createCommerce(request)
    }

    suspend fun getCommerce(): Response<CommerceResponse> {
        return apiService.getCommerce()
    }

    suspend fun checkCommerce(): Response<CommerceCheckResponse> {
        return apiService.checkCommerce()
    }
}
