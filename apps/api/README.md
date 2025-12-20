# Yape Notifier API

Backend Laravel 11 para el sistema de notificación de pagos multi-tenant con soporte para apps duales.

## 📋 Stack Tecnológico

- **PHP 8.2+**
- **Laravel 11**
- **PostgreSQL**
- **Laravel Sanctum** (autenticación)

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

### Desarrollo

```bash
# Instalar dependencias
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
