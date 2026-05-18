# Android — Manejo de token expirado y recuperación del servicio en MIUI

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development o superpowers:executing-plans. Tareas con checkbox `- [ ]` para tracking. Backend con TDD vía PHPUnit; Android sin tests automáticos por convención del repo — verificación con build + run manual.

**Goal:** Eliminar dos bugs operacionales reportados por usuarios reales:
- (A) Token de Sanctum expira y la app queda enviando 401 indefinidamente con notificaciones varadas — sin avisar al usuario.
- (B) En MIUI/Xiaomi (y otros OEMs agresivos), Android mata el binding al `NotificationListenerService` y `requestRebind()` no logra reactivarlo. El usuario debe ir a Ajustes y togglear el permiso manualmente — proceso engorroso y sin guía visual.

**Architecture:** Dos fixes paralelos e independientes. (A) cambia el flujo del worker de envío + storage del token + agrega un canal de notificación de "sesión expirada" que abre `MainActivity` (que ya rutea a login si no hay token). (B) refuerza la UX del `MainActivity` y del `ServiceWatchdogWorker`: mensajes específicos por OEM, shortcut directo al toggle, opcionalmente un wizard de auto-recuperación que abre la pantalla del permiso y muestra instrucciones overlay.

**Tech stack:** Android Kotlin + Hilt-less manual DI (estilo del repo) + WorkManager + Retrofit/OkHttp + SharedPreferences (`PreferencesManager`) + Laravel Sanctum (lado servidor — solo se diagnostica, no se cambia en este plan a menos que se confirme un bug de config).

**Spec:** este mismo documento incluye spec + plan (alcance pequeño, no amerita doc separado).

**Evidencia que sustenta el plan:**
- Audit log [`service_log (2).txt`](service_log (2).txt) — 5 días, 0 AUTH_ERROR pero 171 RECONNECT con muchos falsos positivos.
- Audit log [`service_log-3.txt`](service_log-3.txt) — 8 días, **11 AUTH_ERROR 401** desde 2026-05-16 12:51, `pending=11` al final, sin recovery.
- Screenshots reportados por usuario el 2026-05-17:
  - Phone 1: "⚠️ Conectando... si no conecta, reactiva el permiso" — listener muerto en MIUI.
  - Phone 2: "✅ Capturando OK" arriba, "Token expirado" abajo, **11 pendientes**.

---

## Problema A — Token expirado sin recuperación

### A.1 Diagnóstico

#### Síntomas visibles para el usuario
- Pantalla principal muestra "Capturando OK" en verde — todo parece bien.
- Activity log muestra "API FAIL: 401" y "Token expirado - Inicia sesión nuevamente".
- Card "Pendientes" sube (0 → 1 → 2 → ... → 11) sin que el usuario sepa.
- No hay flujo guiado para volver a loguearse. Las notificaciones se acumulan indefinidamente.

#### Comportamiento actual del código

[`SendNotificationWorker.kt:159-169`](apps/android-client/app/src/main/java/com/yapenotifier/android/worker/SendNotificationWorker.kt#L159-L169) al recibir 401:

```kotlin
if (sendResult.isAuthError) {
    Timber.e("Error de autenticación (${sendResult.httpCode})... Token puede haber expirado. Deteniendo batch.")
    ServiceStatusManager.updateStatus("⚠️ Token expirado - Inicia sesión nuevamente")
    FileLogger.log("SEND", "AUTH_ERROR: http=${sendResult.httpCode}, notifId=${notification.id} - batch stopped, login required", "error")
    authFailed = true
    break
}
```

Lo que hace:
- ✅ Logea el error.
- ✅ Actualiza el banner en memoria de `ServiceStatusManager` (volátil — se pierde al matar el proceso).
- ✅ Termina el batch (no reintenta).

Lo que **no** hace:
- ❌ Llamar `preferencesManager.clearAuthToken()` (la función existe en [`PreferencesManager.kt:170`](apps/android-client/app/src/main/java/com/yapenotifier/android/data/local/PreferencesManager.kt#L170)).
- ❌ Llamar `authInterceptor.clearToken()` para invalidar el cache en memoria del [`AuthInterceptor`](apps/android-client/app/src/main/java/com/yapenotifier/android/data/api/AuthInterceptor.kt) — la app sigue enviando el mismo Bearer expirado.
- ❌ Mostrar una system notification persistente con CTA a re-login.
- ❌ Suspender futuros SEND (circuit breaker). Cada nuevo Yape capturado dispara otro SEND → otro 401 → desgaste de batería / datos / logs.
- ❌ Drenar la cola pendiente al re-loguearse (porque no hay un punto donde reaccionar al re-login).

#### Causa raíz del expiro

El token de Sanctum normalmente **no expira** salvo que se configure. Posibles fuentes:

1. **`config/sanctum.php` con `expiration` no null**. Revisar el valor en `apps/api/config/sanctum.php` y comparar con minutos transcurridos entre login y primer 401.
2. **Un deploy con `php artisan migrate:fresh`** o un drop de `personal_access_tokens` que revoca todos los tokens viejos. Revisar `git log` y deploy log alrededor del 2026-05-16.
3. **Logout intencional desde otro dispositivo** (admin desvincula al captador, o el captador hace logout en otra parte y el backend revoca el token). Revisar el endpoint `/api/devices/{id}/unlink` o cualquier mutación que dispare `tokens()->delete()`.
4. **Token rotation** por parte de `sanctum.expire_on_revoke`.

**Decisión de scope:** el fix de la app debe ser robusto independiente de la causa raíz. Investigar la causa es Task 1 (no bloqueante para el fix).

### A.2 Solución técnica

#### Principios

1. **Detectar 401 a nivel HTTP (no solo en el worker)**: agregar un `AuthExpiredInterceptor` o ampliar el `AuthInterceptor` para reaccionar al `Response.code == 401` y disparar el flujo global de "logout forzado". Así cubre cualquier endpoint, no solo el envío de notificaciones.
2. **Estado global de auth**: una `MutableStateFlow<Boolean>` o un evento broadcast/SharedFlow que cualquier capa pueda observar (`ViewModel`, `Worker`, `Service`).
3. **Limpiar token local atómicamente**: borrar de SharedPreferences + limpiar cache del interceptor + cancelar el WorkManager job de SEND.
4. **Notificar al usuario**: system notification persistente con tap → `MainActivity` (que ya rutea a login si `authToken == null`).
5. **Suspender SEND**: el worker chequea un flag `awaitingLogin` antes de cada batch. Si está en true, retorna `Result.success()` sin tocar la red.
6. **Drenar al re-login**: al login exitoso, encolar un SEND job para procesar los pendientes acumulados.

#### Diseño de componentes

```
┌───────────────────────────────────────────────────────────────────┐
│  HTTP layer                                                        │
│   ┌─────────────────────┐                                          │
│   │  AuthInterceptor    │  ── intercepta 401 ──▶  AuthEventBus     │
│   └─────────────────────┘                          │               │
│                                                    ▼               │
│   ┌─────────────────────────────────────────────────────┐          │
│   │  AuthEventBus (singleton SharedFlow<AuthEvent>)     │          │
│   │     emit(AuthEvent.TokenExpired)                    │          │
│   └─────────────────────────────────────────────────────┘          │
│                                ▲                ▲                  │
│                                │                │                  │
│         observa │       observa │                                  │
│   ┌────────────────┐    ┌──────────────────┐                       │
│   │ AuthExpiryHandler│  │ MainViewModel    │                       │
│   │ (Application-    │  │ (UI live)        │                       │
│   │  level)          │  └──────────────────┘                       │
│   │  · clearToken    │                                             │
│   │  · clearAuthCache│                                             │
│   │  · cancelSendJobs│                                             │
│   │  · showNotif     │                                             │
│   │  · set awaitFlag │                                             │
│   └──────────────────┘                                             │
└───────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────┐
│  SendNotificationWorker.doWork()                                   │
│    if (PreferencesManager.awaitingLogin) return Result.success()   │
│    ... envía batch ...                                             │
│    if (401) → AuthEventBus.emit(TokenExpired); break               │
└───────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────┐
│  LoginViewModel / PinLoginViewModel onLoginSuccess()               │
│    PreferencesManager.setAwaitingLogin(false)                      │
│    PreferencesManager.saveAuthToken(newToken)                      │
│    AuthInterceptor.updateToken(newToken)                           │
│    WorkManager.enqueueUniqueWork("send_pending", ...) ◀── drena    │
└───────────────────────────────────────────────────────────────────┘
```

#### Estados del flag `awaitingLogin`

| Evento | Flag pasa a | Quién lo escribe |
|---|---|---|
| App fresh install / first login | `false` | LoginViewModel |
| 401 detectado | `true` | AuthExpiryHandler |
| Login exitoso | `false` | LoginViewModel / PinLoginViewModel |
| Logout manual (botón "Desvincular dispositivo") | `true` o reset general | MainViewModel |

#### Edge cases

- **Race condition de varios 401 paralelos**: el `AuthEventBus` debe ser idempotente. Si ya hay un evento `TokenExpired` en vuelo (flag `awaitingLogin=true`), nuevos 401 no disparan acciones duplicadas.
- **Re-login con token diferente al expirado**: drenar cola al login exitoso, no antes. El SEND worker debe leer el token fresh de prefs.
- **El usuario nunca abre la app**: la system notification queda persistente (`setOngoing(true)` + `setAutoCancel(false)` + sin TTL). El usuario eventualmente la verá.
- **Push notification interceptada por el OS**: probar con `NotificationCompat.PRIORITY_HIGH` + canal de alta importancia (reusar `ALERT_CHANNEL_ID` del `YapeNotifierApplication`).

---

## Problema B — NotificationListenerService muerto en MIUI/Xiaomi

### B.1 Diagnóstico

#### Síntomas visibles
- App dice "⚠️ Conectando... si no conecta, reactiva el permiso".
- Activity log muestra "Intentando conectar servicio..." cada ~15 min.
- Card "Permisos del Sistema" vacía o sin items renderizados (Phone 1 — bug aparte).
- Botón "ABRIR AJUSTES DE PERMISO" con punto rojo de alerta.
- **El switch en Settings sigue en ON**. Pero el binding está muerto a nivel de sistema.
- Único recovery confiable: toggle off → toggle on en Settings → `onListenerConnected()` se vuelve a llamar.

#### Por qué falla `requestRebind()`

`requestRebind()` es una llamada al sistema que pide reconectar al `NotificationListenerService`. En AOSP funciona. **En MIUI, OnePlus OxygenOS, Huawei EMUI y otros OEMs con "battery savers" agresivos**, el sistema puede:

1. Matar el proceso de la app sin notificar.
2. Mantener el "permiso" como concedido en Settings pero el binding NO se reestablece.
3. Ignorar silenciosamente las llamadas a `requestRebind()`.
4. Solo respetar un toggle manual del permiso porque eso fuerza un re-bind a nivel de framework.

El [`ServiceWatchdogWorker`](apps/android-client/app/src/main/java/com/yapenotifier/android/worker/ServiceWatchdogWorker.kt) ya intenta lo correcto:
- ✅ Verifica permiso → si está revocado, muestra notificación.
- ✅ Verifica `isServiceConnected()` (recency-based, 20min — buena lógica).
- ✅ Llama `ServiceRebinder.rebindNotificationListener()` que internamente hace `requestRebind()`.
- ✅ Después de 3 intentos fallidos + `hasRealDisconnect=true`, muestra una "actionable notification" que abre `ACTION_NOTIFICATION_LISTENER_SETTINGS`.

Lo que falta:
- ❌ La pantalla de Settings que abre es genérica — no instruye al usuario sobre **qué hacer** ahí. El usuario ve un listado de apps con switches y no sabe que tiene que togglear off → on.
- ❌ No detecta el OEM. En MIUI sería razonable mostrar pasos específicos.
- ❌ En la UI principal de la app, el banner "Conectando..." es ambiguo. No diferencia entre "está intentando recuperarse" (transitorio) vs. "los rebinds programáticos fallaron, requiere acción manual del usuario" (terminal sin intervención).
- ❌ La sección "Permisos del Sistema" en `MainActivity` parece no renderizar items en algunos casos (Phone 1) — bug aparte que vale la pena revisar.

### B.2 Solución técnica

#### Estrategia

No podemos vencer al framework de Android — si MIUI mata el binding, **no hay** llamada API que lo resucite sin intervención del usuario. La solución es **mejorar el camino crítico** para que tome 5 segundos en lugar de "engorroso":

1. **Detectar el caso terminal**: si tras N intentos de rebind (`rebindAttempts >= 3` + sin actividad reciente + `hasRealDisconnect=true`), declaramos "modo recovery manual".
2. **Mostrar instrucciones claras** en la UI principal y en la system notification: 
   - "Toca **abrir ajustes**, busca **NotiCentral** en la lista, **apaga** el switch y vuélvelo a **prender**."
   - Incluir una mini-imagen GIF/PNG con la animación si es posible.
3. **Detección de OEM**: `Build.MANUFACTURER` para Xiaomi/Redmi/Poco → mensaje extra "MIUI hace esto con frecuencia. Activa **Autoinicio** y **Sin restricción de batería** para NotiCentral en Ajustes para evitarlo a futuro."
4. **Shortcut explícito al permiso correcto**: ya se usa `ACTION_NOTIFICATION_LISTENER_SETTINGS` (correcto). En OEMs específicos también se puede llevar al usuario a la pantalla de "Autoinicio" (`MIUI` tiene `MiuiAutoStartManager`).
5. **Bug aparte — Permisos del Sistema vacíos**: revisar `MainActivity` por qué no renderiza los items en la card. Sospecho una condición de carrera al cargar `NotificationAccessChecker` antes de que `appContext` esté listo, o el state-flow que alimenta esa sección no emite.

#### Lo que NO se hace en este plan

- **No** se programa un toggle automático del permiso vía `AccessibilityService` — requiere otro permiso poderoso, levanta sospechas de seguridad y los OEMs lo bloquean también.
- **No** se intenta `START_STICKY` redux o foreground service "indestructible" — eso está fuera del control de la app en MIUI.
- **No** se hace fix de `START_FOREGROUND_SERVICE_REASON` para vencer Doze mode — el foreground service ya existe; el problema es el binding del listener específicamente.

---

## Plan de ejecución

### Fase 0 — Diagnóstico backend (no bloqueante)

#### Task 0.1 — Identificar la causa del expiro del token (solo informe)

**Files:** ninguno modificado. Solo lectura.

- [ ] **Paso 1: Revisar `apps/api/config/sanctum.php`**
   ```bash
   grep -n "expiration" apps/api/config/sanctum.php
   ```
   Reportar: ¿`expiration` es `null` (no expira), o un número (minutos)? Si es número, ese es probablemente el culpable.

- [ ] **Paso 2: Verificar deploys del 2026-05-16**
   ```bash
   git log --oneline --since="2026-05-15" --until="2026-05-17" -- apps/api/
   ```
   Reportar: ¿hubo cambio en `personal_access_tokens` schema, en `RouteServiceProvider`, o un `migrate:fresh`?

- [ ] **Paso 3: Buscar revokes en código**
   ```bash
   grep -rn "tokens()->delete()\|tokens()->where" apps/api/app/
   ```
   Reportar: ¿qué endpoints revocan tokens? ¿Alguno se llama desde el panel del super admin al desvincular dispositivos?

- [ ] **Paso 4: Conclusión**
   Tres outputs posibles:
   - (a) `expiration` configurado a N minutos → considerar bajar/subir o eliminar el expiration. El fix de la app sigue siendo necesario.
   - (b) Algún endpoint revoca tokens cuando no debería → arreglar el endpoint.
   - (c) Nada obvio → mantener fix de la app y agregar log más detallado en backend para próximas ocurrencias.

**Output esperado:** un comment en este plan documentando la causa, o un issue/PR si requiere fix backend.

---

### Fase 1 — Fix Problema A (token expirado)

#### Task 1.1 — `PreferencesManager` agrega flag `awaitingLogin`

**Files:**
- Modify: [`apps/android-client/app/src/main/java/com/yapenotifier/android/data/local/PreferencesManager.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/data/local/PreferencesManager.kt)

- [ ] **Paso 1: Agregar la key + lector + setter**

```kotlin
private val AWAITING_LOGIN_KEY = booleanPreferencesKey("awaiting_login")

val awaitingLogin: Flow<Boolean> = context.dataStore.data.map { it[AWAITING_LOGIN_KEY] ?: false }

suspend fun setAwaitingLogin(value: Boolean) {
    context.dataStore.edit { it[AWAITING_LOGIN_KEY] = value }
}

// Helper sync (para llamar desde el Worker sin coroutine si hace falta)
fun awaitingLoginBlocking(): Boolean = runBlocking { awaitingLogin.first() }
```

- [ ] **Paso 2: Verificar que `clearAll()` resetee el flag**
- [ ] **Paso 3: Verificar build** — `cd apps/android-client && ./gradlew :app:compileDebugKotlin`
- [ ] **Paso 4: Commit**
   ```
   git commit -m "feat(android): add awaitingLogin flag to PreferencesManager"
   ```

#### Task 1.2 — `AuthEventBus` para emitir eventos de auth a nivel proceso

**Files:**
- Create: `apps/android-client/app/src/main/java/com/yapenotifier/android/data/auth/AuthEventBus.kt`

- [ ] **Paso 1: Implementar**

```kotlin
package com.yapenotifier.android.data.auth

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class AuthEvent {
    object TokenExpired : AuthEvent()
    object ManualLogout : AuthEvent()
    object LoginSucceeded : AuthEvent()
}

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
```

- [ ] **Paso 2: Build + commit**

#### Task 1.3 — `AuthExpiryHandler` central que reacciona a `TokenExpired`

**Files:**
- Create: `apps/android-client/app/src/main/java/com/yapenotifier/android/data/auth/AuthExpiryHandler.kt`
- Modify: [`YapeNotifierApplication.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/YapeNotifierApplication.kt) (registrar el handler en `onCreate`)

- [ ] **Paso 1: Implementar el handler**

```kotlin
package com.yapenotifier.android.data.auth

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.WorkManager
import com.yapenotifier.android.R
import com.yapenotifier.android.YapeNotifierApplication
import com.yapenotifier.android.data.api.AuthInterceptor
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.ui.SplashActivity
import com.yapenotifier.android.util.FileLogger
import com.yapenotifier.android.util.ServiceStatusManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AuthExpiryHandler(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val authInterceptor: AuthInterceptor,
    private val scope: CoroutineScope,
) {
    fun start() {
        AuthEventBus.events
            .onEach { event ->
                when (event) {
                    is AuthEvent.TokenExpired -> handleTokenExpired()
                    is AuthEvent.ManualLogout -> handleManualLogout()
                    is AuthEvent.LoginSucceeded -> handleLoginSucceeded()
                }
            }
            .launchIn(scope)
    }

    private fun handleTokenExpired() {
        scope.launch(Dispatchers.IO) {
            // Idempotente: si ya estamos esperando login, no hacer nada
            if (preferencesManager.awaitingLoginBlocking()) return@launch

            FileLogger.log("AUTH", "Token expired - clearing local state and prompting user", "error")
            ServiceStatusManager.updateStatus("⚠️ Token expirado - Inicia sesión nuevamente")

            preferencesManager.setAwaitingLogin(true)
            preferencesManager.clearAuthToken()
            authInterceptor.clearToken()

            // Cancela worker de SEND para que no insista contra un token muerto
            WorkManager.getInstance(context).cancelUniqueWork(
                com.yapenotifier.android.worker.SendNotificationWorker.WORK_NAME
            )

            showLoginRequiredNotification()
        }
    }

    private fun handleManualLogout() {
        // El logout manual ya limpia todo via MainViewModel; aquí solo aseguramos consistencia
        scope.launch(Dispatchers.IO) {
            preferencesManager.setAwaitingLogin(true)
        }
    }

    private fun handleLoginSucceeded() {
        scope.launch(Dispatchers.IO) {
            preferencesManager.setAwaitingLogin(false)
            FileLogger.log("AUTH", "Login succeeded - clearing awaitingLogin flag", "info")

            // Cancela la notif persistente de "sesión expirada"
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(LOGIN_REQUIRED_NOTIFICATION_ID)

            // Encola el drain de pendientes (Worker idempotente — si no hay nada, no hace nada)
            com.yapenotifier.android.worker.SendNotificationWorker.enqueueOneTime(context)
        }
    }

    private fun showLoginRequiredNotification() {
        val intent = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, LOGIN_REQUIRED_NOTIFICATION_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, YapeNotifierApplication.ALERT_CHANNEL_ID)
            .setContentTitle("NotiCentral · Sesión expirada")
            .setContentText("Toca para volver a iniciar sesión y reanudar la captura.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Tu sesión expiró. Las nuevas notificaciones de Yape no se están subiendo " +
                "hasta que vuelvas a iniciar sesión."
            ))
            .setSmallIcon(R.drawable.ic_bell_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)            // persistente
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(LOGIN_REQUIRED_NOTIFICATION_ID, notif)
    }

    companion object {
        private const val LOGIN_REQUIRED_NOTIFICATION_ID = 2003
    }
}
```

- [ ] **Paso 2: Wirear en `YapeNotifierApplication.onCreate()`**

Después de inicializar `ServiceStatusManager` y `PreferencesManager`:

```kotlin
val handler = AuthExpiryHandler(
    context = this,
    preferencesManager = preferencesManager,
    authInterceptor = authInterceptor,
    scope = applicationScope,  // un CoroutineScope a nivel app — crear si no existe
)
handler.start()
```

Si no existe `applicationScope`, crear:
```kotlin
val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
```

- [ ] **Paso 3: Build + commit**

#### Task 1.4 — `AuthInterceptor` detecta 401 y emite evento

**Files:**
- Modify: [`apps/android-client/app/src/main/java/com/yapenotifier/android/data/api/AuthInterceptor.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/data/api/AuthInterceptor.kt)

- [ ] **Paso 1: En `intercept()`, después de `chain.proceed(newRequest)`**

```kotlin
val response = chain.proceed(newRequest)

// Solo dispara para requests autenticados (token presente y se envió Bearer)
if (response.code == 401 && token != null) {
    AuthEventBus.emit(AuthEvent.TokenExpired)
    // No mutamos la response. El caller decide qué hacer con el 401.
}

return response
```

- [ ] **Paso 2: Importar `AuthEventBus`, `AuthEvent`**.
- [ ] **Paso 3: Build + commit**

#### Task 1.5 — `SendNotificationWorker` respeta el flag `awaitingLogin` + helper de encolado

**Files:**
- Modify: [`apps/android-client/app/src/main/java/com/yapenotifier/android/worker/SendNotificationWorker.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/worker/SendNotificationWorker.kt)

- [ ] **Paso 1: Al inicio de `doWork()`, antes de cualquier I/O**

```kotlin
if (preferencesManager.awaitingLoginBlocking()) {
    Timber.i("SendNotificationWorker: awaitingLogin=true — skipping batch")
    FileLogger.log("SEND", "Skipped batch: awaitingLogin=true", "info")
    return Result.success()
}
```

- [ ] **Paso 2: En el handler de `isAuthError`, ya no `break`-ear sin emitir**

```kotlin
if (sendResult.isAuthError) {
    AuthEventBus.emit(AuthEvent.TokenExpired)
    // El handler se encarga del resto. Aquí solo paramos el batch.
    authFailed = true
    break
}
```

(El log + status update + clearToken ahora viven en `AuthExpiryHandler`.)

- [ ] **Paso 3: Helper estático `enqueueOneTime(context)`** para que `AuthExpiryHandler` lo invoque al login exitoso

```kotlin
companion object {
    const val WORK_NAME = "send_notification_worker"

    fun enqueueOneTime(context: Context) {
        val req = OneTimeWorkRequestBuilder<SendNotificationWorker>()
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            req,
        )
    }
}
```

- [ ] **Paso 4: Build + commit**

#### Task 1.6 — Login viewmodels notifican éxito al `AuthEventBus`

**Files:**
- Modify: [`apps/android-client/app/src/main/java/com/yapenotifier/android/ui/viewmodel/LoginViewModel.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/ui/viewmodel/LoginViewModel.kt)
- Modify: [`apps/android-client/app/src/main/java/com/yapenotifier/android/ui/viewmodel/PinLoginViewModel.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/ui/viewmodel/PinLoginViewModel.kt)
- Modify: [`apps/android-client/app/src/main/java/com/yapenotifier/android/ui/admin/viewmodel/AdminLoginViewModel.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/ui/admin/viewmodel/AdminLoginViewModel.kt)

- [ ] **Paso 1: En cada VM, tras `saveAuthToken(token)` exitoso, emitir**

```kotlin
AuthEventBus.emit(AuthEvent.LoginSucceeded)
```

- [ ] **Paso 2: Build + commit**

#### Task 1.7 — Indicador visible en UI cuando `awaitingLogin=true`

**Files:**
- Modify: [`apps/android-client/app/src/main/java/com/yapenotifier/android/ui/MainActivity.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/ui/MainActivity.kt) y/o el viewmodel asociado.

- [ ] **Paso 1: Observar `preferencesManager.awaitingLogin` en el ViewModel**
- [ ] **Paso 2: Si `true`, reemplazar el banner "Capturando OK" por uno rojo: "⚠️ Sesión expirada — toca para iniciar sesión"** con click → `SplashActivity` (que rutea a login). El banner muestra también `pending=N`.
- [ ] **Paso 3: Build + commit**

#### Task 1.8 — Smoke test manual del flujo A

- [ ] **Paso 1: Build APK debug** `cd apps/android-client && ./gradlew :app:assembleDebug`
- [ ] **Paso 2: Instalar en dispositivo de prueba.**
- [ ] **Paso 3: Login y dejar capturar al menos 1 Yape (verificar que sube).**
- [ ] **Paso 4: Simular expiración**: desde el backend, revocar el token (`personal_access_tokens` → DELETE WHERE id = X) o configurar `sanctum.expiration` temporalmente a 1 minuto.
- [ ] **Paso 5: Generar otro Yape o esperar al próximo SEND.**
- [ ] **Paso 6: Verificar:**
   - System notification persistente "Sesión expirada" aparece.
   - Tap → abre Splash → login.
   - Tras re-login, las notifs pendientes se envían (chequear `pending=0` en UI y en backend).
- [ ] **Paso 7: Commit del smoke checklist actualizado** (documentar resultado en `docs/superpowers/smoke-tests/`).

---

### Fase 2 — Fix Problema B (recovery del listener en MIUI)

#### Task 2.1 — Helper `OemDetection` para detectar MIUI/Xiaomi/etc.

**Files:**
- Create: `apps/android-client/app/src/main/java/com/yapenotifier/android/util/OemDetection.kt`

- [ ] **Paso 1: Implementar**

```kotlin
package com.yapenotifier.android.util

import android.os.Build

object OemDetection {
    enum class Oem { XIAOMI, HUAWEI, OPPO, VIVO, ONEPLUS, SAMSUNG, OTHER }

    fun current(): Oem = when (Build.MANUFACTURER.lowercase()) {
        "xiaomi", "redmi", "poco" -> Oem.XIAOMI
        "huawei", "honor" -> Oem.HUAWEI
        "oppo" -> Oem.OPPO
        "vivo" -> Oem.VIVO
        "oneplus" -> Oem.ONEPLUS
        "samsung" -> Oem.SAMSUNG
        else -> Oem.OTHER
    }

    fun hasAggressiveBatterySaver(): Boolean = when (current()) {
        Oem.XIAOMI, Oem.HUAWEI, Oem.OPPO, Oem.VIVO -> true
        else -> false
    }

    fun recoveryInstructionsShort(): String = when (current()) {
        Oem.XIAOMI -> "En MIUI: abre Ajustes → busca NotiCentral → apaga el switch y vuélvelo a prender."
        Oem.HUAWEI -> "En EMUI: abre Ajustes → busca NotiCentral → apaga y vuelve a activar el permiso."
        else -> "Abre Ajustes → busca NotiCentral → apaga y vuelve a prender el permiso de notificaciones."
    }
}
```

- [ ] **Paso 2: Build + commit**

#### Task 2.2 — Mejorar la "actionable notification" del watchdog con instrucciones OEM-específicas

**Files:**
- Modify: [`apps/android-client/app/src/main/java/com/yapenotifier/android/worker/ServiceWatchdogWorker.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/worker/ServiceWatchdogWorker.kt) — método `showServiceDisconnectedNotification()`

- [ ] **Paso 1: Reemplazar el `bigText` por algo OEM-aware**

```kotlin
val oem = OemDetection.current()
val bigText = buildString {
    append("El servicio no pudo reconectarse automáticamente.\n\n")
    append("Pasos para reactivar:\n")
    append("1. Toca esta notificación.\n")
    append("2. Busca \"NotiCentral\" en la lista.\n")
    append("3. **Apaga** el switch.\n")
    append("4. Espera 2 segundos.\n")
    append("5. **Vuélvelo a prender**.\n")
    if (OemDetection.hasAggressiveBatterySaver()) {
        append("\n💡 ${OemDetection.recoveryInstructionsShort()}")
    }
}
```

- [ ] **Paso 2: Build + commit**

#### Task 2.3 — Card en `MainActivity` con instrucciones visuales

**Files:**
- Modify: [`apps/android-client/app/src/main/java/com/yapenotifier/android/ui/MainActivity.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/ui/MainActivity.kt)

- [ ] **Paso 1: Cuando `isServiceConnected() == false` AND `rebindAttempts >= 3`** (el caso terminal donde rebinds automáticos ya fallaron), mostrar una **card destacada** (color amber/rojo) con:
   - Título: "Servicio detenido — necesita atención"
   - Subtítulo con instrucciones específicas del OEM (vía `OemDetection`).
   - 3 pasos numerados grandes y claros.
   - Botón gigante: "ABRIR AJUSTES DEL PERMISO".
   - Opcional: pequeño link "¿Por qué pasa esto?" → expande explicación breve sobre OEMs agresivos.

- [ ] **Paso 2:** Si `rebindAttempts < 3`, mostrar la card actual ("Conectando..." amber) — es transitorio.

- [ ] **Paso 3: Build + commit**

#### Task 2.4 — Bug aparte: "Permisos del Sistema" vacíos en Phone 1

**Files:** investigar [`MainActivity.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/ui/MainActivity.kt) y el state-flow que alimenta la card.

- [ ] **Paso 1: Reproducir** (si es posible — puede requerir un dispositivo MIUI específico).
- [ ] **Paso 2:** Logear los valores leídos por `NotificationAccessChecker.isNotificationAccessEnabled()` y la batería al inicio de `MainActivity.onResume()`. Identificar si la card está condicionalmente oculta o si su contenido nunca se asigna.
- [ ] **Paso 3:** Si es un race condition (initialState=empty y el observe no dispara), usar `StateFlow` con valor inicial sincrónico en lugar de Flow + initial=null.
- [ ] **Paso 4: Build + commit con fix.**

#### Task 2.5 — Smoke test manual del flujo B

- [ ] **Paso 1: Build APK + instalar en dispositivo Xiaomi/Redmi.**
- [ ] **Paso 2: Forzar el escenario:** matar la app desde "Recientes" + entrar en Battery saver.
- [ ] **Paso 3:** Esperar 30 minutos (o forzar `WorkManager.enqueueUniqueWork(ServiceWatchdogWorker)`).
- [ ] **Paso 4: Verificar:**
   - Notificación con instrucciones específicas MIUI aparece tras 3 intentos.
   - Tap → abre `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`.
   - Toggle off + on → app se reconecta en < 5 segundos.
- [ ] **Paso 5: Documentar resultado en smoke test doc.**

---

### Fase 3 — Logging mejorado (preventivo)

#### Task 3.1 — Categoría `[AUTH]` en `FileLogger`

**Files:**
- Modify: [`apps/android-client/app/src/main/java/com/yapenotifier/android/util/FileLogger.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/util/FileLogger.kt)

- [ ] **Paso 1: Asegurar que `FileLogger.log("AUTH", ..., level)` quede en disco con el mismo formato.** (Probablemente ya funciona; sin cambios estructurales.)
- [ ] **Paso 2:** Agregar puntos de log:
   - Login exitoso → `[AUTH] LOGIN_OK user=... role=...`
   - Logout manual → `[AUTH] MANUAL_LOGOUT`
   - 401 detectado → `[AUTH] TOKEN_EXPIRED endpoint=...`
   - Clear de token desde handler → `[AUTH] CLEARED_LOCAL_TOKEN`
   - Login tras expirado → `[AUTH] RECOVERED_AFTER_EXPIRY pending_count=N`

- [ ] **Paso 3: Build + commit**

#### Task 3.2 — Endpoint en el HTTP log

**Files:** misma `AuthInterceptor.kt` o un `LoggingInterceptor` separado.

- [ ] **Paso 1:** Cuando se emite `TokenExpired`, registrar también el `request.url.encodedPath` para saber en qué endpoint pasó.

```kotlin
if (response.code == 401 && token != null) {
    FileLogger.log("AUTH", "401 on ${chain.request().url.encodedPath}", "error")
    AuthEventBus.emit(AuthEvent.TokenExpired)
}
```

- [ ] **Paso 2: Build + commit**

---

### Fase 4 — Cierre

#### Task 4.1 — Smoke test consolidado + documentación

**Files:**
- Create: `docs/superpowers/smoke-tests/2026-05-17-android-auth-y-recovery.md`

- [ ] **Paso 1:** Checklist en español con todos los casos cubiertos (token expirado, MIUI recovery, drain post-login, sin regresiones en flujo normal).
- [ ] **Paso 2:** Commit.

#### Task 4.2 — Push y release del APK

- [ ] **Paso 1:** `git push origin benja-version`.
- [ ] **Paso 2:** Build de release: `./gradlew :app:assembleRelease`.
- [ ] **Paso 3:** Distribuir al usuario (canal habitual: tu propio APK; no es Play Store).
- [ ] **Paso 4:** Pedir que los 2 captadores afectados actualicen y avisen si el problema persiste.

---

## Riesgos y decisiones

| Riesgo | Mitigación |
|---|---|
| `AuthEventBus` como singleton estático puede causar leaks de coroutine si el handler no se cancela en process death | Como vive a nivel `Application` y el proceso es el ciclo natural, está OK. `SupervisorJob` evita cascada de cancelaciones. |
| Múltiples 401 simultáneos (varios endpoints fallando) disparan múltiples `TokenExpired` | `AuthExpiryHandler.handleTokenExpired()` chequea `awaitingLoginBlocking()` al inicio. Idempotente. |
| El handler corre en `Dispatchers.IO` y puede tardar — ¿se pierde el primer evento? | `SharedFlow(extraBufferCapacity = 8)` con `DROP_OLDEST` — los eventos esperan en el buffer hasta que el collector lea. |
| El usuario ignora la system notification persistente | Es persistente (`setOngoing(true)`), siempre visible en la barra. Además el banner en la UI principal también lo grita. No más que esto sin push remoto. |
| MIUI mata WorkManager también | Sí, puede pasar. La notif ya fue mostrada antes del kill — sobrevive porque vive en el NotificationManager del sistema, no en el proceso de la app. |
| Cambiar `sanctum.expiration` en backend afecta tokens existentes | Si decidimos cambiarlo (Task 0.1 outcome), evaluar si invalida sesiones activas. Recomendación: dejar `expiration = null` (no expiran) y revocar manualmente cuando haga falta. |
| El "drain" tras login podría disparar muchos uploads simultáneos | El `SendNotificationWorker` ya procesa el batch en orden con un loop. No hay paralelismo accidental. |

---

## Fuera de scope

- **Refresh token flow** (Sanctum no lo soporta nativamente; sería rediseño grande).
- **Persistir el `ServiceStatusManager._statusHistory`** entre reinicios (hoy es `MutableStateFlow` en memoria — se pierde al matar el proceso).
- **AccessibilityService para auto-toggle** del permiso (riesgoso, polémico, bloqueado por OEMs).
- **Watchdog "indestructible" foreground service** (fuera de control de la app en MIUI).
- **Investigación profunda del bug de "Permisos del Sistema vacíos"** más allá del Task 2.4 — si no se reproduce fácilmente, abrimos un issue aparte.

---

## Tabla resumen de tareas

| # | Tarea | Fase | Files key | Modelo recomendado |
|---|---|---|---|---|
| 0.1 | Diagnóstico backend de expiro | 0 | `sanctum.php` (read-only) | haiku |
| 1.1 | `awaitingLogin` flag en `PreferencesManager` | 1 | `PreferencesManager.kt` | haiku |
| 1.2 | `AuthEventBus` | 1 | nuevo `AuthEventBus.kt` | haiku |
| 1.3 | `AuthExpiryHandler` + wiring | 1 | nuevo `AuthExpiryHandler.kt`, `YapeNotifierApplication.kt` | sonnet |
| 1.4 | `AuthInterceptor` emite 401 | 1 | `AuthInterceptor.kt` | haiku |
| 1.5 | Worker respeta `awaitingLogin` | 1 | `SendNotificationWorker.kt` | haiku |
| 1.6 | Login VMs emiten `LoginSucceeded` | 1 | 3 VMs | haiku |
| 1.7 | Banner UI "Sesión expirada" | 1 | `MainActivity.kt` + VM | sonnet |
| 1.8 | Smoke test fase 1 | 1 | doc | (manual) |
| 2.1 | `OemDetection` | 2 | nuevo `OemDetection.kt` | haiku |
| 2.2 | Mejorar `showServiceDisconnectedNotification` | 2 | `ServiceWatchdogWorker.kt` | haiku |
| 2.3 | Card de recovery en `MainActivity` | 2 | `MainActivity.kt` | sonnet |
| 2.4 | Fix "Permisos del Sistema vacíos" | 2 | `MainActivity.kt` (investigación) | sonnet |
| 2.5 | Smoke test fase 2 | 2 | doc | (manual) |
| 3.1 | Categoría `[AUTH]` en FileLogger | 3 | `FileLogger.kt` + callers | haiku |
| 3.2 | Endpoint en log de 401 | 3 | `AuthInterceptor.kt` | haiku |
| 4.1 | Smoke test consolidado | 4 | doc | (manual) |
| 4.2 | Push + release APK | 4 | n/a | (manual) |

**Total: 18 tareas en 5 fases.** Backend casi sin tocar (solo lectura en Task 0.1). Todo el resto es Android nativo.
