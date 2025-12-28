# Gestión de Dependencias - Proceso Profesional

## ⚠️ PROBLEMA IDENTIFICADO

El archivo `composer.lock` se genera **fuera de Docker** en máquinas locales con diferentes versiones de PHP. Esto causa incompatibilidades cuando el lock file se genera con PHP 8.4 pero el Dockerfile usa PHP 8.2.

## ✅ SOLUCIÓN PROFESIONAL

**SIEMPRE usar Docker para actualizar dependencias**, garantizando que `composer.lock` se genere con la misma versión de PHP que usa el Dockerfile (PHP 8.2 LTS).

## 📋 Proceso Correcto

### Opción 1: Usar Makefile (Recomendado)

```bash
# Desde la raíz del proyecto
make composer:update

# Agregar nueva dependencia
make composer:require PACKAGE=laravel/sanctum

# Validar compatibilidad
make composer:validate
```

### Opción 2: Usar Script Directo

```bash
cd apps/api
./update-dependencies.sh
```

### Opción 3: Docker Manual

```bash
cd apps/api
docker run --rm -v $(pwd):/app -w /app php:8.2-cli sh -c \
    "curl -sS https://getcomposer.org/installer | php && php composer.phar update --no-interaction"
```

## 🚫 NUNCA HACER

```bash
# ❌ NO ejecutar composer directamente en tu máquina local
composer update
composer require package/name

# ❌ Esto genera composer.lock con la versión de PHP local
# ❌ Puede ser PHP 8.3, 8.4, etc. → Incompatible con Docker (PHP 8.2)
```

## 🔒 Prevención Automática

1. **Pre-commit hook**: Valida `composer.lock` antes de commitear
2. **Scripts de deploy**: Validan compatibilidad antes del build
3. **Makefile**: Comandos que siempre usan Docker

## 📝 Flujo de Trabajo

1. **Agregar dependencia nueva**:
   ```bash
   make composer:require PACKAGE=nombre/paquete
   git add composer.json composer.lock
   git commit -m "feat: add nombre/paquete"
   ```

2. **Actualizar dependencias existentes**:
   ```bash
   make composer:update
   git add composer.lock
   git commit -m "chore: update dependencies"
   ```

3. **Validar antes de commit**:
   ```bash
   make composer:validate
   ```

## 🎯 Garantías

- ✅ `composer.lock` siempre compatible con PHP 8.2 LTS
- ✅ Misma versión de PHP que Dockerfile
- ✅ Builds reproducibles
- ✅ Sin sorpresas en producción

