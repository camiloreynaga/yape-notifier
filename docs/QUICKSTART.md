# ⚡ Quick Start - Yape Notifier

Guía rápida para levantar el sistema completo (API + Dashboard).

## 🚀 Staging (Testing Local)

```bash
# 1. Configurar variables
cd infra/docker
cp .env.staging.example .env.staging
nano .env.staging  # Configurar DB_PASSWORD

# 2. Levantar servicios
chmod +x deploy-staging.sh
./deploy-staging.sh

# 3. Configurar Laravel
docker compose -f docker-compose.staging.yml exec php-fpm php artisan key:generate
# Copiar APP_KEY a .env.staging
nano .env.staging
docker compose -f docker-compose.staging.yml restart php-fpm
docker compose -f docker-compose.staging.yml exec php-fpm php artisan migrate --force
```

**Acceso:**
- API: `http://localhost:8080/up`
- Dashboard: `http://localhost:8080/`

## 🚀 Producción

```bash
# 1. Configurar variables
cd infra/docker
cp .env.production.example .env.production
nano .env.production  # Configurar DB_PASSWORD, APP_URL, DASHBOARD_API_URL

# 2. Levantar servicios
chmod +x deploy.sh
./deploy.sh

# 3. Configurar Laravel
docker compose -f docker-compose.yml exec php-fpm php artisan key:generate
# Copiar APP_KEY a .env.production
nano .env.production
docker compose -f docker-compose.yml restart php-fpm
docker compose -f docker-compose.yml exec php-fpm php artisan migrate --force
docker compose -f docker-compose.yml exec php-fpm php artisan config:cache
docker compose -f docker-compose.yml exec php-fpm php artisan route:cache
```

**Acceso:**
- API: `https://api.notificaciones.space/up`
- Dashboard: `https://panel.notificaciones.space/health`

## 📋 Comandos Útiles

```bash
# Ver estado
docker compose -f infra/docker/docker-compose.yml ps

# Ver logs
docker compose -f infra/docker/docker-compose.yml logs -f

# Reiniciar
docker compose -f infra/docker/docker-compose.yml restart

# Detener
docker compose -f infra/docker/docker-compose.yml down
```

**Documentación completa:** Ver `docs/DEPLOYMENT.md`


