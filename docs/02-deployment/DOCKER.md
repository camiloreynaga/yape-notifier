# Docker Infrastructure

Infraestructura Docker organizada por entornos siguiendo estándares profesionales.

## 📁 Estructura

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
```

**Acceso:**
- API: `http://localhost:8000/up`
- Dashboard: `http://localhost:3000` (Vite dev server con hot reload)
- Database: `localhost:5432`

### Staging

```bash
cd infra/docker/environments/staging
./setup.sh
nano .env  # Configurar DB_PASSWORD
./deploy.sh
```

**Acceso:**
- API: `http://localhost:8080/up`
- Dashboard: `http://localhost:8080/`

### Production

```bash
cd infra/docker/environments/production
./setup.sh
nano .env  # Configurar DB_PASSWORD seguro
./deploy.sh
```

**Acceso:**
- API: `https://api.notificaciones.space`
- Dashboard: `https://dashboard.notificaciones.space`

## 📋 Comandos Útiles

### Ver logs

```bash
cd infra/docker/environments/[environment]
docker compose --env-file .env logs -f
```

### Detener servicios

```bash
docker compose --env-file .env down
```

### Reconstruir imágenes

```bash
# Con BuildKit habilitado (recomendado - cache optimizado)
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1
docker compose --env-file .env build

# Sin cache (rebuild completo)
docker compose --env-file .env build --no-cache
```

**Nota**: BuildKit está habilitado automáticamente en los scripts `deploy.sh` y `update.sh`. Para builds manuales, exporta las variables de entorno antes de construir.

### Ejecutar comandos en contenedores

```bash
# PHP-FPM
docker compose --env-file .env exec php-fpm php artisan migrate

# Database
docker compose --env-file .env exec db psql -U postgres -d yape_notifier
```

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

**⚠️ IMPORTANTE:** Los archivos `.env` están en `.gitignore` y **NUNCA** deben committearse.

Para producción, considera usar:
- Docker Secrets (con Docker Swarm)
- Variables de entorno del sistema
- Secret management tools (HashiCorp Vault, AWS Secrets Manager)

## 🚀 Optimizaciones de Build

### BuildKit y Cache Optimizado

Todos los Dockerfiles usan **BuildKit** con cache mounts para optimizar builds:

- **Multi-stage builds**: Dependencias instaladas en etapa separada
- **Cache mounts**: Paquetes de Composer y npm se cachean entre builds
- **Layer optimization**: Solo se reconstruyen capas que cambian
- **Validación previa**: `composer.lock` se valida antes del build

**Beneficios**:
- Builds subsecuentes: **~1-2 min** (vs ~5-10 min sin cache)
- Menor uso de ancho de banda
- Builds más rápidos al cambiar solo código

**BuildKit se habilita automáticamente** en los scripts `deploy.sh` y `update.sh`. Para builds manuales:

```bash
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1
docker compose --env-file .env build
```

### ¿Qué es BuildKit y por qué se necesitan esas variables?

**BuildKit** es el motor de construcción moderno de Docker que reemplaza al motor antiguo. Ofrece mejoras significativas de rendimiento y nuevas características como **cache mounts**.

#### Variables de Entorno Requeridas

| Variable | Propósito | ¿Qué pasa sin ella? |
|----------|-----------|---------------------|
| `DOCKER_BUILDKIT=1` | Habilita BuildKit para `docker build` | Docker usa el motor antiguo, no reconoce `--mount=type=cache` |
| `COMPOSE_DOCKER_CLI_BUILD=1` | Hace que `docker compose build` use BuildKit | `docker compose build` puede no usar BuildKit aunque esté habilitado |

#### ¿Por qué son obligatorias en este proyecto?

Los Dockerfiles usan **cache mounts** que solo funcionan con BuildKit:

```dockerfile
# Ejemplo del Dockerfile.php-fpm (línea 28)
RUN --mount=type=cache,target=/root/.composer/cache \
    composer install --no-dev --optimize-autoloader ...
```

**Sin BuildKit:**
- ❌ El build falla o ignora el cache mount
- ❌ Composer descarga TODOS los paquetes en cada build (5-10 minutos)
- ❌ No se aprovecha el cache entre builds
- ❌ Mayor uso de ancho de banda

**Con BuildKit:**
- ✅ El cache mount funciona correctamente
- ✅ Composer solo descarga paquetes nuevos o actualizados (30 seg - 2 min)
- ✅ El cache persiste entre builds
- ✅ Menor uso de ancho de banda

#### Comparación de Tiempos

**Sin BuildKit (motor antiguo):**
```
Build 1: [Descargar Composer packages] ████████████████████ 10 min
Build 2: [Descargar Composer packages] ████████████████████ 10 min
Build 3: [Descargar Composer packages] ████████████████████ 10 min
```

**Con BuildKit (cache mounts):**
```
Build 1: [Descargar Composer packages] ████████████████████ 10 min
Build 2: [Usar cache]                   ██ 30 seg
Build 3: [Usar cache]                   ██ 30 seg
```

#### Configuración Permanente (Opcional)

Si prefieres no exportar las variables cada vez, puedes configurarlas permanentemente:

**Para el usuario actual:**
```bash
# Agregar a ~/.bashrc o ~/.zshrc
echo 'export DOCKER_BUILDKIT=1' >> ~/.bashrc
echo 'export COMPOSE_DOCKER_CLI_BUILD=1' >> ~/.bashrc
source ~/.bashrc
```

**Para todo el sistema:**
```bash
# Agregar a /etc/environment (requiere sudo)
sudo sh -c 'echo "DOCKER_BUILDKIT=1" >> /etc/environment'
sudo sh -c 'echo "COMPOSE_DOCKER_CLI_BUILD=1" >> /etc/environment'
```

**Nota:** Los scripts `deploy.sh` y `update.sh` ya exportan estas variables automáticamente, así que no es necesario configurarlas permanentemente a menos que hagas builds manuales frecuentes.

### Validación de composer.lock

Los scripts de deploy validan automáticamente que `composer.lock` esté sincronizado con `composer.json` antes del build. Si está desactualizado, el deploy falla con instrucciones claras.

## 📝 Notas Importantes

1. **Base de Datos**: Cada entorno tiene su propia base de datos
2. **Volúmenes**: Los volúmenes de Docker son específicos por entorno
3. **Redes**: Cada entorno tiene su propia red Docker para aislamiento
4. **Healthchecks**: Todos los servicios tienen healthchecks configurados
5. **composer.lock**: Debe estar siempre sincronizado con composer.json (validado automáticamente)

## 🔍 Troubleshooting

### Error: "DB_PASSWORD no está configurado"
- Asegúrate de que el archivo `.env` existe y tiene `DB_PASSWORD` configurado

### Error: "Port already in use"
- Verifica que los puertos requeridos estén disponibles o cambia los puertos en `.env`

### Error: "Container unhealthy"
- Revisa los logs: `docker compose --env-file .env logs [service-name]`

## 📚 Documentación Adicional

- **[DEPLOYMENT.md](DEPLOYMENT.md)** - Guía completa de despliegue
- **[QUICKSTART.md](../01-getting-started/QUICKSTART.md)** - Guía rápida de inicio
- `infra/docker/environments/production/BACKUP.md` - Estrategia de backup
- `infra/docker/environments/production/MONITORING.md` - Guía de monitoreo

