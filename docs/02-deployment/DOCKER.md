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
docker compose --env-file .env build --no-cache
```

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

## 📝 Notas Importantes

1. **Base de Datos**: Cada entorno tiene su propia base de datos
2. **Volúmenes**: Los volúmenes de Docker son específicos por entorno
3. **Redes**: Cada entorno tiene su propia red Docker para aislamiento
4. **Healthchecks**: Todos los servicios tienen healthchecks configurados

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

