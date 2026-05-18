package com.yapenotifier.android.ui.model

/**
 * Composed UI status for the main capturer screen.
 * Order matters: TokenExpired has priority over PermissionRevoked over Listener status.
 *
 * Source of truth for each component:
 *   TokenExpired             = PreferencesManager.awaitingLogin (DataStore)
 *   PermissionRevoked        = NotificationAccessChecker.isNotificationAccessEnabled()
 *   ListenerDeadManualNeeded = ServiceStatusManager.recoveryModeFlow == MANUAL_ACTION_REQUIRED
 *                              (stubbed as MutableStateFlow(false) until Task 2.0)
 *   ListenerReconnecting     = ServiceStatusManager.isServiceConnected() == false (otherwise)
 *   CapturingOK              = everything healthy
 */
sealed class AppStatus {
    object TokenExpired : AppStatus()
    object PermissionRevoked : AppStatus()
    object ListenerDeadManualNeeded : AppStatus()
    object ListenerReconnecting : AppStatus()
    object CapturingOK : AppStatus()
}
