# ⚡ Quick Start - Yape Notifier

Guía rápida para levantar el sistema completo (API + Dashboard).

> 📖 **Para una guía completa paso a paso, consulta `docs/DEPLOYMENT.md`**

## 🚀 Producción

### Pasos Rápidos

1. **Preparar servidor** (Droplet, DNS, Docker instalado)
2. **Clonar repositorio** en `/var/apps/yape-notifier`
3. **Configurar `.env`** con `DB_PASSWORD` seguro
4. **Ejecutar `./deploy.sh`**

```bash
# 1. Ir al directorio de producción
cd infra/docker/environments/production

# 2. Configurar variables (primera vez)
./setup.sh
# O manualmente:
cp .env.example .env
nano .env  # Configurar DB_PASSWORD seguro

# 3. Desplegar
./deploy.sh
```

**Acceso:**

- API: `https://api.notificaciones.space/up`
- Dashboard: `https://dashboard.notificaciones.space`

## 🧪 Staging (Testing Local)

```bash
# 1. Ir al directorio de staging
cd infra/docker/environments/staging

# 2. Configurar variables (primera vez)
./setup.sh
# O manualmente:
cp .env.example .env
nano .env  # Configurar DB_PASSWORD

# 3. Desplegar
./deploy.sh
```

**Acceso:**

- API: `http://localhost:8080/up`
- Dashboard: `http://localhost:8080/`

## 💻 Development (Desarrollo Local)

```bash
# 1. Ir al directorio de desarrollo
cd infra/docker/environments/development

# 2. Configurar variables (primera vez)
./setup.sh
# O manualmente:
cp .env.example .env
nano .env  # Configurar si es necesario

# 3. Desplegar
./deploy.sh
```

**Acceso:**

- API: `http://localhost:8000/up`
- Database: `localhost:5432`

## 📋 Comandos Útiles

### Ver estado

```bash
# Production
cd infra/docker/environments/production
docker compose --env-file .env ps

# Staging
cd infra/docker/environments/staging
docker compose --env-file .env ps

# Development
cd infra/docker/environments/development
docker compose --env-file .env ps
```

### Ver logs

```bash
# Todos los logs
docker compose --env-file .env logs -f

# Logs específicos
docker compose --env-file .env logs -f php-fpm
docker compose --env-file .env logs -f caddy
```

### Reiniciar servicios

```bash
docker compose --env-file .env restart
```

### Detener servicios

```bash
docker compose --env-file .env down
```

### Ejecutar comandos Laravel

```bash
# Migraciones
docker compose --env-file .env exec php-fpm php artisan migrate

# Tinker
docker compose --env-file .env exec php-fpm php artisan tinker

# Limpiar cache
docker compose --env-file .env exec php-fpm php artisan optimize:clear
```

## 📚 Documentación Completa

Para más detalles, consulta:

- **`docs/DEPLOYMENT.md`** - Guía completa de despliegue en producción
- **`infra/docker/README.md`** - Documentación técnica detallada
