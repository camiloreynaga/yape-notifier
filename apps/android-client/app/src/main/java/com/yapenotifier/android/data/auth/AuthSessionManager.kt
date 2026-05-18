package com.yapenotifier.android.data.auth

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.yapenotifier.android.R
import com.yapenotifier.android.YapeNotifierApplication
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.ui.SplashActivity
import com.yapenotifier.android.util.FileLogger
import com.yapenotifier.android.util.ServiceStatusManager
import com.yapenotifier.android.worker.SendNotificationWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Autoritative idempotent state machine for the auth lifecycle.
 *
 * The source of truth lives in DataStore (PreferencesManager.awaitingLogin and authToken).
 * This object exists to:
 *   - Atomically transition the auth state on 401 and on login success.
 *   - Drive the user-facing notification ("Sesión expirada").
 *   - Keep the in-memory Retrofit token cache aligned with DataStore.
 *
 * The in-memory mirror (mirrorAwaitingLogin) is provided as a convenience for live UI
 * consumers (StateFlow etc.). The SendNotificationWorker MUST NOT read this mirror —
 * it must read DataStore directly.
 */
object AuthSessionManager {

    private const val LOGIN_REQUIRED_NOTIFICATION_ID = 2003

    // Live UI convenience. NOT the source of truth.
    @Volatile private var mirrorAwaitingLogin: Boolean = false

    private val mutex = Mutex()
    private lateinit var appContext: Context
    private lateinit var appScope: CoroutineScope
    @Volatile private var initialized: Boolean = false

    /**
     * SYNCHRONOUS init. Registers appContext + appScope + initialized = true
     * immediately and returns. Hydration from DataStore runs in the background.
     * Call from YapeNotifierApplication.onCreate() BEFORE any other auth-related code.
     */
    fun initialize(context: Context, scope: CoroutineScope) {
        appContext = context.applicationContext
        appScope = scope
        initialized = true
        FileLogger.log(
            "AUTH",
            "AuthSessionManager initialized (sync). Hydrating mirror in background.",
            "info",
        )

        // Hydrate mirror + keep it in sync with DataStore — background only
        appScope.launch(Dispatchers.IO) {
            val prefs = PreferencesManager(appContext)
            prefs.awaitingLogin
                .onEach { mirrorAwaitingLogin = it }
                .launchIn(this)
        }
    }

    /**
     * Fire-and-forget for non-coroutine callers (OkHttp interceptor).
     * NEVER returns silently — if appScope is not yet set, uses a fallback scope.
     */
    fun handleTokenExpiredAsync(context: Context, endpoint: String) {
        val ctx = context.applicationContext
        val scope = if (initialized && ::appScope.isInitialized) {
            appScope
        } else {
            // Emergency fallback — app is in an odd state, but a 401 must not be lost
            CoroutineScope(SupervisorJob() + Dispatchers.IO)
        }
        scope.launch(Dispatchers.IO) {
            handleTokenExpired(ctx, endpoint)
        }
    }

    suspend fun handleTokenExpired(context: Context, endpoint: String) {
        mutex.withLock {
            val prefs = PreferencesManager(context)
            // Idempotent: source of truth is DataStore, not the mirror
            if (prefs.awaitingLogin.first()) {
                FileLogger.log(
                    "AUTH",
                    "TOKEN_EXPIRED on $endpoint - already awaiting login, ignored",
                    "info",
                )
                return
            }

            FileLogger.log("AUTH", "TOKEN_EXPIRED on $endpoint - clearing local state", "error")

            prefs.setAwaitingLogin(true)
            prefs.clearAuthToken()
            RetrofitClient.updateTokenCache(null)

            ServiceStatusManager.updateStatus("⚠️ Sesión expirada - Inicia sesión nuevamente")
            showLoginRequiredNotification(context)
            AuthEventBus.emit(AuthEvent.TokenExpired)
        }
    }

    /**
     * Called by the 3 login VMs after a successful login, REPLACING the previous direct
     * call to preferencesManager.saveAuthToken(token).
     */
    suspend fun handleLoginSucceeded(context: Context, token: String) {
        mutex.withLock {
            val prefs = PreferencesManager(context)

            // Order matters: token cache before scheduling the drain worker
            prefs.saveAuthToken(token)
            RetrofitClient.updateTokenCache(token)
            prefs.setAwaitingLogin(false)

            cancelLoginRequiredNotification(context)
            FileLogger.log("AUTH", "LOGIN_OK - draining pending notifications", "info")
            AuthEventBus.emit(AuthEvent.LoginSucceeded)

            scheduleSendWorker(context)
        }
    }

    /**
     * Read the in-memory mirror. Use ONLY for live UI consumers.
     * The Worker must call PreferencesManager.awaitingLogin.first() instead.
     */
    fun isAwaitingLoginMirror(): Boolean = mirrorAwaitingLogin

    private fun showLoginRequiredNotification(context: Context) {
        val intent = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            LOGIN_REQUIRED_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, YapeNotifierApplication.ALERT_CHANNEL_ID)
            .setContentTitle("NotiCentral · Sesión expirada")
            .setContentText("Toca para volver a iniciar sesión y reanudar la captura.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Tu sesión expiró. Las nuevas notificaciones de Yape no se están subiendo " +
                        "hasta que vuelvas a iniciar sesión.",
                ),
            )
            .setSmallIcon(R.drawable.ic_bell_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(LOGIN_REQUIRED_NOTIFICATION_ID, notification)
    }

    private fun cancelLoginRequiredNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(LOGIN_REQUIRED_NOTIFICATION_ID)
    }

    private fun scheduleSendWorker(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<SendNotificationWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
