# Yape Notifier Android Client

Aplicación Android para capturar notificaciones de pagos y enviarlas al backend centralizado.

## 📋 Stack Tecnológico

- **Kotlin**
- **Android SDK** (mínimo API 24)
- **MVVM Architecture**
- **Retrofit** (cliente HTTP)
- **Coroutines** (operaciones asíncronas)
- **Room Database** (almacenamiento local)
- **WorkManager** (tareas en background)

## 🏗️ Estructura

```
app/src/main/java/com/yapenotifier/android/
├── data/
│   ├── model/          # Modelos de datos
│   ├── api/            # Cliente Retrofit
│   ├── local/          # DataStore y Room DB
│   ├── parser/         # Parser de notificaciones
│   └── repository/     # Repositorios
├── service/            # NotificationListenerService
├── ui/                 # Activities, Fragments, ViewModels
└── worker/             # WorkManager workers
```

## 🚀 Comandos Básicos

### Build

```bash
# Build debug
./gradlew assembleDebug

# Build release
./gradlew assembleRelease
```

### Testing

```bash
# Tests unitarios
./gradlew test

# Tests de instrumentación
./gradlew connectedAndroidTest
```

### Linting

```bash
./gradlew ktlint
./gradlew ktlintFormat
```

## 🔧 Configuración

### URL de la API

La URL de la API se configura automáticamente según el tipo de build:

**Debug (Desarrollo Local):**

- URL: `http://10.0.2.2:8000/` (emulador Android)
- Configurado en: `app/build.gradle.kts` → `buildTypes.debug`

**Release (Producción):**

- URL: `https://api.notificaciones.space/`
- Configurado en: `app/build.gradle.kts` → `buildTypes.release`

#### Para cambiar la URL manualmente:

1. **Editar `app/build.gradle.kts`:**

```kotlin
buildTypes {
    debug {
        // Para probar con producción en debug, cambia esta línea:
        buildConfigField("String", "API_BASE_URL", "\"https://api.notificaciones.space/\"")
    }
    release {
        buildConfigField("String", "API_BASE_URL", "\"https://api.notificaciones.space/\"")
    }
}
```

2. **Para dispositivo físico en desarrollo local:**
   - Cambiar `10.0.2.2` por la IP local de tu máquina (ej: `192.168.1.100`)

#### Verificar la URL configurada:

La app muestra la URL en los logs al iniciar:

```
RetrofitClient: Creating API service with base URL: https://api.notificaciones.space/
RetrofitClient: Build type: RELEASE
```

### Network Security Config

La app está configurada para:

- ✅ Permitir HTTP solo para `localhost` y `10.0.2.2` (desarrollo)
- ✅ Requerir HTTPS para todas las demás conexiones (producción)
- ✅ Confiar en certificados del sistema (Let's Encrypt, etc.)

Configuración en: `app/src/main/res/xml/network_security_config.xml`

## 🔐 Permisos Requeridos

1. **Acceso a Notificaciones**: Configuración → Acceso especial → Acceso a notificaciones
2. **Optimización de Batería**: Desactivar para Yape Notifier
3. **Auto-inicio**: Activar (OPPO/Xiaomi/Huawei)

Ver `docs/PERMISSIONS.md` para guía detallada.

## 🐛 Bug Crítico Conocido

**Ubicación**: `PaymentNotificationListenerService.kt:67`

**Problema**: Usa `hashCode()` en lugar de `identifier`

```kotlin
// ❌ INCORRECTO
val androidUserId = sbn.user?.hashCode()

// ✅ CORRECTO
val androidUserId = sbn.user?.identifier
```

Ver `../../docs/07-reference/KNOWN_ISSUES.md` para más detalles.

## 📚 Documentación

- **Endpoints**: Ver `docs/ENDPOINTS.md`
- **Permisos**: Ver `docs/PERMISSIONS.md`
- **Arquitectura**: Ver `../../docs/03-architecture/DUAL_APPS.md`
- **Estado de implementación**: Ver `../../docs/07-reference/IMPLEMENTATION_STATUS.md`
- **Roadmap**: Ver `../../docs/07-reference/ROADMAP.md`

## ⚠️ Problemas Conocidos

### El servicio se desconecta después de un tiempo

**Solución**: Desactivar optimización de batería y permitir auto-inicio

### No se capturan notificaciones

**Solución**:

1. Verificar acceso a notificaciones
2. Verificar permisos de la app de origen
3. Revisar logs: `adb logcat | grep PaymentNotificationService`
