# Prompts de Desarrollo - Yape Notifier

**Propósito:** Este documento contiene los prompts listos para copiar y pegar para desarrollar las funcionalidades pendientes de cada proyecto.

**Uso:** Estos prompts están diseñados para ser usados con herramientas de IA (como Cursor, ChatGPT, etc.) para generar código siguiendo las especificaciones del proyecto.

---

---

## PROMPT 1: Android App - Módulo Admin Móvil y Mejoras Captador

````
# PROMPT: Implementar Módulo Admin Móvil y Mejoras para Captador - Yape Notifier Android

## CONTEXTO DEL PROYECTO

Eres un desarrollador trabajando en la app Android de Yape Notifier. Actualmente existe el módulo "Captador" que funciona correctamente, pero NO EXISTE el módulo Admin móvil. Además, faltan algunas mejoras en el módulo Captador.

Stack Tecnológico:
- Kotlin
- Android SDK (mínimo API 24, target API 34)
- MVVM Architecture
- Retrofit (cliente HTTP)
- Coroutines (operaciones asíncronas)
- Material Design 3 Components
- Navigation Component
- ViewBinding
- Room Database (ya implementado)
- WorkManager (ya implementado)

Estado Actual:
- ✅ Backend API completo con todos los endpoints necesarios
- ✅ Dashboard Web completo (referencia de diseño)
- ✅ Módulo Captador funcionando (captura, filtrado, envío)
- ✅ Login/Registro implementado
- ✅ Vinculación por QR implementada
- ❌ Módulo Admin móvil NO EXISTE (debe implementarse desde cero)
- ⚠️ UI para gestionar instancias duales falta
- ⚠️ UI para seleccionar apps a monitorear falta
- ⚠️ Wizard de permisos incompleto (falta guías OEM)

Estructura del Proyecto:
- app/src/main/java/com/yapenotifier/android/
  - ui/ (Activities y Fragments existentes)
  - data/api/ (Retrofit client ya implementado)
  - data/local/ (Room DB ya implementado)
  - util/ (PaymentNotificationFilter, PaymentNotificationParser ya implementados)

## TAREAS CRÍTICAS - MÓDULO ADMIN MÓVIL

### 1. ModeSelectionActivity - Pantalla Inicial de Selección de Modo

Ubicación: app/src/main/java/com/yapenotifier/android/ui/admin/ModeSelectionActivity.kt

Esta debe ser la Activity principal que se muestra al iniciar la app (si no hay usuario logueado o si el usuario puede elegir modo).

Requisitos:
1. Diseño:
   - Logo de la app centrado arriba (NotiCentral con icono de campana)
   - Tagline: "Centraliza las notificaciones de pago de todos tus dispositivos en un solo lugar."
   - Dos cards principales con Material Design 3:
     a) "Entrar como Administrador"
        - Icono: escudo con persona (shield con user)
        - Descripción: "Gestiona dispositivos y visualiza pagos"
        - Chevron (>) a la derecha
     b) "Vincular como Captador"
        - Icono: dispositivo móvil
        - Descripción: "Este dispositivo leerá notificaciones"
        - Chevron (>) a la derecha
   - Footer: "¿Necesitas ayuda para configurar?" con versión de la app

2. Funcionalidad:
   - Al hacer clic en "Administrador" → navegar a AdminLoginActivity
   - Al hacer clic en "Captador" → navegar a LinkDeviceActivity (ya existe)
   - Verificar si hay usuario logueado y commerce_id, si existe, navegar directamente según rol

3. Layout: activity_mode_selection.xml
   - Fondo con gradiente suave (púrpura claro a blanco)
   - ConstraintLayout o LinearLayout vertical
   - Cards con MaterialCardView, elevation, corner radius 16dp
   - Usar colores del tema (primary purple)

### 2. AdminLoginActivity - Login Específico para Admin

Ubicación: app/src/main/java/com/yapenotifier/android/ui/admin/AdminLoginActivity.kt

Requisitos:
1. Diseño similar a LoginActivity existente pero con branding de Admin:
   - Logo con escudo
   - Título: "Admin Portal"
   - Subtítulo: "Centralized Payment Hub"
   - Campos: Email/Phone y Password
   - Botón "Sign In" con flecha
   - Opción "Login with Face ID" (opcional, implementar después)
   - Link "Forgot Password?"
   - Link "Don't have an account? Sign Up" → RegisterActivity

2. Funcionalidad:
   - Validar email/phone y password
   - Llamar API de login (ya existe: POST /api/auth/login)
   - Verificar que el usuario tenga role='admin'
   - Guardar token y datos de usuario
   - Navegar a AdminPanelActivity si tiene commerce_id
   - Navegar a CreateCommerceActivity si no tiene commerce_id

3. ViewModel: AdminLoginViewModel.kt
   - Reutilizar lógica de LoginViewModel pero con validación de rol admin

### 3. AdminPanelActivity - Feed Central de Notificaciones

Ubicación: app/src/main/java/com/yapenotifier/android/ui/admin/AdminPanelActivity.kt

Esta es la pantalla principal del admin móvil.

Requisitos:
1. Estructura con Bottom Navigation:
   - Tab 1: Notificaciones (esta Activity)
   - Tab 2: Dispositivos → AdminDevicesActivity
   - Tab 3: Configuración → AdminSettingsActivity

2. Header:
   - Título: "Panel Admin"
   - Subtítulo: "Central de Pagos"
   - Icono de perfil (circular) en esquina superior derecha → AdminSettingsActivity
   - Barra de búsqueda: "Buscar transacción, alias o monto..."
     - Icono de lupa a la izquierda
     - Búsqueda en tiempo real mientras escribe

3. Filtros (Chips horizontales, scroll horizontal):
   - "Todos" (activo por defecto, color púrpura)
   - "Hoy" (chip gris)
   - "Dispositivo: [nombre]" (chip gris, mostrar nombre del dispositivo)
   - "App: [nombre]" (chip gris, mostrar nombre de la app)
   - Al hacer clic en chip, aplicar filtro y cambiar color

4. Sección "RECIENTES":
   - Título "RECIENTES" a la izquierda
   - "Marcar todo leído" (texto púrpura) a la derecha
   - Lista de cards de notificaciones en RecyclerView

5. Card de Notificación (item_notification_card.xml):
   - Layout horizontal con:
     - Icono de la app a la izquierda (circular, color según app):
       * Yape: púrpura
       * Plin: azul
       * BCP: verde
     - Información principal:
       * Primera línea: "Yape • Caja Principal • iPhone 13" (app • instancia • dispositivo)
       * Segunda línea: tiempo relativo "35 min" a la derecha
       * Título: "Confirmación de Pago" (negrita)
       * Detalle: "SEGUNDINO RICSE DE LA CRUZ te envió un pago por S/70.00"
       * Monto destacado en verde y negrita
     - Footer del card:
       * Badge "Verificado" (verde con checkmark) o "Cód: 262"
       * Link "Detalles >" a la derecha
   - Al hacer clic en card → AdminNotificationDetailActivity

6. Funcionalidades:
   - Pull-to-refresh (SwipeRefreshLayout)
   - Paginación infinita (cargar más al hacer scroll al final)
   - Filtros funcionales (conectar con API GET /api/notifications con query params)
   - Búsqueda en tiempo real (debounce 500ms)
   - Marcar como leído (llamar PUT /api/notifications/{id}/status)
   - Indicador de carga mientras carga datos
   - Manejo de estados vacíos (sin notificaciones)
   - **Actualización automática** (ver sección 6.5)

6.5. Actualización Automática de Notificaciones (Implementación Profesional)

**⚠️ IMPORTANTE:** El backend YA tiene WebSockets implementados (NotificationCreated event, canal `commerce.{commerce_id}`, evento `notification.created`).

**Recomendación:** Para Android, usar **Polling Inteligente** es la mejor opción porque:
- ✅ Más simple de implementar y mantener
- ✅ Mejor consumo de batería (no mantiene conexión abierta)
- ✅ Funciona mejor con el ciclo de vida de Android
- ✅ Latencia aceptable (15 segundos es razonable en móvil)
- ✅ Manejo de errores más robusto

**Implementación Profesional con Polling Inteligente:**

```kotlin
// En AdminPanelViewModel.kt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.ProcessLifecycleOwner
import android.util.Log

class AdminPanelViewModel(application: Application) : AndroidViewModel(application) {
    // ... código existente ...

    // Estados de polling
    private var pollingJob: Job? = null
    private var isPollingActive = false
    private var isUserTyping = false
    private var consecutiveErrors = 0
    private val maxConsecutiveErrors = 3

    private val _pollingState = MutableStateFlow<PollingState>(PollingState.Idle)
    val pollingState: StateFlow<PollingState> = _pollingState

    sealed class PollingState {
        object Idle : PollingState()
        object Active : PollingState()
        object Paused : PollingState()
        data class Error(val message: String) : PollingState()
    }

    /**
     * Inicia polling inteligente con manejo de errores y optimización de batería
     */
    fun startPolling() {
        if (isPollingActive) return

        isPollingActive = true
        consecutiveErrors = 0
        _pollingState.value = PollingState.Active

        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var pollInterval = 15000L // 15 segundos por defecto

            while (isActive && isPollingActive) {
                try {
                    // Verificar condiciones antes de hacer polling
                    if (!shouldPoll()) {
                        delay(5000) // Esperar 5 segundos antes de verificar de nuevo
                        continue
                    }

                    // Hacer polling
                    val success = loadNotifications(refresh = true, silent = true)

                    if (success) {
                        consecutiveErrors = 0
                        pollInterval = 15000L // Resetear intervalo a 15s si fue exitoso
                    } else {
                        consecutiveErrors++
                        // Aumentar intervalo exponencialmente en caso de errores
                        pollInterval = minOf(pollInterval * 2, 120000L) // Máximo 2 minutos

                        if (consecutiveErrors >= maxConsecutiveErrors) {
                            _pollingState.value = PollingState.Error("Error de conexión. Reintentando...")
                            // Esperar más tiempo antes de reintentar
                            delay(30000)
                            consecutiveErrors = 0 // Resetear después de esperar
                        }
                    }

                    delay(pollInterval)
                } catch (e: Exception) {
                    Log.e(TAG, "Error en polling", e)
                    consecutiveErrors++
                    pollInterval = minOf(pollInterval * 2, 120000L)
                    delay(pollInterval)
                }
            }
        }
    }

    /**
     * Detiene el polling
     */
    fun stopPolling() {
        isPollingActive = false
        pollingJob?.cancel()
        pollingJob = null
        _pollingState.value = PollingState.Idle
    }

    /**
     * Pausa temporalmente el polling (ej: cuando usuario está escribiendo)
     */
    fun pausePolling() {
        if (isPollingActive) {
            _pollingState.value = PollingState.Paused
        }
    }

    /**
     * Reanuda el polling después de una pausa
     */
    fun resumePolling() {
        if (isPollingActive && _pollingState.value is PollingState.Paused) {
            _pollingState.value = PollingState.Active
        }
    }

    /**
     * Verifica si se debe hacer polling
     */
    private fun shouldPoll(): Boolean {
        // No hacer polling si:
        // 1. App no está en foreground
        if (!isAppInForeground()) return false

        // 2. Usuario está escribiendo (evitar interrupciones)
        if (isUserTyping) return false

        // 3. Ya hay una carga en progreso
        if (_uiState.value?.loading == true) return false

        return true
    }

    /**
     * Verifica si la app está en foreground
     */
    private fun isAppInForeground(): Boolean {
        return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(
            androidx.lifecycle.Lifecycle.State.STARTED
        )
    }

    /**
     * Carga notificaciones con opción de modo silencioso (sin mostrar loading)
     */
    private suspend fun loadNotifications(refresh: Boolean = false, silent: Boolean = false): Boolean {
        return try {
            val currentState = _uiState.value ?: AdminPanelUiState()
            val page = if (refresh) 1 else currentState.currentPage

            if (!silent) {
                _uiState.value = currentState.copy(loading = true, error = null)
            }

            val response = apiService.getNotifications(
                deviceId = currentFilters["device_id"] as? Long,
                sourceApp = currentFilters["source_app"] as? String,
                packageName = currentFilters["package_name"] as? String,
                appInstanceId = currentFilters["app_instance_id"] as? Long,
                startDate = currentFilters["start_date"] as? String,
                endDate = currentFilters["end_date"] as? String,
                status = currentFilters["status"] as? String,
                excludeDuplicates = currentFilters["exclude_duplicates"] as? Boolean,
                perPage = 50,
                page = page
            )

            if (response.isSuccessful) {
                val paginatedResponse = response.body()
                if (paginatedResponse != null) {
                    val newNotifications = if (refresh) {
                        paginatedResponse.data
                    } else {
                        currentState.notifications + paginatedResponse.data
                    }

                    _uiState.value = AdminPanelUiState(
                        notifications = newNotifications,
                        loading = false,
                        hasMore = paginatedResponse.currentPage < paginatedResponse.lastPage,
                        currentPage = paginatedResponse.currentPage,
                        total = paginatedResponse.total
                    )
                    true
                } else {
                    if (!silent) {
                        _uiState.value = currentState.copy(loading = false, error = "No se pudieron cargar las notificaciones")
                    }
                    false
                }
            } else {
                if (!silent) {
                    _uiState.value = currentState.copy(loading = false, error = "Error ${response.code()}")
                }
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading notifications", e)
            if (!silent) {
                _uiState.value = _uiState.value?.copy(
                    loading = false,
                    error = e.message ?: "Error de conexión"
                )
            }
            false
        }
    }

    /**
     * Marca que el usuario está escribiendo (pausa polling)
     */
    fun setUserTyping(typing: Boolean) {
        isUserTyping = typing
        if (typing) {
            pausePolling()
        } else {
            resumePolling()
        }
    }

    companion object {
        private const val TAG = "AdminPanelViewModel"
    }
}
```

**En AdminPanelActivity.kt:**

```kotlin
override fun onResume() {
    super.onResume()
    viewModel.startPolling() // Iniciar polling cuando Activity está visible
}

override fun onPause() {
    super.onPause()
    viewModel.stopPolling() // Detener polling cuando Activity está en background
}

override fun onDestroy() {
    super.onDestroy()
    viewModel.stopPolling() // Asegurar que se detiene al destruir
}

// En el SearchView o EditText de búsqueda:
searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
    override fun onQueryTextSubmit(query: String?): Boolean = false

    override fun onQueryTextChange(newText: String?): Boolean {
        viewModel.setUserTyping(newText?.isNotEmpty() == true)
        viewModel.setSearchQuery(newText ?: "")
        return true
    }
})
```

**Características Profesionales Implementadas:**
- ✅ **Backoff exponencial**: Aumenta intervalo en caso de errores (15s → 30s → 60s → 120s max)
- ✅ **Detección de errores consecutivos**: Pausa temporal después de 3 errores
- ✅ **Optimización de batería**: No hace polling cuando app está en background
- ✅ **Evita interrupciones**: Pausa cuando usuario está escribiendo
- ✅ **Modo silencioso**: Polling no muestra loading spinner
- ✅ **Manejo robusto de errores**: No crashea la app, solo registra errores
- ✅ **Estado observable**: `pollingState` permite mostrar indicador de estado

7. ViewModel: AdminPanelViewModel.kt
   - LiveData/StateFlow para lista de notificaciones
   - LiveData/StateFlow para filtros activos
   - LiveData/StateFlow para estado de carga
   - Función loadNotifications(filters: NotificationFilters)
   - Función searchNotifications(query: String)
   - Función markAsRead(notificationId: Int)
   - Función markAllAsRead()
   - Función loadMore() para paginación
   - **NUEVO:** Función startPolling() para actualización automática
   - **NUEVO:** Función stopPolling() para detener actualización
   - **NUEVO:** Manejo de ciclo de vida (pausar cuando app en background)

8. Layout: activity_admin_panel.xml
   - CoordinatorLayout como root
   - AppBarLayout con:
     * Toolbar con título y perfil
     * SearchView
     * HorizontalScrollView con chips de filtros
   - SwipeRefreshLayout
   - RecyclerView con LinearLayoutManager vertical
   - BottomNavigationView con 3 tabs

9. Adapter: NotificationAdapter.kt
   - ViewHolder para card de notificación
   - Diferir actualizaciones con DiffUtil
   - Manejar clics para navegar a detalle

### 4. AdminAddDeviceActivity - Generar QR para Vincular Dispositivo

Ubicación: app/src/main/java/com/yapenotifier/android/ui/admin/AdminAddDeviceActivity.kt

Requisitos:
1. Header:
   - Botón back (←)
   - Título: "Connect Device"
   - Subtítulo: "Step 2 of 3: Pairing"

2. Campo "Device Alias":
   - Label: "Device Alias"
   - Input con icono de lápiz
   - Placeholder: "Yape Cashier 1"
   - Texto de ayuda: "This name will appear on your dashboard notifications"
   - Validación: mínimo 3 caracteres, máximo 50

3. Sección "Pairing Code":
   - Card blanco con bordes redondeados
   - Título: "Pairing Code"
   - Instrucciones: "Scan or enter this code on the capturer device"
   - QR Code generado (usar librería ZXing o similar):
     * Tamaño: 250x250dp mínimo
     * Contenido: código generado por API
   - Código numérico debajo del QR:
     * Formato: "849 - 201" (XXX - XXX con guión)
     * Mostrar en caja gris clara
     * Botón "Copiar" al lado con icono de copiar
   - Estado de vinculación:
     * Spinner de carga: "Waiting for device connection..."
     * O mensaje de éxito: "Device linked successfully!"

4. Sección "HOW TO CONNECT":
   - Título: "HOW TO CONNECT"
   - Lista numerada:
     1. "Open the Capturer App" - "On the phone that receives the notifications."
     2. "Select 'Link as Source'" - "Tap the + button on the main screen."
     3. "Scan QR or Enter Code" - "Use the code displayed above to complete pairing."

5. Funcionalidades:
   - Al iniciar Activity, llamar POST /api/devices/link-codes para generar código
   - Generar QR code con el código recibido
   - Polling cada 2 segundos: GET /api/devices/link-codes/{code} para verificar estado
   - Si code.valid == true y code.device_id existe, mostrar éxito y navegar a AdminDevicesActivity
   - Si code expira (verificar expires_at), mostrar mensaje y permitir regenerar
   - Botón "Cancel" en footer para cancelar y volver atrás
   - Al guardar alias, actualizar dispositivo cuando se vincule

6. ViewModel: AdminAddDeviceViewModel.kt
   - LiveData/StateFlow para código de vinculación
   - LiveData/StateFlow para estado (generating, waiting, linked, expired, error)
   - LiveData/StateFlow para device alias
   - Función generateLinkCode()
   - Función checkLinkStatus(code: String)
   - Función startPolling(code: String)
   - Función stopPolling()
   - Función validateAlias(alias: String): Boolean

7. Layout: activity_admin_add_device.xml
   - ScrollView para contenido
   - Card para QR code con padding
   - Input para alias
   - Sección de instrucciones con lista
   - Botón Cancel en footer

### 5. AdminDevicesActivity - Gestión de Dispositivos

Ubicación: app/src/main/java/com/yapenotifier/android/ui/admin/AdminDevicesActivity.kt

Requisitos:
1. Header:
   - Título: "Dispositivos"
   - Botón FAB (+) flotante para agregar dispositivo → AdminAddDeviceActivity

2. Lista de Dispositivos (RecyclerView):
   - Card por dispositivo (expandible):
     * Header del card (siempre visible):
       - Nombre del dispositivo (alias) en negrita
       - Badge de estado: "Online" (verde) o "Offline" (rojo)
       - Última actividad: "Hace X minutos" o "Nunca"
       - Icono de expandir/colapsar (chevron)
     * Contenido expandido (al hacer clic):
       - Salud del dispositivo:
         * Badge: "OK" (verde), "Advertencia" (amarillo), "Error" (rojo)
         * Iconos: batería, WiFi, permisos (checkmarks o X)
       - Lista de instancias de apps:
         * "Instancias detectadas:"
         * Lista con nombre de instancia y package
       - Última notificación recibida:
         * "Última notificación: [app] - [tiempo]"
       - Botones de acción:
         * "Editar" → editar alias
         * "Eliminar" → confirmar y eliminar

3. Funcionalidades:
   - Cargar dispositivos: GET /api/devices
   - Para cada dispositivo, cargar instancias: GET /api/devices/{id}/app-instances
   - Determinar estado online/offline basado en last_seen_at (si < 5 minutos = online)
   - Determinar salud basado en campos de health del dispositivo
   - Actualizar lista cada 30 segundos (o al hacer pull-to-refresh)
   - Navegación a editar dispositivo
   - Eliminar dispositivo con confirmación (DELETE /api/devices/{id})

4. ViewModel: AdminDevicesViewModel.kt
   - LiveData/StateFlow para lista de dispositivos
   - LiveData/StateFlow para estado de carga
   - Función loadDevices()
   - Función loadDeviceInstances(deviceId: Int)
   - Función deleteDevice(deviceId: Int)
   - Función updateDeviceAlias(deviceId: Int, alias: String)

5. Layout: activity_admin_devices.xml
   - CoordinatorLayout
   - AppBarLayout con Toolbar
   - SwipeRefreshLayout
   - RecyclerView con LinearLayoutManager
   - FAB flotante

6. Adapter: DeviceAdapter.kt
   - ViewHolder expandible
   - Manejar clics para expandir/colapsar
   - Mostrar información de salud e instancias

### 6. AdminNotificationDetailActivity - Detalle de Notificación

Ubicación: app/src/main/java/com/yapenotifier/android/ui/admin/AdminNotificationDetailActivity.kt

Requisitos:
1. Mostrar información completa de la notificación:
   - Header con icono de app y título
   - Información del pago:
     * Remitente: nombre completo
     * Monto: destacado grande
     * Moneda: S/ o $
     * Fecha y hora completa
   - Información técnica:
     * App origen
     * Instancia (si tiene label)
     * Dispositivo
     * Package name
     * Android User ID
   - Texto completo de la notificación (title + body)
   - Estado actual: Pendiente / Validado / Inconsistente

2. Acciones:
   - Botón "Marcar como Validado" (si está pendiente)
   - Botón "Marcar como Inconsistente" (si está pendiente)
   - Botón "Volver" o flecha back

3. Funcionalidad:
   - Cargar notificación: GET /api/notifications/{id}
   - Actualizar estado: PUT /api/notifications/{id}/status

### 7. AdminSettingsActivity - Configuración

Ubicación: app/src/main/java/com/yapenotifier/android/ui/admin/AdminSettingsActivity.kt

Requisitos básicos:
1. Información del comercio
2. Gestión de apps monitoreadas (navegar a lista)
3. Cerrar sesión
4. Información de la app (versión)

## TAREAS IMPORTANTES - MEJORAS CAPTADOR

### 8. AppInstancesManagementActivity - Gestión de Instancias Duales

Ubicación: app/src/main/java/com/yapenotifier/android/ui/AppInstancesManagementActivity.kt

Requisitos:
1. Detección automática al abrir:
   - Cargar todas las notificaciones capturadas localmente
   - Agrupar por (packageName + androidUserId)
   - Detectar si hay múltiples instancias del mismo package
   - Mostrar alerta: "Se detectaron X instancias de [Package]"

2. Lista de instancias:
   - Agrupar por package name
   - Para cada instancia mostrar:
     * Package name
     * Android User ID
     * Label actual (si existe) o "Sin nombre"
     * Badge: "Nombrada" (verde) o "Sin nombre" (gris)
   - Botón "Asignar nombre" o hacer clic en instancia sin nombre

3. Diálogo para asignar nombre:
   - Input: "Nombre de la instancia" (ej: "Rocío", "Pamela", "Yape 1")
   - Validación: mínimo 1 carácter, máximo 30
   - Botones: "Guardar", "Cancelar"
   - Al guardar: PUT /api/app-instances/{id} con {instance_label: "nombre"}

4. Sincronización:
   - Cargar instancias desde API: GET /api/app-instances
   - Sincronizar con instancias locales detectadas
   - Mostrar estado de sincronización

5. ViewModel: AppInstancesManagementViewModel.kt
   - Detectar instancias desde Room DB
   - Cargar instancias desde API
   - Actualizar labels
   - Sincronizar

### 9. MonitoredAppsSelectionActivity - Selector de Apps

Ubicación: app/src/main/java/com/yapenotifier/android/ui/MonitoredAppsSelectionActivity.kt

Requisitos:
1. Lista de apps disponibles:
   - Cargar: GET /api/monitor-packages
   - Mostrar para cada app:
     * Icono (si disponible, usar PackageManager para obtener icono)
     * Nombre (display_name)
     * Package name (texto pequeño gris)
     * Switch/Checkbox para habilitar/deshabilitar
     * Estado: "Monitoreada" o "No monitoreada"

2. Filtros:
   - Barra de búsqueda: buscar por nombre o package
   - Chips: "Todas" / "Solo monitoreadas" / "Solo no monitoreadas"

3. Sincronización:
   - Al cambiar switch, actualizar inmediatamente:
     * Si activa: PUT /api/monitor-packages/{id}/enable
     * Si desactiva: PUT /api/monitor-packages/{id}/disable
   - Guardar en local (SettingsRepository) también
   - Mostrar indicador de sincronización

4. Información:
   - Contador: "X apps monitoreadas"
   - Última actualización

5. ViewModel: MonitoredAppsSelectionViewModel.kt
   - Cargar apps desde API
   - Actualizar estado de monitoreo
   - Sincronizar con backend y local

### 10. Mejorar PermissionsWizardActivity - Guías OEM

Ubicación: app/src/main/java/com/yapenotifier/android/ui/PermissionsWizardActivity.kt (ya existe, mejorar)

Requisitos:
1. Detectar OEM del dispositivo:
   - Crear: app/src/main/java/com/yapenotifier/android/util/DeviceOEMDetector.kt
   - Detectar: MIUI, ColorOS, One UI, OxygenOS, Stock Android
   - Usar Build.MANUFACTURER, Build.BRAND, Build.MODEL

2. Mostrar guía específica según OEM:
   - Fragmentos o pantallas con instrucciones paso a paso
   - Screenshots o ilustraciones (usar drawable resources)
   - Botones de acción directa (Intent para abrir settings específicos)

3. OEMs a cubrir:
   - MIUI (Xiaomi/Redmi/POCO)
   - ColorOS (OPPO/Realme)
   - One UI (Samsung)
   - Stock Android (genérico)

## ENDPOINTS API DISPONIBLES

Todos estos endpoints ya están implementados en el backend:

- GET /api/notifications?page=1&per_page=50&device_id=X&app_instance_id=Y&source_app=Z&status=X&search=query
- GET /api/notifications/{id}
- PUT /api/notifications/{id}/status
- GET /api/devices
- POST /api/devices/link-codes (generar código)
- GET /api/devices/link-codes/{code} (verificar estado)
- GET /api/devices/{id}/app-instances
- GET /api/app-instances
- PUT /api/app-instances/{id} (actualizar label)
- GET /api/monitor-packages
- PUT /api/monitor-packages/{id}/enable
- PUT /api/monitor-packages/{id}/disable
- POST /api/auth/login
- GET /api/auth/user

Ver: apps/api/README.md para documentación completa

## ESTRUCTURA DE ARCHIVOS A CREAR

app/src/main/java/com/yapenotifier/android/ui/admin/
├── ModeSelectionActivity.kt
├── AdminLoginActivity.kt
├── AdminPanelActivity.kt
├── AdminAddDeviceActivity.kt
├── AdminDevicesActivity.kt
├── AdminNotificationDetailActivity.kt
├── AdminSettingsActivity.kt
└── viewmodel/
    ├── AdminLoginViewModel.kt
    ├── AdminPanelViewModel.kt
    ├── AdminAddDeviceViewModel.kt
    ├── AdminDevicesViewModel.kt
    └── AdminNotificationDetailViewModel.kt

app/src/main/java/com/yapenotifier/android/ui/
├── AppInstancesManagementActivity.kt
├── MonitoredAppsSelectionActivity.kt
└── viewmodel/
    ├── AppInstancesManagementViewModel.kt
    └── MonitoredAppsSelectionViewModel.kt

app/src/main/java/com/yapenotifier/android/util/
├── DeviceOEMDetector.kt

app/src/main/res/layout/
├── activity_mode_selection.xml
├── activity_admin_login.xml
├── activity_admin_panel.xml
├── activity_admin_add_device.xml
├── activity_admin_devices.xml
├── activity_admin_notification_detail.xml
├── activity_admin_settings.xml
├── activity_app_instances_management.xml
├── activity_monitored_apps_selection.xml
├── item_notification_card.xml
├── item_device_card.xml
└── item_monitored_app.xml

## DEPENDENCIAS NECESARIAS

Agregar a build.gradle (Module: app):

dependencies {
    // QR Code generation
    implementation 'com.journeyapps:zxing-android-embedded:4.3.0'

    // Material Design 3
    implementation 'com.google.android.material:material:1.11.0'

    // Navigation Component (si no está)
    implementation 'androidx.navigation:navigation-fragment-ktx:2.7.6'
    implementation 'androidx.navigation:navigation-ui-ktx:2.7.6'

    // Ya deberían estar:
    // Retrofit, Coroutines, Room, WorkManager, etc.
}

## CONSIDERACIONES DE CALIDAD Y DEVOPS

### Tests Automatizados

**Requisitos:**
1. Tests unitarios para ViewModels:
   - Crear: `app/src/test/java/com/yapenotifier/android/ui/admin/viewmodel/`
   - Testear lógica de negocio, filtros, búsqueda
   - Cobertura mínima: 70% de ViewModels

2. Tests de instrumentación para Activities:
   - Crear: `app/src/androidTest/java/com/yapenotifier/android/ui/admin/`
   - Testear navegación, interacciones de usuario
   - Usar Espresso para UI testing

3. Configurar CI/CD:
   - Crear: `.github/workflows/android-ci.yml`
   - Ejecutar tests en cada PR
   - Build automático en cada commit

**Ejemplo de test unitario:**
```kotlin
// AdminPanelViewModelTest.kt
@Test
fun `loadNotifications should update UI state with notifications`() = runTest {
    // Given
    val mockNotifications = listOf(createMockNotification())
    coEvery { apiService.getNotifications(any()) } returns mockResponse(mockNotifications)

    // When
    viewModel.loadNotifications()

    // Then
    assertEquals(mockNotifications, viewModel.uiState.value.notifications)
}
````

### Variables de Entorno

**Requisitos:**

1. NO hardcodear URLs de API:

   - Usar `BuildConfig.API_BASE_URL`
   - Configurar en `build.gradle`:

   ```gradle
   buildTypes {
       debug {
           buildConfigField "String", "API_BASE_URL", '"http://10.0.2.2:8000/"'
       }
       release {
           buildConfigField "String", "API_BASE_URL", '"https://api.notificaciones.space/"'
       }
   }
   ```

2. NO hardcodear secretos:

   - Usar `gradle.properties` para desarrollo local
   - Usar variables de entorno en CI/CD
   - Documentar en `README.md`

3. Validar configuración al iniciar:
   - Verificar que API_URL esté configurada
   - Mostrar error claro si falta configuración

### Logging Estructurado

**Requisitos:**

1. Usar Timber para logging:

   ```kotlin
   implementation 'com.jakewharton.timber:timber:5.0.1'
   ```

2. NO loggear información sensible:

   - No loggear tokens, passwords, datos personales
   - Usar niveles apropiados (DEBUG, INFO, WARN, ERROR)

3. Agregar contexto útil:
   ```kotlin
   Timber.tag("AdminPanel").d("Loading notifications for commerce: ${commerceId}")
   ```

### CI/CD Pipeline

**Requisitos:**

1. Crear `.github/workflows/android-ci.yml`:

   ```yaml
   name: Android CI
   on: [push, pull_request]
   jobs:
     test:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v3
         - uses: actions/setup-java@v3
         - run: ./gradlew test
         - run: ./gradlew connectedAndroidTest
   ```

2. Build automático:
   - Build debug APK en cada commit
   - Build release AAB en tags
   - Upload artifacts

### Security Best Practices

**Requisitos:**

1. ProGuard/R8 para release:

   - Obfuscar código
   - Reducir tamaño de APK
   - Proteger contra reverse engineering

2. Certificados:

   - Usar keystore seguro
   - No commitear keystore al repositorio
   - Documentar proceso de firma

3. Validación de entrada:
   - Validar todos los inputs del usuario
   - Sanitizar datos antes de enviar a API
   - Manejar errores de red apropiadamente

## CRITERIOS DE ACEPTACIÓN

Módulo Admin:

1. ✅ Pantalla de selección de modo funciona y navega correctamente
2. ✅ Login admin funciona y valida rol
3. ✅ Feed de notificaciones carga y muestra datos reales
4. ✅ Filtros funcionan y actualizan la lista
5. ✅ Búsqueda funciona en tiempo real
6. ✅ Pull-to-refresh actualiza la lista
7. ✅ Paginación infinita funciona
8. ✅ Navegación bottom tabs funciona
9. ✅ Generación de QR funciona y muestra código correcto
10. ✅ Polling detecta cuando dispositivo se vincula
11. ✅ Lista de dispositivos muestra estado correcto
12. ✅ Detalle de notificación muestra toda la información
13. ✅ **Actualización automática funciona** (polling inteligente cada 15s cuando app visible)
14. ✅ **Polling se pausa automáticamente** cuando app está en background
15. ✅ **Polling se pausa** cuando usuario está escribiendo en búsqueda
16. ✅ **Backoff exponencial** en caso de errores (15s → 30s → 60s → 120s max)
17. ✅ **Detección de errores consecutivos** (pausa temporal después de 3 errores)
18. ✅ **Modo silencioso** (polling no muestra loading spinner)
19. ✅ **Estado observable** de polling (permite mostrar indicador visual)
20. ✅ **Manejo robusto de errores** sin crashear la app
21. ✅ **Optimización de batería** (no hace polling innecesario)

Mejoras Captador: 13. ✅ Detecta automáticamente instancias múltiples 14. ✅ Permite asignar nombres a instancias 15. ✅ Sincroniza con backend correctamente 16. ✅ Lista de apps carga desde API 17. ✅ Switches actualizan estado en tiempo real 18. ✅ Wizard detecta OEM y muestra guía específica

Calidad y DevOps: 19. ✅ Tests unitarios implementados (cobertura mínima 70%) 20. ✅ Tests de instrumentación para flujos críticos 21. ✅ CI/CD pipeline configurado y funcionando 22. ✅ Variables de entorno documentadas y validadas 23. ✅ Logging estructurado implementado 24. ✅ ProGuard configurado para release builds

## NOTAS IMPORTANTES

- Usar Material Design 3 para todos los componentes
- Implementar manejo de errores completo (try-catch, mostrar mensajes al usuario)
- Implementar estados de carga (ProgressBar, Shimmer, etc.)
- Usar ViewBinding para todos los layouts (no findViewById)
- Seguir el patrón MVVM existente en el proyecto
- Reutilizar componentes existentes cuando sea posible (RetrofitClient, PreferencesManager, etc.)
- Implementar paginación para listas grandes
- Usar Coroutines para operaciones asíncronas
- Manejar estados offline (guardar en local, sincronizar después)
- Probar en dispositivos reales de diferentes OEMs
- Seguir las convenciones de código existentes en el proyecto
- **NUEVO:** Implementar tests automatizados antes de considerar completo
- **NUEVO:** Configurar CI/CD para validación automática
- **NUEVO:** Documentar y validar variables de entorno
- **NUEVO:** Implementar logging estructurado para debugging

```

---

## PROMPT 2: Dashboard Web - Notificaciones en Tiempo Real y Mejoras UX

```

# PROMPT: Implementar Notificaciones en Tiempo Real y Mejoras UX - Yape Notifier Dashboard Web

## CONTEXTO DEL PROYECTO

Eres un desarrollador trabajando en el Dashboard Web de Yape Notifier. Necesitas implementar actualización en tiempo real del feed de notificaciones y mejorar la UX según los diseños proporcionados.

Stack Tecnológico:

- React 18
- TypeScript
- Vite
- React Query (tanstack/react-query) para cache y sincronización
- React Router v6
- Tailwind CSS
- Lucide React (iconos)
- date-fns (formateo de fechas)
- Laravel Echo + Pusher JS (para WebSockets con Laravel Reverb)

Estado Actual:

- ✅ Feed de notificaciones implementado (NotificationsPage.tsx)
- ✅ Filtros funcionando (dispositivo, app, instancia, fechas, estado)
- ✅ Paginación implementada
- ✅ Gestión de dispositivos completa
- ✅ Gestión de instancias completa
- ✅ Estadísticas y KPIs
- ✅ **Backend con WebSockets implementado** (NotificationCreated event, Reverb configurado)
- ❌ No hay actualización automática en frontend (requiere refresh manual)
- ❌ No hay badge de notificaciones no leídas
- ❌ **Frontend no está conectado a WebSockets** (debe implementarse)
- ⚠️ UX no coincide completamente con diseños (filtros, búsqueda, estados vacíos)

**IMPORTANTE:** El backend YA tiene WebSockets implementados (NotificationCreated event, Reverb configurado). Debes implementar WebSockets directamente en el frontend, NO usar polling.

Estructura del Proyecto:

- src/
  - pages/ (NotificationsPage.tsx, DevicesPage.tsx, etc.)
  - components/ (componentes reutilizables)
  - hooks/ (custom hooks)
  - services/ (apiService.ts)
  - types/ (TypeScript types)
  - contexts/ (AuthContext, etc.)

## TAREAS CRÍTICAS

### 1. Implementar Actualización en Tiempo Real con WebSockets (Implementación Profesional)

**⚠️ IMPORTANTE:** El backend YA tiene WebSockets implementados con:

- **Canal:** `commerce.{commerce_id}` (PrivateChannel)
- **Evento:** `notification.created`
- **Datos completos:** id, user_id, commerce_id, device_id, source_app, package_name, app_instance_id, app_instance_label, device_alias, title, body, amount, currency, payer_name, posted_at, received_at, status, is_duplicate, created_at

Debes conectarte a Laravel Reverb usando Laravel Echo, **NO usar polling**.

Ubicación: `src/services/echo.ts` (crear) y `src/hooks/useNotifications.ts` (modificar)

**Requisitos Profesionales:**

1. Instalar dependencias:

```bash
npm install laravel-echo pusher-js
```

2. Configurar Laravel Echo con manejo robusto de errores y reconexión:

```typescript
// src/services/echo.ts
import Echo from "laravel-echo";
import Pusher from "pusher-js";

// Declarar tipos globales
declare global {
  interface Window {
    Pusher: typeof Pusher;
    Echo: Echo;
  }
}

// Configurar Pusher como global (requerido por Laravel Echo)
window.Pusher = Pusher;

// Obtener token de autenticación (desde tu AuthContext o similar)
const getAuthToken = (): string => {
  return localStorage.getItem("auth_token") || "";
};

// Estado de conexión
let connectionState: "connected" | "disconnected" | "connecting" | "error" =
  "disconnected";
let reconnectAttempts = 0;
const maxReconnectAttempts = 5;
let reconnectTimeout: NodeJS.Timeout | null = null;

// Crear instancia de Echo con configuración profesional
export const echo = new Echo({
  broadcaster: "reverb",
  key: import.meta.env.VITE_REVERB_APP_KEY,
  wsHost: import.meta.env.VITE_REVERB_HOST || window.location.hostname,
  wsPort: import.meta.env.VITE_REVERB_PORT || 8080,
  wssPort: import.meta.env.VITE_REVERB_PORT || 8080,
  forceTLS: (import.meta.env.VITE_REVERB_SCHEME || "https") === "https",
  enabledTransports: ["ws", "wss"],
  authEndpoint: `${import.meta.env.VITE_API_URL}/api/broadcasting/auth`,
  auth: {
    headers: {
      Authorization: `Bearer ${getAuthToken()}`,
      Accept: "application/json",
    },
  },
  // Configuración de reconexión automática
  cluster: undefined, // No necesario para Reverb
});

// Manejo de eventos de conexión
echo.connector.pusher.connection.bind("connected", () => {
  console.log("✅ WebSocket conectado");
  connectionState = "connected";
  reconnectAttempts = 0;

  // Notificar a listeners
  window.dispatchEvent(new CustomEvent("echo:connected"));
});

echo.connector.pusher.connection.bind("disconnected", () => {
  console.log("⚠️ WebSocket desconectado");
  connectionState = "disconnected";

  // Notificar a listeners
  window.dispatchEvent(new CustomEvent("echo:disconnected"));

  // Intentar reconexión automática
  attemptReconnect();
});

echo.connector.pusher.connection.bind("error", (error: any) => {
  console.error("❌ Error de WebSocket:", error);
  connectionState = "error";

  // Notificar a listeners
  window.dispatchEvent(new CustomEvent("echo:error", { detail: error }));

  // Intentar reconexión si no se ha excedido el límite
  if (reconnectAttempts < maxReconnectAttempts) {
    attemptReconnect();
  }
});

// Función de reconexión con backoff exponencial
function attemptReconnect() {
  if (reconnectAttempts >= maxReconnectAttempts) {
    console.error("❌ Máximo de intentos de reconexión alcanzado");
    window.dispatchEvent(new CustomEvent("echo:max-reconnect-attempts"));
    return;
  }

  reconnectAttempts++;
  const delay = Math.min(1000 * Math.pow(2, reconnectAttempts - 1), 30000); // Max 30s

  console.log(
    `🔄 Intentando reconectar en ${delay}ms (intento ${reconnectAttempts}/${maxReconnectAttempts})`
  );

  reconnectTimeout = setTimeout(() => {
    connectionState = "connecting";
    echo.connector.pusher.connect();
  }, delay);
}

// Función para resetear intentos de reconexión (útil después de login)
export function resetReconnectAttempts() {
  reconnectAttempts = 0;
  if (reconnectTimeout) {
    clearTimeout(reconnectTimeout);
    reconnectTimeout = null;
  }
}

// Función para obtener estado de conexión
export function getConnectionState() {
  return connectionState;
}

// Función para desconectar manualmente
export function disconnect() {
  if (reconnectTimeout) {
    clearTimeout(reconnectTimeout);
    reconnectTimeout = null;
  }
  echo.disconnect();
}

// Exportar instancia global
window.Echo = echo;

export default echo;
```

3. Hook profesional para escuchar notificaciones en tiempo real:

```typescript
// src/hooks/useNotifications.ts (modificar completamente)
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef, useCallback } from "react";
import { apiService } from "@/services/api";
import { echo, getConnectionState } from "@/services/echo";
import type {
  NotificationFilters,
  PaginatedResponse,
  Notification,
} from "@/types";
import { useAuth } from "@/contexts/AuthContext"; // Ajustar según tu contexto

interface UseNotificationsOptions {
  filters?: NotificationFilters;
  enabled?: boolean;
  onNewNotification?: (notification: Notification) => void;
}

export function useNotifications(options: UseNotificationsOptions = {}) {
  const { filters = {}, enabled = true, onNewNotification } = options;
  const queryClient = useQueryClient();
  const { user } = useAuth(); // Obtener usuario autenticado
  const commerceId = user?.commerce_id;

  const channelRef = useRef<any>(null);
  const onNewNotificationRef = useRef(onNewNotification);

  // Actualizar ref cuando cambia el callback
  useEffect(() => {
    onNewNotificationRef.current = onNewNotification;
  }, [onNewNotification]);

  // Query inicial para cargar notificaciones
  const query = useQuery<PaginatedResponse<Notification>>({
    queryKey: ["notifications", filters],
    queryFn: () => apiService.getNotifications(filters),
    enabled: enabled && !!commerceId,
    staleTime: 30000, // 30 segundos (WebSockets actualizará antes)
    refetchOnWindowFocus: true, // Refetch cuando ventana recupera foco
  });

  // Escuchar eventos de WebSocket
  useEffect(() => {
    if (!commerceId || !enabled || !echo) {
      return;
    }

    // Verificar estado de conexión
    const connectionState = getConnectionState();
    if (connectionState !== "connected") {
      console.warn("⚠️ WebSocket no está conectado, estado:", connectionState);
    }

    // Suscribirse al canal privado
    const channelName = `commerce.${commerceId}`;
    channelRef.current = echo.private(channelName);

    // Escuchar evento de notificación creada
    // IMPORTANTE: El backend envía el evento como 'notification.created'
    channelRef.current.listen(
      ".notification.created",
      (data: { notification: Notification }) => {
        const notification = data.notification;
        console.log(
          "🔔 Nueva notificación recibida vía WebSocket:",
          notification
        );

        // Llamar callback si existe
        if (onNewNotificationRef.current) {
          onNewNotificationRef.current(notification);
        }

        // Actualizar cache de React Query de forma optimista
        queryClient.setQueryData<PaginatedResponse<Notification>>(
          ["notifications", filters],
          (oldData) => {
            if (!oldData) {
              // Si no hay datos, hacer refetch
              queryClient.invalidateQueries({ queryKey: ["notifications"] });
              return oldData;
            }

            // Verificar si la notificación ya existe (evitar duplicados)
            const exists = oldData.data.some((n) => n.id === notification.id);
            if (exists) {
              // Actualizar notificación existente si cambió
              return {
                ...oldData,
                data: oldData.data.map((n) =>
                  n.id === notification.id ? notification : n
                ),
              };
            }

            // Agregar nueva notificación al inicio
            return {
              ...oldData,
              data: [notification, ...oldData.data],
              total: oldData.total + 1,
            };
          }
        );

        // Invalidar queries relacionadas para mantener consistencia
        queryClient.invalidateQueries({
          queryKey: ["notifications"],
          exact: false, // Invalidar todas las variaciones de filtros
        });
      }
    );

    // Manejar errores de suscripción
    channelRef.current.error((error: any) => {
      console.error("❌ Error en canal WebSocket:", error);
    });

    // Limpiar suscripción al desmontar
    return () => {
      if (channelRef.current) {
        channelRef.current.stopListening(".notification.created");
        echo.leave(channelName);
        channelRef.current = null;
      }
    };
  }, [commerceId, enabled, filters, queryClient]);

  // Escuchar cambios en estado de conexión
  useEffect(() => {
    const handleConnected = () => {
      console.log("✅ WebSocket reconectado, refrescando notificaciones...");
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    };

    const handleDisconnected = () => {
      console.warn("⚠️ WebSocket desconectado");
    };

    window.addEventListener("echo:connected", handleConnected);
    window.addEventListener("echo:disconnected", handleDisconnected);

    return () => {
      window.removeEventListener("echo:connected", handleConnected);
      window.removeEventListener("echo:disconnected", handleDisconnected);
    };
  }, [queryClient]);

  return {
    ...query,
    connectionState: getConnectionState(),
  };
}
```

4. Componente de indicador de estado de conexión:

```typescript
// src/components/WebSocketStatus.tsx
import { useEffect, useState } from "react";
import { Wifi, WifiOff, AlertCircle } from "lucide-react";
import { getConnectionState } from "@/services/echo";

export default function WebSocketStatus() {
  const [status, setStatus] = useState<
    "connected" | "disconnected" | "connecting" | "error"
  >(getConnectionState());

  useEffect(() => {
    const updateStatus = () => {
      setStatus(getConnectionState());
    };

    window.addEventListener("echo:connected", updateStatus);
    window.addEventListener("echo:disconnected", updateStatus);
    window.addEventListener("echo:error", updateStatus);

    return () => {
      window.removeEventListener("echo:connected", updateStatus);
      window.removeEventListener("echo:disconnected", updateStatus);
      window.removeEventListener("echo:error", updateStatus);
    };
  }, []);

  const statusConfig = {
    connected: {
      icon: Wifi,
      color: "text-green-500",
      label: "Conectado",
    },
    disconnected: {
      icon: WifiOff,
      color: "text-gray-400",
      label: "Desconectado",
    },
    connecting: {
      icon: Wifi,
      color: "text-yellow-500",
      label: "Conectando...",
    },
    error: {
      icon: AlertCircle,
      color: "text-red-500",
      label: "Error de conexión",
    },
  };

  const config = statusConfig[status];
  const Icon = config.icon;

  return (
    <div
      className={`flex items-center gap-2 ${config.color}`}
      title={config.label}
    >
      <Icon className="w-4 h-4" />
      <span className="text-xs">{config.label}</span>
    </div>
  );
}
```

5. Actualizar NotificationsPage.tsx:

```typescript
// src/pages/NotificationsPage.tsx
import { useNotifications } from "@/hooks/useNotifications";
import WebSocketStatus from "@/components/WebSocketStatus";

export default function NotificationsPage() {
  const { data, loading, error, connectionState, refetch } = useNotifications({
    filters: { per_page: 50, page: 1 },
    enabled: true,
    onNewNotification: (notification) => {
      // Mostrar toast o notificación
      console.log("Nueva notificación:", notification);
      // Aquí puedes integrar tu sistema de toasts
    },
  });

  return (
    <div>
      {/* Indicador de estado de conexión */}
      <div className="flex justify-end p-4">
        <WebSocketStatus />
      </div>

      {/* Resto del componente */}
      {/* ... */}
    </div>
  );
}
```

**Características Profesionales Implementadas:**

- ✅ **Reconexión automática** con backoff exponencial
- ✅ **Manejo robusto de errores** sin crashear la app
- ✅ **Actualización optimista** del cache (sin refetch innecesario)
- ✅ **Prevención de duplicados** verificando IDs
- ✅ **Indicador visual de estado** de conexión
- ✅ **Limpieza adecuada** de suscripciones
- ✅ **Sincronización con React Query** para consistencia
- ✅ **Callback para nuevas notificaciones** (toasts, sonidos, etc.)

### 2. Badge de Notificaciones No Leídas

Ubicación: src/components/NotificationBadge.tsx (crear)

Requisitos:

1. Componente de badge:

   - Mostrar contador de notificaciones no leídas
   - Actualizar automáticamente cuando llegan nuevas (vía WebSocket)
   - Mostrar en header/navbar junto al icono de notificaciones
   - Animación cuando cambia el número

2. Lógica:

   - Contar notificaciones con status='pending' o is_read=false
   - Actualizar automáticamente cuando llega evento WebSocket
   - Usar React Query para mantener contador actualizado
   - Persistir en localStorage como backup

3. Implementación:

```typescript
// src/hooks/useUnreadNotificationsCount.ts
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { apiService } from "@/services/api";
import { echo } from "@/services/echo";

export function useUnreadNotificationsCount() {
  const queryClient = useQueryClient();
  const commerceId = useCommerceId();

  // Query inicial para obtener contador
  const query = useQuery({
    queryKey: ["notifications", "unread-count"],
    queryFn: async () => {
      const response = await apiService.getNotifications({
        per_page: 1,
        page: 1,
        status: "pending",
      });
      return response.total; // Total de notificaciones pendientes
    },
    staleTime: Infinity, // No hacer refetch automático, WebSocket actualizará
  });

  // Escuchar eventos WebSocket para actualizar contador
  useEffect(() => {
    if (!commerceId || !echo) return;

    const channel = echo.private(`commerce.${commerceId}`);

    channel.listen(".notification.created", () => {
      // Incrementar contador cuando llega nueva notificación
      queryClient.setQueryData<number>(
        ["notifications", "unread-count"],
        (oldCount) => (oldCount ?? 0) + 1
      );
    });

    return () => {
      channel.stopListening(".notification.created");
    };
  }, [commerceId, queryClient]);

  return query;
}
```

4. Componente:

```typescript
// src/components/NotificationBadge.tsx
import { Bell } from "lucide-react";
import { useUnreadNotificationsCount } from "@/hooks/useUnreadNotificationsCount";

export default function NotificationBadge() {
  const { data: count = 0 } = useUnreadNotificationsCount();

  return (
    <div className="relative">
      <Bell className="w-6 h-6" />
      {count > 0 && (
        <span className="absolute -top-2 -right-2 bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">
          {count > 99 ? "99+" : count}
        </span>
      )}
    </div>
  );
}
```

5. Integrar en Layout/Navbar:
   - Agregar NotificationBadge junto al link de notificaciones
   - Al hacer clic, navegar a NotificationsPage y marcar como leídas

### 3. Toast para Nuevas Notificaciones

Ubicación: src/components/NotificationToast.tsx (crear)

Requisitos:

1. Mostrar toast cuando llega nueva notificación vía WebSocket:

   - Aparecer en esquina superior derecha
   - Mostrar: icono de app, remitente, monto
   - Auto-dismiss después de 5 segundos
   - Click en toast → navegar a detalle de notificación
   - Sonido opcional (configurable en settings)

2. Implementación con WebSockets:

```typescript
// src/hooks/useNewNotifications.ts
import { useEffect } from "react";
import { echo } from "@/services/echo";
import type { Notification } from "@/types";

export function useNewNotifications(
  onNewNotification: (notification: Notification) => void
) {
  const commerceId = useCommerceId();

  useEffect(() => {
    if (!commerceId || !echo) return;

    const channel = echo.private(`commerce.${commerceId}`);

    // Escuchar evento de notificación creada
    channel.listen(".notification.created", (data: Notification) => {
      console.log("Nueva notificación recibida vía WebSocket:", data);
      onNewNotification(data);
    });

    return () => {
      channel.stopListening(".notification.created");
    };
  }, [commerceId, onNewNotification]);
}
```

**Ventaja:** Con WebSockets, el toast aparece instantáneamente cuando se crea la notificación, sin necesidad de polling.

3. Componente Toast:

```typescript
// src/components/NotificationToast.tsx
import { X } from "lucide-react";
import { useEffect } from "react";

interface NotificationToastProps {
  notification: Notification;
  onClose: () => void;
  onClick: () => void;
}

export default function NotificationToast({
  notification,
  onClose,
  onClick,
}: NotificationToastProps) {
  useEffect(() => {
    const timer = setTimeout(onClose, 5000);
    return () => clearTimeout(timer);
  }, [onClose]);

  return (
    <div
      className="bg-white shadow-lg rounded-lg p-4 mb-2 cursor-pointer hover:shadow-xl transition-shadow"
      onClick={onClick}
    >
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <div className="font-semibold">{notification.source_app}</div>
          <div className="text-sm text-gray-600">
            {notification.sender_name}
          </div>
          <div className="text-lg font-bold text-green-600">
            {notification.currency} {notification.amount}
          </div>
        </div>
        <button
          onClick={(e) => {
            e.stopPropagation();
            onClose();
          }}
        >
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}
```

4. Integrar en App.tsx o Layout:
   - Usar useNewNotifications hook
   - Mostrar toast cuando llega nueva notificación
   - Gestionar cola de toasts (máximo 3 visibles)

### 4. Mejorar Filtros según Diseños

Ubicación: src/pages/NotificationsPage.tsx (modificar)

Requisitos:

1. Filtros tipo chips más visibles:

   - Chips más grandes con mejor contraste
   - Chip activo: fondo púrpura, texto blanco
   - Chips inactivos: fondo gris claro, texto gris oscuro
   - Hover effect en chips
   - Scroll horizontal si hay muchos filtros

2. Implementación mejorada:

```typescript
// Componente FilterChips
const FilterChips = ({ filters, onFilterChange }) => {
  const filterOptions = [
    { key: "all", label: "Todos", icon: Filter },
    { key: "today", label: "Hoy", icon: Calendar },
    // ... más opciones
  ];

  return (
    <div className="flex gap-2 overflow-x-auto pb-2">
      {filterOptions.map((option) => (
        <button
          key={option.key}
          onClick={() => onFilterChange(option.key)}
          className={`
            px-4 py-2 rounded-full flex items-center gap-2 whitespace-nowrap
            transition-colors
            ${
              filters.active === option.key
                ? "bg-purple-600 text-white"
                : "bg-gray-100 text-gray-700 hover:bg-gray-200"
            }
          `}
        >
          <option.icon className="w-4 h-4" />
          {option.label}
        </button>
      ))}
    </div>
  );
};
```

3. Filtros por dispositivo y app:
   - Dropdowns mejorados con búsqueda
   - Mostrar nombre del dispositivo/app seleccionado en el chip
   - Permitir múltiples selecciones

### 5. Mejorar Búsqueda con Autocompletado

Ubicación: src/components/SearchBar.tsx (crear o modificar)

Requisitos:

1. Búsqueda mejorada:

   - Autocompletado mientras escribe
   - Sugerencias basadas en:
     - Nombres de remitentes
     - Montos
     - Aliases de dispositivos
   - Debounce de 300ms para evitar demasiadas requests
   - Highlight de texto encontrado en resultados

2. Implementación:

```typescript
// src/hooks/useSearchSuggestions.ts
import { useQuery } from "@tanstack/react-query";
import { apiService } from "@/services/api";

export function useSearchSuggestions(query: string) {
  return useQuery({
    queryKey: ["notifications", "search-suggestions", query],
    queryFn: () => apiService.searchNotifications(query),
    enabled: query.length >= 2, // Solo buscar si hay al menos 2 caracteres
    staleTime: 5000,
  });
}
```

3. Componente SearchBar:

```typescript
// src/components/SearchBar.tsx
import { Search, X } from "lucide-react";
import { useState } from "react";
import { useSearchSuggestions } from "@/hooks/useSearchSuggestions";

export default function SearchBar({ onSearch }) {
  const [query, setQuery] = useState("");
  const [showSuggestions, setShowSuggestions] = useState(false);
  const { data: suggestions } = useSearchSuggestions(query);

  return (
    <div className="relative">
      <div className="relative">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
        <input
          type="text"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setShowSuggestions(true);
            onSearch(e.target.value);
          }}
          placeholder="Buscar transacción, alias o monto..."
          className="w-full pl-10 pr-10 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
        />
        {query && (
          <button
            onClick={() => {
              setQuery("");
              onSearch("");
              setShowSuggestions(false);
            }}
            className="absolute right-3 top-1/2 transform -translate-y-1/2"
          >
            <X className="w-5 h-5 text-gray-400" />
          </button>
        )}
      </div>

      {showSuggestions && suggestions && suggestions.length > 0 && (
        <div className="absolute z-10 w-full mt-1 bg-white border rounded-lg shadow-lg">
          {suggestions.map((suggestion) => (
            <div
              key={suggestion.id}
              onClick={() => {
                setQuery(suggestion.sender_name || "");
                onSearch(suggestion.sender_name || "");
                setShowSuggestions(false);
              }}
              className="p-2 hover:bg-gray-100 cursor-pointer"
            >
              {suggestion.sender_name} - {suggestion.currency}{" "}
              {suggestion.amount}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
```

### 6. Estados Vacíos Mejorados

Ubicación: src/components/EmptyState.tsx (crear)

Requisitos:

1. Componente de estado vacío:

   - Icono grande
   - Título descriptivo
   - Mensaje de ayuda
   - Acción sugerida (botón)

2. Implementación:

```typescript
// src/components/EmptyState.tsx
import { Inbox } from "lucide-react";

interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  message: string;
  action?: {
    label: string;
    onClick: () => void;
  };
}

export default function EmptyState({
  icon,
  title,
  message,
  action,
}: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-12 px-4">
      {icon || <Inbox className="w-16 h-16 text-gray-400 mb-4" />}
      <h3 className="text-lg font-semibold text-gray-900 mb-2">{title}</h3>
      <p className="text-gray-600 text-center max-w-md mb-4">{message}</p>
      {action && (
        <button
          onClick={action.onClick}
          className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700"
        >
          {action.label}
        </button>
      )}
    </div>
  );
}
```

3. Usar en NotificationsPage:
   - Cuando no hay notificaciones: "No hay notificaciones aún"
   - Cuando filtros no devuelven resultados: "No se encontraron notificaciones con estos filtros"
   - Cuando hay error: "Error al cargar notificaciones"

### 7. Optimización para Móviles

Ubicación: Varios componentes (modificar)

Requisitos:

1. Navegación bottom tabs en móvil:

   - Crear componente MobileBottomNav
   - Mostrar solo en pantallas < 768px
   - Tabs: Notificaciones, Dispositivos, Configuración

2. Responsive design:

   - Cards de notificaciones apiladas en móvil
   - Filtros en drawer en móvil
   - Búsqueda full-width en móvil

3. Implementación:

```typescript
// src/components/MobileBottomNav.tsx
import { Bell, Smartphone, Settings } from "lucide-react";
import { useNavigate, useLocation } from "react-router-dom";

export default function MobileBottomNav() {
  const navigate = useNavigate();
  const location = useLocation();

  const tabs = [
    { path: "/notifications", icon: Bell, label: "Notificaciones" },
    { path: "/devices", icon: Smartphone, label: "Dispositivos" },
    { path: "/settings", icon: Settings, label: "Configuración" },
  ];

  return (
    <div className="md:hidden fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200">
      <div className="flex justify-around">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = location.pathname === tab.path;

          return (
            <button
              key={tab.path}
              onClick={() => navigate(tab.path)}
              className={`
                flex flex-col items-center py-2 px-4
                ${isActive ? "text-purple-600" : "text-gray-400"}
              `}
            >
              <Icon className="w-6 h-6" />
              <span className="text-xs mt-1">{tab.label}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
```

## ENDPOINTS API DISPONIBLES

Todos estos endpoints ya están implementados:

- GET /api/notifications (con query params para filtros y búsqueda)
- GET /api/notifications/{id}
- PUT /api/notifications/{id}/status
- GET /api/devices
- GET /api/app-instances

Ver: apps/api/README.md para documentación completa

## DEPENDENCIAS NECESARIAS

Instalar las siguientes dependencias:

```bash
npm install laravel-echo pusher-js
```

Ya deberían estar instaladas, pero verificar:

```json
{
  "@tanstack/react-query": "^5.0.0",
  "react": "^18.2.0",
  "react-router-dom": "^6.20.0",
  "tailwindcss": "^3.3.0",
  "lucide-react": "^0.300.0",
  "date-fns": "^2.30.0",
  "laravel-echo": "^1.16.0",
  "pusher-js": "^8.4.0"
}
```

## CONFIGURACIÓN DE WEBSOCKETS

### Variables de Entorno Requeridas

**CRÍTICO:** Variables se inyectan en **build time**, no runtime.

**Para Desarrollo:**
Crear archivo `.env` en `apps/web-dashboard/`:

```env
# API Backend
VITE_API_URL=http://localhost:8000

# Laravel Reverb WebSocket Server
VITE_REVERB_APP_KEY=base64:7IFJN3FFdqSdv3nAdxoRmjMxzI5jqDIG33VJ7XC4LOk
VITE_REVERB_HOST=localhost
VITE_REVERB_PORT=8080
VITE_REVERB_SCHEME=http
```

**Para Producción:**
Configurar en Docker Compose o `.env.production`:

```env
# API Backend
VITE_API_URL=https://api.notificaciones.space

# Laravel Reverb WebSocket Server
VITE_REVERB_APP_KEY=base64:7IFJN3FFdqSdv3nAdxoRmjMxzI5jqDIG33VJ7XC4LOk
VITE_REVERB_HOST=api.notificaciones.space  # Dominio público, no localhost
VITE_REVERB_PORT=8080
VITE_REVERB_SCHEME=https  # HTTPS en producción

# Error Tracking (Opcional)
VITE_SENTRY_DSN=https://tu-dsn@sentry.io/proyecto-id
```

**⚠️ IMPORTANTE:**

- `VITE_REVERB_HOST` en producción debe ser el dominio público (ej: `api.notificaciones.space`)
- NO usar `localhost` o `0.0.0.0` en producción
- `VITE_REVERB_SCHEME` debe ser `https` en producción
- Variables se validan en build time, no runtime

**Validación de Variables:**

Crear `src/config/env.ts`:

```typescript
// src/config/env.ts
const requiredEnvVars = [
  "VITE_API_URL",
  "VITE_REVERB_APP_KEY",
  "VITE_REVERB_HOST",
  "VITE_REVERB_PORT",
] as const;

export function validateEnvVars() {
  const missing: string[] = [];

  requiredEnvVars.forEach((varName) => {
    if (!import.meta.env[varName]) {
      missing.push(varName);
    }
  });

  if (missing.length > 0) {
    throw new Error(
      `❌ Faltan variables de entorno requeridas: ${missing.join(", ")}\n` +
        `Por favor, crea un archivo .env con estas variables.`
    );
  }

  console.log("✅ Variables de entorno validadas correctamente");
}

// Validar al importar
validateEnvVars();

// Exportar variables tipadas
export const env = {
  API_URL: import.meta.env.VITE_API_URL!,
  REVERB_APP_KEY: import.meta.env.VITE_REVERB_APP_KEY!,
  REVERB_HOST: import.meta.env.VITE_REVERB_HOST!,
  REVERB_PORT: parseInt(import.meta.env.VITE_REVERB_PORT || "8080"),
  REVERB_SCHEME: import.meta.env.VITE_REVERB_SCHEME || "http",
} as const;
```

**Importar en `src/services/echo.ts`:**

```typescript
import { env } from "@/config/env";

export const echo = new Echo({
  broadcaster: "reverb",
  key: env.REVERB_APP_KEY,
  wsHost: env.REVERB_HOST,
  wsPort: env.REVERB_PORT,
  // ... resto de configuración
});
```

### Estado del Backend

El backend YA tiene implementado:

- ✅ **Event `NotificationCreated`** con broadcasting (`app/Events/NotificationCreated.php`)
- ✅ **Laravel Reverb** configurado (`config/broadcasting.php`)
- ✅ **Canales privados** configurados (`routes/channels.php`): `commerce.{commerceId}`
- ✅ **Autenticación de canales** con Sanctum (verifica `user.commerce_id === commerceId`)
- ✅ **Datos completos** en broadcast: id, user_id, commerce_id, device_id, source_app, package_name, app_instance_id, app_instance_label, device_alias, title, body, amount, currency, payer_name, posted_at, received_at, status, is_duplicate, created_at
- ✅ **Evento nombre**: `notification.created` (broadcastAs)

**Estructura del Evento Broadcast:**

```typescript
// El backend envía este objeto cuando se crea una notificación:
{
  notification: {
    id: number;
    user_id: number;
    commerce_id: number;
    device_id: number;
    source_app: string;
    package_name: string | null;
    app_instance_id: number | null;
    app_instance_label: string | null;
    device_alias: string | null;
    title: string;
    body: string;
    amount: number | null;
    currency: string | null;
    payer_name: string | null;
    posted_at: string | null; // ISO8601
    received_at: string; // ISO8601
    status: "pending" | "validated" | "inconsistent";
    is_duplicate: boolean;
    created_at: string; // ISO8601
  }
}
```

**Solo necesitas conectarte desde el frontend usando Laravel Echo.**

## CONSIDERACIONES DE CALIDAD Y DEVOPS

### Tests Automatizados

**Requisitos:**

1. Tests unitarios con Jest:

   - Crear: `src/__tests__/hooks/useNotifications.test.ts`
   - Testear lógica de hooks, filtros, búsqueda
   - Cobertura mínima: 70% de hooks y utils

2. Tests de componentes con React Testing Library:

   - Crear: `src/__tests__/components/NotificationBadge.test.tsx`
   - Testear renderizado, interacciones, estados

3. Tests E2E con Playwright (opcional pero recomendado):

   - Testear flujos críticos completos
   - Validar integración con API

4. Configurar CI/CD:
   - Crear: `.github/workflows/web-ci.yml`
   - Ejecutar tests en cada PR
   - Build automático en cada commit

**Ejemplo de test:**

```typescript
// useNotifications.test.ts
describe("useNotifications", () => {
  it("should fetch notifications with filters", async () => {
    const { result } = renderHook(() => useNotifications({ device_id: 1 }));
    await waitFor(() => expect(result.current.data).toBeDefined());
  });
});
```

### Variables de Entorno

**Requisitos:**

1. NO hardcodear URLs o keys:

   - Usar `import.meta.env.VITE_API_URL`
   - Usar `import.meta.env.VITE_REVERB_APP_KEY`
   - Crear `.env.example` con todas las variables

2. Validar variables requeridas:

   ```typescript
   // src/config/env.ts
   const requiredEnvVars = ["VITE_API_URL", "VITE_REVERB_APP_KEY"];
   requiredEnvVars.forEach((varName) => {
     if (!import.meta.env[varName]) {
       throw new Error(`Missing required env var: ${varName}`);
     }
   });
   ```

3. Documentar en README:
   - Lista completa de variables
   - Valores de ejemplo
   - Dónde obtener valores de producción

### Error Tracking

**Requisitos:**

1. **Logger estructurado implementado** (✅ `src/services/logger.ts`):

   - Logging con niveles (debug, info, warn, error)
   - Automáticamente deshabilita logs en producción
   - Preparado para integración con Sentry

2. **Integrar Sentry (opcional):**

   - Ver `ERROR_TRACKING.md` para guía completa
   - Plan free: 5,000 eventos/mes
   - Alternativa: GlitchTip self-hosted (gratis, ilimitado)
   - Solo requiere agregar DSN: `VITE_SENTRY_DSN`

3. **Capturar errores automáticamente:**
   ```typescript
   // Ya implementado en logger.ts
   logger.error("Error de WebSocket", error, { context });
   // Automáticamente envía a Sentry si está configurado
   ```

### Performance Monitoring

**Requisitos:**

1. Web Vitals:

   - Integrar `web-vitals` library
   - Enviar métricas a analytics
   - Alertar si métricas degradan

2. Code Splitting:
   - Lazy load de rutas
   - Lazy load de componentes pesados
   - Optimizar bundle size

### CI/CD Pipeline

**Requisitos:**

1. Crear `.github/workflows/web-ci.yml`:

   ```yaml
   name: Web Dashboard CI
   on: [push, pull_request]
   jobs:
     test:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v3
         - uses: actions/setup-node@v3
         - run: npm ci
         - run: npm run test
         - run: npm run build
   ```

2. Deployment automático:
   - Deploy a staging en cada merge a `develop`
   - Deploy a producción con approval manual
   - Rollback automático si health check falla

### Security

**Requisitos:**

1. Security headers:

   - CSP (Content Security Policy)
   - XSS protection
   - HTTPS only cookies

2. Validación de entrada:
   - Sanitizar inputs de búsqueda
   - Validar filtros antes de enviar a API
   - Manejar XSS en datos del servidor

## CRITERIOS DE ACEPTACIÓN

### Funcionalidad WebSockets:

1. ✅ **WebSockets conectado y funcionando** (Laravel Echo + Reverb)
2. ✅ **Feed se actualiza instantáneamente** (< 1 segundo) cuando llega nueva notificación
3. ✅ **Reconexión automática** con backoff exponencial si se pierde conexión
4. ✅ **Máximo 5 intentos de reconexión** antes de mostrar error
5. ✅ **Actualización optimista** del cache (sin refetch innecesario)
6. ✅ **Prevención de duplicados** verificando IDs de notificaciones
7. ✅ **Indicador visual de estado** de conexión (conectado/desconectado/conectando/error)
8. ✅ **Manejo robusto de errores** sin crashear la app
9. ✅ **Limpieza adecuada** de suscripciones al desmontar componentes
10. ✅ **Sincronización con React Query** para mantener consistencia

### Funcionalidad UX:

11. ✅ **Badge muestra cantidad de no leídas** correctamente
12. ✅ **Badge se actualiza automáticamente** vía WebSocket
13. ✅ **Toast aparece instantáneamente** cuando llega nueva notificación
14. ✅ **Toast navega a detalle** al hacer clic
15. ✅ **Filtros tipo chips** son más visibles y funcionales
16. ✅ **Búsqueda tiene autocompletado** con debounce
17. ✅ **Estados vacíos** son informativos y útiles
18. ✅ **Diseño responsive** funciona en móvil
19. ✅ **Navegación bottom tabs** funciona en móvil
20. ✅ **No hay flickering** al actualizar notificaciones

### Configuración y Validación:

21. ✅ **Variables de entorno validadas** al iniciar la app
22. ✅ **Mensajes de error claros** si faltan variables de entorno
23. ✅ **Configuración tipada** con TypeScript
24. ✅ **Documentación completa** de variables de entorno en README

### Calidad y DevOps:

25. ✅ **Tests unitarios** implementados (cobertura mínima 70%)
26. ✅ **Tests de componentes** implementados (React Testing Library)
27. ✅ **Tests de integración** para WebSockets (mock de Echo)
28. ✅ **CI/CD pipeline** configurado y funcionando
29. ✅ **Error tracking** preparado (logger estructurado, Sentry opcional - ver ERROR_TRACKING.md)
30. ✅ **Performance monitoring** implementado (Web Vitals)
31. ✅ **Security headers** configurados (CSP, XSS protection)
32. ✅ **Code splitting** implementado para optimizar bundle size
33. ✅ **Health check** implementado (`src/services/healthCheck.ts`)
34. ✅ **Manejo de token expirado** en WebSocket
35. ✅ **Logging estructurado** (`src/services/logger.ts`)

## NOTAS IMPORTANTES

- **CRÍTICO:** Usar WebSockets (Laravel Echo) para tiempo real, NO polling
- El backend YA tiene WebSockets implementados, solo necesitas conectarte
- Usar React Query para queries iniciales (carga de datos)
- WebSockets actualiza el cache de React Query automáticamente
- Implementar debounce para búsqueda (300ms)
- Considerar usar useMemo para optimizar renders
- Agregar indicador visual de estado de conexión WebSocket
- Manejar estados de error apropiadamente (desconexión, fallo de autenticación)
- Implementar reconexión automática si se pierde conexión
- Probar en diferentes tamaños de pantalla
- Optimizar para performance (lazy loading, code splitting)
- Seguir las convenciones de código existentes
- **NUEVO:** Implementar tests automatizados antes de considerar completo
- **NUEVO:** Configurar CI/CD para validación automática
- **NUEVO:** Documentar y validar variables de entorno (VITE*REVERB*\*)
- **NUEVO:** Error tracking preparado (ver ERROR_TRACKING.md para implementar Sentry)
- **NUEVO:** Monitorear performance con Web Vitals
- **NUEVO:** Health check implementado para diagnóstico
- **NUEVO:** Manejo robusto de token expirado en WebSocket
- **NUEVO:** Logging estructurado para producción
- **NUEVO:** Ver GUIA_PRODUCCION.md para pasos de deployment

```

---

## PROMPT 3: API - WebSockets para Tiempo Real

```

# PROMPT: Implementar WebSockets para Notificaciones en Tiempo Real - Yape Notifier API

## CONTEXTO DEL PROYECTO

Eres un desarrollador trabajando en el backend Laravel de Yape Notifier. Necesitas implementar WebSockets para que el dashboard web reciba notificaciones en tiempo real sin necesidad de polling constante.

Stack Tecnológico:

- Laravel 11
- PHP 8.2+
- Laravel Reverb (WebSocket server nativo de Laravel) - RECOMENDADO
- O Pusher (servicio externo) - Alternativa
- Broadcasting Events
- Redis (para broadcasting)
- Sanctum (autenticación ya implementada)

Estado Actual:

- ✅ API REST completa implementada
- ✅ Modelo Notification implementado
- ✅ NotificationService crea notificaciones correctamente
- ✅ Autenticación con Sanctum funcionando
- ✅ Multi-tenant con Commerce implementado
- ❌ No hay broadcasting de eventos
- ❌ No hay WebSocket server configurado

Estructura del Proyecto:

- app/
  - Http/Controllers/NotificationController.php
  - Services/NotificationService.php
  - Models/Notification.php
  - Events/ (vacío, crear aquí)
  - Broadcast/ (vacío, crear aquí)

## TAREAS CRÍTICAS

### 1. Instalar y Configurar Laravel Reverb

Requisitos:

1. Instalar Laravel Reverb:

```bash
composer require laravel/reverb
php artisan reverb:install
```

2. Configurar en config/reverb.php:

```php
return [
    'id' => env('REVERB_APP_ID', 'yape-notifier'),
    'key' => env('REVERB_APP_KEY'),
    'secret' => env('REVERB_APP_SECRET'),
    'app_id' => env('REVERB_APP_ID'),
    'options' => [
        'host' => env('REVERB_HOST', '127.0.0.1'),
        'port' => env('REVERB_PORT', 8080),
        'scheme' => env('REVERB_SCHEME', 'http'),
        'useTLS' => env('REVERB_SCHEME', 'http') === 'https',
    ],
];
```

3. Variables de entorno (.env):

```env
REVERB_APP_ID=yape-notifier
REVERB_APP_KEY=base64:tu-key-generada
REVERB_APP_SECRET=tu-secret-generado
REVERB_HOST=0.0.0.0
REVERB_PORT=8080
REVERB_SCHEME=http
```

4. Configurar broadcasting en config/broadcasting.php:

```php
'connections' => [
    'reverb' => [
        'driver' => 'reverb',
        'key' => env('REVERB_APP_KEY'),
        'secret' => env('REVERB_APP_SECRET'),
        'app_id' => env('REVERB_APP_ID'),
        'options' => [
            'host' => env('REVERB_HOST', '127.0.0.1'),
            'port' => env('REVERB_PORT', 8080),
            'scheme' => env('REVERB_SCHEME', 'http'),
            'useTLS' => env('REVERB_SCHEME', 'http') === 'https',
        ],
    ],
    // ... otras conexiones
],
```

5. Configurar BROADCAST_DRIVER en .env:

```env
BROADCAST_DRIVER=reverb
```

### 2. Crear Event de Broadcasting para Notificaciones

Ubicación: app/Events/NotificationCreated.php

Requisitos:

1. Crear Event que implemente ShouldBroadcast:

```php
<?php

namespace App\Events;

use App\Models\Notification;
use Illuminate\Broadcasting\Channel;
use Illuminate\Broadcasting\InteractsWithSockets;
use Illuminate\Broadcasting\PresenceChannel;
use Illuminate\Broadcasting\PrivateChannel;
use Illuminate\Contracts\Broadcasting\ShouldBroadcast;
use Illuminate\Foundation\Events\Dispatchable;
use Illuminate\Queue\SerializesModels;

class NotificationCreated implements ShouldBroadcast
{
    use Dispatchable, InteractsWithSockets, SerializesModels;

    public Notification $notification;

    /**
     * Create a new event instance.
     */
    public function __construct(Notification $notification)
    {
        $this->notification = $notification;
    }

    /**
     * Get the channels the event should broadcast on.
     */
    public function broadcastOn(): array
    {
        // Canal privado por commerce para seguridad multi-tenant
        return [
            new PrivateChannel('commerce.' . $this->notification->commerce_id),
        ];
    }

    /**
     * The event's broadcast name.
     */
    public function broadcastAs(): string
    {
        return 'notification.created';
    }

    /**
     * Get the data to broadcast.
     */
    public function broadcastWith(): array
    {
        return [
            'id' => $this->notification->id,
            'sender_name' => $this->notification->sender_name,
            'amount' => $this->notification->amount,
            'currency' => $this->notification->currency,
            'source_app' => $this->notification->source_app,
            'app_instance_label' => $this->notification->appInstance?->instance_label,
            'device_alias' => $this->notification->device?->alias,
            'status' => $this->notification->status,
            'received_at' => $this->notification->received_at->toIso8601String(),
            'created_at' => $this->notification->created_at->toIso8601String(),
        ];
    }
}
```

2. Importar modelo Notification y relaciones necesarias

### 3. Disparar Event al Crear Notificación

Ubicación: app/Services/NotificationService.php (modificar método createNotification)

Requisitos:

1. Importar el Event:

```php
use App\Events\NotificationCreated;
```

2. Modificar método createNotification:

```php
public function createNotification(array $data): Notification
{
    // Validar datos (ya existe)
    $validated = $this->validateNotificationData($data);

    // Crear notificación (lógica existente)
    $notification = Notification::create([
        'commerce_id' => $validated['commerce_id'],
        'device_id' => $validated['device_id'],
        'package_name' => $validated['package_name'],
        'android_user_id' => $validated['android_user_id'] ?? null,
        'android_uid' => $validated['android_uid'] ?? null,
        'app_instance_id' => $validated['app_instance_id'] ?? null,
        'title' => $validated['title'],
        'body' => $validated['body'],
        'sender_name' => $validated['sender_name'] ?? null,
        'amount' => $validated['amount'] ?? null,
        'currency' => $validated['currency'] ?? null,
        'source_app' => $validated['source_app'],
        'status' => $validated['status'] ?? 'pending',
        'posted_at' => $validated['posted_at'] ?? now(),
        'received_at' => now(),
    ]);

    // Cargar relaciones para el evento
    $notification->load(['appInstance', 'device']);

    // Disparar evento de broadcasting
    broadcast(new NotificationCreated($notification))->toOthers();

    return $notification;
}
```

3. Asegurar que solo se envíe a usuarios del mismo commerce (ya manejado por PrivateChannel)

### 4. Configurar Autenticación de Canales Privados

Ubicación: routes/channels.php

Requisitos:

1. Crear autorización para canal privado de commerce:

```php
<?php

use Illuminate\Support\Facades\Broadcast;

Broadcast::channel('commerce.{commerceId}', function ($user, $commerceId) {
    // Verificar que el usuario pertenezca al commerce
    return (int) $user->commerce_id === (int) $commerceId;
});
```

2. Asegurar que el usuario esté autenticado (middleware ya aplicado por Laravel)

### 5. Configurar Redis para Broadcasting (Opcional pero Recomendado)

Requisitos:

1. Si usas Redis para broadcasting (mejor performance):

```env
BROADCAST_DRIVER=redis
REDIS_HOST=127.0.0.1
REDIS_PASSWORD=null
REDIS_PORT=6379
```

2. Instalar predis si no está:

```bash
composer require predis/predis
```

3. Configurar en config/database.php (ya debería estar configurado)

### 6. Iniciar Servidor Reverb

Requisitos:

1. Comando para desarrollo:

```bash
php artisan reverb:start
```

2. Para producción, usar supervisor o systemd:

```ini
# /etc/supervisor/conf.d/reverb.conf
[program:reverb]
process_name=%(program_name)s_%(process_num)02d
command=php /ruta/a/artisan reverb:start
autostart=true
autorestart=true
user=www-data
numprocs=1
redirect_stderr=true
stdout_logfile=/ruta/a/storage/logs/reverb.log
```

### 7. Endpoint para Autenticación WebSocket (Cliente)

El cliente necesita autenticarse antes de suscribirse a canales privados.

Ubicación: Ya manejado por Laravel, pero verificar configuración

Requisitos:

1. El cliente debe enviar token de autenticación al conectarse
2. Laravel valida el token usando Sanctum
3. Si es válido, permite suscripción al canal

Código del cliente (para referencia, no implementar en backend):

```javascript
// El cliente debe conectarse así:
import Echo from "laravel-echo";
import Pusher from "pusher-js";

window.Pusher = Pusher;

window.Echo = new Echo({
  broadcaster: "reverb",
  key: import.meta.env.VITE_REVERB_APP_KEY,
  wsHost: import.meta.env.VITE_REVERB_HOST,
  wsPort: import.meta.env.VITE_REVERB_PORT,
  wssPort: import.meta.env.VITE_REVERB_PORT,
  forceTLS: (import.meta.env.VITE_REVERB_SCHEME ?? "https") === "https",
  enabledTransports: ["ws", "wss"],
  auth: {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  },
});

// Suscribirse al canal privado
Echo.private(`commerce.${commerceId}`).listen(".notification.created", (e) => {
  console.log("Nueva notificación:", e);
  // Actualizar UI
});
```

### 8. Eventos Adicionales (Opcional)

Crear eventos para otras acciones:

Ubicación: app/Events/

1. NotificationStatusUpdated.php:

```php
class NotificationStatusUpdated implements ShouldBroadcast
{
    public Notification $notification;

    public function __construct(Notification $notification)
    {
        $this->notification = $notification;
    }

    public function broadcastOn(): array
    {
        return [
            new PrivateChannel('commerce.' . $this->notification->commerce_id),
        ];
    }

    public function broadcastAs(): string
    {
        return 'notification.status.updated';
    }
}
```

2. DeviceStatusUpdated.php (para cambios en estado de dispositivos)

### 9. Testing

Ubicación: tests/Feature/NotificationBroadcastingTest.php

Requisitos:

1. Test básico:

```php
<?php

namespace Tests\Feature;

use App\Events\NotificationCreated;
use App\Models\Notification;
use App\Models\User;
use App\Models\Commerce;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Event;
use Tests\TestCase;

class NotificationBroadcastingTest extends TestCase
{
    use RefreshDatabase;

    public function test_notification_created_event_is_broadcasted(): void
    {
        Event::fake();

        $commerce = Commerce::factory()->create();
        $user = User::factory()->create(['commerce_id' => $commerce->id]);

        $notification = Notification::factory()->create([
            'commerce_id' => $commerce->id,
        ]);

        broadcast(new NotificationCreated($notification));

        Event::assertDispatched(NotificationCreated::class);
    }

    public function test_notification_is_broadcasted_to_correct_channel(): void
    {
        $commerce = Commerce::factory()->create();
        $notification = Notification::factory()->create([
            'commerce_id' => $commerce->id,
        ]);

        $event = new NotificationCreated($notification);
        $channels = $event->broadcastOn();

        $this->assertCount(1, $channels);
        $this->assertInstanceOf(PrivateChannel::class, $channels[0]);
        $this->assertEquals('commerce.' . $commerce->id, $channels[0]->name);
    }
}
```

## VARIABLES DE ENTORNO NECESARIAS

Agregar a .env:

```env
# Reverb WebSocket Server
REVERB_APP_ID=yape-notifier
REVERB_APP_KEY=base64:generar-con-php-artisan-reverb:install
REVERB_APP_SECRET=generar-con-php-artisan-reverb:install
REVERB_HOST=0.0.0.0
REVERB_PORT=8080
REVERB_SCHEME=http

# Broadcasting
BROADCAST_DRIVER=reverb
```

Para producción, cambiar REVERB_SCHEME a https y configurar SSL.

## CONSIDERACIONES DE CALIDAD Y DEVOPS

### Tests de Integración

**Requisitos:**

1. Tests para eventos de broadcasting:

   - Crear: `tests/Feature/NotificationBroadcastingTest.php`
   - Testear que eventos se disparan correctamente
   - Testear que eventos llegan al canal correcto
   - Testear autenticación de canales

2. Tests para servidor Reverb:
   - Testear conexión/desconexión
   - Testear reconexión automática
   - Testear manejo de errores

**Ejemplo de test:**

```php
public function test_notification_broadcasts_to_correct_channel(): void
{
    Event::fake();

    $notification = Notification::factory()->create();
    broadcast(new NotificationCreated($notification));

    Event::assertDispatched(NotificationCreated::class, function ($event) use ($notification) {
        return $event->notification->id === $notification->id;
    });
}
```

### Rate Limiting

**Requisitos:**

1. Implementar rate limiting para eventos:

   ```php
   // app/Http/Middleware/ThrottleBroadcasting.php
   public function handle($request, Closure $next)
   {
       $key = 'broadcast:' . $request->user()->id;
       if (RateLimiter::tooManyAttempts($key, 100)) {
           return response()->json(['error' => 'Too many requests'], 429);
       }
       RateLimiter::hit($key, 60); // 100 requests per minute
       return $next($request);
   }
   ```

2. Configurar límites por entorno:
   - Desarrollo: límites altos
   - Producción: límites estrictos

### Monitoring y Métricas

**Requisitos:**

1. Métricas de conexiones:

   - Conexiones activas
   - Conexiones por commerce
   - Tasa de desconexiones

2. Métricas de eventos:

   - Eventos broadcast por minuto
   - Latencia de eventos
   - Tasa de errores

3. Health check endpoint:
   ```php
   // routes/api.php
   Route::get('/health/reverb', function () {
       return response()->json([
           'status' => 'ok',
           'connections' => Reverb::getConnectionCount(),
       ]);
   });
   ```

### Logging Estructurado

**Requisitos:**

1. Logging de eventos importantes:

   ```php
   Log::info('WebSocket connection established', [
       'user_id' => $user->id,
       'commerce_id' => $user->commerce_id,
       'channel' => $channel,
   ]);
   ```

2. Logging de errores con contexto:

   ```php
   Log::error('WebSocket connection failed', [
       'user_id' => $user->id,
       'error' => $exception->getMessage(),
       'trace' => $exception->getTraceAsString(),
   ]);
   ```

3. Integrar con sistema de logging centralizado (opcional):
   - ELK Stack
   - CloudWatch
   - Datadog

### CI/CD

**Requisitos:**

1. Tests automáticos:

   - Ejecutar tests de broadcasting en CI
   - Validar configuración de Reverb
   - Verificar que eventos funcionan

2. Deployment:
   - Validar configuración antes de deploy
   - Health check después de deploy
   - Rollback automático si falla

### Graceful Shutdown

**Requisitos:**

1. Manejar señales de terminación:

   ```php
   // En el proceso de Reverb
   pcntl_signal(SIGTERM, function() {
       // Cerrar conexiones gracefully
       // Guardar estado
       exit(0);
   });
   ```

2. Notificar a clientes antes de cerrar:
   - Enviar mensaje de "servidor reiniciando"
   - Dar tiempo para reconexión

## CRITERIOS DE ACEPTACIÓN

Funcionalidad:

1. ✅ Laravel Reverb instalado y configurado correctamente
2. ✅ Event NotificationCreated se dispara al crear notificación
3. ✅ Event se transmite al canal privado correcto
4. ✅ Solo usuarios del mismo commerce reciben eventos
5. ✅ Autenticación de canales funciona correctamente
6. ✅ Servidor Reverb inicia sin errores
7. ✅ Cliente puede conectarse y recibir eventos
8. ✅ Manejo de reconexión automática (manejado por cliente)

Calidad y DevOps: 9. ✅ Tests de integración implementados y pasando 10. ✅ Rate limiting implementado y configurado 11. ✅ Monitoring y métricas configurados 12. ✅ Health check endpoint implementado 13. ✅ Logging estructurado implementado 14. ✅ CI/CD pipeline configurado 15. ✅ Graceful shutdown implementado 16. ✅ Documentación actualizada

## NOTAS IMPORTANTES

- Usar canales privados para seguridad multi-tenant
- **CRÍTICO:** Implementar rate limiting para prevenir abuso
- Considerar usar Redis para mejor performance en producción
- Configurar SSL/TLS para producción (wss://)
- **CRÍTICO:** Monitorear conexiones WebSocket (logs, métricas)
- Documentar configuración en README.md
- Probar con múltiples clientes conectados simultáneamente
- Manejar desconexiones y reconexiones apropiadamente
- Considerar usar queue para broadcasting si hay muchos eventos
- **NUEVO:** Implementar tests de integración para WebSockets
- **NUEVO:** Configurar monitoring y alertas
- **NUEVO:** Implementar health checks para producción
- **NUEVO:** Logging estructurado para debugging

## ALTERNATIVA: Pusher

Si prefieres usar Pusher en lugar de Reverb:

1. Crear cuenta en Pusher.com
2. Instalar: composer require pusher/pusher-php-server
3. Configurar credenciales en .env
4. Cambiar BROADCAST_DRIVER=pusher
5. El resto de la implementación es similar

Reverb es recomendado porque es nativo de Laravel y no requiere servicio externo.

````

---

## INSTRUCCIONES DE USO

1. **Copiar el prompt completo**: Incluye desde el inicio del bloque de código (```) hasta el final (```)
2. **Cada prompt es independiente**: Puedes usar uno sin los otros
3. **Orden recomendado**:
   - Primero: Android App (más crítico)
   - Segundo: Dashboard Web (mejoras UX)
   - Tercero: API WebSockets (opcional, mejora performance)

---

**Última actualización**: 2025-01-27



````
