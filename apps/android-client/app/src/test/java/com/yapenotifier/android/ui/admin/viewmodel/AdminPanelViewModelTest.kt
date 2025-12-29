package com.yapenotifier.android.ui.admin.viewmodel

import android.app.Application
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.model.Notification
import com.yapenotifier.android.data.model.PaginatedResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AdminPanelViewModelTest {

    @Mock
    private lateinit var mockApiService: ApiService

    private lateinit var application: Application
    private lateinit var viewModel: AdminPanelViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        application = RuntimeEnvironment.getApplication()
        // Note: In a real test, you'd need to inject the mock API service
        // For now, this is a template showing the structure
    }

    @Test
    fun `loadNotifications should update UI state with notifications`() = runTest {
        // Given
        val mockNotifications = listOf(
            createMockNotification(1, "yape", "Test notification 1"),
            createMockNotification(2, "plin", "Test notification 2")
        )
        val mockResponse = PaginatedResponse(
            data = mockNotifications,
            currentPage = 1,
            lastPage = 1,
            perPage = 50,
            total = 2
        )

        // This is a template - actual implementation would require dependency injection
        // whenever(mockApiService.getNotifications(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        //     .thenReturn(Response.success(mockResponse))

        // When
        // viewModel.loadNotifications()

        // Then
        // val uiState = viewModel.uiState.value
        // assertEquals(mockNotifications, uiState?.notifications)
        // assertFalse(uiState?.loading ?: true)
    }

    private fun createMockNotification(
        id: Long,
        sourceApp: String,
        title: String
    ): Notification {
        return Notification(
            id = id,
            userId = 1,
            commerceId = 1,
            deviceId = 1,
            sourceApp = sourceApp,
            packageName = "com.example.app",
            androidUserId = 0,
            androidUid = 0,
            appInstanceId = null,
            title = title,
            body = "Test body",
            amount = 100.0,
            currency = "PEN",
            payerName = "Test User",
            postedAt = null,
            receivedAt = "2024-01-01 12:00:00",
            rawJson = null,
            status = "pending",
            isDuplicate = false,
            createdAt = "2024-01-01 12:00:00",
            updatedAt = "2024-01-01 12:00:00",
            device = null,
            appInstance = null
        )
    }
}

