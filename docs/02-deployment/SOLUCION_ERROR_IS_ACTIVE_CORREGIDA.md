# 🔧 Solución Correcta: Error "column is_active does not exist"

## 📋 Análisis Correcto del Problema

### Situación Real

1. **Migración `2026_01_10_000002` ya está en el servidor** (commit `6fcf9ed`)
   - Esta migración intenta usar `is_active => false`
   - **PERO:** La migración original NO tenía verificación para crear la columna

2. **El servidor ejecutó `2026_01_10_000002` sin tener la columna `is_active`**
   - Resultado: Error `column "is_active" does not exist`
   - La migración falló y puede estar registrada como fallida en la tabla `migrations`

### ❌ Solución Incorrecta (lo que inicialmente propusimos)

Crear una migración `2026_01_09_000002` con fecha anterior:
- **Problema:** Si `2026_01_10_000002` ya está registrada en `migrations`, Laravel NO ejecutará `2026_01_09_000002` porque ya pasó esa fecha
- **Problema:** Crear migraciones con fechas anteriores cuando ya hay migraciones posteriores ejecutadas es una mala práctica

---

## ✅ Solución Correcta

### 1. Migración Mejorada (YA IMPLEMENTADA)

La migración `2026_01_10_000002` ahora tiene verificación robusta:

```php
// Paso 1: Asegurar que la columna is_active existe (robustez)
if (!Schema::hasColumn('users', 'is_active')) {
    Schema::table('users', function (Blueprint $table) {
        $table->boolean('is_active')->default(true)->after('role');
        $table->index('is_active');
    });
}
```

**Ventajas:**
- ✅ Idempotente (se puede ejecutar múltiples veces)
- ✅ Crea la columna si no existe
- ✅ No requiere migración adicional
- ✅ Funciona incluso si la migración ya está registrada

### 2. Script de Fix para Producción

El script `fix-is-active-column.sh` agrega la columna directamente si no existe:

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production
chmod +x fix-is-active-column.sh
./fix-is-active-column.sh
```

**¿Cuándo usarlo?**
- Si la migración `2026_01_10_000002` falló y está registrada como fallida
- Si necesitas agregar la columna manualmente antes de re-ejecutar la migración

---

## 🚀 Pasos para Resolver en Producción

### Opción A: Usar Script de Fix (Recomendado)

```bash
# 1. Conectarse al servidor
ssh deploy@tu-servidor
cd /var/apps/yape-notifier/infra/docker/environments/production

# 2. Actualizar código (tiene la migración mejorada)
cd /var/apps/yape-notifier
git pull origin tenant-version

# 3. Aplicar fix (agrega columna si no existe)
cd infra/docker/environments/production
chmod +x fix-is-active-column.sh
./fix-is-active-column.sh

# 4. Si la migración está registrada como fallida, limpiarla
docker compose --env-file .env exec php-fpm php artisan tinker
```

Dentro de tinker:
```php
// Verificar si está registrada
DB::table('migrations')->where('migration', '2026_01_10_000002_make_user_id_required_in_devices')->first();

// Si está registrada, eliminarla para re-ejecutarla
DB::table('migrations')->where('migration', '2026_01_10_000002_make_user_id_required_in_devices')->delete();
exit
```

```bash
# 5. Re-ejecutar migraciones
docker compose --env-file .env exec php-fpm php artisan migrate

# 6. Continuar con despliegue
./update.sh
```

### Opción B: Agregar Columna Manualmente

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Agregar columna directamente
docker compose --env-file .env exec -T db psql -U postgres -d yape_notifier <<EOF
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true;
CREATE INDEX IF NOT EXISTS users_is_active_index ON users(is_active);
UPDATE users SET is_active = true WHERE is_active IS NULL;
EOF

# Si la migración está registrada como fallida, eliminarla
docker compose --env-file .env exec php-fpm php artisan tinker --execute="DB::table('migrations')->where('migration', '2026_01_10_000002_make_user_id_required_in_devices')->delete();"

# Re-ejecutar migraciones
docker compose --env-file .env exec php-fpm php artisan migrate
```

---

## 📝 Archivos Necesarios

### ✅ Archivos que SÍ necesitas commitear:

1. **`apps/api/database/migrations/2026_01_10_000002_make_user_id_required_in_devices.php`** (modificado)
   - Ya tiene la verificación robusta
   - Crea la columna si no existe

2. **`infra/docker/environments/production/fix-is-active-column.sh`** (nuevo)
   - Script de fix para producción
   - Agrega la columna manualmente si es necesario

### ❌ Archivos que NO necesitas:

1. ~~`apps/api/database/migrations/2026_01_09_000002_add_is_active_to_users_table.php`~~
   - **NO es necesaria** porque `2026_01_10_000002` ya crea la columna si no existe
   - Crear migraciones con fechas anteriores es problemático

---

## 🎯 Resumen de la Solución Correcta

| Aspecto | Solución |
|---------|----------|
| **Problema** | Migración `2026_01_10_000002` intenta usar `is_active` que no existe |
| **Causa** | La migración original no tenía verificación para crear la columna |
| **Solución** | Agregar verificación robusta en `2026_01_10_000002` que crea la columna si no existe |
| **Fix Producción** | Script que agrega la columna manualmente + re-ejecutar migración mejorada |
| **NO hacer** | Crear migración con fecha anterior (`2026_01_09_000002`) |

---

## ✅ Checklist de Solución Correcta

- [ ] Hacer commit solo de `2026_01_10_000002` (mejorada) y `fix-is-active-column.sh`
- [ ] **NO** commitear `2026_01_09_000002` (no es necesaria)
- [ ] En servidor: Ejecutar `fix-is-active-column.sh` para agregar columna
- [ ] En servidor: Si migración está registrada como fallida, eliminarla
- [ ] En servidor: Re-ejecutar migraciones con la versión mejorada
- [ ] Verificar que todo funciona correctamente

---

## 🎓 Lección Aprendida

**Regla de oro:** Si una migración necesita una columna/tabla que no existe, la migración misma debe crearla con verificación robusta (`Schema::hasColumn()`), NO crear una migración separada con fecha anterior.

**Por qué:**
- Las migraciones se ejecutan en orden cronológico
- Si ya hay migraciones posteriores ejecutadas, las anteriores no se ejecutarán
- Es mejor que cada migración sea autosuficiente y robusta

---

**Solución correcta implementada** ✅

Para aplicar la solución, ver los pasos en "Pasos para Resolver en Producción" arriba.



