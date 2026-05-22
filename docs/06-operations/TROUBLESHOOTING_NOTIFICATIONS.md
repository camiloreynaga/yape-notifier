# 🔴 SOLUCIÓN: Notificaciones No Capturadas

## Problema Identificado

Las notificaciones NO se están capturando porque el endpoint `/api/settings/monitored-packages` está devolviendo un **array vacío**.

### Causa Raíz

1. **El endpoint consulta paquetes con `commerce_id = NULL`** (paquetes globales)
2. **Probablemente NO HAY paquetes en la base de datos** o tienen `commerce_id` asignado
3. **El Android no puede saber qué paquetes monitorear** → no captura nada

## Cambios Realizados

### 1. Servicio de Monitor Packages (Backend)
**Archivo**: `apps/api/app/Services/MonitorPackageService.php`

```php
public function getActivePackagesArray(?int $commerceId = null): array
{
    $query = MonitorPackage::active()->ordered();

    if ($commerceId) {
        // Return packages for this commerce OR global packages (commerce_id IS NULL)
        $query->where(function ($q) use ($commerceId) {
            $q->where('commerce_id', $commerceId)
              ->orWhereNull('commerce_id');
        });
    } else {
        // If no commerce_id provided, return only GLOBAL packages (commerce_id IS NULL)
        $query->whereNull('commerce_id');
    }

    return $query->pluck('package_name')->toArray();
}
```

**Cambio**: Ahora filtra correctamente por `commerce_id IS NULL` cuando no se pasa commerce_id.

### 2. Servicio de Notificaciones (Android)
**Archivo**: `apps/android-client/app/src/main/java/com/yapenotifier/android/service/PaymentNotificationListenerService.kt`

**Cambios**:
- ✅ Inicialización con paquete por defecto (`com.bcp.innovacxion.yape.movil`)
- ✅ Uso de `runBlocking` para cargar paquetes antes de procesar notificaciones
- ✅ Extracción mejorada de título y texto (múltiples métodos de compatibilidad)
- ✅ Logging detallado para diagnóstico

## Pasos para Solucionar

### PASO 1: Verificar Estado Actual (Producción)

```bash
# Conectarse al servidor
ssh deploy@tu-servidor

# Ir al directorio de producción
cd /var/apps/yape-notifier/infra/docker/environments/production

# Verificar paquetes en la BD
docker compose --env-file .env exec php-fpm php artisan tinker --execute="
echo 'Total: ' . App\Models\MonitorPackage::count() . PHP_EOL;
echo 'Activos: ' . App\Models\MonitorPackage::where('is_active', true)->count() . PHP_EOL;
echo 'Globales: ' . App\Models\MonitorPackage::whereNull('commerce_id')->count() . PHP_EOL;
echo 'Paquetes: ' . PHP_EOL;
print_r((new App\Services\MonitorPackageService())->getActivePackagesArray());
"
```

### PASO 2: Poblar Monitor Packages (Si está vacío)

```bash
# Ejecutar seeder
docker compose --env-file .env exec php-fpm php artisan db:seed --class=MonitorPackageSeeder

# Verificar que se crearon
docker compose --env-file .env exec php-fpm php artisan tinker --execute="
App\Models\MonitorPackage::all()->each(function(\$p) {
    echo \$p->package_name . ' - Active: ' . (\$p->is_active ? 'YES' : 'NO') . ' - Commerce: ' . (\$p->commerce_id ?? 'NULL') . PHP_EOL;
});
"
```

### PASO 3: Verificar Endpoint API

```bash
# Probar endpoint desde el servidor
curl https://api.notificaciones.space/api/settings/monitored-packages

# Debería devolver algo como:
# {
#   "packages": [
#     "com.bcp.innovacxion.yape.movil",
#     "com.yape.android",
#     "com.plin.android",
#     "com.bcp.bancadigital",
#     ...
#   ]
# }
```

### PASO 4: Desplegar Cambios en el Código

```bash
# En tu máquina local
git add .
git commit -m "fix: corregir captura de notificaciones

- Fix race condition en monitoredPackages initialization
- Mejorar extracción de texto de notificaciones
- Corregir filtrado de paquetes globales en API
- Añadir logging detallado para diagnóstico"

git push origin main  # o tu rama

# En el servidor
cd /var/apps/yape-notifier
git pull origin main
cd infra/docker/environments/production
./update.sh
```

### PASO 5: Verificar en Android

1. **Reinstalar la app** o limpiar datos
2. **Abrir Logcat** y filtrar por `PaymentNotificationService`
3. **Buscar estos logs**:
   ```
   ✅ Initial monitored packages loaded: [...]
   🚀 ¡Conectado! Escuchando notificaciones.
   📦 X apps monitoreadas
   ```
4. **Enviar un pago de prueba** desde Yape
5. **Verificar logs**:
   ```
   📬 Notification received from package: com.bcp.innovacxion.yape.movil
   📋 Notification content - Package: ..., Title: '...', Text: '...'
   ✅ Payment detected! Sender: ..., Amount: ...
   💾 Payment notification saved locally
   ```

## Paquetes que Deberían Estar en la BD

El seeder crea estos paquetes (todos con `commerce_id = NULL`):

```
com.bcp.innovacxion.yape.movil  (Yape - package correcto)
com.yape.android                (Yape alternativo)
com.plin.android                (Plin)
com.bcp.bancadigital            (BCP Digital)
com.bbva.bbvacontinental        (BBVA Continental)
com.scotiabank.mobile           (Scotiabank)
pe.com.interbank.mobilebanking  (Interbank)
```

## Diagnóstico Rápido

### Si el endpoint devuelve `{"packages": []}`

❌ **NO HAY paquetes globales en la BD**

**Solución**: Ejecutar seeder (PASO 2)

### Si el Android no captura notificaciones

1. **Verificar permisos**: Configuración → Acceso a notificaciones → Habilitar app
2. **Verificar logs**: Filtrar por `PaymentNotificationService`
3. **Verificar paquetes**: Buscar log `Initial monitored packages loaded: [...]`
   - Si está vacío: El endpoint no está devolviendo paquetes
   - Si tiene paquetes pero no captura: Verificar que el package name sea correcto

### Package Name Correcto de Yape

⚠️ **IMPORTANTE**: El package name correcto de Yape es:
```
com.bcp.innovacxion.yape.movil
```

NO es:
- ❌ `com.yape.android`
- ❌ `com.bcp.innovacxion.yapeapp`

## Comandos Útiles para Diagnóstico

### Ver logs del servicio en Android (Logcat)

```bash
adb logcat | grep PaymentNotificationService
```

### Ver todas las notificaciones capturadas (Android)

```bash
# En la app, ir a "Notificaciones Guardadas" (debug)
# O usar adb:
adb shell "run-as com.yapenotifier.android sqlite3 /data/data/com.yapenotifier.android/databases/yape_notifier.db 'SELECT * FROM captured_notifications;'"
```

### Verificar API desde Android

```bash
# En el código Android, el endpoint es:
# GET {BASE_URL}/api/settings/monitored-packages
# 
# Verificar que BASE_URL apunte a producción:
# - Producción: https://api.notificaciones.space
# - Local: http://10.0.2.2:8000 (emulador) o http://192.168.x.x:8000 (dispositivo físico)
```

## Resumen de Archivos Modificados

1. ✅ `apps/api/app/Services/MonitorPackageService.php` - Filtrado correcto de paquetes globales
2. ✅ `apps/android-client/app/src/main/java/com/yapenotifier/android/service/PaymentNotificationListenerService.kt` - Race condition, extracción de texto, logging
3. ✅ `apps/api/check-monitor-packages.php` - Script de verificación (nuevo)

## Próximos Pasos

1. ✅ Verificar estado en producción (PASO 1)
2. ✅ Poblar paquetes si es necesario (PASO 2)
3. ✅ Desplegar cambios (PASO 4)
4. ✅ Probar en Android (PASO 5)

---

**Autor**: Senior Developer  
**Fecha**: 2025-01-10  
**Prioridad**: 🔴 CRÍTICA

