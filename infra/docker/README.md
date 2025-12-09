# Entorno Docker - Yape Notifier

Este directorio contiene toda la configuración necesaria para ejecutar el proyecto Yape Notifier en un entorno Docker completo.

## 🚀 Inicio Rápido

### Windows (PowerShell)

```powershell
cd infra/docker
.\setup.ps1
```

### Linux/Mac

```bash
cd infra/docker
chmod +x setup.sh
./setup.sh
```

O usando Make:

```bash
cd infra/docker
make setup
```

## 📋 Servicios Incluidos

- **app**: Contenedor PHP 8.2-FPM con Laravel
- **nginx**: Servidor web Nginx para la API
- **dashboard**: Dashboard web (React + Vite) en producción
- **dashboard-dev**: Dashboard web en modo desarrollo (con hot-reload)
- **db**: Base de datos PostgreSQL 15
- **redis**: Cache y sesiones Redis 7

## 🔧 Comandos Útiles

### Usando Make (recomendado)

```bash
make help          # Ver todos los comandos disponibles
make up            # Iniciar contenedores
make down          # Detener contenedores
make restart       # Reiniciar contenedores
make logs          # Ver logs en tiempo real
make shell         # Acceder al shell del contenedor
make artisan CMD="migrate"  # Ejecutar comando artisan
make migrate       # Ejecutar migraciones
make fresh         # Resetear base de datos
make test          # Ejecutar tests
```

### Usando Docker Compose directamente

```bash
# Iniciar contenedores
docker-compose up -d

# Ver logs
docker-compose logs -f

# Ver logs de un servicio específico
docker-compose logs -f app

# Detener contenedores
docker-compose down

# Reiniciar un servicio
docker-compose restart app

# Acceder al shell del contenedor
docker-compose exec app bash

# Ejecutar comandos artisan
docker-compose exec app php artisan migrate
docker-compose exec app php artisan route:list
docker-compose exec app php artisan tinker

# Ver estado de contenedores
docker-compose ps

# Reconstruir imágenes
docker-compose build --no-cache
```

## 🌐 Acceso a los Servicios

Una vez iniciados los contenedores:

- **API Laravel**: http://localhost:8000
- **Dashboard Web (Producción)**: http://localhost:3000
- **Dashboard Web (Desarrollo)**: http://localhost:3001 (solo con perfil `dev`)
- **PostgreSQL**: localhost:5432
  - Usuario: `postgres`
  - Contraseña: `password` (por defecto)
  - Base de datos: `yape_notifier`
- **Redis**: localhost:6379

## 📝 Configuración

### Variables de Entorno

Edita el archivo `.env` en este directorio para configurar:

```env
APP_PORT=8000
DASHBOARD_PORT=3000
DASHBOARD_DEV_PORT=3001
DASHBOARD_API_URL=http://localhost:8000
DB_DATABASE=yape_notifier
DB_USERNAME=postgres
DB_PASSWORD=tu_password
DB_PORT=5432
REDIS_PORT=6379
```

### Modo Desarrollo vs Producción

**Producción (por defecto):**
```bash
docker-compose up -d
```
Incluye el dashboard en modo producción (build estático con nginx).

**Desarrollo:**
```bash
docker-compose --profile dev up -d
```
Incluye el dashboard en modo desarrollo con hot-reload en el puerto 3001.

### Configuración de Laravel

El archivo `.env` de Laravel se encuentra en `apps/api/.env` y se configura automáticamente durante el setup.

## 🗄️ Base de Datos

### Conectar desde fuera de Docker

Puedes conectar a la base de datos usando cualquier cliente PostgreSQL:

- **Host**: localhost
- **Puerto**: 5432
- **Usuario**: postgres
- **Contraseña**: password (o la que configuraste)
- **Base de datos**: yape_notifier

### Ejecutar migraciones

```bash
docker-compose exec app php artisan migrate
```

### Resetear base de datos

```bash
docker-compose exec app php artisan migrate:fresh
```

### Ejecutar seeders

```bash
docker-compose exec app php artisan db:seed
```

## 🧪 Testing

```bash
# Ejecutar todos los tests
docker-compose exec app php artisan test

# Ejecutar tests específicos
docker-compose exec app php artisan test --filter NotificationTest
```

## 🐛 Debugging

### Ver logs de Laravel

```bash
docker-compose exec app tail -f storage/logs/laravel.log
```

### Ver logs de Nginx

```bash
docker-compose logs nginx
```

### Ver logs de PostgreSQL

```bash
docker-compose logs db
```

### Acceder a la base de datos

```bash
docker-compose exec db psql -U postgres -d yape_notifier
```

## 🔄 Actualizar Dependencias

```bash
# Actualizar dependencias de Composer
docker-compose exec app composer update

# Actualizar dependencias y reconstruir
docker-compose exec app composer install
docker-compose restart app
```

## 🧹 Limpieza

### Limpiar contenedores y volúmenes

```bash
make clean
# o
docker-compose down -v
```

### Limpiar imágenes no utilizadas

```bash
docker system prune -a
```

## ⚠️ Solución de Problemas

### Error: "Port already in use"

Si el puerto 8000 está en uso, cambia `APP_PORT` en el archivo `.env`:

```env
APP_PORT=8001
```

### Error: "Permission denied" en storage

```bash
docker-compose exec app chmod -R 775 storage bootstrap/cache
docker-compose exec app chown -R www-data:www-data storage bootstrap/cache
```

### Error: "Connection refused" a la base de datos

Verifica que el contenedor de la base de datos esté corriendo:

```bash
docker-compose ps
```

Si no está corriendo:

```bash
docker-compose up -d db
```

### Reconstruir todo desde cero

```bash
docker-compose down -v
docker-compose build --no-cache
docker-compose up -d
```

## 📚 Recursos Adicionales

- [Documentación de Docker Compose](https://docs.docker.com/compose/)
- [Documentación de Laravel](https://laravel.com/docs)
- [Documentación de PostgreSQL](https://www.postgresql.org/docs/)

