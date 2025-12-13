# 🐳 Docker Infrastructure - Yape Notifier

Infraestructura Docker profesional y centralizada para Yape Notifier.

## 📁 Estructura

```
infra/docker/
├── docker-compose.yml              # Producción (API + Dashboard + Caddy + PostgreSQL)
├── docker-compose.staging.yml      # Staging (para desarrollo/testing)
├── docker-compose.test.yml         # Testing
│
├── dockerfiles/
│   ├── Dockerfile.php-fpm         # PHP-FPM para Laravel API
│   └── Dockerfile.dashboard        # Dashboard React/Vite
│
├── nginx/
│   ├── api.conf                    # Configuración Nginx para API
│   └── dashboard.conf              # Configuración Nginx para Dashboard
│
├── caddy/
│   ├── Caddyfile                   # Caddy producción (HTTPS automático)
│   └── Caddyfile.staging          # Caddy staging (HTTP)
│
├── php/
│   ├── local.ini                   # Configuración PHP para desarrollo
│   └── production.ini              # Configuración PHP para producción
│
├── deploy.sh                       # Script deployment producción
├── deploy-staging.sh               # Script deployment staging
├── setup-production.sh             # Script configuración inicial producción
├── setup.sh                        # Script configuración inicial desarrollo
├── .env.production.example         # Plantilla variables producción
└── .env.staging.example            # Plantilla variables staging
```

## 🏗️ Arquitectura

```
Internet
   │
   ▼
[ Caddy :80, :443 ]
   │ (HTTPS automático con Let's Encrypt)
   ├─► api.notificaciones.space → [ Nginx API :80 ] → [ PHP-FPM :9000 ] → [ Laravel API ]
   └─► dashboard.notificaciones.space → [ Dashboard :80 ] → [ React App ]
   │
   ▼
[ PostgreSQL :5432 ]
   (interno, no expuesto)
```

### Servicios

- **Caddy**: Reverse proxy con HTTPS automático (Let's Encrypt)
- **Nginx API**: Servidor web para Laravel (PHP-FPM)
- **PHP-FPM**: Aplicación Laravel 11
- **Dashboard**: Frontend React servido por Nginx
- **PostgreSQL**: Base de datos (no expuesta públicamente)

## 🚀 Uso Rápido

### Producción

```bash
cd infra/docker

# 1. Configurar variables de entorno
cp .env.production.example .env.production
nano .env.production  # Configurar DB_PASSWORD, APP_URL, DASHBOARD_API_URL

# 2. Verificar Caddyfile (ya configurado para notificaciones.space)
nano caddy/Caddyfile  # Revisar configuración si es necesario

# 3. Desplegar
chmod +x deploy.sh
./deploy.sh
```

**Acceso:**

- API: `https://api.notificaciones.space`
- Dashboard: `https://dashboard.notificaciones.space`

### Staging (Desarrollo/Testing)

```bash
cd infra/docker

# 1. Configurar variables de entorno
cp .env.staging.example .env.staging
nano .env.staging  # Configurar DB_PASSWORD

# 2. Desplegar
chmod +x deploy-staging.sh
./deploy-staging.sh
```

**Acceso:**

- API: `http://localhost:8080/up`
- Dashboard: `http://localhost:8080/`

## 📋 Comandos Útiles

### Ver Estado

```bash
# Producción
docker compose -f docker-compose.yml ps

# Staging
docker compose -f docker-compose.staging.yml ps
```

### Ver Logs

```bash
# Todos los logs
docker compose -f docker-compose.yml logs -f

# Logs específicos
docker compose -f docker-compose.yml logs -f caddy
docker compose -f docker-compose.yml logs -f php-fpm
docker compose -f docker-compose.yml logs -f nginx-api
docker compose -f docker-compose.yml logs -f dashboard
docker compose -f docker-compose.yml logs -f db
```

### Reiniciar Servicios

```bash
# Todos los servicios
docker compose -f docker-compose.yml restart

# Servicio específico
docker compose -f docker-compose.yml restart php-fpm
docker compose -f docker-compose.yml restart caddy
```

### Ejecutar Comandos Laravel

```bash
# Artisan commands
docker compose -f docker-compose.yml exec php-fpm php artisan migrate
docker compose -f docker-compose.yml exec php-fpm php artisan tinker

# Composer
docker compose -f docker-compose.yml exec php-fpm composer install
```

### Reconstruir Imágenes

```bash
# Reconstruir todas las imágenes
docker compose -f docker-compose.yml build --no-cache

# Reconstruir una imagen específica
docker compose -f docker-compose.yml build --no-cache php-fpm
docker compose -f docker-compose.yml build --no-cache dashboard
```

### Detener y Limpiar

```bash
# Detener servicios
docker compose -f docker-compose.yml down

# Detener y eliminar volúmenes (¡CUIDADO! Elimina la base de datos)
docker compose -f docker-compose.yml down -v
```

## 🔧 Configuración

### Variables de Entorno

Las variables de entorno se configuran en:

- `.env.production` - Para producción (crear desde `.env.production.example`)
- `.env.staging` - Para staging (crear desde `.env.staging.example`)

**Archivos de ejemplo disponibles:**

- `.env.production.example` - Plantilla con todas las variables para producción
- `.env.staging.example` - Plantilla con todas las variables para staging

**Para crear los archivos .env:**

```bash
# Producción
cp .env.production.example .env.production
nano .env.production  # Configurar valores reales

# Staging
cp .env.staging.example .env.staging
nano .env.staging  # Configurar valores reales
```

Ver `docs/DEPLOYMENT.md` para la lista completa de variables requeridas y sus descripciones.

### Caddyfile

El `Caddyfile` maneja el enrutamiento y HTTPS automático. Configura tus subdominios en:

- `caddy/Caddyfile` - Para producción
- `caddy/Caddyfile.staging` - Para staging

### Nginx

Las configuraciones de Nginx están en:

- `nginx/api.conf` - Para la API Laravel
- `nginx/dashboard.conf` - Para el Dashboard React

## 📚 Documentación Completa

Para una guía completa de deployment, configuración y solución de problemas, consulta:

- **Guía Principal**: [`docs/DEPLOYMENT.md`](../../docs/DEPLOYMENT.md)
- **Quick Start**: [`docs/QUICKSTART.md`](../../docs/QUICKSTART.md)

## 🐛 Troubleshooting

### Error: "Certificate not obtained" (Caddy)

Verifica que los DNS estén propagados y que los subdominios en `Caddyfile` coincidan con los registros DNS.

### Error: "502 Bad Gateway"

Verifica que Nginx y PHP-FPM estén corriendo:

```bash
docker compose -f docker-compose.yml ps
docker compose -f docker-compose.yml restart nginx-api php-fpm
```

### Error: "Database connection failed"

Verifica las variables de entorno `DB_*` en `.env.production` o `.env.staging`.

### Ver Logs Detallados

```bash
docker compose -f docker-compose.yml logs -f [servicio]
```

## 🔄 Actualizaciones

Para actualizar la aplicación:

```bash
cd /var/apps/yape-notifier
git pull origin main
cd infra/docker
docker compose -f docker-compose.yml build
docker compose -f docker-compose.yml up -d
docker compose -f docker-compose.yml exec php-fpm php artisan migrate --force
```

---

**Ubicación de archivos:** Toda la infraestructura Docker está centralizada en `infra/docker/` 🎯
