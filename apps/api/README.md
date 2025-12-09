# Yape Notifier API

Backend API desarrollado en Laravel 11 para el sistema de validación de pagos mediante notificaciones.

## Requisitos

- PHP 8.2+
- Composer
- PostgreSQL o MySQL
- Laravel 11

## Instalación

1. Instalar dependencias:
```bash
composer install
```

2. Configurar variables de entorno:
```bash
cp .env.example .env
php artisan key:generate
```

3. Configurar base de datos en `.env`:
```
DB_CONNECTION=pgsql
DB_HOST=127.0.0.1
DB_PORT=5432
DB_DATABASE=yape_notifier
DB_USERNAME=postgres
DB_PASSWORD=your_password
```

4. Ejecutar migraciones:
```bash
php artisan migrate
```

5. Iniciar servidor:
```bash
php artisan serve
```

## Endpoints API

### Autenticación
- `POST /api/register` - Registrar nuevo usuario
- `POST /api/login` - Iniciar sesión
- `POST /api/logout` - Cerrar sesión
- `GET /api/me` - Obtener usuario autenticado

### Dispositivos
- `GET /api/devices` - Listar dispositivos
- `POST /api/devices` - Crear dispositivo
- `GET /api/devices/{id}` - Obtener dispositivo
- `PUT /api/devices/{id}` - Actualizar dispositivo
- `DELETE /api/devices/{id}` - Eliminar dispositivo
- `POST /api/devices/{id}/toggle-status` - Activar/desactivar dispositivo

### Notificaciones
- `POST /api/notifications` - Crear notificación
- `GET /api/notifications` - Listar notificaciones
- `GET /api/notifications/{id}` - Obtener notificación
- `GET /api/notifications/statistics` - Estadísticas
- `PATCH /api/notifications/{id}/status` - Actualizar estado

## Autenticación

La API utiliza Laravel Sanctum para autenticación. Incluye el token en el header:

```
Authorization: Bearer {token}
```

## Docker

Para desarrollo local con Docker:

```bash
cd infra/docker
docker-compose up -d
```

