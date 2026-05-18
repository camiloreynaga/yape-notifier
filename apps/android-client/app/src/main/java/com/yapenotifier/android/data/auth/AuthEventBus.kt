package com.yapenotifier.android.data.auth

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Events emitted by the authentication system.
 * Used for live UI updates only — not as a source of truth.
 * The autoritative state lives in [AuthSessionManager] and DataStore.
 */
sealed class AuthEvent {
    /** Server returned 401 on an authenticated request. The session is no longer valid. */
    object TokenExpired : AuthEvent()

    /** User successfully completed login (any of the 3 login VMs). */
    object LoginSucceeded : AuthEvent()

    /** User manually logged out (e.g. tapped "Desvincular dispositivo"). */
    object ManualLogout : AuthEvent()
}

/**
 * Process-level event bus for auth lifecycle events.
 *
 * IMPORTANT: this bus is a COMPLEMENT to [AuthSessionManager], not a replacement.
 * Critical side effects (clear token, persist awaitingLogin, show notification)
 * happen inside [AuthSessionManager] regardless of whether any subscriber is listening.
 *
 * Use this bus only to update LIVE UI in response to auth events — never to drive
 * critical recovery logic. Dropping a buffered event is acceptable for UI; it is
 * NOT acceptable for state cleanup, which is why state cleanup lives elsewhere.
 */
object AuthEventBus {
    private val _events = MutableSharedFlow<AuthEvent>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun emit(event: AuthEvent) {
        _events.tryEmit(event)
    }
}
