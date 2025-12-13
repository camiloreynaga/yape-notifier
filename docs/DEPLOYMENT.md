# 🚀 Guía de Deployment - Yape Notifier

Guía completa para desplegar Yape Notifier en DigitalOcean usando Docker y Caddy.

## 📋 Tabla de Contenidos

1. [Estructura del Proyecto](#estructura-del-proyecto)
2. [Prerrequisitos](#prerrequisitos)
3. [Configuración Inicial](#configuración-inicial)
4. [Deployment en Staging](#deployment-en-staging)
5. [Deployment en Producción](#deployment-en-producción)
6. [Verificación y Testing](#verificación-y-testing)
7. [Troubleshooting](#troubleshooting)
8. [Mantenimiento](#mantenimiento)

---

## 🏗️ Estructura del Proyecto

```
yape-notifier/
├── apps/
│   ├── api/              # Laravel API
│   └── web-dashboard/    # React Dashboard
│
└── infra/
    └── docker/           # ✅ Toda la configuración Docker está aquí
        ├── docker-compose.yml          # Producción
        ├── docker-compose.staging.yml # Staging
        ├── dockerfiles/
        │   ├── Dockerfile.php-fpm     # PHP-FPM para Laravel
        │   └── Dockerfile.dashboard    # Dashboard React
        ├── nginx/
        │   ├── api.conf                # Configuración Nginx para API
        │   └── dashboard.conf          # Configuración Nginx para Dashboard
        ├── caddy/
        │   ├── Caddyfile               # Caddy para producción
        │   └── Caddyfile.staging      # Caddy para staging
        ├── deploy.sh                   # Script de deployment producción
        └── deploy-staging.sh           # Script de deployment staging
```

**Toda la infraestructura Docker está centralizada en `infra/docker/`**

---

## ✅ Prerrequisitos

- ✅ Droplet de DigitalOcean (mínimo 2GB RAM, recomendado 4GB)
- ✅ Acceso SSH al Droplet
- ✅ Dominio `notificaciones.space` configurado
- ✅ Docker y Docker Compose instalados

---

## ⚙️ Configuración Inicial

### Paso 1: Instalar Docker

```bash
# Actualizar sistema
apt update && apt upgrade -y

# Instalar Docker Engine
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# Instalar Docker Compose Plugin
apt install docker-compose-plugin -y

# Verificar
docker --version
docker compose version
```

### Paso 2: Configurar Firewall

```bash
apt install ufw -y
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable
```

### Paso 3: Clonar Proyecto

```bash
mkdir -p /var/apps
cd /var/apps
git clone https://github.com/TU_USUARIO/yape-notifier.git
cd yape-notifier
```

---

## 🧪 Deployment en Staging

### Paso 1: Configurar Variables

```bash
cd /var/apps/yape-notifier/infra/docker
cp .env.staging.example .env.staging
nano .env.staging
# Configurar al menos: DB_PASSWORD
```

### Paso 2: Levantar Staging

```bash
# Opción A: Usar script automático
chmod +x deploy-staging.sh
./deploy-staging.sh

# Opción B: Manual
docker compose -f docker-compose.staging.yml up -d --build
```

### Paso 3: Configurar Laravel

```bash
# Generar APP_KEY
docker compose -f docker-compose.staging.yml exec php-fpm php artisan key:generate
# Copiar APP_KEY a .env.staging
nano .env.staging
docker compose -f docker-compose.staging.yml restart php-fpm

# Ejecutar migraciones
docker compose -f docker-compose.staging.yml exec php-fpm php artisan migrate --force
```

### Paso 4: Verificar

```bash
# Health check
curl http://localhost:8080/up

# Dashboard
curl http://localhost:8080/health
```

**Acceso:**
- API: `http://TU_IP:8080/up`
- Dashboard: `http://TU_IP:8080/`

---

## 🚀 Deployment en Producción

### Paso 1: Configurar DNS

En tu proveedor de DNS, agregar:
```
Tipo: A
Nombre: api
Valor: IP_DEL_DROPLET

Tipo: A
Nombre: panel
Valor: IP_DEL_DROPLET
```

### Paso 2: Configurar Variables

```bash
cd /var/apps/yape-notifier/infra/docker
cp .env.production.example .env.production
nano .env.production
# Configurar: DB_PASSWORD, APP_URL, DASHBOARD_API_URL
```

### Paso 3: Levantar Producción

```bash
# Opción A: Usar script automático
chmod +x deploy.sh
./deploy.sh

# Opción B: Manual
docker compose -f docker-compose.yml up -d --build
```

### Paso 4: Configurar Laravel

```bash
# Generar APP_KEY
docker compose -f docker-compose.yml exec php-fpm php artisan key:generate
# Copiar APP_KEY a .env.production
nano .env.production
docker compose -f docker-compose.yml restart php-fpm

# Ejecutar migraciones
docker compose -f docker-compose.yml exec php-fpm php artisan migrate --force

# Optimizar Laravel
docker compose -f docker-compose.yml exec php-fpm php artisan config:cache
docker compose -f docker-compose.yml exec php-fpm php artisan route:cache
docker compose -f docker-compose.yml exec php-fpm php artisan view:cache
```

### Paso 5: Verificar

```bash
# Health check
curl https://api.notificaciones.space/up

# Dashboard
curl https://panel.notificaciones.space/health
```

---

## ✅ Verificación y Testing

### Verificar Contenedores

```bash
# Producción
docker compose -f infra/docker/docker-compose.yml ps

# Staging
docker compose -f infra/docker/docker-compose.staging.yml ps
```

### Verificar Logs

```bash
# Todos los logs
docker compose -f infra/docker/docker-compose.yml logs -f

# Logs específicos
docker compose -f infra/docker/docker-compose.yml logs -f php-fpm
docker compose -f infra/docker/docker-compose.yml logs -f caddy
```

### Probar Endpoints

```bash
# API Health
curl https://api.notificaciones.space/up

# API Endpoints
curl https://api.notificaciones.space/api/register
curl https://api.notificaciones.space/api/login

# Dashboard
curl https://panel.notificaciones.space/health
```

---

## 🐛 Troubleshooting

### Error 502 Bad Gateway

```bash
docker compose -f infra/docker/docker-compose.yml restart php-fpm nginx-api
docker compose -f infra/docker/docker-compose.yml logs php-fpm
```

### Error 403 Forbidden

```bash
docker compose -f infra/docker/docker-compose.yml exec php-fpm chown -R www-data:www-data /var/www/storage
docker compose -f infra/docker/docker-compose.yml exec php-fpm chmod -R 775 /var/www/storage
```

### SSL/Certificado no válido

```bash
docker compose -f infra/docker/docker-compose.yml logs caddy
dig api.notificaciones.space
docker compose -f infra/docker/docker-compose.yml restart caddy
```

### Dashboard no carga

```bash
# Verificar que el dashboard esté corriendo
docker compose -f infra/docker/docker-compose.yml ps dashboard

# Ver logs
docker compose -f infra/docker/docker-compose.yml logs dashboard

# Verificar build
docker compose -f infra/docker/docker-compose.yml build dashboard --no-cache
```

---

## 🔄 Mantenimiento

### Actualizar Código

```bash
cd /var/apps/yape-notifier
git pull origin main
cd infra/docker
./deploy.sh  # o ./deploy-staging.sh para staging
```

### Reiniciar Servicios

```bash
docker compose -f infra/docker/docker-compose.yml restart
```

### Ver Uso de Recursos

```bash
docker stats
```

### Limpiar Todo (¡CUIDADO!)

```bash
docker compose -f infra/docker/docker-compose.yml down -v
```

---

## 📊 Arquitectura

```
Internet
   │
   ▼
[ Caddy :80, :443 ]
   │ (HTTPS automático)
   ├─► api.notificaciones.space → [ Nginx API ] → [ PHP-FPM ]
   └─► panel.notificaciones.space → [ Dashboard ]
   │
   ▼
[ PostgreSQL :5432 ]
   (interno, no expuesto)
```

---

## ✅ Checklist

### Pre-Deployment
- [ ] Docker y Docker Compose instalados
- [ ] Firewall configurado
- [ ] DNS configurado (producción)
- [ ] Variables de entorno configuradas

### Deployment
- [ ] Servicios levantados
- [ ] APP_KEY generado
- [ ] Migraciones ejecutadas
- [ ] Permisos correctos
- [ ] Health checks funcionando

### Post-Deployment
- [ ] HTTPS funcionando
- [ ] API accesible
- [ ] Dashboard accesible
- [ ] Logs sin errores críticos

---

**Ubicación de archivos:** Todo está en `infra/docker/` 🎯


