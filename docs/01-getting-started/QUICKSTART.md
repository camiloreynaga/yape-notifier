# ⚡ Quick Start - Yape Notifier

Guía rápida para levantar el sistema completo (API + Dashboard).

> 📖 **Para una guía completa paso a paso, consulta [DEPLOYMENT.md](../02-deployment/DEPLOYMENT.md)**

---

## 💻 Desarrollo Local (Windows)

### Comandos Rápidos (Copiar y Pegar)

#### 1. Iniciar Docker (Windows)

```powershell
# Iniciar Docker Desktop manualmente desde el menú de Windows
# Esperar a que Docker Desktop esté completamente iniciado (ícono verde)
```

#### 2. Levantar Backend

```powershell
cd apps\api
docker-compose up -d
docker-compose ps
```

#### 3. Ejecutar Migraciones

```powershell
docker-compose exec app php artisan migrate --force
```

#### 4. Verificar Todo

```powershell
# Ejecutar script de verificación
.\check-migrations.ps1
```

#### 5. Ver Logs

```powershell
# En una terminal separada
docker-compose logs -f app
```

#### 6. Android: Reinstalar App

```powershell
cd ..\android-client

# Limpiar y compilar
.\gradlew clean assembleDebug

# Desinstalar versión anterior
adb uninstall com.yapenotifier.android

# Instalar nueva versión
adb install app\build\outputs\apk\debug\app-debug.apk

# Ver logs
adb logcat -c
adb logcat -s YapeNotifier:* LinkDeviceViewModel:* RetrofitClient:*
```

### Prueba Rápida

#### Generar Código QR (desde web dashboard)

1. Login en https://dashboard.notificaciones.space
2. Ir a "Dispositivos" → "Agregar Dispositivo"
3. Click en "Generar Código QR"
4. Copiar el código (ej: `ABC12345`)

#### Vincular desde Android

1. Abrir la app
2. Escanear QR o ingresar código
3. Click en "Vincular"
4. ✅ Debería mostrar "Dispositivo vinculado exitosamente"

### Verificación Rápida en BD

```powershell
docker-compose exec app php artisan tinker
```

```php
// En tinker:
$device = \App\Models\Device::latest()->first();
echo "Device ID: " . $device->id . "\n";
echo "Commerce ID: " . $device->commerce_id . "\n";
echo "User ID: " . ($device->user_id ?? 'NULL') . "\n";

// Verificar que commerce_id NO es NULL y user_id ES NULL
```

### Troubleshooting Rápido

#### Docker no inicia
```powershell
# Reiniciar Docker Desktop
# Cerrar Docker Desktop completamente
# Abrir Docker Desktop nuevamente
# Esperar a que el ícono esté verde
```

#### Migraciones fallan
```powershell
# Ver error específico
docker-compose exec app php artisan migrate --force

# Si hay error de conexión a BD:
docker-compose restart db
docker-compose exec app php artisan migrate --force
```

#### App Android no conecta
```kotlin
// Verificar URL en build.gradle.kts (línea 48):
buildConfigField("String", "API_BASE_URL", "\"https://api.notificaciones.space/\"")

// Recompilar:
.\gradlew clean assembleDebug
```

### Checklist Mínimo

- [ ] Docker Desktop corriendo (ícono verde)
- [ ] `docker-compose ps` muestra contenedores "Up"
- [ ] `php artisan migrate:status` muestra migraciones "Ran"
- [ ] App Android instalada
- [ ] Código QR generado
- [ ] Dispositivo vinculado exitosamente

**Si todos los checks son ✅, el sistema está funcionando correctamente.**

---

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

- **[DEPLOYMENT.md](../02-deployment/DEPLOYMENT.md)** - Guía completa de despliegue en producción
- **[DEPLOYMENT_CHECKLIST.md](../02-deployment/DEPLOYMENT_CHECKLIST.md)** - Checklist de despliegue
- **[TESTING.md](../04-development/TESTING.md)** - Guía completa de pruebas
- **[TESTING_QR_LINKING.md](../04-development/TESTING_QR_LINKING.md)** - Guía de pruebas del sistema QR
- **[DEVICE_LINKING_GUIDE.md](../05-features/DEVICE_LINKING_GUIDE.md)** - Guía de vinculación de dispositivos
- **[DEVICE_LINKING_FLOW.md](../05-features/DEVICE_LINKING_FLOW.md)** - Flujo detallado de vinculación
- **[TROUBLESHOOTING.md](../06-operations/TROUBLESHOOTING.md)** - Diagnóstico y solución de problemas
