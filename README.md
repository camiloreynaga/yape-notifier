# Yape & Bank Notification Payment Validator

Sistema para leer, procesar y validar notificaciones de pagos desde aplicaciones móviles (Yape, Plin, bancos) en dispositivos Android, consolidando la información en un backend centralizado.

## 🎯 Objetivo

Desarrollar una solución compuesta por una app Android y un backend en Laravel que permita:

- Leer notificaciones de pago (Yape, Plin y bancos) desde dispositivos Android
- Procesar y parsear automáticamente la información relevante (monto, pagador, origen)
- Enviar dichas notificaciones a una API central
- Registrar y consolidar los pagos en una base de datos
- Permitir visualizar y validar pagos desde un dashboard central

## 📁 Estructura del Monorepo

```
yape-notifier/
├── apps/
│   ├── api/              # Backend Laravel (PHP 8.2+, Laravel 11)
│   ├── android-client/   # App Android (Kotlin, MVVM)
│   └── web-dashboard/    # Dashboard web (React + TypeScript)
├── infra/
│   └── docker/           # Dockerfiles y configuraciones
├── Makefile              # Scripts compartidos para desarrollo
├── render.yaml           # Configuración para deploy en Render
└── README.md
```

## 🛠️ Stack Tecnológico

### Backend
- **PHP 8.2+**
- **Laravel 11**
- **PostgreSQL** o **MySQL**
- **Laravel Sanctum** (autenticación)
- **Docker** (para desarrollo y producción)

### Frontend Móvil
- **Kotlin**
- **Android SDK** (mínimo API 24)
- **MVVM Architecture**
- **Retrofit** (cliente HTTP)
- **Coroutines** (operaciones asíncronas)
- **DataStore** (almacenamiento local)

### Dashboard Web
- **React 18**
- **TypeScript**
- **Vite**
- **Tailwind CSS**

### Infraestructura
- **Render** (MVP - deploy recomendado)
- **Railway** (alternativa)
- **DigitalOcean Droplet** (producción futura)
- **Docker Compose** (desarrollo local)

---

## 🚀 Inicio Rápido

### 🐳 Opción 1: Docker (Recomendado)

La forma más rápida de empezar es usando Docker:

```bash
cd infra/docker

# Windows (PowerShell)
.\setup.ps1

# Linux/Mac
chmod +x setup.sh
./setup.sh
```

El API estará disponible en: **http://localhost:8000**

El script automáticamente:
- ✅ Crea archivos `.env` necesarios
- ✅ Construye las imágenes Docker
- ✅ Inicia los contenedores
- ✅ Instala dependencias de Composer
- ✅ Genera la clave de aplicación
- ✅ Ejecuta las migraciones

### 📦 Opción 2: Instalación Manual

#### Prerrequisitos

- PHP 8.2+ y Composer
- PostgreSQL o MySQL
- Android Studio y SDK de Android (para la app móvil)
- Node.js 18+ (para el dashboard web)
- Git

#### Backend (Laravel)

```bash
cd apps/api
composer install
cp .env.example .env
php artisan key:generate
php artisan migrate
php artisan serve
```

#### Android App

1. Abrir `apps/android-client` en Android Studio
2. Sincronizar dependencias Gradle
3. Configurar URL de la API en `RetrofitClient.kt`:
   ```kotlin
   private const val BASE_URL = "http://10.0.2.2:8000/"  // Emulador
   // O
   private const val BASE_URL = "http://TU_IP_LOCAL:8000/"  // Dispositivo físico
   ```
4. Ejecutar en dispositivo físico o emulador

#### Dashboard Web

```bash
cd apps/web-dashboard
npm install
npm run dev
```

El dashboard estará disponible en: **http://localhost:3000**

### 🛠️ Usando Makefile (Scripts Compartidos)

El proyecto incluye un `Makefile` con comandos útiles:

```bash
# Ver todos los comandos disponibles
make help

# Instalar dependencias de todas las apps
make install

# Iniciar entorno de desarrollo completo
make dev

# Iniciar solo el backend
make dev:api

# Iniciar solo el dashboard
make dev:dashboard

# Ejecutar todos los tests
make test

# Build de todas las apps
make build

# Docker
make docker-up      # Iniciar contenedores
make docker-down    # Detener contenedores
make docker-logs    # Ver logs
make docker-shell   # Acceder al shell

# Migraciones
make migrate        # Ejecutar migraciones
make migrate:fresh  # Resetear y ejecutar migraciones

# Linting
make lint           # Verificar estilo
make lint:fix       # Corregir estilo

# Limpiar builds y caches
make clean
```

---

## 📡 API Endpoints

### Autenticación

| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| POST | `/api/register` | Registrar nuevo usuario | No |
| POST | `/api/login` | Iniciar sesión | No |
| POST | `/api/logout` | Cerrar sesión | Sí |
| GET | `/api/me` | Obtener usuario autenticado | Sí |

**Ejemplo de registro:**
```bash
curl -X POST http://localhost:8000/api/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "password": "password123",
    "password_confirmation": "password123"
  }'
```

**Ejemplo de login:**
```bash
curl -X POST http://localhost:8000/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

### Dispositivos

| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| GET | `/api/devices` | Listar dispositivos | Sí |
| POST | `/api/devices` | Crear dispositivo | Sí |
| GET | `/api/devices/{id}` | Obtener dispositivo | Sí |
| PUT | `/api/devices/{id}` | Actualizar dispositivo | Sí |
| DELETE | `/api/devices/{id}` | Eliminar dispositivo | Sí |
| POST | `/api/devices/{id}/toggle-status` | Activar/desactivar | Sí |

**Ejemplo de crear dispositivo:**
```bash
curl -X POST http://localhost:8000/api/devices \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "name": "Mi Dispositivo Android",
    "platform": "android"
  }'
```

### Notificaciones

| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| POST | `/api/notifications` | Crear notificación | Sí |
| GET | `/api/notifications` | Listar notificaciones | Sí |
| GET | `/api/notifications/{id}` | Obtener notificación | Sí |
| GET | `/api/notifications/statistics` | Estadísticas | Sí |
| PATCH | `/api/notifications/{id}/status` | Actualizar estado | Sí |

**Ejemplo de crear notificación:**
```bash
curl -X POST http://localhost:8000/api/notifications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "device_id": "uuid-del-dispositivo",
    "source_app": "yape",
    "title": "Pago recibido",
    "body": "Recibiste S/ 150.00 de Juan Pérez",
    "amount": 150.00,
    "currency": "PEN",
    "payer_name": "Juan Pérez"
  }'
```

**Filtros disponibles para GET /api/notifications:**
- `device_id` - Filtrar por dispositivo
- `source_app` - Filtrar por app (yape, plin, bcp, etc.)
- `start_date` - Fecha inicial
- `end_date` - Fecha final
- `status` - Estado (pending, validated, inconsistent)
- `exclude_duplicates` - Excluir duplicados (true/false)
- `per_page` - Resultados por página (default: 50)

### Autenticación

La API utiliza Laravel Sanctum para autenticación. Incluye el token en el header:

```
Authorization: Bearer {token}
```

---

## 🔐 Flujo de Autenticación e Identificación de Dispositivos

### ¿Por qué se necesita el inicio de sesión?

El inicio de sesión es **NECESARIO** porque:

1. **Autenticación de Usuario (Laravel Sanctum)**
   - Todas las rutas de notificaciones están protegidas con `auth:sanctum`
   - Sin autenticación, la API rechazaría todas las peticiones con error 401

2. **Asociación de Notificaciones con Usuario**
   - Cada notificación se guarda con un `user_id` en la base de datos
   - Permite que múltiples usuarios tengan sus propios dispositivos y notificaciones

3. **Registro Automático de Dispositivo**
   - Al hacer login, la app automáticamente registra el dispositivo en el backend
   - Crea la relación entre el usuario y el dispositivo físico

### ¿Cómo identifica la app Android el dispositivo?

La app Android identifica el dispositivo usando un **sistema de dos niveles**:

1. **Generación/Obtención del UUID**
   - Al iniciar sesión, la app genera o recupera un UUID único del dispositivo
   - Se guarda localmente en `PreferencesManager` (DataStore encriptado)

2. **Registro en el Backend**
   - Al hacer login, la app envía el UUID al backend para crear/actualizar el dispositivo
   - El backend crea un registro en la tabla `devices` asociado al usuario autenticado

3. **Envío de Notificaciones**
   - Cuando la app detecta una notificación, incluye el `device_id` (UUID) en la petición
   - El backend valida que el dispositivo pertenezca al usuario del token

**Flujo completo:**
```
Usuario inicia sesión → Obtiene token → Genera UUID → Registra dispositivo → 
Guarda token y device_id → Detecta notificación → Envía con device_id y token → 
Backend valida y procesa
```

### Múltiples Dispositivos

- Cada dispositivo tiene su propio UUID único
- Todos están asociados al mismo usuario
- El backend identifica qué dispositivo específico envió cada notificación
- Puedes ver estadísticas y filtrar por dispositivo

---

## 📱 Configuración de la App Android

### Configurar URL de la API

Edita `apps/android-client/app/src/main/java/com/yapenotifier/android/data/api/RetrofitClient.kt`:

**Para emulador:**
```kotlin
private const val BASE_URL = "http://10.0.2.2:8000/"
```

**Para dispositivo físico (misma red WiFi):**
```kotlin
private const val BASE_URL = "http://192.168.1.XXX:8000/"  // Tu IP local
```

**Para desarrollo con redes diferentes:**
Ver sección "Desarrollo con Redes Diferentes" más abajo.

### Activar Permisos de Notificaciones

La app requiere permisos especiales para leer notificaciones:

1. Instala la app en tu dispositivo
2. Ve a **Configuración → Accesibilidad → Servicios instalados** (o **Configuración → Notificaciones → Acceso a notificaciones**)
3. Activa **"Yape Notifier"**
4. Regresa a la app y verifica que el servicio esté activado

### Probar en Dispositivo Físico

1. **Habilitar Modo Desarrollador:**
   - Configuración → Acerca del teléfono
   - Toca 7 veces en "Número de compilación"

2. **Habilitar Depuración USB:**
   - Configuración → Opciones de desarrollador
   - Activa "Depuración USB"

3. **Conectar y verificar:**
   ```bash
   adb devices
   ```

4. **Instalar desde Android Studio:**
   - Selecciona tu dispositivo en la barra superior
   - Haz clic en Run (▶️)

---

## 🌐 Desarrollo con Redes Diferentes

Si el teléfono Android y el backend están en redes WiFi diferentes, tienes varias opciones:

### Opción 1: Túnel Local (Recomendado para desarrollo rápido)

**Cloudflare Tunnel (gratis, sin límites):**
```bash
# Instalar
choco install cloudflared  # Windows
brew install cloudflared   # Mac

# Crear túnel
cloudflared tunnel --url http://localhost:8000

# Usar la URL que aparece en RetrofitClient.kt
```

**ngrok:**
```bash
ngrok http 8000
# Usar la URL HTTPS que aparece
```

**Ventajas:**
- ✅ Rápido de configurar (5 minutos)
- ✅ Funciona desde cualquier red
- ✅ Gratis para desarrollo

**Desventajas:**
- ❌ URL cambia cada vez que reinicias (versión gratuita)
- ❌ Requiere conexión a internet

### Opción 2: Desplegar en Servidor (Recomendado para desarrollo continuo)

Despliega el backend en **Render** o **Railway** para tener una URL permanente.

**Ventajas:**
- ✅ URL permanente (no cambia)
- ✅ HTTPS incluido automáticamente
- ✅ Disponible 24/7
- ✅ Mejor para pruebas con múltiples dispositivos

**Desventajas:**
- ❌ Requiere configuración inicial (15-30 minutos)
- ❌ Puede tener costos (aunque muchos tienen planes gratuitos)

Ver sección "🚀 Deploy en Render" más abajo para instrucciones detalladas.

---

## 🐳 Docker

### Comandos Principales

```bash
cd infra/docker

# Iniciar contenedores
docker-compose up -d

# Ver logs
docker-compose logs -f

# Ver logs de un servicio específico
docker-compose logs -f app

# Detener contenedores
docker-compose down

# Reiniciar contenedores
docker-compose restart

# Acceder al shell del contenedor
docker-compose exec app bash

# Ejecutar comandos artisan
docker-compose exec app php artisan migrate
docker-compose exec app php artisan test
```

### Servicios Disponibles

- **API Laravel**: http://localhost:8000
- **Dashboard Web**: http://localhost:3000 (producción) o http://localhost:3001 (desarrollo)
- **PostgreSQL**: localhost:5432
  - Usuario: `postgres`
  - Contraseña: `password` (por defecto)
  - Base de datos: `yape_notifier`
- **Redis**: localhost:6379

### Solución de Problemas

**Error: "Port already in use"**
```bash
# Cambiar puerto en infra/docker/.env
APP_PORT=8001
```

**Error: "Permission denied" en storage**
```bash
docker-compose exec app chmod -R 775 storage bootstrap/cache
docker-compose exec app chown -R www-data:www-data storage bootstrap/cache
```

**Reconstruir todo desde cero**
```bash
docker-compose down -v
docker-compose build --no-cache
docker-compose up -d
```

---

## 🚀 Deploy en Render

### Prerrequisitos

1. Cuenta en [Render](https://render.com) (gratis)
2. Repositorio en GitHub
3. Git configurado localmente

### Pasos para Deploy

#### 1. Subir el código a GitHub

```bash
# Agregar el remoto (reemplaza con tu URL)
git remote add origin https://github.com/TU_USUARIO/yape-notifier.git

# Subir el código
git push -u origin master
```

#### 2. Crear cuenta en Render

1. Ve a [https://render.com](https://render.com)
2. Haz clic en "Get Started for Free"
3. Inicia sesión con tu cuenta de GitHub

#### 3. Crear Base de Datos PostgreSQL

1. En el dashboard de Render, haz clic en **"New +"**
2. Selecciona **"PostgreSQL"**
3. Configura:
   - **Name**: `yape-notifier-db`
   - **Database**: `yape_notifier`
   - **User**: `yape_user`
   - **Region**: `Oregon` (o la más cercana a ti)
   - **Plan**: `Free`
4. Haz clic en **"Create Database"**
5. **Guarda las credenciales** que aparecen

#### 4. Crear Web Service

1. En el dashboard, haz clic en **"New +"**
2. Selecciona **"Web Service"**
3. Conecta tu repositorio de GitHub:
   - Selecciona el repositorio `yape-notifier`
   - Haz clic en **"Connect"**

#### 5. Configurar el Web Service

**Configuración básica:**
- **Name**: `yape-notifier-api`
- **Region**: `Oregon` (o la misma que la base de datos)
- **Branch**: `master`
- **Root Directory**: `apps/api`
- **Runtime**: `PHP`
- **Build Command**: 
  ```bash
  composer install --no-dev --optimize-autoloader && php artisan key:generate --force
  ```
- **Start Command**: 
  ```bash
  php artisan serve --host=0.0.0.0 --port=$PORT
  ```

**Variables de Entorno:**

Agrega las siguientes variables de entorno en la sección "Environment":

```env
APP_NAME=Yape Notifier API
APP_ENV=production
APP_DEBUG=false
APP_URL=https://yape-notifier-api.onrender.com
LOG_CHANNEL=stderr
LOG_LEVEL=error

DB_CONNECTION=pgsql
DB_HOST=<HOST_DE_LA_BASE_DE_DATOS>
DB_PORT=<PUERTO_DE_LA_BASE_DE_DATOS>
DB_DATABASE=<NOMBRE_DE_LA_BASE_DE_DATOS>
DB_USERNAME=<USUARIO_DE_LA_BASE_DE_DATOS>
DB_PASSWORD=<CONTRASEÑA_DE_LA_BASE_DE_DATOS>

CACHE_DRIVER=file
SESSION_DRIVER=file
QUEUE_CONNECTION=sync
```

**Nota:** Los valores de `DB_*` los obtienes de la base de datos que creaste en el paso 3.

#### 6. Ejecutar Migraciones

Después del primer deploy:

1. En el dashboard de Render, ve a tu servicio web
2. Haz clic en **"Shell"** (en la barra lateral)
3. Ejecuta:
   ```bash
   php artisan migrate --force
   ```

#### 7. Obtener la URL

Una vez que el deploy termine, Render te dará una URL como:
```
https://yape-notifier-api.onrender.com
```

**Nota:** En el plan gratuito, el servicio se "duerme" después de 15 minutos de inactividad. La primera petición puede tardar ~30 segundos en despertar.

#### 8. Actualizar la App Android

Una vez que tengas la URL de Render, actualiza `RetrofitClient.kt`:

```kotlin
// apps/android-client/app/src/main/java/com/yapenotifier/android/data/api/RetrofitClient.kt
object RetrofitClient {
    private const val BASE_URL = "https://yape-notifier-api.onrender.com/"
    // ...
}
```

### Deploy Automático

Render automáticamente hace deploy cada vez que haces push a la rama `master` de tu repositorio.

### Solución de Problemas en Render

**Error: "Application failed to respond"**
- Verifica que el `Start Command` sea correcto
- Revisa los logs en Render Dashboard → Logs

**Error: "Database connection failed"**
- Verifica que las variables de entorno `DB_*` sean correctas
- Asegúrate de que la base de datos esté en la misma región que el servicio web

**Error: "500 Internal Server Error"**
- Revisa los logs en Render Dashboard → Logs
- Verifica que las migraciones se hayan ejecutado
- Verifica que `APP_KEY` esté configurado (se genera automáticamente con el build command)

**El servicio tarda mucho en responder**
- Esto es normal en el plan gratuito (se "duerme" después de 15 min)
- Considera usar un servicio de "ping" para mantenerlo activo
- O actualiza a un plan de pago

---

## 🧪 Testing

### Backend (Laravel)

#### Ejecutar Tests

**Usando Docker:**
```bash
cd infra/docker

# Ejecutar todos los tests
docker-compose exec app php artisan test

# Ejecutar solo tests unitarios
docker-compose exec app php artisan test --testsuite=Unit

# Ejecutar solo tests de integración
docker-compose exec app php artisan test --testsuite=Feature

# Ejecutar un test específico
docker-compose exec app php artisan test --filter AuthTest

# Con cobertura de código
docker-compose exec app php artisan test --coverage
```

**Localmente:**
```bash
cd apps/api
php artisan test
```

#### Estructura de Tests

```
tests/
├── Feature/          # Tests de integración (API, endpoints)
│   ├── AuthTest.php
│   ├── DeviceTest.php
│   └── NotificationTest.php
└── Unit/             # Tests unitarios (servicios, modelos)
    └── NotificationServiceTest.php
```

#### Escribir Tests

**Test de Feature (API):**
```php
<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class MyFeatureTest extends TestCase
{
    use RefreshDatabase;

    public function test_user_can_do_something(): void
    {
        $user = User::factory()->create();
        $token = $user->createToken('test-token')->plainTextToken;

        $response = $this->withHeader('Authorization', "Bearer $token")
            ->getJson('/api/endpoint');

        $response->assertStatus(200);
    }
}
```

**Test Unitario:**
```php
<?php

namespace Tests\Unit;

use App\Services\MyService;
use Tests\TestCase;

class MyServiceTest extends TestCase
{
    public function test_it_does_something(): void
    {
        $service = new MyService();
        $result = $service->doSomething();

        $this->assertNotNull($result);
    }
}
```

#### Factories

Las factories se encuentran en `database/factories/`:
- `UserFactory` - Crear usuarios de prueba
- `DeviceFactory` - Crear dispositivos de prueba
- `NotificationFactory` - Crear notificaciones de prueba

**Ejemplo:**
```php
$user = User::factory()->create();
$device = Device::factory()->create(['user_id' => $user->id]);
```

### Android

```bash
cd apps/android-client

# Tests unitarios
./gradlew test

# Tests de instrumentación
./gradlew connectedAndroidTest

# Todos los tests
./gradlew check
```

### Mejores Prácticas

1. **Nombres descriptivos**: Los nombres de los tests deben describir claramente qué están probando
2. **Arrange-Act-Assert**: Estructura tus tests en estas tres fases
3. **Un test, una aserción**: Cada test debe verificar una sola cosa
4. **Usa factories**: No crees datos manualmente, usa factories
5. **Tests independientes**: Cada test debe poder ejecutarse de forma independiente
6. **Mocking**: Usa mocks para dependencias externas

---

## 🔍 Linting

### Backend (Laravel Pint)

Laravel Pint es el linter oficial de Laravel basado en PHP-CS-Fixer.

**Verificar estilo de código:**
```bash
cd apps/api

# Usando Docker
docker-compose exec app ./vendor/bin/pint --test

# Localmente
./vendor/bin/pint --test
```

**Corregir automáticamente:**
```bash
# Usando Docker
docker-compose exec app ./vendor/bin/pint

# Localmente
./vendor/bin/pint
```

**Usando Makefile:**
```bash
make lint        # Verificar
make lint:fix    # Corregir
```

### Android (ktlint)

**Verificar:**
```bash
cd apps/android-client
./gradlew ktlint
```

**Corregir automáticamente:**
```bash
./gradlew ktlintFormat
```

---

## 🏗️ Arquitectura

### Backend (Laravel)

El backend sigue una arquitectura limpia:

- **Controllers REST**: Manejo de peticiones HTTP
- **Services**: Lógica de negocio
- **Repositories**: Acceso a datos
- **Form Requests**: Validación de entrada
- **Models**: Eloquent ORM

**Estructura:**
```
app/
├── Http/
│   ├── Controllers/     # Controladores REST
│   └── Requests/       # Form Requests (validación)
├── Models/              # Modelos Eloquent
├── Services/            # Lógica de negocio
└── Providers/           # Service Providers
```

### Android App

Arquitectura **MVVM**:

- **Models**: Entidades de datos
- **Views**: Activities y Fragments
- **ViewModels**: Lógica de presentación
- **Repository**: Acceso a datos (API y local)
- **Services**: NotificationListenerService para capturar notificaciones

**Estructura:**
```
app/src/main/java/com/yapenotifier/android/
├── data/
│   ├── model/          # Modelos de datos
│   ├── api/            # Cliente Retrofit
│   ├── local/          # DataStore
│   ├── parser/         # Parser de notificaciones
│   └── repository/     # Repositorios
├── service/            # NotificationListenerService
└── ui/                 # Activities, Fragments, ViewModels
```

### Dashboard Web

Arquitectura basada en React con TypeScript:

- **Pages**: Páginas principales de la aplicación
- **Components**: Componentes reutilizables
- **Contexts**: Contextos de React (Auth, etc.)
- **Services**: Cliente API
- **Types**: Tipos TypeScript

---

## 🔐 Seguridad

- Comunicación HTTPS entre app y API (en producción)
- Autenticación con tokens (Laravel Sanctum)
- Hashing seguro de contraseñas (bcrypt/Argon2)
- Validación de permisos y autorización
- Variables de entorno para secretos
- Detección de notificaciones duplicadas
- Validación de entrada en todos los endpoints

---

## 📋 MVP (Minimum Viable Product)

Para la primera versión funcional:

### Backend
- ✅ Autenticación básica (login/registro)
- ✅ Registro de dispositivos
- ✅ Endpoint `POST /api/notifications`
- ✅ Endpoint `GET /api/notifications`
- ✅ Persistencia en BD
- ✅ Detección de duplicados
- ✅ Estadísticas de notificaciones

### Android
- ✅ Lectura de notificaciones (Yape/Bancos)
- ✅ Parseo básico de monto y texto
- ✅ Envío automático a la API
- ✅ Almacenamiento local de tokens

### Dashboard Web
- ✅ Autenticación (Login/Registro)
- ✅ Dashboard con estadísticas
- ✅ Gestión de notificaciones
- ✅ Gestión de dispositivos
- ✅ Exportación a CSV

### Infraestructura
- ✅ Deploy en Docker
- ✅ Base de datos PostgreSQL
- ✅ Redis para cache
- ✅ Deploy en Render (configurado)

---

## 🗺️ Roadmap

- [ ] Reglas avanzadas de validación de pagos
- [ ] Exportación a Excel
- [ ] Integración con Google Sheets
- [ ] Migración a DigitalOcean Droplet
- [ ] Sistema de alertas y notificaciones
- [ ] Análisis y reportes avanzados
- [ ] API GraphQL (opcional)
- [ ] Webhooks para integraciones externas

---

## 🛠️ Comandos Útiles

### Backend

```bash
# Migraciones
php artisan migrate
php artisan migrate:fresh
php artisan migrate:rollback

# Seeders
php artisan db:seed

# Cache
php artisan cache:clear
php artisan config:clear
php artisan route:clear

# Testing
php artisan test
php artisan test --coverage

# Linting
./vendor/bin/pint
./vendor/bin/pint --test
```

### Docker

```bash
# Ver estado
docker-compose ps

# Logs
docker-compose logs -f
docker-compose logs -f app

# Reiniciar
docker-compose restart app

# Ejecutar comandos
docker-compose exec app php artisan [comando]
docker-compose exec app composer [comando]
```

### Android

```bash
# Build
./gradlew assembleDebug

# Tests
./gradlew test
./gradlew connectedAndroidTest

# Linting
./gradlew ktlint
./gradlew ktlintFormat
```

### Makefile

```bash
# Ver ayuda
make help

# Desarrollo
make dev              # Iniciar todo
make dev:api          # Solo backend
make dev:dashboard    # Solo dashboard

# Testing
make test             # Todos los tests
make test:api         # Tests del backend
make test:android     # Tests de Android

# Build
make build            # Build de todas las apps

# Docker
make docker-up        # Iniciar contenedores
make docker-down      # Detener contenedores
make docker-logs      # Ver logs
make docker-shell     # Acceder al shell

# Utilidades
make migrate          # Ejecutar migraciones
make lint             # Verificar estilo
make lint:fix         # Corregir estilo
make clean            # Limpiar builds
```

---

## 🤝 Contribución

Este es un proyecto privado. Para contribuciones, contacta al equipo de desarrollo.

### Guía de Contribución

1. Crear una rama desde `master`
2. Realizar cambios y commits descriptivos
3. Ejecutar tests y linting antes de commitear
4. Crear un Pull Request con descripción clara
5. Esperar revisión y aprobación

### Estándares de Código

- Seguir PSR-12 para PHP
- Seguir Kotlin Coding Conventions para Android
- Escribir tests para nuevas funcionalidades
- Documentar funciones y clases complejas
- Mantener cobertura de código > 80%

---

## 🐛 Solución de Problemas

### Error: "Class 'App\Models\User' not found"
```bash
cd apps/api
composer dump-autoload
```

### Error: "SQLSTATE[HY000] [2002] Connection refused"
Verifica que la base de datos esté corriendo y las credenciales en `.env` sean correctas

### La app Android no captura notificaciones
- Verifica que el servicio de notificaciones esté activado en Configuración
- Verifica que la app de pago (Yape, banco, etc.) tenga permisos de notificación
- Revisa los logs en Android Studio (Logcat)

### Error de conexión en la app Android
- Verifica que la URL de la API sea correcta
- Verifica que el dispositivo/emulador tenga acceso a internet
- Verifica que el servidor API esté corriendo
- Si están en redes diferentes, usa un túnel o despliega en un servidor

### Error: "Network Error" o "CORS Error" (Dashboard)
- Verifica que la API esté corriendo
- Verifica la configuración de CORS en Laravel
- Verifica la URL en `.env` o `src/config/api.ts`

### Error en Render: "Application failed to respond"
- Verifica que el `Start Command` sea correcto
- Revisa los logs en Render Dashboard → Logs
- Verifica que las variables de entorno estén configuradas

---

## 📝 Licencia

[Especificar licencia]

---

## 👥 Autores

[Especificar autores]

---

## 🆘 Soporte

Para problemas o preguntas:

1. Revisar este README
2. Verificar los logs: `docker-compose logs -f` o en Render Dashboard
3. Consultar los issues existentes
4. Contactar al equipo de desarrollo

---

**Nota**: Este proyecto está en desarrollo activo. La documentación se actualiza regularmente.
