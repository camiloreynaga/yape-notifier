package com.yapenotifier.android.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.local.db.AppDatabase
import com.yapenotifier.android.data.model.NotificationData
import com.yapenotifier.android.data.repository.AuthStatus
import com.yapenotifier.android.data.repository.NotificationRepository
import com.yapenotifier.android.data.repository.SendResult
import com.yapenotifier.android.util.FileLogger
import com.yapenotifier.android.util.PaymentNotificationParser
import com.yapenotifier.android.util.ServiceStatusManager
import com.yapenotifier.android.util.SourceAppMapper
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SendNotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val notificationDao = AppDatabase.getDatabase(appContext).capturedNotificationDao()
    private val repository = NotificationRepository(appContext)
    private val preferencesManager = PreferencesManager(appContext)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override suspend fun doWork(): Result {
        Timber.d("Starting worker to send pending notifications...")

        // Guard DOBLE: don't burn the network if we know auth is unusable.
        // Source of truth = DataStore (read directly, never the AuthSessionManager mirror).
        val awaitingLogin = preferencesManager.awaitingLogin.first()
        val authTokenValue = preferencesManager.authToken.first()
        if (awaitingLogin || authTokenValue.isNullOrBlank()) {
            FileLogger.log(
                "SEND",
                "Skipped batch: awaitingLogin=$awaitingLogin, tokenPresent=${!authTokenValue.isNullOrBlank()}",
                "info",
            )
            return Result.success()
        }

        // Professional Architecture: Authentication is OPTIONAL
        // Device linking via QR is the authorization mechanism
        // If device has commerce_id (from QR), it can send notifications without user authentication

        // Verify device is linked (has commerce_id)
        val commerceId = preferencesManager.commerceId.first()
        if (commerceId.isNullOrBlank()) {
            Timber.w("Dispositivo no vinculado. Por favor, escanea el código QR.")
            ServiceStatusManager.updateStatus("⚠️ Dispositivo no vinculado - Escanea QR")
            FileLogger.log("SEND", "BLOCKED: device not linked (commerceId null) - notifications will not be sent", "error")
            return Result.failure()
        }
        
        // Authentication status (optional - for logging only)
        val authStatus = repository.getAuthStatus()
        val isAuthenticated = authStatus == AuthStatus.AUTHENTICATED
        Timber.d("Device linked to commerce: $commerceId, User authenticated: $isAuthenticated")

        val pendingNotifications = notificationDao.getPendingNotifications()
        if (pendingNotifications.isEmpty()) {
            Timber.d("No pending notifications to send.")
            return Result.success()
        }
        
        // Fetch deviceId once for all notifications in this batch
        val deviceId = preferencesManager.deviceUuid.first() ?: ""
        if (deviceId.isEmpty()) {
            Timber.e("DeviceID is not set. Cannot send notifications.")
            FileLogger.log("SEND", "BLOCKED: deviceUuid not set - notifications will not be sent", "error")
            return Result.failure()
        }

        Timber.d("Found ${pendingNotifications.size} pending notifications for deviceId: $deviceId")
        FileLogger.log("SEND", "Starting batch: ${pendingNotifications.size} pending notifications", "info")
        var allSucceeded = true
        var authFailed = false

        for (notification in pendingNotifications) {
            // Parse payment details to extract amount, currency, and payer name
            val paymentDetails = PaymentNotificationParser.parse(notification.title, notification.body)

            // Map package_name to source_app (required by backend validation)
            var sourceApp = SourceAppMapper.mapPackageToSourceApp(notification.packageName)
            
            // If package mapping failed, try to infer from notification content
            // This handles cases where the package is our own app (com.yapenotifier.android)
            if (sourceApp == null) {
                Timber.d("Package mapping failed for '${notification.packageName}', attempting to infer from content")
                sourceApp = SourceAppMapper.inferSourceAppFromContent(notification.title, notification.body)
            }
            
            // If still null, we cannot determine source_app - mark as failed
            if (sourceApp == null) {
                Timber.w("Could not determine source_app for notification ID: ${notification.id}, package: ${notification.packageName}, title: '${notification.title}'. Marking as FAILED.")
                notificationDao.updateStatus(notification.id, "FAILED")
                continue
            }

            // If payment details couldn't be parsed, we still send the notification
            // but with null values for amount, currency, payer_name, and security_code
            // The backend can handle these null values
            val amount = paymentDetails?.amount
            val currency = paymentDetails?.currency ?: "PEN"
            val payerName = paymentDetails?.sender
            val securityCode = paymentDetails?.securityCode

            // Format posted_at timestamp if available
            val postedAt = notification.postedAt?.let {
                dateFormat.format(Date(it))
            }

            // Format received_at (when we captured it)
            val receivedAt = dateFormat.format(Date(notification.timestamp))

            // Build raw_json with all available metadata
            val rawJson = mutableMapOf<String, Any>(
                "package_name" to notification.packageName,
                "title" to notification.title,
                "body" to notification.body,
                "original_timestamp" to notification.timestamp
            ).apply {
                notification.androidUserId?.let { put("android_user_id", it) }
                notification.androidUid?.let { put("android_uid", it) }
                notification.postedAt?.let { put("posted_at", it) }
            }

            val notificationData = NotificationData(
                deviceId = deviceId,
                sourceApp = sourceApp, // Mapped source_app, not package_name
                packageName = notification.packageName,
                androidUserId = notification.androidUserId,
                androidUid = notification.androidUid,
                title = notification.title,
                body = notification.body, // Original body text
                amount = amount,
                currency = currency,
                payerName = payerName,
                securityCode = securityCode,
                postedAt = postedAt,
                receivedAt = receivedAt,
                rawJson = rawJson,
                status = "pending"
            )

            Timber.d("Sending notification ID: ${notification.id}")
            Timber.d("  deviceId: $deviceId")
            Timber.d("  sourceApp: $sourceApp")
            Timber.d("  packageName: ${notification.packageName}")
            Timber.d("  amount: $amount")
            Timber.d("  currency: $currency")
            Timber.d("  payerName: $payerName")
            Timber.d("  securityCode: $securityCode")
            Timber.d("  receivedAt: $receivedAt")
            Timber.d("  body length: ${notification.body.length} chars")

            val sendResult = repository.sendNotification(notificationData)

            when (sendResult) {
                is SendResult.Success -> {
                    notificationDao.updateStatus(notification.id, "SENT")
                    Timber.i("Successfully sent notification ID: ${notification.id}")
                }
                is SendResult.Error -> {
                    if (sendResult.isAuthError) {
                        Timber.e("Auth error (${sendResult.httpCode}) on notifId=${notification.id}. Dispatching to AuthSessionManager.")
                        FileLogger.log(
                            "SEND",
                            "AUTH_ERROR: http=${sendResult.httpCode}, notifId=${notification.id} - dispatched to AuthSessionManager",
                            "error",
                        )
                        com.yapenotifier.android.data.auth.AuthSessionManager.handleTokenExpiredAsync(
                            context = applicationContext,
                            endpoint = "/api/notifications",
                        )
                        authFailed = true
                        break
                    } else {
                        Timber.e("Failed to send notification ID: ${notification.id}. Error: ${sendResult.message}. Will retry later.")
                        FileLogger.log(
                            "SEND",
                            "ERROR: http=${sendResult.httpCode}, notifId=${notification.id}, msg=${sendResult.message}",
                            "error"
                        )
                        allSucceeded = false
                    }
                }
                is SendResult.NetworkError -> {
                    Timber.e("Network error while sending notification ID: ${notification.id}. Exception: ${sendResult.exception.message}. Will retry later.")
                    FileLogger.log(
                        "SEND",
                        "NETWORK_ERROR: notifId=${notification.id}, exception=${sendResult.exception.message}",
                        "error"
                    )
                    allSucceeded = false
                }
            }
        }

        // Si falló la autenticación durante el procesamiento, retornar success para evitar reintentos
        if (authFailed) {
            Timber.w("Batch detenido por error de autenticación. Las notificaciones pendientes se enviarán después del login.")
            return Result.success()
        }

        if (allSucceeded) {
            FileLogger.log("SEND", "Batch complete: all ${pendingNotifications.size} notifications sent successfully", "info")
            return Result.success()
        } else {
            return Result.retry()
        }
    }

    companion object {
        const val TAG = "SendNotificationWorker"
    }
}
