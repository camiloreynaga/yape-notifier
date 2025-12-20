# Yape Notifier - Docker Infrastructure

Infraestructura Docker organizada por entornos siguiendo estándares profesionales.

## 📁 Estructura

```
infra/docker/
├── dockerfiles/              # Dockerfiles compartidos
│   ├── Dockerfile.php-fpm
│   └── Dockerfile.dashboard
├── configs/                  # Configuraciones compartidas
│   ├── nginx/                # Configuraciones de Nginx
│   │   ├── api.conf
│   │   └── dashboard.conf
│   └── php/                  # Configuraciones de PHP
│       ├── local.ini
│       └── production.ini
└── environments/             # Entornos separados
    ├── development/          # Entorno de desarrollo
    │   ├── docker-compose.yml
    │   ├── .env.example
    │   ├── deploy.sh
    │   └── setup.sh
    ├── staging/              # Entorno de staging
    │   ├── docker-compose.yml
    │   ├── Caddyfile
    │   ├── .env.example
    │   ├── deploy.sh
    │   └── setup.sh
    └── production/           # Entorno de producción
        ├── docker-compose.yml
        ├── Caddyfile
        ├── .env.example
        ├── deploy.sh
        └── setup.sh
```

## 🚀 Inicio Rápido

### Development (Desarrollo Local)

```bash
cd infra/docker/environments/development

# Primera vez: configuración inicial
./setup.sh

# Editar .env si es necesario
nano .env

# Desplegar
./deploy.sh

# O manualmente:
docker compose --env-file .env up -d
```

**Acceso:**

- API: `http://localhost:8000/up`
- Dashboard: `http://localhost:3000` (Vite dev server con hot reload)
- Database: `localhost:5432`

**Nota**: El dashboard corre en Docker con hot reload. El código se monta como volumen para desarrollo rápido. Ver `docs/DEVELOPMENT_WORKFLOW.md` para más detalles.

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

**Acceso:**

- API: `http://localhost:8080/up`
- Dashboard: `http://localhost:8080/`

### Production

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

## 📋 Comandos Útiles

### Ver logs

```bash
# Development
cd environments/development
docker compose --env-file .env logs -f

# Staging
cd environments/staging
docker compose --env-file .env logs -f

# Production
cd environments/production
docker compose --env-file .env logs -f
```

### Detener servicios

```bash
docker compose --env-file .env down
```

### Detener y eliminar volúmenes

```bash
docker compose --env-file .env down -v
```

### Reconstruir imágenes

```bash
docker compose --env-file .env build --no-cache
```

### Ejecutar comandos en contenedores

```bash
# PHP-FPM
docker compose --env-file .env exec php-fpm php artisan migrate

# Database
docker compose --env-file .env exec db psql -U postgres -d yape_notifier
```

## 🔧 Configuración de Entornos

### Variables de Entorno

Cada entorno tiene su propio archivo `.env.example` que debe copiarse a `.env` y configurarse:

- **Development**: Configuración básica para desarrollo local
- **Staging**: Similar a producción pero con HTTP y puertos alternativos
- **Production**: Configuración completa con HTTPS y optimizaciones

### Requisitos por Entorno

#### Development

- Docker y Docker Compose
- Puerto 8000 disponible
- Puerto 5432 disponible (opcional, puede cambiarse)

#### Staging

- Docker y Docker Compose
- Puerto 8080 disponible
- Puerto 8443 disponible
- Archivo `.env` con `DB_PASSWORD` configurado

#### Production

- Docker y Docker Compose
- Puertos 80 y 443 disponibles
- DNS configurado:
  - `api.notificaciones.space`
  - `dashboard.notificaciones.space`
- Archivo `.env` con `DB_PASSWORD` seguro configurado

## 🏗️ Arquitectura

### Development

```
Nginx (puerto 8000) -> PHP-FPM -> PostgreSQL (puerto 5432)
```

### Staging

```
Caddy (HTTP, puerto 8080) -> Nginx -> PHP-FPM -> PostgreSQL
                            -> Dashboard
```

### Production

```
Caddy (HTTPS, puertos 80/443) -> Nginx -> PHP-FPM -> PostgreSQL
                              -> Dashboard
```

## 🔒 Seguridad

### Gestión de Secretos

**⚠️ IMPORTANTE:** Los archivos `.env` están en `.gitignore` y **NUNCA** deben committearse.

Para producción, considera usar:

- **Docker Secrets** (con Docker Swarm)
- **Variables de entorno del sistema**
- **Secret management tools** (HashiCorp Vault, AWS Secrets Manager)

Ver `docker-compose.secrets.yml.example` para ejemplo con Docker Secrets.

## 📝 Notas Importantes

1. **Seguridad**: Nunca commitees archivos `.env` al repositorio. Solo los `.env.example` deben estar en el control de versiones.

2. **Base de Datos**: Cada entorno tiene su propia base de datos:

   - Development: `yape_notifier_dev`
   - Staging: `yape_notifier_staging`
   - Production: `yape_notifier`

3. **Volúmenes**: Los volúmenes de Docker son específicos por entorno para evitar conflictos.

4. **Redes**: Cada entorno tiene su propia red Docker para aislamiento.

5. **Healthchecks**: Todos los servicios tienen healthchecks configurados para garantizar disponibilidad.

## 🔍 Troubleshooting

### Error: "DB_PASSWORD no está configurado"

- Asegúrate de que el archivo `.env` existe y tiene `DB_PASSWORD` configurado con un valor no vacío.

### Error: "Port already in use"

- Verifica que los puertos requeridos estén disponibles o cambia los puertos en `.env`.

### Error: "Container unhealthy"

- Revisa los logs: `docker compose --env-file .env logs [service-name]`
- Verifica que las dependencias estén correctamente configuradas.

### Error: "Network conflict"

- Elimina redes Docker no utilizadas: `docker network prune`

## 📚 Documentación Adicional

Para más detalles sobre el despliegue, consulta:

- `docs/DEPLOYMENT.md` - Guía completa de despliegue
- `docs/QUICKSTART.md` - Guía rápida de inicio
- `ANALISIS_ESTANDARES.md` - Análisis de estándares profesionales y mejoras recomendadas
- `environments/production/BACKUP.md` - Estrategia de backup y disaster recovery
- `environments/production/MONITORING.md` - Guía de monitoreo y observabilidad

## 🚀 Mejoras Recomendadas (Estándares Profesionales)

Para alcanzar estándares profesionales completos, considera implementar:

1. **CI/CD Pipeline** - Automatización de deployments
2. **Monitoreo y Observabilidad** - Logging centralizado, métricas, alertas
3. **Backup Automatizado** - Estrategia de backup y disaster recovery
4. **Security Scanning** - Escaneo de vulnerabilidades en CI/CD
5. **Secret Management** - Gestión profesional de secretos (ver `docker-compose.secrets.yml.example`)

Ver `ANALISIS_ESTANDARES.md` para análisis detallado y roadmap de mejoras.
