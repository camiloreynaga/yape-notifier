# Guía de Inicio del Proyecto

## Yape & Bank Notification Payment Validator

Este documento describe los pasos iniciales para arrancar el proyecto y las herramientas necesarias, asumiendo:

- Backend en **Laravel**.
- App Android en **Kotlin**.
- Monorepo gestionado con **Git** y editado en **Cursor**.
- Deploy inicial (MVP) en Railway.

---

## 1. Herramientas necesarias

### 1.1. Herramientas generales

- **Git**
  - Para control de versiones.
- **Cursor** (editor principal)
  - Para escribir y navegar el código en el monorepo.
- **GitHub** (u otro remoto)
  - Para alojar el repositorio del proyecto.

### 1.2. Backend (Laravel)

- **PHP 8.2+**
- **Composer**
- **Laravel 10/11** (CLI: `laravel new` o instalación manual).
- **Base de datos local (para desarrollo):**
  - PostgreSQL o MySQL/MariaDB.
- **Docker y Docker Compose** (opcional pero recomendado):
  - Facilita levantar:
    - Contenedor de PHP-FPM / Laravel.
    - Contenedor de Nginx o Caddy.
    - Contenedor de base de datos.

### 1.3. Android

- **Android Studio**
  - Para compilar y emular la app Android.
- **SDK de Android**
- **Dispositivo físico Android** (recomendado) o emulador Genymotion / emulador oficial.
- Kotlin configurado en Android Studio (default).

### 1.4. Infraestructura y despliegue

- **Cuenta en Railway**
  - Para desplegar el backend como MVP.
  - Para crear una base de datos administrada.
- (Futuro) **Cuenta en DigitalOcean**
  - Para montar el entorno de producción en un Droplet con Docker.

---

## 2. Estructura del monorepo

En la raíz del proyecto:

```text
yape-notifier/
  apps/
    api/              # Backend Laravel
    android-client/   # App Android en Kotlin
    web-dashboard/    # Dashboard web (futuro)
  infra/
    docker/           # Dockerfiles, configs, compose adicionales
  docs/
    requirements.md
    setup-and-tools.md
  .cursorrules        # Instrucciones para Cursor AI
  .gitignore
  README.md
```

---

## 3. Pasos iniciales para el backend (Laravel)

### 3.1. Crear proyecto Laravel

```bash
cd apps/api
composer create-project laravel/laravel . --prefer-dist
```

O si ya tienes Composer global:

```bash
cd apps/api
laravel new .
```

### 3.2. Configurar base de datos

Editar `.env`:

```env
DB_CONNECTION=pgsql  # o mysql
DB_HOST=127.0.0.1
DB_PORT=5432
DB_DATABASE=yape_notifier
DB_USERNAME=tu_usuario
DB_PASSWORD=tu_password
```

### 3.3. Instalar dependencias adicionales

```bash
composer require laravel/sanctum
php artisan vendor:publish --provider="Laravel\Sanctum\SanctumServiceProvider"
php artisan migrate
```

---

## 4. Pasos iniciales para Android

### 4.1. Crear proyecto en Android Studio

1. Abrir Android Studio.
2. New Project → Empty Activity.
3. Nombre: `YapeNotifier` o similar.
4. Package name: `com.yourcompany.yapenotifier`.
5. Language: **Kotlin**.
6. Minimum SDK: API 24 (Android 7.0) o superior (para NotificationListenerService).

### 4.2. Configurar dependencias en `build.gradle.kts`

```kotlin
dependencies {
    // Retrofit para HTTP
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.datastore:datastore-preferences-core:1.0.0")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
}
```

### 4.3. Permisos en `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

---

## 5. Configuración de Docker (desarrollo local)

Ver `infra/docker/` para Dockerfiles y `docker-compose.yml` cuando estén disponibles.

---

## 6. Configuración para Railway (MVP)

### 6.1. Variables de entorno en Railway

- `APP_ENV=production`
- `APP_DEBUG=false`
- `APP_KEY=` (generar con `php artisan key:generate`)
- `DB_CONNECTION=pgsql` (o mysql)
- `DB_HOST=` (proporcionado por Railway)
- `DB_DATABASE=` (proporcionado por Railway)
- `DB_USERNAME=` (proporcionado por Railway)
- `DB_PASSWORD=` (proporcionado por Railway)

### 6.2. Build y Start commands en Railway

**Build Command:**
```bash
cd apps/api && composer install --no-dev --optimize-autoloader
```

**Start Command:**
```bash
cd apps/api && php artisan migrate --force && php artisan serve --host=0.0.0.0 --port=$PORT
```

---

## 7. Próximos pasos

1. Implementar autenticación en Laravel (Sanctum).
2. Crear modelos y migraciones (Account, Device, Notification).
3. Implementar endpoints REST.
4. Desarrollar NotificationListenerService en Android.
5. Implementar parser de notificaciones.
6. Crear cliente HTTP para enviar notificaciones a la API.
7. Configurar despliegue en Railway.

---

## Referencias

- [Laravel Documentation](https://laravel.com/docs)
- [Laravel Sanctum](https://laravel.com/docs/sanctum)
- [Android NotificationListenerService](https://developer.android.com/reference/android/service/notification/NotificationListenerService)
- [Railway Documentation](https://docs.railway.app/)





