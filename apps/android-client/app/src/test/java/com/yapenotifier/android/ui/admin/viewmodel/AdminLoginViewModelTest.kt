package com.yapenotifier.android.ui.admin.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.model.*
import com.yapenotifier.android.data.repository.CommerceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*
import retrofit2.Response

/**
 * Tests unitarios profesionales para AdminLoginViewModel.
 * Professional approach: Tests aislados con mocks, usando coroutines test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdminLoginViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var application: Application
    private lateinit var apiService: ApiService
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var commerceRepository: CommerceRepository
    private lateinit var viewModel: AdminLoginViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        application = mock()
        apiService = mock()
        preferencesManager = mock()
        commerceRepository = mock()
        
        viewModel = AdminLoginViewModel(
            application,
            apiService,
            preferencesManager,
            commerceRepository
        )
    }

    @Test
    fun `login with valid credentials and existing commerce should succeed`() = runTest(testDispatcher) {
        // Given
        val email = "admin@test.com"
        val password = "password123"
        
        val authResponse = AuthResponse(
            token = "test_token",
            user = User(
                id = 1,
                name = "Admin",
                email = email,
                role = "admin",
                commerceId = 1
            )
        )
        
        val commerceCheckResponse = CommerceCheckResponse(
            hasCommerce = true,
            commerceId = 1
        )

        whenever(apiService.login(any())).thenReturn(
            Response.success(authResponse)
        )
        whenever(apiService.getCurrentUser()).thenReturn(
            Response.success(authResponse.user)
        )
        whenever(apiService.checkCommerce()).thenReturn(
            Response.success(commerceCheckResponse)
        )
        whenever(preferencesManager.saveAuthToken(any())).thenReturn(Unit)
        whenever(preferencesManager.saveUserEmail(any())).thenReturn(Unit)

        // When
        viewModel.login(email, password)
        advanceUntilIdle()

        // Then
        val result = viewModel.loginResult.value
        assert(result?.success == true)
        assert(result?.needsCommerceCreation == false)
        verify(preferencesManager).saveAuthToken("test_token")
        verify(preferencesManager).saveUserEmail(email)
    }

    @Test
    fun `login with valid credentials but no commerce should require creation`() = runTest(testDispatcher) {
        // Given
        val email = "admin@test.com"
        val password = "password123"
        
        val authResponse = AuthResponse(
            token = "test_token",
            user = User(
                id = 1,
                name = "Admin",
                email = email,
                role = "admin",
                commerceId = null
            )
        )
        
        val commerceCheckResponse = CommerceCheckResponse(
            hasCommerce = false,
            commerceId = null
        )

        whenever(apiService.login(any())).thenReturn(
            Response.success(authResponse)
        )
        whenever(apiService.getCurrentUser()).thenReturn(
            Response.success(authResponse.user)
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
        assert(result?.needsCommerceCreation == true)
    }

    @Test
    fun `login with invalid credentials should fail`() = runTest(testDispatcher) {
        // Given
        val email = "admin@test.com"
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
    fun `login with non-admin user should fail`() = runTest(testDispatcher) {
        // Given
        val email = "user@test.com"
        val password = "password123"
        
        val authResponse = AuthResponse(
            token = "test_token",
            user = User(
                id = 1,
                name = "User",
                email = email,
                role = "capturer", // Not admin
                commerceId = null
            )
        )

        whenever(apiService.login(any())).thenReturn(
            Response.success(authResponse)
        )
        whenever(apiService.getCurrentUser()).thenReturn(
            Response.success(authResponse.user)
        )

        // When
        viewModel.login(email, password)
        advanceUntilIdle()

        // Then
        val result = viewModel.loginResult.value
        assert(result?.success == false)
        assert(result?.message?.contains("administradores") == true)
    }

    @Test
    fun `login should set loading state correctly`() = runTest(testDispatcher) {
        // Given
        val email = "admin@test.com"
        val password = "password123"
        
        whenever(apiService.login(any())).thenReturn(
            Response.error(500, okhttp3.ResponseBody.create(null, "Server Error"))
        )

        // When
        viewModel.login(email, password)
        
        // Then - loading should be true initially
        assert(viewModel.isLoading.value == true)
        
        advanceUntilIdle()
        
        // Then - loading should be false after completion
        assert(viewModel.isLoading.value == false)
    }
}

