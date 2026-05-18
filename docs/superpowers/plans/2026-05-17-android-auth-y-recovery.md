# Android — Manejo de token expirado y recuperación del servicio en MIUI

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development. Tareas con checkbox `- [ ]` para tracking. Android sin tests automáticos en este repo — verificación con build + smoke manual. Backend solo investigación (no cambios obligatorios).

**Goal:** Eliminar dos bugs operacionales reportados por usuarios reales:
- (A) Token de Sanctum expira y la app queda enviando 401 indefinidamente con notificaciones varadas — sin avisar al usuario.
- (B) En MIUI/Xiaomi (y otros OEMs agresivos), Android mata el binding al `NotificationListenerService` y `requestRebind()` no logra reactivarlo. El usuario debe ir a Ajustes y togglear el permiso manualmente — proceso engorroso y sin guía visual.

**Architecture:** Un `AuthSessionManager` idempotente, autoritativo, con DataStore como fuente única de verdad para `awaitingLogin`. El bus de eventos queda como complemento de UI, no como pieza crítica. El interceptor real (lambda inline en `RetrofitClient.kt`) detecta 401 y dispara el manager por la versión async (sin `runBlocking`). Las 3 login VMs invocan `AuthSessionManager.handleLoginSucceeded()` en lugar de `saveAuthToken()` directo. El worker chequea `awaitingLogin` al inicio y termina rápido sin tocar la red. Para MIUI, UX guiada con estado terminal explícito (`MANUAL_ACTION_REQUIRED`).

**Tech stack:** Android Kotlin + manual DI estilo del repo + WorkManager + Retrofit/OkHttp + DataStore (`PreferencesManager`) + Room (`CapturedNotificationDao`). Backend: Laravel Sanctum (lado servidor — solo se diagnostica).

**Spec:** este documento incluye spec + plan; alcance limitado, no amerita doc separado.

**Evidencia que sustenta el plan:**
- Audit log [`service_log (2).txt`](service_log (2).txt) — 5 días, 0 AUTH_ERROR pero 171 RECONNECT con muchos falsos positivos.
- Audit log [`service_log-3.txt`](service_log-3.txt) — 8 días, **11 AUTH_ERROR 401** desde 2026-05-16 12:51, `pending=11` al final, sin recovery.
- Screenshots reportados por usuario el 2026-05-17:
  - Phone 1: "⚠️ Conectando... si no conecta, reactiva el permiso" — listener muerto en MIUI.
  - Phone 2: "✅ Capturando OK" arriba, "Token expirado" abajo, **11 pendientes**.

---

## Hechos verificados del codebase (antes de tocar nada)

Cada uno con file:line para que cualquier subagent encuentre el contexto sin re-investigar.

| Hecho | Verificado en |
|---|---|
| `AuthInterceptor.kt` class **es código muerto**: nunca se instancia. | [`AuthInterceptor.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/data/api/AuthInterceptor.kt) (existe pero no se importa) |
| El interceptor real es un lambda inline con `tokenCache: AtomicReference<String?>`. | [`RetrofitClient.kt:69-87`](apps/android-client/app/src/main/java/com/yapenotifier/android/data/api/RetrofitClient.kt#L69-L87) |
| Cache se mantiene sincronizado con DataStore vía `observeTokenChanges()`. | [`RetrofitClient.kt:135-146`](apps/android-client/app/src/main/java/com/yapenotifier/android/data/api/RetrofitClient.kt#L135-L146) |
| `RetrofitClient.clearTokenCache()` existe y vacía el `AtomicReference`. | [`RetrofitClient.kt:151-154`](apps/android-client/app/src/main/java/com/yapenotifier/android/data/api/RetrofitClient.kt#L151-L154) |
| `SendNotificationWorker` solo marca `status="SENT"` dentro de `SendResult.Success`. En 401 → `break`, no toca DB. | [`SendNotificationWorker.kt:154-169`](apps/android-client/app/src/main/java/com/yapenotifier/android/worker/SendNotificationWorker.kt#L154-L169) |
| El worker se encola **sin nombre único** desde 3 lugares (listener, login, captured-list), todos OneTime. | [`PaymentNotificationListenerService.kt:349`](apps/android-client/app/src/main/java/com/yapenotifier/android/service/PaymentNotificationListenerService.kt#L349), [`LoginViewModel.kt:200`](apps/android-client/app/src/main/java/com/yapenotifier/android/ui/viewmodel/LoginViewModel.kt#L200), [`CapturedNotificationsViewModel.kt:86`](apps/android-client/app/src/main/java/com/yapenotifier/android/ui/viewmodel/CapturedNotificationsViewModel.kt#L86) |
| Cola pendiente en Room (`captured_notifications`), columna `status`. | [`CapturedNotificationDao.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/data/local/db/CapturedNotificationDao.kt) |
| `rebindAttempts` y demás timestamps **ya persisten** en SharedPreferences vía `ServiceStatusManager`. | [`ServiceStatusManager.kt:13-25,186-195`](apps/android-client/app/src/main/java/com/yapenotifier/android/util/ServiceStatusManager.kt#L13-L25) |
| Las 3 login VMs (capturer, PIN, admin) usan exactamente `preferencesManager.saveAuthToken(token)`. | [`LoginViewModel.kt:59`](apps/android-client/app/src/main/java/com/yapenotifier/android/ui/viewmodel/LoginViewModel.kt#L59), [`PinLoginViewModel.kt:43`](apps/android-client/app/src/main/java/com/yapenotifier/android/ui/viewmodel/PinLoginViewModel.kt#L43), [`AdminLoginViewModel.kt:76`](apps/android-client/app/src/main/java/com/yapenotifier/android/ui/admin/viewmodel/AdminLoginViewModel.kt#L76) |
| `targetSdk = 34`. `POST_NOTIFICATIONS` ya declarado en manifest. | [`build.gradle.kts:36`](apps/android-client/app/build.gradle.kts#L36), [`AndroidManifest.xml:8`](apps/android-client/app/src/main/AndroidManifest.xml#L8) |
| Nombre visible al usuario en notificaciones: **"NotiCentral"** (confirmado en [`ServiceWatchdogWorker.kt:276,318`](apps/android-client/app/src/main/java/com/yapenotifier/android/worker/ServiceWatchdogWorker.kt#L276)). | Mantener consistencia: NotiCentral siempre. |

---

## Reglas de diseño no negociables (las 6 condiciones)

1. **No `runBlocking` dentro del interceptor OkHttp.** Trabajo I/O = `applicationScope.launch(Dispatchers.IO)`.
2. **`AuthSessionManager` expone dos formas:**
   - `fun handleTokenExpiredAsync(context, endpoint)` — fire-and-forget desde el interceptor.
   - `suspend fun handleTokenExpired(context, endpoint)` — para callers ya en coroutine.
   - `suspend fun handleLoginSucceeded(context, token)` — siempre suspendida; las VMs ya están en `viewModelScope`.
3. **Fuente de verdad de `awaitingLogin` = DataStore** (`PreferencesManager.awaitingLogin: Flow<Boolean>`). El mirror en memoria es opcional, **inicializado** explícitamente al arrancar.
4. **`AuthSessionManager.initialize(context)`** llamado desde `YapeNotifierApplication.onCreate()` antes que cualquier otra cosa que dependa del estado.
5. **"NotiCentral"** es el nombre visible en TODA la UX (notif, banner, card, instrucciones OEM). El identifier técnico `Yape Notifier` solo se queda en `AndroidManifest.xml`, copyright y wordmark del landing.
6. **Worker nunca marca SENT en 401.** Ya está así en el código — solo verificar que el rewrite no lo rompa.

---

## Problema A — Token expirado sin recuperación

### A.1 Diagnóstico (sintetizado)

**Síntomas:**
- UI muestra "Capturando OK" en verde mientras el log dice "Token expirado".
- Pendientes sube (0 → 11) sin alerta visible al usuario.
- No hay flujo guiado para re-login. 11 Yapes varados al final del log.

**Comportamiento actual ([`SendNotificationWorker.kt:159-169`](apps/android-client/app/src/main/java/com/yapenotifier/android/worker/SendNotificationWorker.kt#L159-L169)):**

```kotlin
if (sendResult.isAuthError) {
    ServiceStatusManager.updateStatus("⚠️ Token expirado - Inicia sesión nuevamente")
    FileLogger.log("SEND", "AUTH_ERROR: http=${sendResult.httpCode}, notifId=${notification.id} - batch stopped, login required", "error")
    authFailed = true
    break
}
```

Lo que falta:
- Limpiar el token del DataStore (cascadea al tokenCache automáticamente).
- Persistir `awaitingLogin=true` (no hay flag).
- Mostrar system notification persistente con CTA → `SplashActivity` (que ya rutea a login si `authToken==null`).
- Bloquear nuevos batches mientras `awaitingLogin=true` (circuit breaker).
- Drenar pendientes al re-login.

**Causa raíz del expiro:** queda en Task 0.1 (lectura). El fix de la app debe ser robusto independiente de la causa.

### A.2 Arquitectura aprobada

```
┌──────────────────────────────────────────────────────────────────────┐
│  PreferencesManager (DataStore — SOURCE OF TRUTH)                    │
│    val authToken: Flow<String?>                                       │
│    val awaitingLogin: Flow<Boolean>     ◀── verdad autoritativa       │
│    suspend fun saveAuthToken(t)                                       │
│    suspend fun clearAuthToken()                                       │
│    suspend fun setAwaitingLogin(b)                                    │
│    fun awaitingLoginBlocking(): Boolean (helper sólo para Worker)     │
└──────────────────────────────────────────────────────────────────────┘
            ▲                                ▲
            │ read/write                     │ read/write
            │                                │
┌───────────────────────┐         ┌────────────────────────────────────┐
│  RetrofitClient       │         │  AuthSessionManager                 │
│  (interceptor lambda) │         │  (idempotente, autoritativo)        │
│   if (401 && tok!=nul)│ async── │   handleTokenExpiredAsync(ctx, ep)  │
│     authSession       │ ──────▶ │   suspend handleTokenExpired(...)   │
│       .handleTokenExp │         │   suspend handleLoginSucceeded(...) │
│       Async(...)      │         │   initialize(ctx)                   │
└───────────────────────┘         └────────────────────────────────────┘
                                              ▲
                  ┌───────────────────────────┼───────────────────────┐
                  │                           │                       │
       SendNotificationWorker       3 login VMs                AuthEventBus
       (chequea awaitingLogin       (llaman handleLogin       (solo UI live —
        al inicio; salta batch)      Succeeded en lugar       NO crítico)
                                     de saveAuthToken)
```

### A.3 Diseño concreto del `AuthSessionManager`

```kotlin
object AuthSessionManager {

    // Mirror en memoria SOLO para optimizar lecturas síncronas desde Worker.
    // La fuente de verdad sigue siendo PreferencesManager.awaitingLogin (Flow).
    @Volatile private var mirrorAwaitingLogin: Boolean = false
    @Volatile private var initialized: Boolean = false

    private val mutex = Mutex()
    private lateinit var appScope: CoroutineScope

    /** Llamar desde YapeNotifierApplication.onCreate() ANTES que cualquier otro init. */
    suspend fun initialize(context: Context, scope: CoroutineScope) {
        appScope = scope
        // Hidratar mirror desde DataStore
        val prefs = PreferencesManager(context)
        mirrorAwaitingLogin = prefs.awaitingLogin.first()
        initialized = true
        FileLogger.log("AUTH", "AuthSessionManager initialized: awaitingLogin=$mirrorAwaitingLogin", "info")

        // Observar cambios futuros del DataStore para mantener el mirror sincronizado
        prefs.awaitingLogin
            .onEach { mirrorAwaitingLogin = it }
            .launchIn(appScope)
    }

    /** Fire-and-forget para callers no-coroutine (interceptor OkHttp). */
    fun handleTokenExpiredAsync(context: Context, endpoint: String) {
        if (!initialized) return  // antes del init, no hay nada que hacer
        appScope.launch(Dispatchers.IO) {
            handleTokenExpired(context, endpoint)
        }
    }

    /** Variante suspend para callers ya en coroutine (Worker si lo decide, futuros). */
    suspend fun handleTokenExpired(context: Context, endpoint: String) {
        mutex.withLock {
            // Idempotente: si ya estamos esperando login, no repetir efectos secundarios
            val prefs = PreferencesManager(context)
            if (prefs.awaitingLogin.first()) {
                FileLogger.log("AUTH", "TOKEN_EXPIRED on $endpoint - already awaiting login, ignored", "info")
                return
            }

            FileLogger.log("AUTH", "TOKEN_EXPIRED on $endpoint - clearing local state", "error")
            prefs.setAwaitingLogin(true)
            prefs.clearAuthToken()          // cascadea: observeTokenChanges → tokenCache=null
            RetrofitClient.clearTokenCache() // defensa en profundidad (cache se invalida ya, pero por seguridad)

            ServiceStatusManager.updateStatus("⚠️ Sesión expirada - Inicia sesión nuevamente")
            showLoginRequiredNotification(context)
            AuthEventBus.emit(AuthEvent.TokenExpired) // solo para UI live, NO es la pieza crítica
        }
    }

    /** Llamado por las 3 login VMs en lugar de PreferencesManager.saveAuthToken(). */
    suspend fun handleLoginSucceeded(context: Context, token: String) {
        mutex.withLock {
            val prefs = PreferencesManager(context)
            prefs.saveAuthToken(token)
            prefs.setAwaitingLogin(false)
            // tokenCache se rehidrata solo vía observeTokenChanges

            cancelLoginRequiredNotification(context)
            FileLogger.log("AUTH", "LOGIN_OK - drenando pendientes", "info")
            AuthEventBus.emit(AuthEvent.LoginSucceeded)

            // El SendNotificationWorker chequea awaitingLogin al inicio. Ya está en false.
            // Encolamos uno explícito para drenar lo que quedó pendiente.
            scheduleSendWorker(context)
        }
    }

    /** Helper para Worker: lectura síncrona desde mirror. Si no inicializado, fallback a false. */
    fun isAwaitingLogin(): Boolean = mirrorAwaitingLogin

    // ... showLoginRequiredNotification, cancelLoginRequiredNotification, scheduleSendWorker
}
```

**Notas:**
- `mutex.withLock` garantiza atomicidad: si llegan 5 × 401 paralelos, solo el primero corre el bloque completo. Los otros ven `awaitingLogin=true` y salen.
- `mirrorAwaitingLogin` solo se lee desde `isAwaitingLogin()`, que el Worker usa para evitar arrancar batches innecesarios. El Worker no escribe nunca al mirror.
- El `AuthEventBus` queda como complemento — la UI puede escucharlo para actualizar banners en tiempo real, pero si se pierde un evento la verdad sigue en DataStore.
- **No hay `runBlocking` en ningún punto**. El interceptor llama `handleTokenExpiredAsync` que internamente lanza coroutine.

### A.4 Cambios en `RetrofitClient.kt`

El lambda actual se queda intacto en el flujo de request. **Solo se agrega un check post-response:**

```kotlin
val authInterceptor = okhttp3.Interceptor { chain ->
    val originalRequest = chain.request()
    val token = tokenCache.get()

    val requestBuilder = originalRequest.newBuilder()
        .addHeader("Accept", "application/json")
        .addHeader("Content-Type", "application/json")
    token?.let { requestBuilder.addHeader("Authorization", "Bearer $it") }

    val response = chain.proceed(requestBuilder.build())

    // Detección 401 SOLO si llevaba token (evita loops con requests anónimos)
    if (response.code == 401 && token != null) {
        AuthSessionManager.handleTokenExpiredAsync(
            context = context,
            endpoint = originalRequest.url.encodedPath,
        )
    }

    response
}
```

`context` ya está disponible en el scope de `createApiService(context)` — solo capturarlo en la closure del lambda.

`AuthInterceptor.kt` (class) se elimina como dead code.

### A.5 Cambios en `SendNotificationWorker.kt`

**Inicio del `doWork()`:**

```kotlin
override suspend fun doWork(): Result {
    if (AuthSessionManager.isAwaitingLogin()) {
        FileLogger.log("SEND", "Skipped batch: awaitingLogin=true (post-401, pre-login)", "info")
        return Result.success()
    }
    // ... resto igual
}
```

**Handler de 401 simplificado:**

```kotlin
if (sendResult.isAuthError) {
    AuthSessionManager.handleTokenExpiredAsync(
        context = applicationContext,
        endpoint = "/api/notifications", // o el endpoint real
    )
    authFailed = true
    break
}
```

No tocar nada más del worker. La línea `notificationDao.updateStatus(notification.id, "SENT")` queda solo en `SendResult.Success` (no la modifiquemos).

**Importante:** NO introducir `WORK_NAME` ni `cancelUniqueWork`. El guard `isAwaitingLogin()` es suficiente. Si llegan nuevos Yapes mientras esperamos login, el worker se encola, chequea el flag, retorna `success` sin tocar la red. Limpio, sin race conditions con WorkManager.

### A.6 Cambios en las 3 login VMs

Patrón uniforme. En cada VM, reemplazar:

```kotlin
preferencesManager.saveAuthToken(authResponse.token)
```

por:

```kotlin
AuthSessionManager.handleLoginSucceeded(context, authResponse.token)
```

Las 3 VMs ya están en `viewModelScope.launch` → es legal llamar suspend. `context` se obtiene vía `application: Application` que ya inyectan (verificar en cada VM).

### A.7 Banner UI con jerarquía explícita

En el VM que alimenta `MainActivity` (probablemente `MainViewModel`):

```kotlin
sealed class AppStatus {
    object TokenExpired : AppStatus()              // prioridad máxima
    object PermissionRevoked : AppStatus()
    object ListenerDeadManualNeeded : AppStatus()   // rebindAttempts >= 3
    object ListenerReconnecting : AppStatus()
    object CapturingOK : AppStatus()
}

val status: StateFlow<AppStatus> = combine(
    preferencesManager.awaitingLogin,                              // 1. token
    permissionFlow,                                                // 2. permiso
    serviceConnectedFlow,                                          // 3. listener
    rebindAttemptsFlow,                                             // 4. recovery mode
) { awaiting, hasPermission, connected, attempts ->
    when {
        awaiting -> AppStatus.TokenExpired
        !hasPermission -> AppStatus.PermissionRevoked
        !connected && attempts >= 3 -> AppStatus.ListenerDeadManualNeeded
        !connected -> AppStatus.ListenerReconnecting
        else -> AppStatus.CapturingOK
    }
}.stateIn(viewModelScope, SharingStarted.Eagerly, AppStatus.CapturingOK)
```

La UI mapea cada estado a un banner. **Si `awaiting=true`, NUNCA se muestra "Capturando OK"**, aunque el listener esté activo.

---

## Problema B — NotificationListenerService muerto en MIUI/Xiaomi

### B.1 Diagnóstico

Ya cubierto: en MIUI, Redmi, POCO, EMUI y otros con battery savers agresivos, el sistema mata el binding sin matar el proceso. `requestRebind()` se ignora silenciosamente. Único recovery confiable: toggle off + on del permiso en Settings.

El watchdog actual ya hace lo correcto a nivel programático ([`ServiceWatchdogWorker.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/worker/ServiceWatchdogWorker.kt)):
- ✅ Reintenta rebind hasta 3 veces.
- ✅ Después de 3 fallos + `hasRealDisconnect`, muestra notificación al usuario.
- ✅ La notif abre `ACTION_NOTIFICATION_LISTENER_SETTINGS`.

Lo que falta:
- ❌ Instrucciones claras de qué hacer en la pantalla de Settings (toggle off → on).
- ❌ Mensajes específicos por OEM.
- ❌ Card destacada en `MainActivity` cuando se entra en estado terminal.
- ❌ Investigar bug aparte de "Permisos del Sistema vacíos" en Phone 1.

### B.2 Cambios

#### B.2.1 — Helper `OemDetection`

```kotlin
object OemDetection {
    enum class Oem { XIAOMI, HUAWEI, OPPO, VIVO, ONEPLUS, SAMSUNG, OTHER }

    fun current(): Oem {
        val mfr = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val model = Build.MODEL.lowercase()

        return when {
            "xiaomi" in mfr || "redmi" in brand || "poco" in brand || "redmi" in model || "poco" in model -> Oem.XIAOMI
            "huawei" in mfr || "honor" in brand -> Oem.HUAWEI
            "oppo" in mfr || "realme" in brand -> Oem.OPPO
            "vivo" in mfr -> Oem.VIVO
            "oneplus" in mfr -> Oem.ONEPLUS
            "samsung" in mfr -> Oem.SAMSUNG
            else -> Oem.OTHER
        }
    }

    fun hasAggressiveBatterySaver(): Boolean = current() in setOf(
        Oem.XIAOMI, Oem.HUAWEI, Oem.OPPO, Oem.VIVO,
    )

    fun extraHint(): String? = when (current()) {
        Oem.XIAOMI -> "En MIUI también activa Autoinicio y Sin restricción de batería para NotiCentral."
        Oem.HUAWEI -> "En EMUI también permite Autoinicio para NotiCentral en Ajustes."
        Oem.OPPO, Oem.VIVO -> "Permite Autoinicio y desactiva la optimización de batería para NotiCentral."
        else -> null
    }
}
```

#### B.2.2 — Notificación con instrucciones planas (sin markdown)

Reemplazar el `bigText` actual en `ServiceWatchdogWorker.showServiceDisconnectedNotification()`:

```kotlin
val bigText = buildString {
    append("El servicio no pudo reconectarse automáticamente.\n\n")
    append("Pasos para reactivar:\n")
    append("1. Toca esta notificación.\n")
    append("2. Busca NotiCentral en la lista.\n")
    append("3. Apaga el switch.\n")
    append("4. Espera 2 segundos.\n")
    append("5. Vuélvelo a prender.\n")
    OemDetection.extraHint()?.let {
        append("\n")
        append(it)
    }
}
```

#### B.2.3 — Card de recovery en `MainActivity`

Cuando `AppStatus.ListenerDeadManualNeeded`:

```
┌──────────────────────────────────────────────────────┐
│ ⚠️  Servicio detenido — necesita atención            │
│                                                      │
│ Android suspendió la captura de notificaciones.      │
│ Reactiva siguiendo estos pasos:                      │
│                                                      │
│  ① Toca el botón abajo                               │
│  ② Busca "NotiCentral" en la lista                   │
│  ③ Apaga el switch del permiso                       │
│  ④ Espera 2 segundos                                 │
│  ⑤ Vuelve a prender el switch                        │
│                                                      │
│ [ ABRIR AJUSTES DEL PERMISO ]                        │
│                                                      │
│ <expandible> ¿Por qué pasa esto?                     │
└──────────────────────────────────────────────────────┘
```

El "¿Por qué pasa esto?" es un `<details>`-style expandible que explica brevemente que MIUI/EMUI suspenden servicios en background, y muestra `OemDetection.extraHint()` si aplica.

Cuando `AppStatus.ListenerReconnecting`: card amber actual con "Intentando reconectar... (rebind $N de 3)".

#### B.2.4 — Bug "Permisos del Sistema vacíos" (investigación)

Task aparte. Hipótesis: la card en Phone 1 estaba vacía porque el `StateFlow` que la alimenta tenía `initialValue=null` y nunca recibió emisión válida. Investigar en `MainActivity`/`MainViewModel`.

---

## Plan de ejecución

### Fase 0 — Diagnóstico backend (no bloqueante)

#### Task 0.1 — Identificar causa del expiro de token (solo informe)

**Files:** ninguno modificado.

- [ ] **Paso 1: Revisar `apps/api/config/sanctum.php`**
   ```bash
   grep -n "expiration" apps/api/config/sanctum.php
   ```
- [ ] **Paso 2: Verificar deploys del 2026-05-16**
   ```bash
   git log --oneline --since="2026-05-15" --until="2026-05-17" -- apps/api/
   ```
- [ ] **Paso 3: Buscar revokes en código**
   ```bash
   grep -rn "tokens()->delete()" apps/api/app/
   ```
- [ ] **Paso 4: Reportar causa o "no determinable"**. El fix de la app no depende de esto.

---

### Fase 1 — Fix Problema A (token expirado)

#### Task 1.1 — `PreferencesManager` agrega `awaitingLogin`

**Files:**
- Modify: [`PreferencesManager.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/data/local/PreferencesManager.kt)

- [ ] **Paso 1:** Agregar key + flow + setter:
   ```kotlin
   private val AWAITING_LOGIN_KEY = booleanPreferencesKey("awaiting_login")
   val awaitingLogin: Flow<Boolean> = context.dataStore.data.map { it[AWAITING_LOGIN_KEY] ?: false }
   suspend fun setAwaitingLogin(value: Boolean) {
       context.dataStore.edit { it[AWAITING_LOGIN_KEY] = value }
   }
   ```
- [ ] **Paso 2:** Confirmar que `clearAll()` también resetea este flag.
- [ ] **Paso 3:** Build: `./gradlew :app:compileDebugKotlin`.
- [ ] **Paso 4:** Commit.

#### Task 1.2 — `AuthEvent` + `AuthEventBus` (solo para UI live)

**Files:**
- Create: `apps/android-client/app/src/main/java/com/yapenotifier/android/data/auth/AuthEventBus.kt`

- [ ] **Paso 1:** Implementar `AuthEvent` sealed class y `AuthEventBus` object con `MutableSharedFlow(extraBufferCapacity=8, onBufferOverflow=DROP_OLDEST)`. Es complemento, no fuente de verdad.
- [ ] **Paso 2:** Build + commit.

#### Task 1.3 — `AuthSessionManager` (pieza autoritativa)

**Files:**
- Create: `apps/android-client/app/src/main/java/com/yapenotifier/android/data/auth/AuthSessionManager.kt`

- [ ] **Paso 1:** Implementar exactamente el diseño de la sección A.3. Métodos públicos:
   - `suspend fun initialize(context, scope)` — hidrata mirror desde DataStore.
   - `fun handleTokenExpiredAsync(context, endpoint)` — fire-and-forget para interceptor.
   - `suspend fun handleTokenExpired(context, endpoint)` — variante para coroutines.
   - `suspend fun handleLoginSucceeded(context, token)` — para login VMs.
   - `fun isAwaitingLogin(): Boolean` — solo para Worker.
- [ ] **Paso 2:** Implementar `showLoginRequiredNotification(context)` con `setOngoing(true)`, canal `ALERT_CHANNEL_ID`, intent → `SplashActivity`. Texto SIN markdown: "Sesión expirada. Toca para volver a iniciar sesión y reanudar la captura."
- [ ] **Paso 3:** Implementar `cancelLoginRequiredNotification(context)` que cancela el ID 2003 del NotificationManager.
- [ ] **Paso 4:** Implementar `scheduleSendWorker(context)` — un OneTimeWorkRequest simple, mismo patrón que [`PaymentNotificationListenerService.scheduleSendNotificationWorker()`](apps/android-client/app/src/main/java/com/yapenotifier/android/service/PaymentNotificationListenerService.kt#L340).
- [ ] **Paso 5:** Build + commit.

#### Task 1.4 — Inicialización en `YapeNotifierApplication.onCreate()`

**Files:**
- Modify: [`YapeNotifierApplication.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/YapeNotifierApplication.kt)

- [ ] **Paso 1:** Si no existe ya, crear un `applicationScope: CoroutineScope` con `SupervisorJob() + Dispatchers.Default`.
- [ ] **Paso 2:** En `onCreate()`, después de `ServiceStatusManager.init(this)`, lanzar:
   ```kotlin
   applicationScope.launch {
       AuthSessionManager.initialize(this@YapeNotifierApplication, applicationScope)
   }
   ```
- [ ] **Paso 3:** Build + commit.

#### Task 1.5 — `RetrofitClient` detecta 401 (sin `runBlocking`)

**Files:**
- Modify: [`RetrofitClient.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/data/api/RetrofitClient.kt)
- Delete: [`AuthInterceptor.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/data/api/AuthInterceptor.kt) (dead code)

- [ ] **Paso 1:** Reescribir el lambda según sección A.4 — captura `context` en closure, mantiene el `requestBuilder`, agrega check `response.code == 401 && token != null` → `AuthSessionManager.handleTokenExpiredAsync(context, request.url.encodedPath)`.
- [ ] **Paso 2:** Eliminar `AuthInterceptor.kt` (dead code).
- [ ] **Paso 3:** Build + commit.

#### Task 1.6 — `SendNotificationWorker` respeta `awaitingLogin`

**Files:**
- Modify: [`SendNotificationWorker.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/worker/SendNotificationWorker.kt)

- [ ] **Paso 1:** Al inicio de `doWork()`:
   ```kotlin
   if (AuthSessionManager.isAwaitingLogin()) {
       FileLogger.log("SEND", "Skipped batch: awaitingLogin=true (post-401, pre-login)", "info")
       return Result.success()
   }
   ```
- [ ] **Paso 2:** En el handler `isAuthError`, simplificar — solo `handleTokenExpiredAsync(...)` + `break`. Quitar el `ServiceStatusManager.updateStatus(...)` y el log de AUTH_ERROR (ahora viven en `AuthSessionManager`).
- [ ] **Paso 3:** **NO introducir `WORK_NAME`. NO llamar `cancelUniqueWork`.** El guard es suficiente.
- [ ] **Paso 4:** Confirmar que `notificationDao.updateStatus(id, "SENT")` solo se llama dentro de `SendResult.Success` (debe ser intacto).
- [ ] **Paso 5:** Build + commit.

#### Task 1.7 — Login VMs llaman `handleLoginSucceeded`

**Files:**
- Modify: [`LoginViewModel.kt:59`](apps/android-client/app/src/main/java/com/yapenotifier/android/ui/viewmodel/LoginViewModel.kt#L59)
- Modify: [`PinLoginViewModel.kt:43`](apps/android-client/app/src/main/java/com/yapenotifier/android/ui/viewmodel/PinLoginViewModel.kt#L43)
- Modify: [`AdminLoginViewModel.kt:76`](apps/android-client/app/src/main/java/com/yapenotifier/android/ui/admin/viewmodel/AdminLoginViewModel.kt#L76)

- [ ] **Paso 1:** En cada VM, reemplazar:
   ```kotlin
   preferencesManager.saveAuthToken(authResponse.token)
   ```
   por:
   ```kotlin
   AuthSessionManager.handleLoginSucceeded(context, authResponse.token)
   ```
- [ ] **Paso 2:** Verificar que `context` está disponible (todos los VMs deberían tener `Application` inyectada — si no, hacer pequeño refactor).
- [ ] **Paso 3:** Build + commit.

#### Task 1.8 — `MainViewModel` calcula `AppStatus` con jerarquía explícita

**Files:**
- Modify: `MainViewModel.kt` (ubicación a confirmar)
- Modify: [`MainActivity.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/ui/MainActivity.kt)

- [ ] **Paso 1:** Crear sealed class `AppStatus` (o donde corresponda según convención del repo).
- [ ] **Paso 2:** En el VM, crear `StateFlow<AppStatus>` combinando los 4 flows (sección A.7).
- [ ] **Paso 3:** En `MainActivity`, observar el state y mapear cada `AppStatus` a su banner correspondiente. El banner de `TokenExpired` es rojo, clickeable, lleva al login. El banner de `ListenerDeadManualNeeded` es la card destacada de B.2.3.
- [ ] **Paso 4:** Confirmar: cuando `TokenExpired`, NUNCA se muestra "Capturando OK".
- [ ] **Paso 5:** Build + commit.

#### Task 1.9 — Smoke test fase 1

- [ ] **Paso 1:** `./gradlew :app:assembleDebug` → instalar en dispositivo.
- [ ] **Paso 2:** Login + capturar 1 Yape (verificar upload OK).
- [ ] **Paso 3:** Forzar expiración (revocar token desde backend o `sanctum.expiration=1`).
- [ ] **Paso 4:** Generar otro Yape o esperar al próximo SEND.
- [ ] **Paso 5:** Verificar:
   - Notif persistente "NotiCentral · Sesión expirada" aparece.
   - Banner rojo en pantalla "Sesión expirada — toca para iniciar sesión".
   - `pending=N` visible.
   - Tap → Splash → login.
   - Tras re-login, pendientes se envían, banner desaparece.
- [ ] **Paso 6:** Caso negativo: con `awaitingLogin=true`, ningún SEND llega a la red — confirmar en logs.
- [ ] **Paso 7:** Documentar en `docs/superpowers/smoke-tests/2026-05-17-android-auth-y-recovery.md`.

---

### Fase 2 — Fix Problema B (MIUI recovery)

#### Task 2.1 — `OemDetection`

**Files:**
- Create: `apps/android-client/app/src/main/java/com/yapenotifier/android/util/OemDetection.kt`

- [ ] **Paso 1:** Implementar según sección B.2.1 (MANUFACTURER + BRAND + MODEL).
- [ ] **Paso 2:** Build + commit.

#### Task 2.2 — Mejorar notif de watchdog (sin markdown, con OEM hint)

**Files:**
- Modify: [`ServiceWatchdogWorker.kt::showServiceDisconnectedNotification`](apps/android-client/app/src/main/java/com/yapenotifier/android/worker/ServiceWatchdogWorker.kt#L303)

- [ ] **Paso 1:** Reemplazar el `bigText` actual con el nuevo de la sección B.2.2.
- [ ] **Paso 2:** Cambiar el título a "NotiCentral · Servicio detenido" (consistencia).
- [ ] **Paso 3:** Build + commit.

#### Task 2.3 — Card de recovery en MainActivity

**Files:**
- Modify: [`MainActivity.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/ui/MainActivity.kt)

- [ ] **Paso 1:** Cuando `AppStatus.ListenerDeadManualNeeded`, renderizar la card de B.2.3 con los 5 pasos numerados.
- [ ] **Paso 2:** Botón grande "ABRIR AJUSTES DEL PERMISO" → `Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)`.
- [ ] **Paso 3:** Sección expandible "¿Por qué pasa esto?" — muestra `OemDetection.extraHint()` si no es null.
- [ ] **Paso 4:** Build + commit.

#### Task 2.4 — Investigar "Permisos del Sistema vacíos" (Phone 1)

**Files:** TBD según investigación.

- [ ] **Paso 1:** Reproducir si es posible (puede requerir dispositivo MIUI específico).
- [ ] **Paso 2:** Agregar logs al `MainActivity.onResume()` con valor de `NotificationAccessChecker.isNotificationAccessEnabled(this)` y de `PowerManager.isIgnoringBatteryOptimizations`.
- [ ] **Paso 3:** Si es un StateFlow con `initial=null` que nunca recibe emisión, cambiar a `StateFlow` con valor inicial síncrono.
- [ ] **Paso 4:** Build + commit. Si no se reproduce, abrir issue para investigar después.

#### Task 2.5 — Smoke test fase 2

- [ ] **Paso 1:** Install APK en dispositivo Xiaomi/Redmi.
- [ ] **Paso 2:** Matar la app desde "Recientes" + battery saver activo.
- [ ] **Paso 3:** Forzar watchdog (`WorkManager.enqueueUniqueWork(ServiceWatchdogWorker.WORK_NAME, REPLACE, ...)` desde adb o un menú debug).
- [ ] **Paso 4:** Tras 3 intentos fallidos:
   - Verificar notif con instrucciones específicas MIUI.
   - Verificar card en MainActivity (sin "Capturando OK" engañoso).
   - Tap → Settings → toggle off + on → reconecta en < 5s.
- [ ] **Paso 5:** Documentar.

---

### Fase 3 — Logging mejorado

#### Task 3.1 — Categoría `[AUTH]` consistente

**Files:**
- Modify: [`AuthSessionManager.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/data/auth/AuthSessionManager.kt)
- Modify: [`RetrofitClient.kt`](apps/android-client/app/src/main/java/com/yapenotifier/android/data/api/RetrofitClient.kt)

- [ ] **Paso 1:** Agregar logs en puntos clave:
   - `AuthSessionManager.initialize` → `[AUTH] init: awaitingLogin=X`
   - `handleTokenExpired` → `[AUTH] TOKEN_EXPIRED endpoint=/api/...`
   - `handleLoginSucceeded` → `[AUTH] LOGIN_OK token=***xxx (last 4 chars)`
   - Interceptor 401 → `[AUTH] 401 on /api/...`
- [ ] **Paso 2:** Build + commit.

---

### Fase 4 — Cierre

#### Task 4.1 — Smoke test consolidado

**Files:**
- Create: `docs/superpowers/smoke-tests/2026-05-17-android-auth-y-recovery.md`

- [ ] **Paso 1:** Checklist completo:
   - Flujo normal (login → captura → SEND OK).
   - Flujo 401 (token expira → notif → tap → login → drenado de pendientes).
   - Flujo MIUI (rebind falla 3 veces → card + notif → toggle manual → recover).
   - Caso negativo: con `awaitingLogin=true`, ningún SEND llega a red.
   - Caso negativo: con `POST_NOTIFICATIONS` denegado, banner UI sigue visible (defensa en profundidad).
- [ ] **Paso 2:** Commit.

#### Task 4.2 — Push + release APK

- [ ] **Paso 1:** `git push origin benja-version`.
- [ ] **Paso 2:** `./gradlew :app:assembleRelease`.
- [ ] **Paso 3:** Distribuir a los 2 captadores afectados.
- [ ] **Paso 4:** Pedir confirmación en 24-48h.

---

## Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Mirror en memoria desincronizado del DataStore | `initialize()` hidrata al arrancar. `observeTokenChanges`-style flow mantiene sincronización ante cambios. La verdad última está en DataStore — el mirror solo optimiza lecturas síncronas del Worker. |
| Múltiples 401 en paralelo | `Mutex` en `AuthSessionManager`. El primer caller ejecuta el bloque; los demás ven `awaitingLogin=true` y salen. |
| `AuthSessionManager.initialize()` no ha corrido cuando ocurre el primer 401 | `handleTokenExpiredAsync()` chequea `initialized` al inicio y retorna sin acción. La probabilidad es muy baja (initialize corre en `Application.onCreate` antes de cualquier UI). |
| `RetrofitClient` se crea antes que `Application.onCreate` complete | En el flujo actual, `RetrofitClient.createApiService` se llama desde `AppModule` que es lazy. `Application.onCreate` es sincrónico y completa antes que cualquier `Activity.onCreate`. Riesgo bajo. |
| `POST_NOTIFICATIONS` denegado por el usuario | Banner UI + card en pantalla principal cubren. Notif es un canal entre varios, no el único. |
| Cambio en config Sanctum invalida tokens existentes (si Task 0.1 detecta `expiration` mal) | Los tokens se invalidan, el flow nuevo los maneja con dignidad: notif + banner + drain post-login. Mejor que el estado actual. |
| WorkManager mata el worker mientras chequea `isAwaitingLogin()` | El check es síncrono, dura microsegundos. WorkManager no interrumpe operaciones que vuelven `Result.success` rápido. |
| MIUI mata WorkManager también, no se ejecuta watchdog | Sí pasa. Ya hoy. El usuario ve la pantalla al abrir la app, con el banner correcto. Cuando WorkManager se restaura, retoma. |
| Eliminar `AuthInterceptor.kt` (dead code) rompe algún import oculto | `grep -r "AuthInterceptor"` antes de borrar. Si no aparece importado en ninguna parte productiva, eliminar limpio. |

---

## Fuera de scope

- **Refresh token flow** (Sanctum no lo soporta nativamente).
- **`enqueueUniqueWork("send_pending", APPEND_OR_REPLACE, ...)` para los 3 origins del worker** — mejora estructural posterior, sin urgencia ahora.
- **Persistir `_statusHistory` entre reinicios** — hoy es Flow en memoria. Out of scope.
- **AccessibilityService para auto-toggle del permiso** — riesgoso y bloqueado por OEMs.
- **Foreground service "indestructible"** — fuera del control de la app en MIUI.
- **Limpieza de archivos duplicados `CapturedNotification.kt` y DAOs paralelos** — bug aparte, no bloqueante.

---

## Tabla resumen de tareas

| # | Tarea | Fase | Files key | Modelo |
|---|---|---|---|---|
| 0.1 | Diagnóstico backend de expiro | 0 | `sanctum.php` (read-only) | haiku |
| 1.1 | `awaitingLogin` en `PreferencesManager` | 1 | `PreferencesManager.kt` | haiku |
| 1.2 | `AuthEvent` + `AuthEventBus` (UI complement) | 1 | `AuthEventBus.kt` (nuevo) | haiku |
| 1.3 | `AuthSessionManager` (autoritativo) | 1 | `AuthSessionManager.kt` (nuevo) | sonnet |
| 1.4 | `AuthSessionManager.initialize` en Application | 1 | `YapeNotifierApplication.kt` | haiku |
| 1.5 | `RetrofitClient` detecta 401 + borrar `AuthInterceptor.kt` | 1 | `RetrofitClient.kt`, eliminar `AuthInterceptor.kt` | sonnet |
| 1.6 | Worker respeta `isAwaitingLogin()` | 1 | `SendNotificationWorker.kt` | haiku |
| 1.7 | 3 login VMs llaman `handleLoginSucceeded` | 1 | 3 VMs | haiku |
| 1.8 | `MainViewModel` calcula `AppStatus` con jerarquía | 1 | `MainViewModel.kt`, `MainActivity.kt` | sonnet |
| 1.9 | Smoke test fase 1 | 1 | doc | (manual) |
| 2.1 | `OemDetection` (MANUFACTURER + BRAND + MODEL) | 2 | `OemDetection.kt` (nuevo) | haiku |
| 2.2 | Mejorar notif del watchdog (plain text + OEM hint) | 2 | `ServiceWatchdogWorker.kt` | haiku |
| 2.3 | Card de recovery en MainActivity | 2 | `MainActivity.kt` | sonnet |
| 2.4 | Investigar "Permisos del Sistema vacíos" | 2 | `MainActivity.kt` | sonnet |
| 2.5 | Smoke test fase 2 (dispositivo MIUI) | 2 | doc | (manual) |
| 3.1 | Categoría `[AUTH]` en logs | 3 | varios | haiku |
| 4.1 | Smoke test consolidado | 4 | doc | (manual) |
| 4.2 | Push + release APK | 4 | n/a | (manual) |

**Total:** 18 tareas en 5 fases. **17 son código** (16 Android + 1 backend read-only). 2 son smoke tests manuales + 1 release.
