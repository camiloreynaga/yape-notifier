# 🐳 Docker Infrastructure - Yape Notifier

Infraestructura Docker profesional y centralizada para Yape Notifier.

## 📁 Estructura

```
infra/docker/
├── docker-compose.yml              # Producción (API + Dashboard + Caddy)
├── docker-compose.staging.yml      # Staging
├── deploy.sh                       # Script deployment producción
├── deploy-staging.sh               # Script deployment staging
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
│   ├── Caddyfile                   # Caddy producción (HTTPS)
│   └── Caddyfile.staging          # Caddy staging (HTTP)
│
└── .env.production.example         # Plantilla variables producción
    .env.staging.example            # Plantilla variables staging
```

## 🚀 Uso Rápido

### Staging

```bash
cd infra/docker
cp .env.staging.example .env.staging
nano .env.staging  # Configurar DB_PASSWORD
chmod +x deploy-staging.sh
./deploy-staging.sh
```

**Acceso:** `http://localhost:8080`

### Producción

```bash
cd infra/docker
cp .env.production.example .env.production
nano .env.production  # Configurar DB_PASSWORD, APP_URL, etc.
chmod +x deploy.sh
./deploy.sh
```

**Acceso:** 
- API: `https://api.notificaciones.space`
- Dashboard: `https://panel.notificaciones.space`

## 📋 Comandos Útiles

```bash
# Ver estado
docker compose -f docker-compose.yml ps

# Ver logs
docker compose -f docker-compose.yml logs -f

# Reiniciar
docker compose -f docker-compose.yml restart

# Detener
docker compose -f docker-compose.yml down

# Reconstruir
docker compose -f docker-compose.yml build --no-cache
```

## 🏗️ Arquitectura

- **Caddy**: Reverse proxy con HTTPS automático
- **Nginx API**: Servidor web para Laravel
- **PHP-FPM**: Aplicación Laravel
- **Dashboard**: Frontend React servido por Nginx
- **PostgreSQL**: Base de datos (no expuesta)

## 📚 Documentación Completa

Ver `docs/DEPLOYMENT.md` para guía completa.
