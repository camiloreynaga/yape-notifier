# ✅ Checklist de Despliegue - DigitalOcean

> **Referencias relacionadas:**
> - [DEPLOYMENT.md](DEPLOYMENT.md) - Guía completa de despliegue
> - [DEPLOY_GUIDE_PRODUCTION.md](DEPLOY_GUIDE_PRODUCTION.md) - Guía detallada paso a paso
> - [DIGITAL_OCEAN_DEPLOYMENT.md](DIGITAL_OCEAN_DEPLOYMENT.md) - Guía específica de DigitalOcean

Guía rápida para desplegar la nueva arquitectura QR en producción.

---

## 🎯 Resumen de Cambios

### Backend

- ✅ QR como autorización primaria (ya implementado)
- ✅ Dispositivos sin autenticación (ya implementado)
- ✅ Migraciones listas (pendiente ejecutar)

### Android

- ⚠️ Actualizar BASE_URL
- ⚠️ Recompilar APK

---

## 📋 Checklist Backend (30 minutos)

### 1. Conectar al Servidor

```bash
□ ssh root@tu-droplet-ip
□ cd /var/apps/yape-notifier/infra/docker/environments/production
```

### 2. Hacer Backup

```bash
□ docker exec yape-notifier-db-prod pg_dump -U postgres -d yape_notifier > backup_$(date +%Y%m%d).sql
□ Verificar que el backup se creó: ls -lh backup_*.sql
```

### 3. Actualizar Código

```bash
□ cd /var/apps/yape-notifier
□ git stash (si hay cambios locales)
□ git pull origin main
□ Verificar migraciones: ls apps/api/database/migrations/ | grep commerce
```

### 4. Ejecutar Script Automatizado

```bash
□ cd infra/docker/environments/production
□ chmod +x update-architecture.sh
□ ./update-architecture.sh
□ Seguir las instrucciones del script
```

**O manualmente:**

```bash
□ docker compose --env-file .env exec php-fpm php artisan migrate:status
□ docker compose --env-file .env exec php-fpm php artisan migrate
□ docker compose --env-file .env exec php-fpm php artisan config:clear
```

### 5. Verificar Backend

```bash
□ curl https://api.notificaciones.space/up
   Resultado esperado: {"status":"ok"}

□ docker compose --env-file .env logs php-fpm --tail=50
   Buscar errores

□ docker exec -it yape-notifier-db-prod psql -U postgres -d yape_notifier
   \d devices
   Verificar: user_id nullable, commerce_id nullable
   \q
```

---

## 📱 Checklist Android (15 minutos)

### 1. Actualizar Configuración

**Archivo:** `apps/android-client/app/src/main/java/com/yapenotifier/android/data/remote/ApiConfig.kt`

```kotlin
□ Cambiar:
  const val BASE_URL = "https://api.notificaciones.space/"

  (O tu dominio/IP de DigitalOcean)
```

### 2. Compilar APK

```bash
□ cd apps/android-client
□ ./gradlew clean
□ ./gradlew assembleRelease
□ Verificar APK: ls -lh app/build/outputs/apk/release/app-release.apk
```

### 3. Probar APK Localmente

```bash
□ adb install -r app/build/outputs/apk/release/app-release.apk
□ Abrir app
□ Verificar que conecta a producción
```

---

## 🧪 Checklist Pruebas (20 minutos)

### 1. Prueba de Vinculación

**Dashboard:**

```
□ Login: https://dashboard.notificaciones.space/login
□ Ir a "Dispositivos"
□ Generar código QR
□ Copiar código de 6 dígitos
```

**Android:**

```
□ Abrir app (recién instalada)
□ Estado inicial: "⚠️ Dispositivo no vinculado"
□ Botón "Vincular Dispositivo"
□ Escanear QR o ingresar código
□ Estado final: "✅ Modo Capturador (sin usuario)"
```

**Backend:**

```bash
□ docker compose --env-file .env logs php-fpm | grep "Device linked"
  Verificar: commerce_id asignado, user_id null
```

### 2. Prueba de Captura de Notificaciones

**Android:**

```
□ Dar permisos de notificación
□ Simular notificación de Yape (o usar app de prueba)
□ Verificar que se captura
□ Verificar que se envía al backend
```

**Dashboard:**

```
□ Ir a "Notificaciones"
□ Verificar que aparece la notificación capturada
□ Verificar monto, nombre, fecha
```

### 3. Prueba de Login Opcional (Admin)

**Android:**

```
□ Menú → Admin Login
□ Ingresar credenciales de admin
□ Estado: "✅ Usuario: admin@example.com"
□ Verificar que sigue capturando notificaciones
```

### 4. Prueba de Desvinculación

**Dashboard:**

```
□ Ir a "Dispositivos"
□ Seleccionar dispositivo
□ Botón "Desvincular"
□ Confirmar
```

**Android:**

```
□ Estado: "⚠️ Dispositivo no vinculado"
□ Intentar enviar notificación
□ Error esperado: "Dispositivo no vinculado"
```

---

## 📊 Checklist Monitoreo (Primeras 24h)

### Logs

```bash
□ docker compose --env-file .env logs -f php-fpm
  Buscar:
  ✅ "Device linked to commerce via code"
  ✅ "Notification creation request received"
  ❌ Errores de base de datos
```

### Métricas

```bash
□ docker exec -it yape-notifier-db-prod psql -U postgres -d yape_notifier

□ SELECT COUNT(*) FROM devices WHERE commerce_id IS NULL;
  Resultado esperado: 0 (o muy pocos)

□ SELECT COUNT(*) FROM notifications WHERE created_at >= CURRENT_DATE;
  Verificar que hay notificaciones nuevas

□ \q
```

---

## 🚨 Troubleshooting Rápido

### Problema: Migraciones fallan

```bash
Solución:
□ docker compose --env-file .env exec php-fpm php artisan migrate:status
□ Verificar error específico
□ Restaurar backup si es necesario
```

### Problema: Android no conecta

```bash
Solución:
□ Verificar BASE_URL (debe terminar en /)
□ Verificar que el dominio resuelve (ping)
□ Ver logs de Android: adb logcat | grep "ApiService"
```

### Problema: "Dispositivo no vinculado"

```bash
Solución:
□ Verificar que el QR no expiró (válido 24h)
□ Verificar que el código no se usó antes
□ Ver logs: docker compose --env-file .env logs php-fpm | grep "linkDevice"
```

---

## 📦 Distribución del APK

### Opción 1: Google Drive

```
□ Subir app-release.apk a Google Drive
□ Compartir link con "Cualquiera con el enlace"
□ Enviar link a usuarios
```

### Opción 2: Dropbox

```
□ Subir app-release.apk a Dropbox
□ Compartir link público
□ Enviar link a usuarios
```

### Opción 3: Servidor Propio

```
□ scp app-release.apk root@tu-droplet:/var/www/html/downloads/
□ Compartir: https://tu-dominio.com/downloads/app-release.apk
```

### Instrucciones para Usuarios

```
1. Descargar APK desde el link
2. Habilitar "Instalar apps de fuentes desconocidas"
3. Instalar APK
4. Abrir app
5. Dar permisos de notificación
6. Escanear QR del dashboard
7. ¡Listo!
```

---

## ✅ Checklist Final

### Backend

```
□ Código actualizado
□ Migraciones ejecutadas
□ Schema verificado
□ Dispositivos migrados (si aplica)
□ Logs sin errores
□ Endpoints funcionando
```

### Android

```
□ BASE_URL actualizado
□ APK compilado
□ APK probado
□ Vinculación funciona
□ Captura funciona
□ Envío funciona
```

### Distribución

```
□ APK subido
□ Link compartido
□ Instrucciones enviadas
□ Monitoreo activo
```

---

## 🎉 ¡Despliegue Completo!

**Tiempo estimado total:** 1-2 horas

**Documentación completa:**

- [Guía de Despliegue Detallada](docs/02-deployment/DIGITAL_OCEAN_DEPLOYMENT.md)
- [Arquitectura QR](docs/03-architecture/QR_AUTHORIZATION_ARCHITECTURE.md)
- [Ciclo de Vida de Dispositivos](docs/05-features/DEVICE_LIFECYCLE.md)

**Soporte:**

- Ver logs: `docker compose --env-file .env logs -f php-fpm`
- Ver BD: `docker exec -it yape-notifier-db-prod psql -U postgres -d yape_notifier`
- Ver Android: `adb logcat | grep "YapeNotifier"`

---

**¿Listo para desplegar?** 🚀

1. Conecta a tu servidor
2. Ejecuta `./update-architecture.sh`
3. Actualiza y compila Android
4. Prueba end-to-end
5. Distribuye APK

**¡Éxito!** 🎊
