# 🔍 Causa Raíz del Error: `is_active` column does not exist

## 📋 Análisis del Problema

### Situación en el Servidor (Producción)

**Migraciones que SÍ están en el servidor:**
- ✅ `2026_01_10_000002_make_user_id_required_in_devices.php` (commit `6fcf9ed`)
  - Esta migración intenta crear un usuario con `is_active => false`
  - **PROBLEMA:** La columna `is_active` no existe en la tabla `users`

**Migración que NO está en el servidor:**
- ❌ `2026_01_09_000002_add_is_active_to_users_table.php` 
  - Esta migración crea la columna `is_active`
  - **NUNCA se hizo commit/push al servidor**

### Orden de Ejecución Esperado vs Real

**Orden CORRECTO (lo que debería pasar):**
1. `2026_01_09_000002_add_is_active_to_users_table.php` → Crea columna `is_active`
2. `2026_01_10_000002_make_user_id_required_in_devices.php` → Usa columna `is_active` ✅

**Orden REAL en el servidor (lo que pasó):**
1. `2026_01_10_000002_make_user_id_required_in_devices.php` → Intenta usar `is_active` ❌
2. Error: `column "is_active" does not exist`

---

## 🎯 Causa Raíz

**El problema fue un error en el flujo de desarrollo:**

1. Se creó la migración `2026_01_10_000002` que **depende** de `is_active`
2. Se hizo commit y push de `2026_01_10_000002` al servidor
3. **NUNCA se creó/commit la migración `2026_01_09_000002`** que crea `is_active`
4. El servidor intentó ejecutar `2026_01_10_000002` sin tener la columna necesaria
5. **Resultado:** Error de migración

---

## ✅ Solución Implementada

### 1. Migración Creada Retroactivamente

Se creó la migración faltante:
- `2026_01_09_000002_add_is_active_to_users_table.php`
- Fecha `2026_01_09` para que se ejecute ANTES de `2026_01_10_000002`

### 2. Migración Mejorada con Verificación

Se agregó verificación robusta en `2026_01_10_000002`:
```php
// Verifica que la columna existe antes de usarla
if (!Schema::hasColumn('users', 'is_active')) {
    // Crea la columna si no existe
}
```

### 3. Script de Fix para Producción

Script que agrega la columna directamente si no existe:
- `infra/docker/environments/production/fix-is-active-column.sh`

---

## 🚀 Pasos para Resolver

### Paso 1: Hacer Commit y Push de la Migración Faltante

```bash
# Verificar que la migración está en staging
git status

# Hacer commit
git commit -m "fix: add missing is_active column migration for users table

- Add migration 2026_01_09_000002_add_is_active_to_users_table.php
- Improve 2026_01_10_000002 with robust column check
- Add production fix script"

# Push al servidor
git push origin tenant-version
```

### Paso 2: En el Servidor - Aplicar Fix

```bash
# Conectarse al servidor
ssh deploy@tu-servidor
cd /var/apps/yape-notifier

# Actualizar código
git pull origin tenant-version

# Aplicar fix
cd infra/docker/environments/production
chmod +x fix-is-active-column.sh
./fix-is-active-column.sh

# Continuar con despliegue
./update.sh
```

---

## 📝 Lecciones Aprendidas

### ✅ Buenas Prácticas para Evitar Este Problema

1. **Siempre verificar dependencias entre migraciones**
   - Si una migración usa una columna, asegurarse de que existe una migración anterior que la crea

2. **Usar fechas de migración consistentes**
   - Migraciones relacionadas deben tener fechas consecutivas
   - Ejemplo: `2026_01_09_000002` (crea columna) → `2026_01_10_000002` (usa columna)

3. **Probar migraciones localmente antes de push**
   ```bash
   # Probar orden de migraciones
   php artisan migrate:fresh
   php artisan migrate
   ```

4. **Usar verificaciones robustas en migraciones complejas**
   ```php
   // En lugar de asumir que la columna existe
   if (!Schema::hasColumn('table', 'column')) {
       // Crear columna
   }
   ```

5. **Revisar migraciones en PR antes de merge**
   - Verificar que todas las dependencias estén incluidas
   - Verificar orden de ejecución

---

## 🔍 Cómo Detectar Este Tipo de Problemas Antes

### Checklist Pre-Commit de Migraciones

- [ ] ¿La migración usa columnas/tablas que existen?
- [ ] ¿Hay migraciones anteriores que crean esas dependencias?
- [ ] ¿El orden de fechas es correcto?
- [ ] ¿Se probó localmente con `migrate:fresh`?
- [ ] ¿Se incluyeron todas las migraciones relacionadas en el mismo commit?

### Comando de Verificación

```bash
# Verificar orden de migraciones
ls -1 apps/api/database/migrations/ | grep "2026_01" | sort

# Verificar dependencias en código
grep -r "is_active" apps/api/database/migrations/
```

---

## 📊 Resumen

| Aspecto | Estado |
|---------|--------|
| **Causa** | Migración `2026_01_09_000002` nunca se subió al servidor |
| **Síntoma** | Error: `column "is_active" does not exist` |
| **Solución** | Crear migración faltante + script de fix |
| **Prevención** | Verificar dependencias antes de commit |

---

**Problema identificado y solucionado** ✅

Para aplicar la solución, ver [SOLUCION_ERROR_IS_ACTIVE.md](SOLUCION_ERROR_IS_ACTIVE.md)



