# 🔧 Solución: Error "column is_active does not exist"

## 📋 Problema

**Error:**
```
SQLSTATE[42703]: Undefined column: 7 ERROR: column "is_active" of relation "users" does not exist
```

**Causa:**
La migración `2026_01_10_000002_make_user_id_required_in_devices.php` intenta crear un usuario con el campo `is_active`, pero la tabla `users` no tiene esa columna porque nunca se creó una migración para agregarla.

---

## ✅ Solución Implementada

### 1. Migración Nueva: `2026_01_09_000002_add_is_active_to_users_table.php`

Se creó una migración que agrega la columna `is_active` a la tabla `users`:

```php
Schema::table('users', function (Blueprint $table) {
    $table->boolean('is_active')->default(true)->after('role');
    $table->index('is_active');
});
```

**Características:**
- ✅ Se ejecuta ANTES de la migración que la usa (fecha `2026_01_09`)
- ✅ Valor por defecto: `true` (usuarios activos)
- ✅ Incluye índice para búsquedas rápidas

### 2. Migración Mejorada: `2026_01_10_000002_make_user_id_required_in_devices.php`

Se agregó una verificación robusta que asegura que la columna existe antes de usarla:

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
- ✅ Funciona incluso si la migración anterior no se ejecutó
- ✅ Idempotente (se puede ejecutar múltiples veces)
- ✅ No rompe si la columna ya existe

---

## 🚀 Solución en Producción

### Opción A: Script Automático (Recomendado)

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Ejecutar script de fix
chmod +x fix-is-active-column.sh
./fix-is-active-column.sh
```

**¿Qué hace el script?**
1. ✅ Verifica si la columna `is_active` existe
2. ✅ Si no existe, la agrega directamente a la base de datos
3. ✅ Crea el índice necesario
4. ✅ Actualiza usuarios existentes a `is_active = true`
5. ✅ Registra la migración en la tabla `migrations` si es necesario
6. ✅ Permite continuar con el despliegue

### Opción B: Solución Manual

Si prefieres hacerlo manualmente:

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# 1. Conectar a PostgreSQL
docker compose --env-file .env exec -it db psql -U postgres -d yape_notifier

# 2. Agregar columna is_active
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true;
CREATE INDEX IF NOT EXISTS users_is_active_index ON users(is_active);
UPDATE users SET is_active = true WHERE is_active IS NULL;

# 3. Salir
\q

# 4. Registrar migración (si no está registrada)
docker compose --env-file .env exec php-fpm php artisan tinker
```

Dentro de tinker:
```php
$batch = DB::table('migrations')->max('batch') ?? 0;
DB::table('migrations')->insert([
    'migration' => '2026_01_09_000002_add_is_active_to_users_table',
    'batch' => $batch + 1
]);
exit
```

### Opción C: Re-ejecutar Migraciones

Si prefieres que Laravel maneje todo:

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# 1. Agregar columna manualmente (una vez)
docker compose --env-file .env exec -T db psql -U postgres -d yape_notifier -c \
    "ALTER TABLE users ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true;"

# 2. Continuar con migraciones
docker compose --env-file .env exec php-fpm php artisan migrate
```

---

## 🔍 Verificación

Después de aplicar la solución, verifica:

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# 1. Verificar que la columna existe
docker compose --env-file .env exec -T db psql -U postgres -d yape_notifier -c \
    "SELECT column_name, data_type, column_default FROM information_schema.columns WHERE table_name='users' AND column_name='is_active';"

# Debe mostrar:
# column_name | data_type | column_default
# is_active   | boolean   | true

# 2. Verificar migraciones
docker compose --env-file .env exec php-fpm php artisan migrate:status

# 3. Verificar que no hay errores
docker compose --env-file .env exec php-fpm php artisan migrate
```

---

## 📝 Orden de Ejecución de Migraciones

El orden correcto es:

1. ✅ `2024_01_01_000001_create_users_table.php` - Crea tabla users
2. ✅ `2025_01_15_000002_add_commerce_to_users_table.php` - Agrega commerce_id y role
3. ✅ `2026_01_09_000002_add_is_active_to_users_table.php` - **NUEVA: Agrega is_active**
4. ✅ `2026_01_10_000001_add_pin_to_users_table.php` - Agrega pin
5. ✅ `2026_01_10_000002_make_user_id_required_in_devices.php` - Usa is_active (ahora funciona)

---

## 🎯 Prevención Futura

Para evitar este tipo de problemas en el futuro:

1. **Siempre crear migraciones para nuevas columnas** antes de usarlas
2. **Usar verificaciones robustas** (`Schema::hasColumn()`) en migraciones complejas
3. **Probar migraciones localmente** antes de desplegar
4. **Revisar dependencias** entre migraciones antes de commit

---

## ✅ Checklist de Solución

- [ ] Ejecutar script `fix-is-active-column.sh` o solución manual
- [ ] Verificar que la columna `is_active` existe
- [ ] Verificar que la migración está registrada
- [ ] Continuar con `./deploy.sh` o `./update.sh`
- [ ] Verificar que las migraciones se ejecutan sin errores
- [ ] Verificar que la API funciona correctamente

---

## 🆘 Si el Problema Persiste

Si después de aplicar la solución el error persiste:

1. **Verificar logs:**
   ```bash
   docker compose --env-file .env logs php-fpm --tail=100 | grep -i "is_active\|migration"
   ```

2. **Verificar estado de la base de datos:**
   ```bash
   docker compose --env-file .env exec -it db psql -U postgres -d yape_notifier
   \d users  # Ver estructura de la tabla
   ```

3. **Verificar migraciones ejecutadas:**
   ```bash
   docker compose --env-file .env exec php-fpm php artisan migrate:status
   ```

4. **Forzar re-ejecución de migración específica:**
   ```bash
   # Si la migración falló a medias, puede necesitar limpieza
   docker compose --env-file .env exec php-fpm php artisan migrate:rollback --step=1
   docker compose --env-file .env exec php-fpm php artisan migrate
   ```

---

**Solución implementada y probada** ✅

Para más información sobre despliegue, ver [GUIA_PRODUCCION_PASO_A_PASO.md](GUIA_PRODUCCION_PASO_A_PASO.md)




