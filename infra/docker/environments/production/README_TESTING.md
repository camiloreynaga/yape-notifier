# Testing en Producción con Docker

> **Nota**: Esta es la documentación específica del entorno de producción. Para la guía completa de testing, ver `../../../../docs/04-development/TESTING.md`.

Este directorio contiene la configuración para ejecutar pruebas usando las imágenes de producción de Docker.

## 📋 Requisitos

- Docker y Docker Compose instalados
- Archivo `.env` configurado en este directorio (puedes basarte en `.env.example`)

## 🚀 Uso Rápido

### Linux/macOS

```bash
# Construir imágenes de prueba
make test:prod:build

# Ejecutar todos los tests
make test:prod

# Ejecutar solo tests de API
make test:prod:api

# Ejecutar solo tests de Dashboard
make test:prod:dashboard

# Limpiar contenedores y volúmenes
make test:prod:cleanup
```

### Windows (PowerShell)

```powershell
# Construir imágenes de prueba
.\run-tests.ps1 build

# Ejecutar todos los tests
.\run-tests.ps1 all

# Ejecutar solo tests de API
.\run-tests.ps1 api

# Ejecutar solo tests de Dashboard
.\run-tests.ps1 dashboard

# Limpiar contenedores y volúmenes
.\run-tests.ps1 cleanup
```

### Linux/macOS (Script directo)

```bash
# Construir imágenes de prueba
./run-tests.sh build

# Ejecutar todos los tests
./run-tests.sh all

# Ejecutar solo tests de API
./run-tests.sh api

# Ejecutar solo tests de Dashboard
./run-tests.sh dashboard

# Limpiar contenedores y volúmenes
./run-tests.sh cleanup
```

## 🔧 Comandos Docker Compose Directos

Si prefieres usar Docker Compose directamente:

```bash
# Construir imágenes
docker compose -f docker-compose.test.yml --env-file .env build

# Ejecutar tests de API
docker compose -f docker-compose.test.yml --env-file .env run --rm api-test php artisan test

# Ejecutar tests de Dashboard
docker compose -f docker-compose.test.yml --env-file .env run --rm dashboard-test npm run test:ci

# Ejecutar tests con filtros (solo tests unitarios)
docker compose -f docker-compose.test.yml --env-file .env run --rm api-test php artisan test --filter=Unit

# Limpiar todo
docker compose -f docker-compose.test.yml --env-file .env down -v
```

## 📁 Estructura

- `docker-compose.test.yml`: Configuración de Docker Compose para pruebas
- `run-tests.sh`: Script bash para ejecutar pruebas (Linux/macOS)
- `run-tests.ps1`: Script PowerShell para ejecutar pruebas (Windows)
- `Dockerfile.test.production`: Dockerfile para API de pruebas (en `apps/api/`)

## 🧪 Servicios de Prueba

### API Tests (`api-test`)
- Basado en la imagen de producción pero con dependencias de desarrollo
- Ejecuta PHPUnit/Pest tests
- Requiere base de datos PostgreSQL (`db-test`)
- Genera reportes de cobertura en `/var/www/coverage`

### Dashboard Tests (`dashboard-test`)
- Basado en Node.js Alpine
- Ejecuta Vitest tests
- Genera reportes de cobertura en `/app/coverage`

### Base de Datos (`db-test`)
- PostgreSQL 15 Alpine
- Base de datos separada para pruebas (`yape_notifier_test`)
- Se limpia automáticamente antes de cada ejecución

## 📊 Cobertura de Código

Los reportes de cobertura se generan en volúmenes Docker:

- **API**: `api_test_coverage_prod` → `/var/www/coverage/html`
- **Dashboard**: `dashboard_test_coverage_prod` → `/app/coverage`

Para acceder a los reportes:

```bash
# Inspeccionar volumen de cobertura de API
docker run --rm -v yape-notifier_api_test_coverage_prod:/data alpine ls -la /data

# Copiar reporte de cobertura de API a local
docker run --rm -v yape-notifier_api_test_coverage_prod:/data -v $(pwd):/output alpine cp -r /data/html /output/api-coverage

# Copiar reporte de cobertura de Dashboard a local
docker run --rm -v yape-notifier_dashboard_test_coverage_prod:/data -v $(pwd):/output alpine cp -r /data /output/dashboard-coverage
```

## 🔍 Opciones Avanzadas

### Ejecutar tests específicos

```bash
# Solo tests unitarios
docker compose -f docker-compose.test.yml --env-file .env run --rm api-test php artisan test --filter=Unit

# Solo tests de feature
docker compose -f docker-compose.test.yml --env-file .env run --rm api-test php artisan test --filter=Feature

# Test específico
docker compose -f docker-compose.test.yml --env-file .env run --rm api-test php artisan test tests/Unit/PaymentNotificationValidatorTest.php
```

### Modo interactivo

```bash
# Acceder al shell del contenedor de API
docker compose -f docker-compose.test.yml --env-file .env run --rm api-test sh

# Acceder al shell del contenedor de Dashboard
docker compose -f docker-compose.test.yml --env-file .env run --rm dashboard-test sh
```

### Ver logs

```bash
# Ver logs de todos los servicios
docker compose -f docker-compose.test.yml --env-file .env logs -f

# Ver logs solo de API
docker compose -f docker-compose.test.yml --env-file .env logs -f api-test
```

## ⚙️ Variables de Entorno

Las pruebas usan las siguientes variables de entorno (configuradas en `.env`):

- `DB_DATABASE`: Base de datos para pruebas (default: `yape_notifier_test`)
- `DB_USERNAME`: Usuario de PostgreSQL (default: `postgres`)
- `DB_PASSWORD`: Contraseña de PostgreSQL
- `APP_ENV`: Entorno de la aplicación (forzado a `testing`)

## 🐛 Troubleshooting

### Error: "Database connection failed"
- Verifica que el servicio `db-test` esté corriendo y saludable
- Verifica las credenciales en `.env`
- Espera unos segundos después de iniciar `db-test` para que esté listo

### Error: "Container already exists"
```bash
# Limpiar contenedores existentes
docker compose -f docker-compose.test.yml --env-file .env down
```

### Error: "Permission denied" en scripts
```bash
# Dar permisos de ejecución (Linux/macOS)
chmod +x run-tests.sh
```

### Limpiar todo y empezar de nuevo
```bash
# Detener y eliminar contenedores, volúmenes y redes
docker compose -f docker-compose.test.yml --env-file .env down -v --remove-orphans

# Limpiar imágenes no utilizadas (opcional)
docker image prune -f
```

## 📝 Notas

- Las pruebas usan una base de datos separada para no afectar datos de producción
- Los volúmenes de cobertura persisten entre ejecuciones
- Las imágenes se construyen basándose en los Dockerfiles de producción
- Los tests se ejecutan en modo `testing` con `APP_DEBUG=true` para mejor debugging

---

## 📚 Referencias

- **Guía completa de testing**: Ver `../../../../docs/04-development/TESTING.md`
- **Docker**: Ver `../../../../docs/02-deployment/DOCKER.md`
- **Deployment**: Ver `../../../../docs/02-deployment/DEPLOYMENT.md`

