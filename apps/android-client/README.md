# Yape Notifier Android App

Aplicación Android desarrollada en Kotlin para capturar y enviar notificaciones de pagos.

## Requisitos

- Android Studio
- Android SDK (mínimo API 24)
- Kotlin
- Dispositivo Android físico (recomendado) o emulador

## Configuración

1. Abrir el proyecto en Android Studio
2. Actualizar la URL de la API en `RetrofitClient.kt`:
```kotlin
private const val BASE_URL = "https://tu-api-url.com/"
```

3. Sincronizar dependencias Gradle

## Permisos

La aplicación requiere:
- Permiso de acceso a notificaciones (NotificationListenerService)
- Permiso de Internet

Para activar el servicio de notificaciones:
1. Abrir Configuración del sistema
2. Ir a "Servicios de notificación"
3. Activar "Yape Notifier"

## Apps Soportadas

- Yape
- Plin
- BCP
- Interbank
- BBVA
- Scotiabank

## Estructura

- `data/` - Modelos, API, repositorios
- `service/` - NotificationListenerService
- `parser/` - Parser de notificaciones
- `ui/` - Activities, ViewModels
- `data/local/` - DataStore para preferencias

## Build

```bash
./gradlew assembleDebug
```

