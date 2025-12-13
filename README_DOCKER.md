# 🐳 Docker - Yape Notifier

## 📍 Ubicación

**Toda la configuración Docker está en `infra/docker/`**

```
infra/docker/
├── dockerfiles/              # Dockerfiles compartidos
│   ├── Dockerfile.php-fpm
│   └── Dockerfile.dashboard
├── configs/                  # Configuraciones compartidas
│   ├── nginx/                # Configuraciones de Nginx
│   └── php/                  # Configuraciones de PHP
└── environments/             # Entornos separados
    ├── development/          # Entorno de desarrollo
    ├── staging/              # Entorno de staging
    └── production/           # Entorno de producción
        ├── docker-compose.yml
        ├── Caddyfile
        ├── .env.example
        ├── deploy.sh
        └── setup.sh
```

## 🚀 Inicio Rápido

### Producción

```bash
cd infra/docker/environments/production

# Primera vez: configuración inicial
./setup.sh

# Editar .env y configurar DB_PASSWORD seguro
nano .env

# Desplegar
./deploy.sh
```

**Acceso:**
- API: `https://api.notificaciones.space`
- Dashboard: `https://dashboard.notificaciones.space`

### Staging

```bash
cd infra/docker/environments/staging

# Primera vez: configuración inicial
./setup.sh

# Editar .env y configurar DB_PASSWORD
nano .env

# Desplegar
./deploy.sh
```

### Development

```bash
cd infra/docker/environments/development

# Primera vez: configuración inicial
./setup.sh

# Editar .env si es necesario
nano .env

# Desplegar
./deploy.sh
```

## 📚 Documentación

- **`docs/DEPLOYMENT.md`** - Guía completa de despliegue en producción
- **`infra/docker/README.md`** - Documentación técnica detallada
- **`infra/docker/ANALISIS_ESTANDARES.md`** - Análisis de estándares profesionales

## ✅ Arquitectura

```
Caddy (HTTPS) → Nginx API → PHP-FPM (Laravel)
              → Dashboard (React)
              → PostgreSQL (interno)
```

**Todo centralizado y organizado por entornos** 🎯
