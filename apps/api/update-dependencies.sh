#!/bin/bash

# ============================================
# Script PROFESIONAL para actualizar dependencias
# ============================================
# PROBLEMA: composer.lock se genera en máquinas locales con diferentes versiones de PHP
# SOLUCIÓN: Este script SIEMPRE usa Docker con PHP 8.2 LTS (mismo que Dockerfile)
# 
# USO:
#   cd apps/api
#   ./update-dependencies.sh
#
# O desde la raíz del proyecto:
#   make composer:update
# ============================================

set -e

# Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Verificar que estamos en el directorio correcto
if [ ! -f "composer.json" ]; then
    error "Este script debe ejecutarse desde apps/api"
    exit 1
fi

info "Actualizando dependencias usando PHP 8.2 LTS (mismo que Dockerfile)..."

# Verificar que composer.json especifica PHP 8.2
PHP_VERSION_REQUIREMENT=$(grep -o '"php":\s*"[^"]*"' composer.json | head -1)
if ! echo "$PHP_VERSION_REQUIREMENT" | grep -q "8\.2"; then
    error "composer.json no especifica PHP 8.2"
    error "Versión encontrada: $PHP_VERSION_REQUIREMENT"
    exit 1
fi

# Actualizar composer.lock usando PHP 8.2 en Docker
info "Ejecutando: composer update usando PHP 8.2 LTS..."
docker run --rm -v "$(pwd):/app" -w /app php:8.2-cli sh -c \
    "curl -sS https://getcomposer.org/installer | php && php composer.phar update --no-interaction"

if [ $? -ne 0 ]; then
    error "Error al actualizar dependencias"
    exit 1
fi

# Verificar que el nuevo lock file es compatible
info "Verificando compatibilidad del nuevo composer.lock..."
VALIDATION_OUTPUT=$(docker run --rm -v "$(pwd):/app" -w /app php:8.2-cli sh -c \
    "curl -sS https://getcomposer.org/installer | php && php composer.phar install --dry-run --no-dev --no-interaction" 2>&1 || true)

if echo "$VALIDATION_OUTPUT" | grep -q "lock file is not up to date\|does not satisfy\|compatible set"; then
    error "El nuevo composer.lock aún tiene problemas"
    echo "$VALIDATION_OUTPUT" | grep -E "lock file|does not satisfy|compatible set" | head -3
    exit 1
fi

info "✅ composer.lock actualizado y compatible con PHP 8.2 LTS"
info ""
info "Próximos pasos:"
info "  1. Revisa los cambios: git diff composer.lock"
info "  2. Commit: git add composer.lock"
info "  3. Commit: git commit -m 'chore: update composer.lock using PHP 8.2 LTS'"
info "  4. Push: git push"

