# Yape Notifier API

Backend Laravel 11 para el sistema de notificación de pagos multi-tenant con soporte para apps duales.

## 📋 Stack Tecnológico

- **PHP 8.2+**
- **Laravel 11**
- **PostgreSQL**
- **Laravel Sanctum** (autenticación)
- **Laravel Reverb** (WebSocket server para notificaciones en tiempo real)

## 🏗️ Estructura

```
app/
├── Http/
│   ├── Controllers/     # Controladores REST
│   └── Requests/       # Form Requests (validación)
├── Models/              # Modelos Eloquent
├── Services/            # Lógica de negocio
└── Providers/           # Service Providers
```

## 🚀 Comandos Básicos

### Gestión de Dependencias

**⚠️ CRÍTICO**: Siempre usar Docker para actualizar dependencias PHP:

```bash
# Desde la raíz del proyecto
make composer:update     # Actualizar dependencias
make composer:require PACKAGE=nombre/paquete  # Agregar dependencia
make composer:validate   # Validar compatibilidad

# O desde apps/api
./update-dependencies.sh
```

**NUNCA ejecutar `composer update` directamente** - esto genera `composer.lock` con la versión de PHP local (puede ser 8.3, 8.4, etc.) y causa incompatibilidades con Docker (PHP 8.2).

Ver [README_DEPENDENCIES.md](README_DEPENDENCIES.md) para el proceso completo.

### Desarrollo

```bash
# Instalar dependencias (solo lectura, no modifica composer.lock)
composer install

# Configurar entorno
cp .env.example .env
php artisan key:generate

# Migraciones
php artisan migrate

# Servidor de desarrollo
php artisan serve
```

### Testing

```bash
# Todos los tests
php artisan test

# Tests unitarios
php artisan test --testsuite=Unit

# Tests de integración
php artisan test --testsuite=Feature
```

### WebSocket Server (Reverb)

```bash
# Iniciar servidor Reverb (desarrollo)
php artisan reverb:start

# Para producción, usar supervisor o systemd
# Ver configuración en infra/docker/
```

**Variables de entorno requeridas (.env):**
```env
# Reverb WebSocket Server
REVERB_APP_ID=yape-notifier
REVERB_APP_KEY=base64:tu-key-generada
REVERB_APP_SECRET=tu-secret-generado
REVERB_HOST=0.0.0.0
REVERB_PORT=8080
REVERB_SCHEME=http

# Broadcasting
BROADCAST_CONNECTION=reverb
```

Para generar las keys de Reverb:
```bash
php artisan reverb:install
```

### Producción

```bash
# Optimizar
php artisan config:cache
php artisan route:cache
php artisan view:cache
```

## 📦 Modelos Principales

- **Commerce**: Multi-tenant (aislamiento de datos por negocio)
- **AppInstance**: Apps duales `(device_id + package_name + android_user_id)`
- **Device**: Dispositivos Android con salud
- **Notification**: Notificaciones de pago con deduplicación

## 🔌 Endpoints Principales

- **Autenticación**: `/api/register`, `/api/login`, `/api/logout`
- **Commerce**: `/api/commerces`
- **Dispositivos**: `/api/devices`
- **Notificaciones**: `/api/notifications`
- **App Instances**: `/api/app-instances`

## 📚 Documentación

- **Arquitectura**: Ver `../../docs/03-architecture/`
- **Deployment**: Ver `../../docs/02-deployment/DEPLOYMENT.md`
- **Quick Start**: Ver `../../docs/01-getting-started/QUICKSTART.md`
- **Estado de implementación**: Ver `../../docs/07-reference/IMPLEMENTATION_STATUS.md`
- **Bugs conocidos**: Ver `../../docs/07-reference/KNOWN_ISSUES.md`

## 🐛 Solución de Problemas

### Error: "Device not found"

- Verificar que el dispositivo esté registrado con el UUID correcto

### Error: "Commerce not found"

- El usuario debe tener un commerce asociado
- Crear commerce con `POST /api/commerces`

## 📝 Notas Técnicas

- **Multi-tenant**: Todos los queries filtran por `commerce_id`
- **Deduplicación**: Por `package_name + android_user_id + posted_at + body`
- **Apps Duales**: Se distinguen por `package_name + android_user_id`
