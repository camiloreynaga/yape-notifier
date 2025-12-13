# 🔍 REVISIÓN CRÍTICA - Yape Notifier API

## Arquitecto de Software Senior / DevOps Senior / Tech Lead

**Fecha de Revisión:** 2025-01-XX  
**Nivel de Revisión:** Producción Real  
**Stack:** Laravel 11, Docker, PostgreSQL, DigitalOcean

---

## 📊 RESUMEN EJECUTIVO

### Estado General del Proyecto

El proyecto muestra una **base sólida** con buena estructura de monorepo, uso de Services, validaciones con FormRequests y configuración Docker funcional. Sin embargo, **NO está listo para producción** sin implementar las mejoras críticas identificadas.

### Hallazgos Principales

| Categoría          | Críticos | Importantes | Mejoras Futuras |
| ------------------ | -------- | ----------- | --------------- |
| **Arquitectura**   | 3        | 2           | 2               |
| **Backend/API**    | 3        | 3           | 0               |
| **Seguridad**      | 4        | 2           | 0               |
| **Base de Datos**  | 3        | 2           | 0               |
| **Docker/DevOps**  | 5        | 6           | 0               |
| **Deploy/Infra**   | 4        | 3           | 0               |
| **Observabilidad** | 3        | 0           | 0               |
| **Testing**        | 2        | 0           | 0               |
| **TOTAL**          | **27**   | **18**      | **2**           |

### Top 10 Críticos (Prioridad Máxima)

1. 🔴 **Rate Limiting** - Sin protección contra abuso
2. 🔴 **Manejo de Excepciones** - Errores no estructurados
3. 🔴 **Exposición de Datos** - User completo en respuestas
4. 🔴 **CORS Permisivo** - Vulnerable a CSRF
5. 🔴 **Backups Automáticos** - Sin recuperación ante desastres
6. 🔴 **Healthchecks Reales** - No detectan problemas reales
7. 🔴 **Logging Estructurado** - Difícil debugging
8. 🔴 **Secrets Management** - Secrets en disco sin encriptar
9. 🔴 **Volúmenes Bind Mount** - Código modificable desde host
10. 🔴 **Tests de Carga** - Sin validación de requisitos

### Tiempo Estimado de Implementación

- **Críticos (MVP):** 3-5 días de desarrollo
- **Importantes (Post-MVP):** 5-7 días adicionales
- **Mejoras Futuras:** 2-3 días

### Recomendación Final

**NO desplegar a producción** hasta implementar al menos los **10 críticos** identificados. El proyecto tiene buena base pero requiere trabajo significativo en seguridad, observabilidad y operaciones antes de estar listo para clientes reales.

---

## 📋 ÍNDICE

1. [ARQUITECTURA GENERAL](#1-arquitectura-general)
2. [BACKEND / API (Laravel)](#2-backend--api-laravel)
3. [SEGURIDAD](#3-seguridad)
4. [BASE DE DATOS](#4-base-de-datos)
5. [DOCKER & DEVOPS](#5-docker--devops)
6. [DEPLOY & INFRAESTRUCTURA](#6-deploy--infraestructura)
7. [OBSERVABILIDAD](#7-observabilidad)
8. [TESTING](#8-testing)

---

## 1. ARQUITECTURA GENERAL

### ✅ Aspectos Positivos

- **Monorepo bien estructurado**: Separación clara entre `apps/api`, `apps/android-client`, `apps/web-dashboard`
- **Uso de Services**: Separación de lógica de negocio en `DeviceService` y `NotificationService`
- **FormRequests**: Validación centralizada con `FormRequest` classes
- **Relaciones Eloquent**: Modelos con relaciones bien definidas

### 🔴 CRÍTICO

#### 1.1 Falta de Rate Limiting

**Problema:** No hay rate limiting configurado en ninguna ruta.

**Impacto:**

- Vulnerable a ataques de fuerza bruta en `/api/login` y `/api/register`
- Posible DoS por spam de notificaciones en `/api/notifications`
- Sin protección contra abuso de API

**Solución:**

```php
// bootstrap/app.php
->withMiddleware(function (Middleware $middleware) {
    $middleware->api(prepend: [
        \Laravel\Sanctum\Http\Middleware\EnsureFrontendRequestsAreStateful::class,
    ]);

    // AGREGAR:
    $middleware->throttleApi('60,1'); // 60 requests per minute

    // O más específico:
    $middleware->alias([
        'throttle.auth' => \Illuminate\Routing\Middleware\ThrottleRequests::class,
    ]);
});

// routes/api.php
Route::post('/login', [AuthController::class, 'login'])
    ->middleware('throttle:5,1'); // 5 intentos por minuto

Route::post('/register', [AuthController::class, 'register'])
    ->middleware('throttle:3,1'); // 3 registros por minuto

Route::post('/notifications', [NotificationController::class, 'store'])
    ->middleware(['auth:sanctum', 'throttle:100,1']); // 100 por minuto
```

#### 1.2 Falta de Manejo Centralizado de Excepciones

**Problema:** `bootstrap/app.php` tiene `withExceptions` vacío.

**Impacto:**

- Errores no estructurados
- Información sensible puede filtrarse en producción
- Difícil debugging y logging

**Solución:**

```php
// bootstrap/app.php
->withExceptions(function (Exceptions $exceptions) {
    $exceptions->render(function (Throwable $e, Request $request) {
        if ($request->is('api/*')) {
            return response()->json([
                'message' => $e->getMessage(),
                'errors' => $e instanceof ValidationException
                    ? $e->errors()
                    : null,
            ], $this->getStatusCode($e));
        }
    });

    // Logging estructurado
    $exceptions->report(function (Throwable $e) {
        Log::error('Exception occurred', [
            'message' => $e->getMessage(),
            'file' => $e->getFile(),
            'line' => $e->getLine(),
            'trace' => $e->getTraceAsString(),
        ]);
    });
})
```

#### 1.3 Falta de Repositorios

**Problema:** Acceso directo a modelos desde Services y Controllers.

**Impacto:**

- Difícil testear (mocking complejo)
- Lógica de acceso a datos dispersa
- Cambios en BD requieren modificar múltiples lugares

**Solución:**

```php
// app/Repositories/NotificationRepository.php
class NotificationRepository
{
    public function findByUserAndFilters(User $user, array $filters): Builder
    {
        $query = Notification::where('user_id', $user->id)
            ->with('device')
            ->orderBy('received_at', 'desc');

        // Aplicar filtros...
        return $query;
    }

    public function findDuplicate(array $data, Device $device): ?Notification
    {
        // Lógica de duplicados
    }
}

// app/Services/NotificationService.php
public function __construct(
    protected NotificationRepository $repository
) {}
```

### 🟡 IMPORTANTE

#### 1.4 Falta de DTOs (Data Transfer Objects)

**Problema:** Arrays pasados directamente entre capas.

**Impacto:**

- Sin validación de tipos en tiempo de compilación
- Difícil mantener contratos entre capas
- Propenso a errores

**Solución:**

```php
// app/DTOs/CreateNotificationDTO.php
class CreateNotificationDTO
{
    public function __construct(
        public readonly string $deviceUuid,
        public readonly string $sourceApp,
        public readonly string $body,
        public readonly ?float $amount = null,
        public readonly ?string $currency = null,
        // ...
    ) {}

    public static function fromRequest(CreateNotificationRequest $request): self
    {
        return new self(...);
    }
}
```

#### 1.5 Falta de Eventos/Listeners para Notificaciones

**Problema:** Lógica síncrona en `NotificationService::createNotification()`.

**Impacto:**

- Difícil escalar (webhooks, analytics, etc.)
- Acoplamiento fuerte
- No se puede procesar asíncronamente

**Solución:**

```php
// app/Events/NotificationCreated.php
class NotificationCreated
{
    public function __construct(
        public Notification $notification
    ) {}
}

// app/Listeners/SendWebhook.php
class SendWebhook implements ShouldQueue
{
    public function handle(NotificationCreated $event): void
    {
        // Enviar webhook asíncrono
    }
}
```

---

## 2. BACKEND / API (Laravel)

### ✅ Aspectos Positivos

- **Controllers delgados**: Buena separación con Services
- **Validación con FormRequests**: Bien implementada
- **Uso de Eloquent**: Relaciones y queries eficientes
- **Sanctum para autenticación**: Elección adecuada

### 🔴 CRÍTICO

#### 2.1 Exposición de Datos Sensibles en Respuestas

**Problema:** `AuthController::register()` y `login()` devuelven el objeto `User` completo.

**Impacto:**

- Posible exposición de `email_verified_at`, `remember_token`, etc.
- Información innecesaria en cada respuesta

**Solución:**

```php
// app/Http/Resources/UserResource.php
class UserResource extends JsonResource
{
    public function toArray($request): array
    {
        return [
            'id' => $this->id,
            'name' => $this->name,
            'email' => $this->email,
            'email_verified_at' => $this->email_verified_at?->toIso8601String(),
        ];
    }
}

// AuthController.php
return response()->json([
    'message' => 'User registered successfully',
    'user' => new UserResource($user),
    'token' => $token,
], 201);
```

#### 2.2 Validación de Device UUID Insegura

**Problema:** `NotificationController::store()` busca device por UUID sin validar ownership explícitamente.

**Impacto:**

- Aunque `findDeviceByUuid` filtra por `user_id`, el código no es explícito
- Posible confusión en mantenimiento futuro

**Solución:**

```php
// Ya está bien, pero hacer más explícito:
$device = $this->deviceService->findDeviceByUuid($user, $deviceUuid);

if (!$device) {
    return response()->json([
        'message' => 'Device not found or does not belong to you',
    ], 404);
}
```

#### 2.3 Falta de Validación de Estado de Usuario

**Problema:** No se valida si el usuario está activo/baneado antes de autenticar.

**Impacto:**

- Usuarios deshabilitados pueden seguir accediendo
- Sin control de acceso granular

**Solución:**

```php
// app/Models/User.php
protected $fillable = [
    'name',
    'email',
    'password',
    'is_active', // AGREGAR
    'banned_at', // AGREGAR
];

// app/Http/Controllers/AuthController.php
public function login(LoginRequest $request): JsonResponse
{
    $user = User::where('email', $request->email)->first();

    if (!$user || !Hash::check($request->password, $user->password)) {
        throw ValidationException::withMessages([
            'email' => ['The provided credentials are incorrect.'],
        ]);
    }

    // AGREGAR:
    if (!$user->is_active || $user->banned_at) {
        throw ValidationException::withMessages([
            'email' => ['Your account has been disabled.'],
        ]);
    }

    // ...
}
```

### 🟡 IMPORTANTE

#### 2.4 Lógica de Duplicados Mejorable

**Problema:** `checkDuplicate()` usa ventana de 5 segundos fija y compara solo `body`.

**Impacto:**

- Puede fallar con notificaciones similares pero diferentes
- No considera `amount` ni `payer_name`

**Solución:**

```php
protected function checkDuplicate(array $data, Device $device): bool
{
    $receivedAt = isset($data['received_at'])
        ? Carbon::parse($data['received_at'])
        : now();

    $startTime = $receivedAt->copy()->subSeconds(10);
    $endTime = $receivedAt->copy()->addSeconds(10);

    $query = Notification::where('device_id', $device->id)
        ->where('source_app', $data['source_app'])
        ->whereBetween('received_at', [$startTime, $endTime]);

    // Comparar múltiples campos
    if (isset($data['amount'])) {
        $query->where('amount', $data['amount']);
    }

    if (isset($data['payer_name'])) {
        $query->where('payer_name', $data['payer_name']);
    }

    // Hash del body para comparación más robusta
    $bodyHash = hash('sha256', $data['body']);
    $query->whereRaw('SHA256(body) = ?', [$bodyHash]);

    return $query->exists();
}
```

#### 2.5 Falta de Paginación en Estadísticas

**Problema:** `getStatistics()` carga todos los registros en memoria para agrupar por fecha.

**Impacto:**

- Con muchos datos, puede causar OOM (Out of Memory)
- Performance degradada

**Solución:**

```php
// Usar agregaciones de BD en lugar de cargar todo
$byDate = (clone $query)
    ->select(
        DB::raw("DATE(received_at) as date"),
        DB::raw('count(*) as count'),
        DB::raw('COALESCE(sum(amount), 0) as total_amount')
    )
    ->groupBy(DB::raw('DATE(received_at)'))
    ->get()
    ->mapWithKeys(function ($item) {
        return [$item->date => [
            'count' => (int) $item->count,
            'total_amount' => (float) $item->total_amount,
        ]];
    })
    ->toArray();
```

#### 2.6 Falta de Caché en Estadísticas

**Problema:** `getStatistics()` ejecuta múltiples queries pesadas en cada request.

**Impacto:**

- Alto uso de CPU y BD
- Respuestas lentas

**Solución:**

```php
public function getStatistics(User $user, array $filters = []): array
{
    $cacheKey = "user_stats_{$user->id}_" . md5(serialize($filters));

    return Cache::remember($cacheKey, now()->addMinutes(5), function () use ($user, $filters) {
        // Lógica actual...
    });
}
```

---

## 3. SEGURIDAD

### 🔴 CRÍTICO

#### 3.1 CORS Demasiado Permisivo

**Problema:** `config/cors.php` permite `'*'` en `allowed_origins` (aunque hay lista específica).

**Impacto:**

- Vulnerable a CSRF desde cualquier origen
- Información sensible expuesta

**Solución:**

```php
// config/cors.php
'allowed_origins' => [
    env('APP_URL', 'http://localhost:8000'),
    env('FRONTEND_URL', 'https://notificaciones.space'),
    // NO usar '*' en producción
],

'allowed_origins_patterns' => [
    // Si necesitas subdominios:
    '/^https:\/\/.*\.notificaciones\.space$/',
],
```

#### 3.2 Falta de Validación de Token Expiration

**Problema:** Sanctum configurado con `expiration: null` (tokens nunca expiran).

**Impacto:**

- Tokens comprometidos son válidos indefinidamente
- Sin rotación de tokens

**Solución:**

```php
// config/sanctum.php
'expiration' => env('SANCTUM_TOKEN_EXPIRATION', 60 * 24), // 24 horas

// O implementar refresh tokens
```

#### 3.3 Falta de Hashing en UUID de Dispositivos

**Problema:** UUIDs se almacenan en texto plano.

**Impacto:**

- Si se filtra la BD, se pueden asociar dispositivos a usuarios
- Tracking cruzado posible

**Solución:**

```php
// app/Models/Device.php
protected static function boot()
{
    parent::boot();

    static::creating(function ($device) {
        if (empty($device->uuid)) {
            $device->uuid = (string) Str::uuid();
        }
        // Hash del UUID para almacenamiento
        $device->uuid_hash = hash('sha256', $device->uuid);
    });
}

// Buscar por hash
public function findDeviceByUuidHash(User $user, string $uuid): ?Device
{
    $hash = hash('sha256', $uuid);
    return Device::where('user_id', $user->id)
        ->where('uuid_hash', $hash)
        ->first();
}
```

#### 3.4 Falta de Rate Limiting por Usuario

**Problema:** Rate limiting global, no por usuario autenticado.

**Impacto:**

- Un usuario puede saturar la API
- Sin protección contra abuso individual

**Solución:**

```php
// Usar throttle con identificador de usuario
Route::middleware(['auth:sanctum', 'throttle:100,1'])->group(function () {
    // Rutas protegidas
});

// O custom rate limiter
RateLimiter::for('notifications', function (Request $request) {
    return Limit::perMinute(100)->by($request->user()?->id ?: $request->ip());
});
```

### 🟡 IMPORTANTE

#### 3.5 Falta de Validación de Email

**Problema:** No se valida ni verifica email en registro.

**Impacto:**

- Cuentas con emails falsos
- Sin recuperación de contraseña

**Solución:**

```php
// Agregar verificación de email
// app/Http/Controllers/AuthController.php
public function register(RegisterRequest $request): JsonResponse
{
    $user = User::create([...]);

    // Enviar email de verificación
    $user->sendEmailVerificationNotification();

    // ...
}
```

#### 3.6 Falta de Logging de Actividades Sospechosas

**Problema:** No se registran intentos de login fallidos, accesos no autorizados, etc.

**Impacto:**

- Sin trazabilidad de ataques
- Difícil detectar patrones de abuso

**Solución:**

```php
// app/Http/Controllers/AuthController.php
public function login(LoginRequest $request): JsonResponse
{
    $user = User::where('email', $request->email)->first();

    if (!$user || !Hash::check($request->password, $user->password)) {
        // LOGGING
        Log::warning('Failed login attempt', [
            'email' => $request->email,
            'ip' => $request->ip(),
            'user_agent' => $request->userAgent(),
        ]);

        throw ValidationException::withMessages([...]);
    }

    // Logging de login exitoso
    Log::info('User logged in', [
        'user_id' => $user->id,
        'ip' => $request->ip(),
    ]);

    // ...
}
```

---

## 4. BASE DE DATOS

### ✅ Aspectos Positivos

- **Índices bien definidos**: En `notifications` para queries frecuentes
- **Foreign keys con cascade**: Buen manejo de integridad referencial
- **Tipos de datos apropiados**: `decimal(10,2)` para montos

### 🔴 CRÍTICO

#### 4.1 Falta de Índice Compuesto en Búsqueda de Duplicados

**Problema:** `checkDuplicate()` usa múltiples `where` pero el índice no está optimizado.

**Impacto:**

- Queries lentas con muchos registros
- Escalabilidad limitada

**Solución:**

```php
// database/migrations/..._create_notifications_table.php
// AGREGAR índice compuesto optimizado:
$table->index(['device_id', 'source_app', 'received_at', 'body'], 'notifications_duplicate_check');

// O mejor, usar hash del body:
$table->string('body_hash', 64)->nullable()->after('body');
$table->index(['device_id', 'source_app', 'received_at', 'body_hash'], 'notifications_duplicate_check');
```

#### 4.2 Falta de Soft Deletes

**Problema:** `delete()` elimina registros permanentemente.

**Impacto:**

- Pérdida de datos históricos
- Sin auditoría

**Solución:**

```php
// app/Models/Notification.php
use Illuminate\Database\Eloquent\SoftDeletes;

class Notification extends Model
{
    use SoftDeletes;

    protected $dates = ['deleted_at'];
}

// Migration
$table->softDeletes();
```

#### 4.3 Falta de Particionamiento en Tabla de Notificaciones

**Problema:** Tabla `notifications` crecerá indefinidamente.

**Impacto:**

- Performance degradada con millones de registros
- Queries lentas

**Solución:**

```sql
-- PostgreSQL: Particionar por mes/año
CREATE TABLE notifications_2025_01 PARTITION OF notifications
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

-- O implementar archiving automático
```

### 🟡 IMPORTANTE

#### 4.4 Falta de Campos de Auditoría

**Problema:** No se registra quién creó/modificó registros.

**Impacto:**

- Sin trazabilidad
- Difícil debugging

**Solución:**

```php
// Agregar campos
$table->unsignedBigInteger('created_by')->nullable();
$table->unsignedBigInteger('updated_by')->nullable();

// O usar paquete como spatie/laravel-activitylog
```

#### 4.5 Falta de Índice en `users.email`

**Problema:** Aunque hay `unique`, el índice puede no estar optimizado para búsquedas.

**Solución:**

```php
// Ya está con unique, pero verificar que sea case-insensitive si es necesario
$table->string('email')->unique()->index();
```

---

## 5. DOCKER & DEVOPS

### ✅ Aspectos Positivos

- **Multi-stage build implícito**: Dockerfile optimizado
- **Healthchecks**: Bien configurados
- **Separación staging/producción**: `docker-compose.staging.yml`
- **OPcache configurado**: Para producción
- **PHP-FPM tuning**: Configuración de workers adecuada

### 🔴 CRÍTICO

#### 5.1 Dockerfile Expone Secretos en Build

**Problema:** `composer install` se ejecuta durante build, puede cachear credenciales.

**Impacto:**

- Posible filtración de secrets en imagen
- Vulnerabilidad de seguridad
- Imagen puede contener información sensible

**Solución:**

```dockerfile
# Dockerfile.production - VERSIÓN MEJORADA
FROM php:8.2-fpm-alpine AS builder

WORKDIR /var/www

# Instalar dependencias del sistema
RUN apk add --no-cache \
    git \
    curl \
    libpng-dev \
    oniguruma-dev \
    libxml2-dev \
    libzip-dev \
    zip \
    unzip \
    postgresql-dev \
    freetype-dev \
    libjpeg-turbo-dev \
    icu-dev

# Instalar extensiones PHP
RUN docker-php-ext-configure gd --with-freetype --with-jpeg \
    && docker-php-ext-install -j$(nproc) \
        pdo pdo_pgsql pgsql mbstring exif pcntl bcmath gd zip opcache intl

# Instalar Composer
COPY --from=composer:latest /usr/bin/composer /usr/bin/composer

# Copiar solo archivos de dependencias (mejor caching)
COPY composer.json composer.lock* ./

# Instalar dependencias SIN scripts (no ejecutar scripts que requieren .env)
RUN composer install --no-dev --optimize-autoloader --no-interaction --prefer-dist --no-scripts

# ============================================
# STAGE 2: Production
# ============================================
FROM php:8.2-fpm-alpine AS production

WORKDIR /var/www

# Copiar extensiones y configuración PHP desde builder
COPY --from=builder /usr/local/etc/php/conf.d/ /usr/local/etc/php/conf.d/
COPY --from=builder /usr/local/lib/php/extensions/ /usr/local/lib/php/extensions/

# Instalar solo runtime dependencies (sin dev tools)
RUN apk add --no-cache \
    libpng \
    oniguruma \
    libxml2 \
    libzip \
    postgresql-libs \
    freetype \
    libjpeg-turbo \
    icu-libs

# Copiar vendor desde builder
COPY --from=builder /var/www/vendor /var/www/vendor

# Copiar código de aplicación
COPY . /var/www

# Configurar OPcache (ya configurado en builder, pero asegurar)
RUN echo "opcache.validate_timestamps=0" >> /usr/local/etc/php/conf.d/opcache.ini

# Configurar PHP-FPM
RUN echo "pm = dynamic" >> /usr/local/etc/php-fpm.d/www.conf \
    && echo "pm.max_children = 50" >> /usr/local/etc/php-fpm.d/www.conf \
    && echo "pm.start_servers = 5" >> /usr/local/etc/php-fpm.d/www.conf \
    && echo "pm.min_spare_servers = 5" >> /usr/local/etc/php-fpm.d/www.conf \
    && echo "pm.max_spare_servers = 35" >> /usr/local/etc/php-fpm.d/www.conf \
    && echo "pm.max_requests = 500" >> /usr/local/etc/php-fpm.d/www.conf

# Crear directorios y permisos
RUN mkdir -p /var/www/storage/framework/{sessions,views,cache} \
    && mkdir -p /var/www/storage/logs \
    && mkdir -p /var/www/bootstrap/cache \
    && chown -R www-data:www-data /var/www \
    && chmod -R 775 /var/www/storage \
    && chmod -R 775 /var/www/bootstrap/cache

# Ejecutar scripts de composer SOLO en runtime (con .env disponible)
# NO en build time

EXPOSE 9000

CMD ["php-fpm", "-F"]
```

**Nota:** Los scripts de composer (como `post-install-cmd`) deben ejecutarse en runtime, no en build.

#### 5.2 Volúmenes Bind Mount en Producción

**Problema:** `docker-compose.yml` usa bind mount `./apps/api:/var/www` en producción.

**Impacto:**

- Código puede modificarse desde host (riesgo de seguridad)
- Inconsistencias entre contenedores
- Performance degradada (I/O más lento)
- Cambios en host afectan contenedor sin rebuild

**Solución:**

```yaml
# docker-compose.yml - VERSIÓN PRODUCCIÓN
services:
  php-fpm:
    build:
      context: ./apps/api
      dockerfile: Dockerfile.production
    # NO usar bind mount en producción:
    # volumes:
    #   - ./apps/api:/var/www  # ❌ ELIMINAR

    # Solo montar volúmenes nombrados para datos persistentes:
    volumes:
      - storage_data:/var/www/storage
      - cache_data:/var/www/bootstrap/cache
      # Logs pueden ir a volumen o stdout
      - ./logs:/var/log/php-fpm:rw

volumes:
  storage_data:
    driver: local
  cache_data:
    driver: local
```

**Para desarrollo, mantener bind mount pero en `docker-compose.dev.yml` separado.**

#### 5.3 Falta de .dockerignore

**Problema:** No hay `.dockerignore` en `apps/api/`, se copia todo al contexto de build.

**Impacto:**

- Builds lentos (copia archivos innecesarios)
- Posible filtración de archivos sensibles (.env, etc.)
- Contexto de build grande
- Vulnerabilidades de seguridad

**Solución:**

```dockerignore
# apps/api/.dockerignore

# Archivos de entorno (NUNCA incluir)
.env
.env.*
!.env.example

# Git
.git
.gitignore
.gitattributes

# Dependencias (se instalan en build)
vendor/
node_modules/

# Logs y cache
storage/logs/*
!storage/logs/.gitkeep
bootstrap/cache/*
!bootstrap/cache/.gitkeep
storage/framework/cache/*
storage/framework/sessions/*
storage/framework/views/*

# Tests
tests/
.phpunit.result.cache
phpunit.xml

# IDE
.idea/
.vscode/
*.swp
*.swo
*~

# OS
.DS_Store
Thumbs.db

# Build artifacts
build/
dist/

# Documentación (opcional, reducir tamaño)
docs/
*.md
!README.md

# Scripts de desarrollo
docker-compose*.yml
Dockerfile.dev
Makefile
```

#### 5.4 Healthcheck Demasiado Simple

**Problema:** Healthcheck solo verifica que PHP funciona (`php -r 'exit(0);'`), no que la app responde.

**Impacto:**

- Contenedor puede estar "sano" pero app caída (BD desconectada, etc.)
- Sin detección real de problemas
- Orquestadores (Docker Swarm, Kubernetes) no detectan fallos reales

**Solución:**

```yaml
# docker-compose.yml
services:
  php-fpm:
    healthcheck:
      # Opción 1: Verificar endpoint de health check real
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://nginx/up"]
      # Requiere wget instalado en imagen

      # Opción 2: Verificar que PHP-FPM responde
      test: ["CMD-SHELL", "php-fpm-healthcheck || exit 1"]
      # Requiere script de healthcheck

      # Opción 3: Verificar socket PHP-FPM
      test: ["CMD-SHELL", "test -S /var/run/php-fpm.sock || exit 1"]

      # Opción 4: Verificar conexión a BD (más completo)
      test: ["CMD-SHELL", "php -r 'try { new PDO(\"pgsql:host=db;dbname=${DB_DATABASE}\", \"${DB_USERNAME}\", \"${DB_PASSWORD}\"); exit(0); } catch (Exception $e) { exit(1); }'"]

      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  nginx:
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost/up"]
      interval: 30s
      timeout: 10s
      retries: 3

  db:
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME:-postgres} -d ${DB_DATABASE:-yape_notifier}"]
      interval: 10s
      timeout: 5s
      retries: 5
```

**Recomendación:** Usar Opción 4 para PHP-FPM (verifica BD también).

#### 5.5 Falta de Multi-Stage Build Optimizado

**Problema:** Dockerfile no usa multi-stage build explícito, imagen final incluye herramientas de build.

**Impacto:**

- Imagen más grande (más superficie de ataque)
- Incluye git, composer, etc. innecesarios en producción
- Mayor tiempo de pull/push

**Solución:**
Ver solución en 5.1 (ya incluye multi-stage).

#### 5.6 Composer Install Falla Silenciosamente

**Problema:** `composer install` tiene `|| true`, falla silenciosamente.

**Impacto:**

- Build puede "succeed" sin dependencias instaladas
- Errores ocultos
- App falla en runtime sin aviso

**Solución:**

```dockerfile
# Dockerfile.production
# ELIMINAR || true
RUN composer install --no-dev --optimize-autoloader --no-interaction --prefer-dist --no-scripts

# Si realmente necesitas continuar en caso de error (no recomendado):
# RUN composer install ... || (echo "Composer install failed" && exit 1)
```

### 🟡 IMPORTANTE

#### 5.7 Falta de Límites de Recursos

**Problema:** No hay `deploy.resources` en docker-compose.

**Impacto:**

- Un contenedor puede consumir todos los recursos del host
- Sin protección contra OOM (Out of Memory)
- Sin garantías de recursos mínimos
- Difícil planificar capacidad

**Solución:**

```yaml
# docker-compose.yml
services:
  php-fpm:
    deploy:
      resources:
        limits:
          cpus: "2.0" # Máximo 2 CPUs
          memory: 1G # Máximo 1GB RAM
        reservations:
          cpus: "0.5" # Mínimo 0.5 CPU garantizado
          memory: 512M # Mínimo 512MB RAM garantizado

  nginx:
    deploy:
      resources:
        limits:
          cpus: "0.5"
          memory: 256M
        reservations:
          cpus: "0.1"
          memory: 128M

  db:
    deploy:
      resources:
        limits:
          cpus: "1.0"
          memory: 2G
        reservations:
          cpus: "0.5"
          memory: 1G

  caddy:
    deploy:
      resources:
        limits:
          cpus: "0.5"
          memory: 128M
        reservations:
          cpus: "0.1"
          memory: 64M
```

**Nota:** `deploy.resources` solo funciona en Docker Swarm mode. Para Docker Compose standalone, usar `mem_limit` y `cpus` (deprecated) o actualizar a Docker Compose v2 con Swarm.

#### 5.8 Falta de Logging Centralizado

**Problema:** Logs solo en archivos locales, no centralizados.

**Impacto:**

- Difícil debugging en producción (múltiples contenedores)
- Sin agregación de logs
- Difícil correlacionar eventos entre servicios
- Sin búsqueda centralizada

**Solución:**

```yaml
# docker-compose.yml
services:
  php-fpm:
    logging:
      driver: "json-file"
      options:
        max-size: "10m" # Rotar a 10MB
        max-file: "3" # Mantener 3 archivos
        labels: "service=api,environment=production"

    # O usar driver de logging externo (recomendado para producción):
    # logging:
    #   driver: "syslog"
    #   options:
    #     syslog-address: "tcp://log-server:514"
    #     syslog-facility: "daemon"
    #     tag: "yape-notifier-api"

    # O usar Fluentd/Fluent Bit:
    # logging:
    #   driver: "fluentd"
    #   options:
    #     fluentd-address: "localhost:24224"
    #     tag: "docker.{{.Name}}"

  nginx:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
        labels: "service=nginx"

  db:
    logging:
      driver: "json-file"
      options:
        max-size: "50m" # BD genera más logs
        max-file: "5"
        labels: "service=postgres"
```

**Herramientas recomendadas para producción:**

- **Loki + Grafana** (open source, ligero)
- **ELK Stack** (Elasticsearch, Logstash, Kibana)
- **Datadog Logs** (SaaS, fácil setup)
- **CloudWatch Logs** (si usas AWS)

#### 5.9 Falta de Secrets Management

**Problema:** Secrets en `.env` files en disco, no en Docker secrets.

**Impacto:**

- Secrets en disco sin encriptar
- Acceso no controlado (cualquiera con acceso al host puede leer)
- Sin rotación automática
- Difícil auditar acceso a secrets

**Solución:**

```yaml
# docker-compose.yml
services:
  php-fpm:
    secrets:
      - app_key
      - db_password
    environment:
      APP_KEY_FILE: /run/secrets/app_key
      DB_PASSWORD_FILE: /run/secrets/db_password
    # O usar directamente (Docker inyecta el valor):
    # environment:
    #   APP_KEY: /run/secrets/app_key  # Docker lee el archivo automáticamente

  db:
    secrets:
      - db_password
    environment:
      POSTGRES_PASSWORD_FILE: /run/secrets/db_password

secrets:
  app_key:
    file: ./secrets/app_key.txt
    # O usar external (gestión externa):
    # external: true

  db_password:
    file: ./secrets/db_password.txt
    # external: true
```

**Para producción real, usar:**

- **HashiCorp Vault** (secrets management)
- **AWS Secrets Manager** (si usas AWS)
- **DigitalOcean Secrets** (si usas DO App Platform)
- **Docker Swarm Secrets** (nativo, pero requiere Swarm)

#### 5.10 Falta de Network Security

**Problema:** Todos los servicios en la misma red, sin segmentación.

**Impacto:**

- Si un servicio es comprometido, puede acceder a todos
- Sin aislamiento de red
- Difícil cumplir compliance (PCI-DSS, etc.)

**Solución:**

```yaml
# docker-compose.yml
networks:
  frontend:
    driver: bridge
    internal: false # Puede acceder a internet

  backend:
    driver: bridge
    internal: true # NO puede acceder a internet (solo comunicación interna)

services:
  caddy:
    networks:
      - frontend

  nginx:
    networks:
      - frontend
      - backend

  php-fpm:
    networks:
      - backend

  db:
    networks:
      - backend
    # NO exponer puerto al host
    # ports:
    #   - "5432:5432"  # ❌ ELIMINAR
```

#### 5.11 Falta de Actualización Automática de Imágenes

**Problema:** No hay proceso para actualizar imágenes base (PHP, PostgreSQL, etc.).

**Impacto:**

- Vulnerabilidades no parcheadas
- Sin actualizaciones de seguridad automáticas

**Solución:**

```bash
# Script de actualización
#!/bin/bash
# update-images.sh

set -e

echo "Actualizando imágenes base..."
docker-compose pull

echo "Reconstruyendo servicios..."
docker-compose build --pull  # --pull fuerza actualizar imágenes base

echo "Reiniciando servicios..."
docker-compose up -d

echo "Verificando health..."
sleep 10
docker-compose ps

# Cron job semanal:
# 0 2 * * 0 /path/to/update-images.sh
```

**O usar herramientas:**

- **Dependabot** (GitHub)
- **Renovate** (auto-update Docker images)
- **Watchtower** (actualiza contenedores automáticamente - ⚠️ usar con cuidado)

#### 5.12 Falta de Estrategia de Rollback en Docker

**Problema:** No hay proceso para revertir a versión anterior de imagen.

**Impacto:**

- Sin recuperación rápida ante errores
- Downtime prolongado

**Solución:**

```bash
# Script de rollback
#!/bin/bash
# rollback-docker.sh

VERSION=$1  # ej: v1.2.3

if [ -z "$VERSION" ]; then
    echo "Uso: $0 <version>"
    echo "Versiones disponibles:"
    docker images | grep yape-notifier-api
    exit 1
fi

echo "Haciendo rollback a versión $VERSION..."

# Tag actual como backup
docker tag yape-notifier-api:latest yape-notifier-api:backup-$(date +%Y%m%d-%H%M%S)

# Tag versión anterior como latest
docker tag yape-notifier-api:$VERSION yape-notifier-api:latest

# Reiniciar servicios
docker-compose up -d --force-recreate php-fpm

# Verificar health
sleep 10
curl -f http://localhost/up || {
    echo "Rollback falló, restaurando..."
    docker tag yape-notifier-api:backup-* yape-notifier-api:latest
    docker-compose up -d --force-recreate php-fpm
    exit 1
}

echo "Rollback exitoso a $VERSION"
```

**Mejor práctica:** Usar tags semánticos (`v1.2.3`) y mantener historial de imágenes.

---

## 6. DEPLOY & INFRAESTRUCTURA

### 🔴 CRÍTICO

#### 6.1 Falta de Backup Automático de BD

**Problema:** No hay estrategia de backups documentada ni automatizada.

**Impacto:**

- Pérdida de datos en caso de fallo
- Sin recuperación ante desastres
- Sin cumplimiento de políticas de retención
- Pérdida de negocio si falla el servidor

**Solución:**

```bash
#!/bin/bash
# backup-db.sh - Backup diario de PostgreSQL

set -e

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/var/backups/yape-notifier"
RETENTION_DAYS=30

mkdir -p $BACKUP_DIR

# Backup usando pg_dump desde contenedor
docker-compose exec -T db pg_dump -U postgres yape_notifier \
    --format=custom \
    --file=/tmp/backup_$DATE.dump

# Copiar backup fuera del contenedor
docker cp yape-notifier-db:/tmp/backup_$DATE.dump $BACKUP_DIR/

# Comprimir
gzip $BACKUP_DIR/backup_$DATE.dump

# Limpiar backup del contenedor
docker-compose exec db rm /tmp/backup_$DATE.dump

# Retener solo últimos N días
find $BACKUP_DIR -name "backup_*.dump.gz" -mtime +$RETENTION_DAYS -delete

# Verificar integridad del backup más reciente
LATEST_BACKUP=$(ls -t $BACKUP_DIR/backup_*.dump.gz | head -1)
if [ -n "$LATEST_BACKUP" ]; then
    echo "Backup creado: $LATEST_BACKUP"
    echo "Tamaño: $(du -h $LATEST_BACKUP | cut -f1)"
fi

# Opcional: Subir a S3/Backblaze/etc.
# aws s3 cp $LATEST_BACKUP s3://backups-yape-notifier/

# Cron job (diario a las 2 AM):
# 0 2 * * * /var/apps/yape-notifier/scripts/backup-db.sh >> /var/log/backup-db.log 2>&1
```

**Script de restauración:**

```bash
#!/bin/bash
# restore-db.sh

BACKUP_FILE=$1

if [ -z "$BACKUP_FILE" ]; then
    echo "Uso: $0 <backup_file.dump.gz>"
    echo "Backups disponibles:"
    ls -lh /var/backups/yape-notifier/
    exit 1
fi

# Descomprimir si es necesario
if [[ $BACKUP_FILE == *.gz ]]; then
    gunzip -c $BACKUP_FILE > /tmp/restore.dump
    BACKUP_FILE=/tmp/restore.dump
fi

# Restaurar
docker-compose exec -T db pg_restore -U postgres -d yape_notifier --clean --if-exists < $BACKUP_FILE

echo "Base de datos restaurada desde $BACKUP_FILE"
```

#### 6.2 Falta de Monitoreo y Alertas

**Problema:** No hay sistema de monitoreo (CPU, memoria, disco, BD).

**Impacto:**

- Sin detección proactiva de problemas
- Downtime no detectado
- Sin métricas de performance
- Sin alertas tempranas

**Solución Completa:**

**1. Health Checks Externos:**

```bash
# Uptime Robot / Pingdom configurar:
# - URL: https://api.notificaciones.space/up
# - Interval: 5 minutos
# - Alertas: Email, SMS, Slack
```

**2. Monitoreo de Servidor (DigitalOcean):**

- Activar **DigitalOcean Monitoring** (incluido en droplets)
- Configurar alertas para:
  - CPU > 80%
  - Memoria > 90%
  - Disco > 85%
  - Tráfico de red anómalo

**3. Monitoreo de Aplicación:**

```php
// Integrar Sentry
// composer require sentry/sentry-laravel

// config/sentry.php (auto-generado)
SENTRY_LARAVEL_DSN=your-dsn-here

// En bootstrap/app.php
->withExceptions(function (Exceptions $exceptions) {
    $exceptions->report(function (Throwable $e) {
        if (app()->bound('sentry')) {
            app('sentry')->captureException($e);
        }
    });
})
```

**4. Monitoreo de Base de Datos:**

```bash
# Script de monitoreo de BD
#!/bin/bash
# monitor-db.sh

# Verificar conexiones
CONNECTIONS=$(docker-compose exec -T db psql -U postgres -d yape_notifier -t -c "SELECT count(*) FROM pg_stat_activity;")

if [ $CONNECTIONS -gt 50 ]; then
    echo "ALERTA: Demasiadas conexiones ($CONNECTIONS)"
    # Enviar alerta
fi

# Verificar tamaño de BD
DB_SIZE=$(docker-compose exec -T db psql -U postgres -d yape_notifier -t -c "SELECT pg_size_pretty(pg_database_size('yape_notifier'));")

echo "Tamaño BD: $DB_SIZE"

# Verificar tablas grandes
docker-compose exec -T db psql -U postgres -d yape_notifier -c "
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
LIMIT 10;
"
```

**5. Logs Centralizados:**

```yaml
# docker-compose.yml - Agregar servicio de logging
services:
  # ... otros servicios

  loki:
    image: grafana/loki:latest
    volumes:
      - loki_data:/loki
    command: -config.file=/etc/loki/local-config.yaml
    networks:
      - yape-network

  promtail:
    image: grafana/promtail:latest
    volumes:
      - ./logs:/var/log:ro
      - ./promtail-config.yml:/etc/promtail/config.yml
    command: -config.file=/etc/promtail/config.yml
    networks:
      - yape-network
    depends_on:
      - loki

volumes:
  loki_data:
```

**Herramientas Recomendadas:**

- **Uptime Robot** (gratis) - Health checks externos
- **DigitalOcean Monitoring** (incluido) - Métricas de servidor
- **Sentry** (freemium) - Errores de aplicación
- **Grafana Cloud** (freemium) - Métricas y logs
- **Datadog** (pago) - Todo-en-uno

#### 6.3 Falta de SSL/TLS Verification

**Problema:** Caddy maneja HTTPS, pero no hay verificación de certificados en app Android.

**Impacto:**

- Posible MITM (Man-in-the-Middle)
- Comunicación no segura
- Vulnerable a ataques de interceptación

**Solución:**

```kotlin
// apps/android-client/.../RetrofitClient.kt

object RetrofitClient {
    // Obtener pin del certificado:
    // openssl s_client -connect api.notificaciones.space:443 -servername api.notificaciones.space < /dev/null 2>/dev/null | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64

    private val certificatePinner = CertificatePinner.Builder()
        .add("api.notificaciones.space", "sha256/TU_HASH_AQUI")
        .build()

    fun createApiService(context: Context): ApiService {
        val okHttpClient = OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        // ...
    }
}
```

**Verificar certificado en servidor:**

```bash
# Obtener hash del certificado
openssl s_client -connect api.notificaciones.space:443 \
    -servername api.notificaciones.space < /dev/null 2>/dev/null | \
    openssl x509 -pubkey -noout | \
    openssl pkey -pubin -outform der | \
    openssl dgst -sha256 -binary | \
    openssl enc -base64
```

#### 6.4 Falta de Estrategia de Zero-Downtime Deploy

**Problema:** Deploy actual detiene servicios durante actualización.

**Impacto:**

- Downtime durante deploy
- Pérdida de requests en proceso
- Mala experiencia de usuario

**Solución:**

```bash
#!/bin/bash
# deploy-zero-downtime.sh

set -e

cd /var/apps/yape-notifier

# 1. Pull código
git pull origin main

# 2. Build nueva imagen (sin afectar actual)
docker-compose build php-fpm

# 3. Crear nuevo contenedor con nombre temporal
docker-compose -f docker-compose.yml \
    -f docker-compose.blue-green.yml \
    up -d --scale php-fpm-blue=1 --no-deps php-fpm-blue

# 4. Health check del nuevo contenedor
sleep 10
if ! curl -f http://localhost:9001/up; then
    echo "Nuevo contenedor no está sano, abortando deploy"
    docker-compose -f docker-compose.blue-green.yml down
    exit 1
fi

# 5. Cambiar Nginx a nuevo backend (blue-green switch)
# Actualizar upstream en Nginx
docker-compose exec nginx nginx -s reload

# 6. Esperar que requests terminen en contenedor viejo
sleep 30

# 7. Detener contenedor viejo
docker-compose stop php-fpm

# 8. Limpiar
docker-compose -f docker-compose.blue-green.yml down
docker system prune -f

echo "Deploy completado sin downtime"
```

**O usar Docker Swarm con rolling updates:**

```yaml
# docker-compose.swarm.yml
services:
  php-fpm:
    deploy:
      replicas: 2
      update_config:
        parallelism: 1 # Actualizar 1 a la vez
        delay: 10s # Esperar 10s entre updates
        failure_action: rollback
        monitor: 30s
      rollback_config:
        parallelism: 1
        delay: 5s
```

### 🟡 IMPORTANTE

#### 6.5 Falta de CI/CD Pipeline

**Problema:** Deploy manual, sin automatización.

**Impacto:**

- Errores humanos en deploy
- Sin tests automáticos antes de deploy
- Sin validación de código
- Deploys inconsistentes

**Solución Completa:**

```yaml
# .github/workflows/deploy.yml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: postgres
          POSTGRES_DB: yape_notifier_test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v3

      - name: Setup PHP
        uses: shivammathur/setup-php@v2
        with:
          php-version: "8.2"
          extensions: pdo, pdo_pgsql, mbstring, xml, ctype, json

      - name: Install dependencies
        run: |
          cd apps/api
          composer install --prefer-dist --no-progress

      - name: Run tests
        env:
          DB_CONNECTION: pgsql
          DB_HOST: postgres
          DB_PORT: 5432
          DB_DATABASE: yape_notifier_test
          DB_USERNAME: postgres
          DB_PASSWORD: postgres
        run: |
          cd apps/api
          php artisan test --coverage

      - name: Upload coverage
        uses: codecov/codecov-action@v3
        with:
          file: ./apps/api/coverage.xml

  build:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'

    steps:
      - uses: actions/checkout@v3

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v2

      - name: Login to Container Registry
        uses: docker/login-action@v2
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push
        uses: docker/build-push-action@v4
        with:
          context: ./apps/api
          file: ./apps/api/Dockerfile.production
          push: true
          tags: |
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}

  deploy:
    needs: [test, build]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'

    steps:
      - uses: actions/checkout@v3

      - name: Deploy to DigitalOcean
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.DROPLET_IP }}
          username: ${{ secrets.DROPLET_USER }}
          key: ${{ secrets.SSH_KEY }}
          script: |
            cd /var/apps/yape-notifier
            git pull origin main

            # Pull nueva imagen
            docker-compose pull php-fpm || true

            # Deploy con zero-downtime
            docker-compose up -d --build --no-deps php-fpm

            # Ejecutar migraciones
            docker-compose exec -T php-fpm php artisan migrate --force

            # Limpiar cache
            docker-compose exec -T php-fpm php artisan config:cache
            docker-compose exec -T php-fpm php artisan route:cache

            # Verificar health
            sleep 10
            curl -f http://localhost/up || exit 1
```

#### 6.6 Falta de Estrategia de Rollback

**Problema:** No hay proceso documentado para revertir deploys.

**Impacto:**

- Sin recuperación rápida ante errores
- Downtime prolongado
- Pérdida de datos si migración falla

**Solución:**

```bash
#!/bin/bash
# rollback.sh - Rollback completo

set -e

cd /var/apps/yape-notifier

# Opción 1: Rollback de código (git)
if [ "$1" = "code" ]; then
    PREVIOUS_COMMIT=$(git log -2 --format=%H | tail -1)
    echo "Revirtiendo a commit: $PREVIOUS_COMMIT"

    git checkout $PREVIOUS_COMMIT

    docker-compose up -d --build

    sleep 10
    curl -f http://localhost/up || {
        echo "Rollback falló, restaurando..."
        git checkout main
        docker-compose up -d --build
        exit 1
    }

    echo "Rollback de código exitoso"

# Opción 2: Rollback de imagen Docker
elif [ "$1" = "image" ]; then
    IMAGE_TAG=$2

    if [ -z "$IMAGE_TAG" ]; then
        echo "Uso: $0 image <tag>"
        echo "Tags disponibles:"
        docker images | grep yape-notifier-api
        exit 1
    fi

    # Backup actual
    docker tag yape-notifier-api:latest \
        yape-notifier-api:backup-$(date +%Y%m%d-%H%M%S)

    # Tag versión anterior
    docker tag yape-notifier-api:$IMAGE_TAG yape-notifier-api:latest

    # Reiniciar
    docker-compose up -d --force-recreate php-fpm

    sleep 10
    curl -f http://localhost/up || {
        echo "Rollback falló, restaurando..."
        docker tag yape-notifier-api:backup-* yape-notifier-api:latest
        docker-compose up -d --force-recreate php-fpm
        exit 1
    }

    echo "Rollback de imagen exitoso a $IMAGE_TAG"

# Opción 3: Rollback de base de datos
elif [ "$1" = "db" ]; then
    BACKUP_FILE=$2

    if [ -z "$BACKUP_FILE" ]; then
        echo "Uso: $0 db <backup_file>"
        ls -lh /var/backups/yape-notifier/
        exit 1
    fi

    echo "⚠️  ADVERTENCIA: Esto restaurará la BD a un estado anterior"
    read -p "¿Continuar? (yes/no): " confirm

    if [ "$confirm" != "yes" ]; then
        echo "Cancelado"
        exit 0
    fi

    # Restaurar backup
    ./scripts/restore-db.sh $BACKUP_FILE

    echo "Rollback de BD exitoso"

else
    echo "Uso: $0 {code|image|db} [argumentos]"
    echo ""
    echo "Comandos:"
    echo "  code              - Revertir código a commit anterior"
    echo "  image <tag>        - Revertir a imagen Docker específica"
    echo "  db <backup_file>   - Restaurar base de datos desde backup"
    exit 1
fi
```

#### 6.7 Falta de Documentación de Runbooks

**Problema:** No hay procedimientos documentados para incidentes comunes.

**Impacto:**

- Tiempo de respuesta lento ante incidentes
- Errores humanos al resolver problemas
- Sin conocimiento compartido

**Solución:**
Crear `docs/RUNBOOKS.md` con:

- Procedimientos de rollback
- Cómo verificar salud del sistema
- Cómo restaurar backups
- Contactos de emergencia
- Escalación de incidentes

---

## 7. OBSERVABILIDAD

### 🔴 CRÍTICO

#### 7.1 Logging No Estructurado

**Problema:** Logs en texto plano, no JSON estructurado.

**Impacto:**

- Difícil parsear y analizar
- Sin agregación eficiente

**Solución:**

```php
// config/logging.php
'channels' => [
    'stack' => [
        'driver' => 'stack',
        'channels' => ['daily', 'stderr'],
    ],

    'daily' => [
        'driver' => 'daily',
        'path' => storage_path('logs/laravel.log'),
        'level' => env('LOG_LEVEL', 'debug'),
        'days' => 14,
        'tap' => [App\Logging\JsonFormatter::class], // AGREGAR
    ],
],

// app/Logging/JsonFormatter.php
class JsonFormatter
{
    public function __invoke($logger)
    {
        foreach ($logger->getHandlers() as $handler) {
            $handler->setFormatter(new \Monolog\Formatter\JsonFormatter());
        }
    }
}
```

#### 7.2 Falta de Tracing/APM

**Problema:** No hay distributed tracing ni APM (Application Performance Monitoring).

**Impacto:**

- Sin visibilidad de performance
- Difícil identificar cuellos de botella

**Solución:**

- **Laravel Telescope** (dev/staging)
- **Sentry Performance** (producción)
- **New Relic** o **Datadog** (enterprise)

#### 7.3 Falta de Métricas de Negocio

**Problema:** No se trackean métricas de negocio (notificaciones por día, usuarios activos, etc.).

**Impacto:**

- Sin datos para decisiones de negocio
- Sin alertas de anomalías

**Solución:**

```php
// app/Services/NotificationService.php
public function createNotification(...): Notification
{
    // ...

    // Métricas
    \Log::channel('metrics')->info('notification.created', [
        'source_app' => $data['source_app'],
        'amount' => $data['amount'],
        'user_id' => $device->user_id,
        'timestamp' => now()->toIso8601String(),
    ]);

    // ...
}
```

---

## 8. TESTING

### 🔴 CRÍTICO

#### 8.1 Cobertura de Tests Insuficiente

**Problema:** Solo hay tests básicos de Feature, falta cobertura completa.

**Impacto:**

- Bugs en producción
- Refactoring riesgoso

**Solución:**

```bash
# Ejecutar con cobertura
php artisan test --coverage

# Objetivo: >80% cobertura
```

**Tests faltantes:**

- Tests unitarios de Services
- Tests de edge cases (duplicados, validaciones)
- Tests de performance
- Tests de integración con BD

#### 8.2 Falta de Tests de Carga/Performance

**Problema:** No hay tests de carga para validar el objetivo de 10-20 notificaciones/segundo.

**Impacto:**

- Sin garantía de cumplir requisitos
- Posible degradación no detectada

**Solución:**

```php
// tests/Performance/NotificationLoadTest.php
class NotificationLoadTest extends TestCase
{
    public function test_can_handle_20_notifications_per_second(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create(['user_id' => $user->id]);
        $token = $user->createToken('test')->plainTextToken;

        $start = microtime(true);
        $count = 0;

        for ($i = 0; $i < 200; $i++) {
            $this->withHeader('Authorization', "Bearer $token")
                ->postJson('/api/notifications', [...]);
            $count++;
        }

        $duration = microtime(true) - $start;
        $rate = $count / $duration;

        $this->assertGreaterThanOrEqual(20, $rate, "Rate: $rate req/s");
    }
}
```

---

## 📊 RESUMEN DE PRIORIDADES

### 🔴 CRÍTICO (Hacer ANTES de producción)

1. **Rate Limiting** - Protección contra abuso
2. **Manejo de Excepciones** - Logging y seguridad
3. **Exposición de Datos** - UserResource
4. **CORS** - Restringir orígenes
5. **Backups Automáticos** - Recuperación ante desastres
6. **Healthchecks Reales** - Detección de problemas
7. **Logging Estructurado** - Observabilidad
8. **Tests de Carga** - Validar requisitos

### 🟡 IMPORTANTE (Hacer en primera iteración post-MVP)

1. **Repositorios** - Mejor arquitectura
2. **Eventos/Listeners** - Escalabilidad
3. **Caché en Estadísticas** - Performance
4. **Soft Deletes** - Auditoría
5. **CI/CD Pipeline** - Automatización
6. **Monitoreo** - Alertas proactivas

### 🟢 MEJORA FUTURA

1. **DTOs** - Type safety
2. **Particionamiento de BD** - Escalabilidad a largo plazo
3. **APM/Tracing** - Observabilidad avanzada
4. **Refresh Tokens** - Seguridad mejorada

---

## 🎯 RECOMENDACIONES FINALES

### Para MVP (Producción Inicial)

1. ✅ Implementar rate limiting básico
2. ✅ Agregar manejo de excepciones
3. ✅ Configurar backups diarios
4. ✅ Estructurar logs (JSON)
5. ✅ Agregar healthchecks reales
6. ✅ Restringir CORS
7. ✅ Tests de carga básicos

### Para Producción Escalada

1. ✅ Implementar repositorios
2. ✅ Agregar eventos/listeners
3. ✅ Caché en estadísticas
4. ✅ CI/CD pipeline
5. ✅ Monitoreo completo (Sentry, Uptime Robot)
6. ✅ Soft deletes y auditoría

### Arquitectura a Largo Plazo

1. ✅ Migrar a microservicios si crece (opcional)
2. ✅ Implementar colas (Redis/Beanstalkd) para notificaciones
3. ✅ CDN para assets estáticos
4. ✅ Load balancer si hay múltiples instancias
5. ✅ Base de datos read replicas para estadísticas

---

**Revisado por:** [Tu Nombre]  
**Fecha:** 2025-01-XX  
**Próxima Revisión:** Post-implementación de críticos
