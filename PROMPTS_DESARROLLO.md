# Prompts de Desarrollo - Yape Notifier

Este documento contiene los prompts listos para copiar y pegar para desarrollar las funcionalidades pendientes de cada proyecto.

---

## PROMPT 1: Android App - Módulo Admin Móvil y Mejoras Captador

```
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

7. ViewModel: AdminPanelViewModel.kt
   - LiveData/StateFlow para lista de notificaciones
   - LiveData/StateFlow para filtros activos
   - LiveData/StateFlow para estado de carga
   - Función loadNotifications(filters: NotificationFilters)
   - Función searchNotifications(query: String)
   - Función markAsRead(notificationId: Int)
   - Función markAllAsRead()
   - Función loadMore() para paginación

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

Mejoras Captador:
13. ✅ Detecta automáticamente instancias múltiples
14. ✅ Permite asignar nombres a instancias
15. ✅ Sincroniza con backend correctamente
16. ✅ Lista de apps carga desde API
17. ✅ Switches actualizan estado en tiempo real
18. ✅ Wizard detecta OEM y muestra guía específica

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
- WebSockets o Polling (implementar primero polling, luego WebSockets)

Estado Actual:
- ✅ Feed de notificaciones implementado (NotificationsPage.tsx)
- ✅ Filtros funcionando (dispositivo, app, instancia, fechas, estado)
- ✅ Paginación implementada
- ✅ Gestión de dispositivos completa
- ✅ Gestión de instancias completa
- ✅ Estadísticas y KPIs
- ❌ No hay actualización automática (requiere refresh manual)
- ❌ No hay badge de notificaciones no leídas
- ⚠️ UX no coincide completamente con diseños (filtros, búsqueda, estados vacíos)

Estructura del Proyecto:
- src/
  - pages/ (NotificationsPage.tsx, DevicesPage.tsx, etc.)
  - components/ (componentes reutilizables)
  - hooks/ (custom hooks)
  - services/ (apiService.ts)
  - types/ (TypeScript types)
  - contexts/ (AuthContext, etc.)

## TAREAS CRÍTICAS

### 1. Implementar Actualización en Tiempo Real con Polling

Ubicación: src/hooks/useNotifications.ts (crear o modificar)

Requisitos:
1. Usar React Query para polling inteligente:
   - Poll cada 10 segundos cuando la página está activa
   - NO hacer polling cuando el tab está en background
   - Usar document.visibilityState para detectar visibilidad
   - Pausar polling cuando el usuario está escribiendo en búsqueda

2. Implementación:
```typescript
import { useQuery } from '@tanstack/react-query';
import { apiService } from '@/services/api';
import type { NotificationFilters, PaginatedResponse, Notification } from '@/types';

export function useNotifications(filters: NotificationFilters, options?: {
  enabled?: boolean;
  refetchInterval?: number;
}) {
  const isVisible = useDocumentVisibility();
  
  return useQuery<PaginatedResponse<Notification>>({
    queryKey: ['notifications', filters],
    queryFn: () => apiService.getNotifications(filters),
    refetchInterval: (query) => {
      // Solo hacer polling si el tab está visible y no hay errores
      if (!isVisible || query.state.error) return false;
      return options?.refetchInterval ?? 10000; // 10 segundos por defecto
    },
    refetchIntervalInBackground: false,
    staleTime: 5000, // Considerar datos frescos por 5 segundos
    enabled: options?.enabled !== false,
  });
}
```

3. Hook para detectar visibilidad del documento:
```typescript
// src/hooks/useDocumentVisibility.ts
import { useState, useEffect } from 'react';

export function useDocumentVisibility(): boolean {
  const [isVisible, setIsVisible] = useState(!document.hidden);
  
  useEffect(() => {
    const handleVisibilityChange = () => {
      setIsVisible(!document.hidden);
    };
    
    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, []);
  
  return isVisible;
}
```

4. Actualizar NotificationsPage.tsx:
   - Reemplazar llamada directa a API con useNotifications hook
   - Agregar indicador visual de "sincronizando..." cuando está haciendo polling
   - Mostrar timestamp de última actualización

### 2. Badge de Notificaciones No Leídas

Ubicación: src/components/NotificationBadge.tsx (crear)

Requisitos:
1. Componente de badge:
   - Mostrar contador de notificaciones no leídas
   - Actualizar automáticamente cuando llegan nuevas
   - Mostrar en header/navbar junto al icono de notificaciones
   - Animación cuando cambia el número

2. Lógica:
   - Contar notificaciones con status='pending' o is_read=false
   - Usar React Query para mantener contador actualizado
   - Persistir en localStorage como backup

3. Implementación:
```typescript
// src/hooks/useUnreadNotificationsCount.ts
import { useQuery } from '@tanstack/react-query';
import { apiService } from '@/services/api';

export function useUnreadNotificationsCount() {
  return useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: async () => {
      const response = await apiService.getNotifications({
        per_page: 1,
        page: 1,
        status: 'pending',
      });
      return response.total; // Total de notificaciones pendientes
    },
    refetchInterval: 10000, // Actualizar cada 10 segundos
  });
}
```

4. Componente:
```typescript
// src/components/NotificationBadge.tsx
import { Bell } from 'lucide-react';
import { useUnreadNotificationsCount } from '@/hooks/useUnreadNotificationsCount';

export default function NotificationBadge() {
  const { data: count = 0 } = useUnreadNotificationsCount();
  
  return (
    <div className="relative">
      <Bell className="w-6 h-6" />
      {count > 0 && (
        <span className="absolute -top-2 -right-2 bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">
          {count > 99 ? '99+' : count}
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
1. Mostrar toast cuando llega nueva notificación:
   - Aparecer en esquina superior derecha
   - Mostrar: icono de app, remitente, monto
   - Auto-dismiss después de 5 segundos
   - Click en toast → navegar a detalle de notificación
   - Sonido opcional (configurable en settings)

2. Implementación:
```typescript
// src/hooks/useNewNotifications.ts
import { useQuery } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';
import { apiService } from '@/services/api';

export function useNewNotifications(onNewNotification: (notification: Notification) => void) {
  const previousIdsRef = useRef<Set<number>>(new Set());
  
  const { data } = useQuery({
    queryKey: ['notifications', 'latest'],
    queryFn: () => apiService.getNotifications({ per_page: 5, page: 1 }),
    refetchInterval: 10000,
  });
  
  useEffect(() => {
    if (!data?.data) return;
    
    const currentIds = new Set(data.data.map(n => n.id));
    const newNotifications = data.data.filter(n => !previousIdsRef.current.has(n.id));
    
    newNotifications.forEach(notification => {
      onNewNotification(notification);
    });
    
    previousIdsRef.current = currentIds;
  }, [data, onNewNotification]);
}
```

3. Componente Toast:
```typescript
// src/components/NotificationToast.tsx
import { X } from 'lucide-react';
import { useEffect } from 'react';

interface NotificationToastProps {
  notification: Notification;
  onClose: () => void;
  onClick: () => void;
}

export default function NotificationToast({ notification, onClose, onClick }: NotificationToastProps) {
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
          <div className="text-sm text-gray-600">{notification.sender_name}</div>
          <div className="text-lg font-bold text-green-600">
            {notification.currency} {notification.amount}
          </div>
        </div>
        <button onClick={(e) => { e.stopPropagation(); onClose(); }}>
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
    { key: 'all', label: 'Todos', icon: Filter },
    { key: 'today', label: 'Hoy', icon: Calendar },
    // ... más opciones
  ];
  
  return (
    <div className="flex gap-2 overflow-x-auto pb-2">
      {filterOptions.map(option => (
        <button
          key={option.key}
          onClick={() => onFilterChange(option.key)}
          className={`
            px-4 py-2 rounded-full flex items-center gap-2 whitespace-nowrap
            transition-colors
            ${filters.active === option.key 
              ? 'bg-purple-600 text-white' 
              : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
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
     * Nombres de remitentes
     * Montos
     * Aliases de dispositivos
   - Debounce de 300ms para evitar demasiadas requests
   - Highlight de texto encontrado en resultados

2. Implementación:
```typescript
// src/hooks/useSearchSuggestions.ts
import { useQuery } from '@tanstack/react-query';
import { apiService } from '@/services/api';

export function useSearchSuggestions(query: string) {
  return useQuery({
    queryKey: ['notifications', 'search-suggestions', query],
    queryFn: () => apiService.searchNotifications(query),
    enabled: query.length >= 2, // Solo buscar si hay al menos 2 caracteres
    staleTime: 5000,
  });
}
```

3. Componente SearchBar:
```typescript
// src/components/SearchBar.tsx
import { Search, X } from 'lucide-react';
import { useState } from 'react';
import { useSearchSuggestions } from '@/hooks/useSearchSuggestions';

export default function SearchBar({ onSearch }) {
  const [query, setQuery] = useState('');
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
              setQuery('');
              onSearch('');
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
          {suggestions.map(suggestion => (
            <div
              key={suggestion.id}
              onClick={() => {
                setQuery(suggestion.sender_name || '');
                onSearch(suggestion.sender_name || '');
                setShowSuggestions(false);
              }}
              className="p-2 hover:bg-gray-100 cursor-pointer"
            >
              {suggestion.sender_name} - {suggestion.currency} {suggestion.amount}
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
import { Inbox } from 'lucide-react';

interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  message: string;
  action?: {
    label: string;
    onClick: () => void;
  };
}

export default function EmptyState({ icon, title, message, action }: EmptyStateProps) {
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
import { Bell, Smartphone, Settings } from 'lucide-react';
import { useNavigate, useLocation } from 'react-router-dom';

export default function MobileBottomNav() {
  const navigate = useNavigate();
  const location = useLocation();
  
  const tabs = [
    { path: '/notifications', icon: Bell, label: 'Notificaciones' },
    { path: '/devices', icon: Smartphone, label: 'Dispositivos' },
    { path: '/settings', icon: Settings, label: 'Configuración' },
  ];
  
  return (
    <div className="md:hidden fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200">
      <div className="flex justify-around">
        {tabs.map(tab => {
          const Icon = tab.icon;
          const isActive = location.pathname === tab.path;
          
          return (
            <button
              key={tab.path}
              onClick={() => navigate(tab.path)}
              className={`
                flex flex-col items-center py-2 px-4
                ${isActive ? 'text-purple-600' : 'text-gray-400'}
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

Ya deberían estar instaladas, pero verificar:

```json
{
  "@tanstack/react-query": "^5.0.0",
  "react": "^18.2.0",
  "react-router-dom": "^6.20.0",
  "tailwindcss": "^3.3.0",
  "lucide-react": "^0.300.0",
  "date-fns": "^2.30.0"
}
```

## CRITERIOS DE ACEPTACIÓN

1. ✅ Feed se actualiza automáticamente cada 10 segundos cuando tab está visible
2. ✅ No hace polling cuando tab está en background
3. ✅ Badge muestra cantidad de no leídas correctamente
4. ✅ Badge se actualiza automáticamente
5. ✅ Toast aparece cuando llega nueva notificación
6. ✅ Toast navega a detalle al hacer clic
7. ✅ Filtros tipo chips son más visibles y funcionales
8. ✅ Búsqueda tiene autocompletado
9. ✅ Estados vacíos son informativos
10. ✅ Diseño responsive funciona en móvil
11. ✅ Navegación bottom tabs funciona en móvil
12. ✅ No hay flickering al actualizar
13. ✅ Manejo de errores si falla la conexión

## NOTAS IMPORTANTES

- Usar React Query para todas las queries (no fetch directo)
- Implementar debounce para búsqueda (300ms)
- Usar document.visibilityState para pausar polling
- Considerar usar useMemo para optimizar renders
- Agregar indicador visual de "sincronizando..."
- Manejar estados de error apropiadamente
- Probar en diferentes tamaños de pantalla
- Optimizar para performance (lazy loading, code splitting)
- Seguir las convenciones de código existentes
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
import Echo from 'laravel-echo';
import Pusher from 'pusher-js';

window.Pusher = Pusher;

window.Echo = new Echo({
    broadcaster: 'reverb',
    key: import.meta.env.VITE_REVERB_APP_KEY,
    wsHost: import.meta.env.VITE_REVERB_HOST,
    wsPort: import.meta.env.VITE_REVERB_PORT,
    wssPort: import.meta.env.VITE_REVERB_PORT,
    forceTLS: (import.meta.env.VITE_REVERB_SCHEME ?? 'https') === 'https',
    enabledTransports: ['ws', 'wss'],
    auth: {
        headers: {
            Authorization: `Bearer ${token}`,
        },
    },
});

// Suscribirse al canal privado
Echo.private(`commerce.${commerceId}`)
    .listen('.notification.created', (e) => {
        console.log('Nueva notificación:', e);
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

## CRITERIOS DE ACEPTACIÓN

1. ✅ Laravel Reverb instalado y configurado correctamente
2. ✅ Event NotificationCreated se dispara al crear notificación
3. ✅ Event se transmite al canal privado correcto
4. ✅ Solo usuarios del mismo commerce reciben eventos
5. ✅ Autenticación de canales funciona correctamente
6. ✅ Servidor Reverb inicia sin errores
7. ✅ Cliente puede conectarse y recibir eventos
8. ✅ Manejo de reconexión automática (manejado por cliente)
9. ✅ Tests pasan correctamente
10. ✅ Documentación actualizada

## NOTAS IMPORTANTES

- Usar canales privados para seguridad multi-tenant
- Implementar rate limiting para eventos si es necesario
- Considerar usar Redis para mejor performance en producción
- Configurar SSL/TLS para producción (wss://)
- Monitorear conexiones WebSocket (logs, métricas)
- Documentar configuración en README.md
- Probar con múltiples clientes conectados simultáneamente
- Manejar desconexiones y reconexiones apropiadamente
- Considerar usar queue para broadcasting si hay muchos eventos

## ALTERNATIVA: Pusher

Si prefieres usar Pusher en lugar de Reverb:

1. Crear cuenta en Pusher.com
2. Instalar: composer require pusher/pusher-php-server
3. Configurar credenciales en .env
4. Cambiar BROADCAST_DRIVER=pusher
5. El resto de la implementación es similar

Reverb es recomendado porque es nativo de Laravel y no requiere servicio externo.
```

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



