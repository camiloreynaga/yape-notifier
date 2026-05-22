package com.yapenotifier.android.ui.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.model.*
import com.yapenotifier.android.data.repository.CommerceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*
import retrofit2.Response

/**
 * Tests unitarios profesionales para LoginViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var application: Application
    private lateinit var apiService: ApiService
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var commerceRepository: CommerceRepository
    private lateinit var viewModel: LoginViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        application = mock()
        apiService = mock()
        preferencesManager = mock()
        commerceRepository = mock()
        
        // Mock device UUID flow
        whenever(preferencesManager.deviceUuid).thenReturn(flowOf("test-uuid-123"))
        
        viewModel = LoginViewModel(
            application,
            apiService,
            preferencesManager,
            commerceRepository
        )
    }

    @Test
    fun `login with valid credentials should succeed`() = runTest(testDispatcher) {
        // Given
        val email = "user@test.com"
        val password = "password123"
        val deviceUuid = "test-uuid-123"
        
        val authResponse = AuthResponse(
            token = "test_token",
            user = User(
                id = 1,
                name = "User",
                email = email,
                role = "capturer",
                commerceId = 1
            )
        )
        
        val deviceResponse = DeviceResponse(
            device = Device(
                id = 1,
                uuid = deviceUuid,
                name = "Test Device",
                platform = "android",
                commerceId = 1
            ),
            message = "Device registered"
        )
        
        val commerceCheckResponse = CommerceCheckResponse(
            hasCommerce = true,
            commerceId = 1
        )

        whenever(apiService.login(any())).thenReturn(
            Response.success(authResponse)
        )
        whenever(apiService.createDevice(any())).thenReturn(
            Response.success(deviceResponse)
        )
        whenever(apiService.checkCommerce()).thenReturn(
            Response.success(commerceCheckResponse)
        )
        whenever(preferencesManager.saveAuthToken(any())).thenReturn(Unit)
        whenever(preferencesManager.saveUserEmail(any())).thenReturn(Unit)
        whenever(preferencesManager.saveDeviceId(any())).thenReturn(Unit)
        whenever(preferencesManager.saveCommerceId(any())).thenReturn(Unit)

        // When
        viewModel.login(email, password)
        advanceUntilIdle()

        // Then
        val result = viewModel.loginResult.value
        assert(result?.success == true)
        assert(result?.needsCommerceCreation == false)
        assert(result?.needsDeviceLinking == false)
        verify(preferencesManager).saveAuthToken("test_token")
        verify(preferencesManager).saveUserEmail(email)
        verify(preferencesManager).saveDeviceId("1")
    }

    @Test
    fun `login with device not linked should require linking`() = runTest(testDispatcher) {
        // Given
        val email = "user@test.com"
        val password = "password123"
        val deviceUuid = "test-uuid-123"
        
        val authResponse = AuthResponse(
            token = "test_token",
            user = User(
                id = 1,
                name = "User",
                email = email,
                role = "capturer",
                commerceId = 1
            )
        )
        
        val deviceResponse = DeviceResponse(
            device = Device(
                id = 1,
                uuid = deviceUuid,
                name = "Test Device",
                platform = "android",
                commerceId = null // Device not linked
            ),
            message = "Device registered"
        )
        
        val commerceCheckResponse = CommerceCheckResponse(
            hasCommerce = true,
            commerceId = 1
        )

        whenever(apiService.login(any())).thenReturn(
            Response.success(authResponse)
        )
        whenever(apiService.createDevice(any())).thenReturn(
            Response.success(deviceResponse)
        )
        whenever(apiService.checkCommerce()).thenReturn(
            Response.success(commerceCheckResponse)
        )

        // When
        viewModel.login(email, password)
        advanceUntilIdle()

        // Then
        val result = viewModel.loginResult.value
        assert(result?.success == true)
        assert(result?.needsDeviceLinking == true)
    }

    @Test
    fun `login with invalid credentials should fail`() = runTest(testDispatcher) {
        // Given
        val email = "user@test.com"
        val password = "wrong_password"
        
        whenever(apiService.login(any())).thenReturn(
            Response.error(401, okhttp3.ResponseBody.create(null, "Unauthorized"))
        )

        // When
        viewModel.login(email, password)
        advanceUntilIdle()

        // Then
        val result = viewModel.loginResult.value
        assert(result?.success == false)
        assert(result?.message != null)
    }

    @Test
    fun `login should handle network errors gracefully`() = runTest(testDispatcher) {
        // Given
        val email = "user@test.com"
        val password = "password123"
        
        whenever(apiService.login(any())).thenThrow(
            java.io.IOException("Network error")
        )

        // When
        viewModel.login(email, password)
        advanceUntilIdle()

        // Then
        val result = viewModel.loginResult.value
        assert(result?.success == false)
        assert(result?.message?.contains("conexión") == true || result?.message?.contains("Network") == true)
    }
}

