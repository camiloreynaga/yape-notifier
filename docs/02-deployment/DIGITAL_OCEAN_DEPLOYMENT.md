# 🚀 Despliegue Completo en DigitalOcean

Guía paso a paso para desplegar tu MVP de Yape Notifier en DigitalOcean.

---

## 📋 Resumen de Cambios

### Backend (Laravel API)
- ✅ Arquitectura QR ya implementada
- ✅ Migraciones ya ejecutadas
- ✅ Lógica de vinculación/desvinculación lista
- ⚠️ **Necesita:** Ejecutar migraciones en producción

### Android App
- ✅ Modo capturador anónimo implementado
- ✅ Lógica de QR como autorización primaria
- ⚠️ **Necesita:** Actualizar `BASE_URL` y recompilar APK

---

## 🎯 Plan de Despliegue

```
PASO 1: Preparar Servidor DigitalOcean
   ↓
PASO 2: Actualizar Código Backend
   ↓
PASO 3: Ejecutar Migraciones
   ↓
PASO 4: Verificar Backend
   ↓
PASO 5: Actualizar Android App
   ↓
PASO 6: Compilar y Distribuir APK
   ↓
PASO 7: Pruebas End-to-End
```

---

## 🖥️ PASO 1: Preparar Servidor DigitalOcean

### 1.1 Conectar al Droplet

```bash
# Conectar vía SSH
ssh root@tu-droplet-ip

# O si tienes configurado un usuario
ssh tu-usuario@tu-droplet-ip
```

### 1.2 Verificar Estado Actual

```bash
# Ver contenedores corriendo
docker ps

# Ver estado de servicios
cd /var/apps/yape-notifier/infra/docker/environments/production
docker compose --env-file .env ps
```

**Resultado esperado:**
```
NAME                              STATUS
yape-notifier-php-fpm-prod        Up
yape-notifier-db-prod       Up (healthy)
yape-notifier-nginx-api-prod      Up
yape-notifier-dashboard-prod      Up
yape-notifier-caddy-prod          Up
yape-notifier-reverb-prod         Up (opcional)
```

---

## 🔄 PASO 2: Actualizar Código Backend

### 2.1 Hacer Backup

```bash
cd /var/apps/yape-notifier

# Backup de la base de datos
docker exec yape-notifier-db-prod pg_dump \
  -U postgres \
  -d yape_notifier \
  > backup_$(date +%Y%m%d_%H%M%S).sql

# Backup del código actual
tar -czf backup_code_$(date +%Y%m%d_%H%M%S).tar.gz apps/api
```

### 2.2 Actualizar Código desde Git

```bash
cd /var/apps/yape-notifier

# Ver rama actual
git branch

# Guardar cambios locales (si los hay)
git stash

# Actualizar desde repositorio
git pull origin main

# O si usas otra rama
git pull origin production
```

### 2.3 Verificar Archivos Críticos

```bash
# Verificar que las migraciones están presentes
ls -la apps/api/database/migrations/ | grep -E "(commerce|user_id_nullable)"

# Deberías ver:
# 2025_01_15_000003_add_commerce_to_devices_table.php
# 2025_12_28_000001_make_user_id_nullable_in_devices_table.php
```

---

## 🗄️ PASO 3: Ejecutar Migraciones

### 3.1 Verificar Migraciones Pendientes

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Ver migraciones pendientes
docker compose --env-file .env exec php-fpm php artisan migrate:status
```

**Buscar estas migraciones:**
```
┌─────────────────────────────────────────────────────────────────┬─────────┐
│ Migration                                                       │ Ran?    │
├─────────────────────────────────────────────────────────────────┼─────────┤
│ 2025_01_15_000003_add_commerce_to_devices_table                │ No      │
│ 2025_12_28_000001_make_user_id_nullable_in_devices_table       │ No      │
└─────────────────────────────────────────────────────────────────┴─────────┘
```

### 3.2 Ejecutar Migraciones

```bash
# Ejecutar migraciones
docker compose --env-file .env exec php-fpm php artisan migrate

# Confirmar con 'yes' cuando pregunte
```

**Salida esperada:**
```
Running migrations.

2025_01_15_000003_add_commerce_to_devices_table ............. DONE
2025_12_28_000001_make_user_id_nullable_in_devices_table ... DONE
```

### 3.3 Verificar Schema de Base de Datos

```bash
# Conectar a PostgreSQL
docker exec -it yape-notifier-db-prod psql -U postgres -d yape_notifier

# Verificar schema de devices
\d devices

# Deberías ver:
# - user_id: bigint (nullable)
# - commerce_id: bigint (nullable)
# - uuid: uuid (unique)
```

**Salida esperada:**
```sql
                                        Table "public.devices"
        Column         |              Type              | Collation | Nullable |
-----------------------+--------------------------------+-----------+----------+
 id                    | bigint                         |           | not null |
 user_id               | bigint                         |           |          | ← NULLABLE ✅
 commerce_id           | bigint                         |           |          | ← NULLABLE ✅
 uuid                  | uuid                           |           | not null |
 name                  | character varying(255)         |           | not null |
 alias                 | character varying(255)         |           |          |
 platform              | character varying(255)         |           | not null |
 is_active             | boolean                        |           | not null |
 last_seen_at          | timestamp(0) without time zone |           |          |
 created_at            | timestamp(0) without time zone |           |          |
 updated_at            | timestamp(0) without time zone |           |          |
Indexes:
    "devices_pkey" PRIMARY KEY, btree (id)
    "devices_uuid_unique" UNIQUE CONSTRAINT, btree (uuid)
```

```bash
# Salir de PostgreSQL
\q
```

---

## 🔍 PASO 4: Migrar Dispositivos Existentes (Opcional)

### 4.1 Diagnóstico de Dispositivos

```bash
# Conectar a PostgreSQL
docker exec -it yape-notifier-db-prod psql -U postgres -d yape_notifier

# Ver dispositivos sin commerce_id
SELECT 
    COUNT(*) as total,
    COUNT(commerce_id) as con_comercio,
    COUNT(*) - COUNT(commerce_id) as sin_comercio
FROM devices;
```

**Interpretación:**
```
 total | con_comercio | sin_comercio
-------|--------------|-------------
   10  |      8       |      2

✅ 8 dispositivos OK (tienen commerce_id)
⚠️ 2 dispositivos sin commerce_id → Necesitan migración
```

### 4.2 Opción A: Auto-reparación Pasiva (Recomendada)

**No hacer nada.** Los dispositivos se auto-repararán cuando los usuarios hagan login.

**Ventajas:**
- ✅ Sin riesgo
- ✅ Sin downtime
- ✅ Auto-reparación automática

**Desventajas:**
- ⚠️ Dispositivos sin login reciente quedan sin reparar

### 4.3 Opción B: Migración Proactiva (Si tienes > 5 dispositivos sin commerce_id)

```sql
-- Conectado a PostgreSQL
-- Sincronizar commerce_id de usuarios a dispositivos

UPDATE devices d
SET 
    commerce_id = u.commerce_id,
    updated_at = NOW()
FROM users u
WHERE 
    d.user_id = u.id
    AND d.commerce_id IS NULL
    AND u.commerce_id IS NOT NULL;

-- Ver cuántos se actualizaron
-- Ejemplo: UPDATE 5
```

**Verificar:**
```sql
-- Contar dispositivos sin commerce_id
SELECT COUNT(*) FROM devices WHERE commerce_id IS NULL;

-- Resultado esperado: 0 (o muy pocos)
```

```bash
# Salir de PostgreSQL
\q
```

---

## ✅ PASO 5: Verificar Backend

### 5.1 Verificar Logs

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Ver logs de PHP-FPM
docker compose --env-file .env logs -f php-fpm --tail=50

# Buscar errores (Ctrl+C para salir)
```

**Buscar:**
- ✅ Sin errores de migración
- ✅ Sin errores de base de datos
- ⚠️ Si hay errores, revisar y corregir antes de continuar

### 5.2 Probar Endpoints

```bash
# Verificar que la API responde
curl https://api.notificaciones.space/up

# Resultado esperado:
# {"status":"ok"}

# Probar endpoint de dispositivos (requiere auth)
curl -X GET https://api.notificaciones.space/api/devices \
  -H "Authorization: Bearer TU_TOKEN_AQUI" \
  -H "Accept: application/json"
```

### 5.3 Verificar Rutas de Vinculación

```bash
# Ver rutas relacionadas con device linking
docker compose --env-file .env exec php-fpm php artisan route:list | grep -E "(device|link)"

# Deberías ver:
# POST   /api/device-link/link-by-code
# POST   /api/device-link/generate-code
# POST   /api/devices/{id}/unlink
# POST   /api/notifications (sin auth:sanctum)
```

---

## 📱 PASO 6: Actualizar Android App

### 6.1 Actualizar BASE_URL

**Archivo:** `apps/android-client/app/src/main/java/com/yapenotifier/android/data/remote/ApiConfig.kt`

```kotlin
object ApiConfig {
    // Cambiar de localhost a tu dominio de producción
    const val BASE_URL = "https://api.notificaciones.space/"
    
    // O si usas IP directa
    // const val BASE_URL = "https://tu-droplet-ip/"
}
```

### 6.2 Verificar Configuración de Red

**Archivo:** `apps/android-client/app/src/main/AndroidManifest.xml`

Verificar que tenga:

```xml
<manifest ...>
    <!-- Permisos de red -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <application
        android:usesCleartextTraffic="false"
        ...>
        
        <!-- Network security config para HTTPS -->
        <meta-data
            android:name="android.net.http.cleartext_traffic_policy"
            android:value="false" />
    </application>
</manifest>
```

### 6.3 Verificar Configuración de Seguridad

**Archivo:** `apps/android-client/app/src/main/res/xml/network_security_config.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Producción: Solo HTTPS -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
    
    <!-- Solo para desarrollo local (comentar en producción) -->
    <!--
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
    -->
</network-security-config>
```

---

## 🔨 PASO 7: Compilar APK de Producción

### 7.1 Preparar Keystore (Si no lo tienes)

```bash
cd apps/android-client

# Generar keystore (primera vez)
keytool -genkey -v \
  -keystore yape-notifier-release.keystore \
  -alias yape-notifier \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Te pedirá:
# - Password del keystore (guárdalo en lugar seguro)
# - Nombre, organización, etc.
```

### 7.2 Configurar keystore.properties

**Archivo:** `apps/android-client/keystore.properties`

```properties
storeFile=yape-notifier-release.keystore
storePassword=TU_PASSWORD_KEYSTORE
keyAlias=yape-notifier
keyPassword=TU_PASSWORD_KEY
```

**⚠️ IMPORTANTE:** 
- Este archivo NO debe estar en Git
- Guarda las contraseñas en un lugar seguro (1Password, etc.)

### 7.3 Compilar APK Release

```bash
cd apps/android-client

# Limpiar builds anteriores
./gradlew clean

# Compilar APK de release (firmado)
./gradlew assembleRelease

# APK estará en:
# app/build/outputs/apk/release/app-release.apk
```

**Verificar APK:**
```bash
# Ver información del APK
aapt dump badging app/build/outputs/apk/release/app-release.apk | grep -E "(package|version)"

# Deberías ver:
# package: name='com.yapenotifier.android'
# versionCode='1' versionName='1.0'
```

### 7.4 Probar APK Localmente (Antes de Distribuir)

```bash
# Instalar en dispositivo conectado
adb install app/build/outputs/apk/release/app-release.apk

# O si ya está instalado
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## 🧪 PASO 8: Pruebas End-to-End

### 8.1 Prueba de Vinculación de Dispositivo

**En Dashboard Web:**

1. Login como admin
   ```
   URL: https://dashboard.notificaciones.space/login
   Email: admin@example.com
   Password: tu-password
   ```

2. Ir a "Dispositivos"

3. Generar código QR
   ```
   Botón: "Generar Código QR"
   → Se muestra QR con código de 6 dígitos
   ```

**En Android App:**

4. Abrir app (recién instalada)
   ```
   Estado inicial: "⚠️ Dispositivo no vinculado"
   ```

5. Escanear QR
   ```
   Botón: "Vincular Dispositivo"
   → Escanear QR del dashboard
   → Ingresar código de 6 dígitos
   ```

6. Verificar vinculación
   ```
   Estado esperado: "✅ Modo Capturador (sin usuario)"
   ```

**Verificar en Backend:**

```bash
# Ver logs de vinculación
docker compose --env-file .env logs php-fpm | grep "Device linked"

# Deberías ver:
# Device linked to commerce via code
# device_id: 1
# commerce_id: 5
# user_id: null  ← Sin usuario (modo capturador) ✅
```

### 8.2 Prueba de Captura de Notificaciones

**En Android App:**

1. Dar permisos de notificaciones
   ```
   Settings → Apps → Yape Notifier → Permissions
   → Notification access: ON
   ```

2. Simular notificación de Yape
   ```
   Abrir app de Yape
   → Recibir pago de prueba
   → O usar app de prueba de notificaciones
   ```

3. Verificar captura
   ```
   En Yape Notifier:
   → Ver notificación capturada
   → Estado: "Enviando..."
   → Estado: "✅ Enviada"
   ```

**Verificar en Backend:**

```bash
# Ver logs de notificaciones
docker compose --env-file .env logs php-fpm | grep "Notification creation"

# Deberías ver:
# Notification creation request received
# device_uuid: abc-123
# authenticated: false  ← Sin autenticación ✅
# commerce_id: 5        ← Vinculado a comercio ✅
```

**Verificar en Dashboard:**

```
URL: https://dashboard.notificaciones.space/notifications

Deberías ver:
- Notificación recibida
- Monto parseado
- Nombre del remitente
- Fecha/hora
```

### 8.3 Prueba de Login Opcional (Admin)

**En Android App:**

1. Ir a "Admin Login"
   ```
   Menú → Admin Login
   ```

2. Ingresar credenciales
   ```
   Email: admin@example.com
   Password: tu-password
   ```

3. Verificar estado
   ```
   Estado esperado: "✅ Usuario: admin@example.com"
   ```

**Verificar que sigue funcionando:**
- ✅ Captura de notificaciones continúa
- ✅ Envío a backend continúa
- ✅ No se duplicó el dispositivo

### 8.4 Prueba de Desvinculación

**En Dashboard Web:**

1. Ir a "Dispositivos"

2. Seleccionar dispositivo

3. Desvincular
   ```
   Botón: "Desvincular"
   → Confirmar
   ```

**En Android App:**

4. Verificar estado
   ```
   Estado esperado: "⚠️ Dispositivo no vinculado"
   ```

5. Intentar enviar notificación
   ```
   Resultado esperado: Error "Dispositivo no vinculado"
   ```

**Verificar en Backend:**

```bash
# Ver logs de desvinculación
docker compose --env-file .env logs php-fpm | grep "Device unlinked"

# Deberías ver:
# Device unlinked from commerce and user
# device_id: 1
# old_commerce_id: 5
# old_user_id: null
```

---

## 📊 PASO 9: Monitoreo Post-Despliegue

### 9.1 Monitorear Logs (Primeras 24 horas)

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Ver logs en tiempo real
docker compose --env-file .env logs -f php-fpm

# Buscar:
# ✅ "Device linked to commerce via code"
# ✅ "Notification creation request received"
# ✅ "Device commerce_id auto-synced"
# ❌ Errores de base de datos
# ❌ "Unique violation"
```

### 9.2 Verificar Métricas

```bash
# Conectar a PostgreSQL
docker exec -it yape-notifier-db-prod psql -U postgres -d yape_notifier

# Dispositivos vinculados
SELECT 
    COUNT(*) as total_devices,
    COUNT(commerce_id) as devices_with_commerce,
    COUNT(user_id) as devices_with_user,
    COUNT(*) FILTER (WHERE commerce_id IS NOT NULL AND user_id IS NULL) as capturer_mode
FROM devices;

# Notificaciones recibidas hoy
SELECT 
    COUNT(*) as notifications_today,
    COUNT(DISTINCT device_id) as unique_devices,
    COUNT(DISTINCT commerce_id) as unique_commerces
FROM notifications
WHERE created_at >= CURRENT_DATE;

# Salir
\q
```

### 9.3 Configurar Alertas (Opcional)

**Crear script de monitoreo:**

```bash
# Archivo: /var/apps/yape-notifier/scripts/health-check.sh
#!/bin/bash

# Verificar que API responde
if ! curl -f https://api.notificaciones.space/up > /dev/null 2>&1; then
    echo "❌ API no responde"
    # Enviar alerta (email, Slack, etc.)
fi

# Verificar dispositivos sin commerce_id
ORPHAN_DEVICES=$(docker exec yape-notifier-db-prod psql \
    -U yapenotifier \
    -d yapenotifier_prod \
    -t -c "SELECT COUNT(*) FROM devices WHERE commerce_id IS NULL")

if [ "$ORPHAN_DEVICES" -gt 5 ]; then
    echo "⚠️ Hay $ORPHAN_DEVICES dispositivos sin commerce_id"
    # Enviar alerta
fi

echo "✅ Sistema OK"
```

**Configurar cron:**

```bash
# Editar crontab
crontab -e

# Ejecutar cada 5 minutos
*/5 * * * * /var/apps/yape-notifier/scripts/health-check.sh >> /var/log/yape-health.log 2>&1
```

---

## 🚨 Troubleshooting

### Problema 1: Migraciones fallan

**Error:**
```
SQLSTATE[42P01]: Undefined table: devices
```

**Solución:**
```bash
# Verificar que la BD existe
docker exec -it yape-notifier-db-prod psql -U postgres -l

# Si no existe, crearla
docker exec -it yape-notifier-db-prod psql -U postgres -c "CREATE DATABASE yapenotifier_prod"

# Ejecutar todas las migraciones desde cero
docker compose --env-file .env exec php-fpm php artisan migrate:fresh
```

### Problema 2: Android app no conecta a API

**Error en logs:**
```
java.net.UnknownHostException: api.notificaciones.space
```

**Solución:**
```kotlin
// Verificar BASE_URL en ApiConfig.kt
const val BASE_URL = "https://api.notificaciones.space/"  // Con / al final

// Verificar que el dispositivo tiene internet
// Verificar que el dominio resuelve (ping desde navegador)
```

### Problema 3: "Dispositivo no vinculado" después de escanear QR

**Verificar:**

```bash
# Ver logs de vinculación
docker compose --env-file .env logs php-fpm | grep -A 10 "linkDevice"

# Verificar que el dispositivo se creó
docker exec -it yape-notifier-db-prod psql -U postgres -d yape_notifier \
  -c "SELECT id, uuid, commerce_id, user_id FROM devices ORDER BY created_at DESC LIMIT 5"
```

**Posibles causas:**
- ❌ Código QR expirado (válido 24 horas)
- ❌ Código QR ya usado
- ❌ Error de red al vincular

### Problema 4: Notificaciones no llegan al backend

**Verificar:**

```bash
# Ver logs de Android (con dispositivo conectado)
adb logcat | grep "SendNotificationWorker"

# Ver logs de backend
docker compose --env-file .env logs php-fpm | grep "Notification creation"
```

**Posibles causas:**
- ❌ Dispositivo sin commerce_id
- ❌ Permisos de notificación no otorgados
- ❌ Error de red (verificar BASE_URL)

---

## 📋 Checklist Final

### Backend
```
□ Código actualizado desde Git
□ Migraciones ejecutadas correctamente
□ Schema de devices verificado (user_id nullable, commerce_id nullable)
□ Dispositivos existentes migrados (si aplica)
□ Logs sin errores
□ Endpoints de vinculación funcionando
□ Endpoint de notificaciones sin auth:sanctum
```

### Android App
```
□ BASE_URL actualizado a producción
□ Network security config configurado (solo HTTPS)
□ APK compilado y firmado
□ APK probado localmente
□ Vinculación de dispositivo funciona
□ Captura de notificaciones funciona
□ Envío a backend funciona
□ Login opcional funciona
□ Desvinculación funciona
```

### Distribución
```
□ APK subido a Google Drive / Dropbox / etc.
□ Link compartido con usuarios
□ Instrucciones de instalación enviadas
□ Monitoreo configurado
□ Alertas configuradas (opcional)
```

---

## 🎉 ¡Despliegue Completo!

Tu MVP ahora está corriendo en producción con la nueva arquitectura:

✅ **Backend:**
- Dispositivos pueden vincularse sin autenticación
- QR es la autorización primaria
- Multi-tenant seguro
- Historial completo preservado

✅ **Android:**
- Modo capturador anónimo
- Login opcional para admins
- Vinculación por QR
- Envío de notificaciones sin auth

✅ **Usuarios:**
- Experiencia simplificada
- Sin login obligatorio
- Solo escanear QR y listo

---

## 📚 Referencias

- [Device Lifecycle](../05-features/DEVICE_LIFECYCLE.md)
- [Device Commerce Transfer](../05-features/DEVICE_COMMERCE_TRANSFER.md)
- [QR Authorization Architecture](../03-architecture/QR_AUTHORIZATION_ARCHITECTURE.md)
- [Migration Guide](../06-operations/MIGRATION_EXISTING_DEVICES.md)
- [Docker Deployment](DEPLOYMENT.md)


