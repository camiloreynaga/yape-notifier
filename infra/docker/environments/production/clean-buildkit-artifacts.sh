#!/bin/bash

# ============================================
# Script para Limpiar Artefactos de BuildKit
# ============================================
# Elimina archivos temporales creados por Docker BuildKit
# que pueden aparecer en el directorio de trabajo
#
# Uso: ./clean-buildkit-artifacts.sh
# ============================================

set -e

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
if [ ! -f "docker-compose.yml" ]; then
    error "Este script debe ejecutarse desde infra/docker/environments/production"
    exit 1
fi

info "Limpiando artefactos de Docker BuildKit..."
info ""

# Lista de archivos/patrones a eliminar (artefactos comunes de BuildKit)
ARTIFACTS=(
    "composer.phar"
    "CACHED"
    "exporting"
    "naming"
    "reading"
    "resolve"
    "transferring"
    "writing"
    "="
    "[dashboard"
    "[dashboard]"
    "[nginx-api"
    "[nginx-api]"
    "[php-fpm"
    "[php-fpm]"
    "[reverb"
    "[reverb]"
    "[internal]"
)

CLEANED=0
NOT_FOUND=0

for ARTIFACT in "${ARTIFACTS[@]}"; do
    # Buscar archivos que coincidan (incluyendo en subdirectorios)
    FOUND=$(find . -maxdepth 3 -name "$ARTIFACT" -type f 2>/dev/null || echo "")
    
    if [ -n "$FOUND" ]; then
        while IFS= read -r FILE; do
            if [ -f "$FILE" ]; then
                info "Eliminando: $FILE"
                rm -f "$FILE"
                CLEANED=$((CLEANED + 1))
            fi
        done <<< "$FOUND"
    else
        NOT_FOUND=$((NOT_FOUND + 1))
    fi
done

# Limpiar archivos que empiezan con [
info "Limpiando archivos que empiezan con '['..."
find . -maxdepth 3 -name '\[*' -type f 2>/dev/null | while read -r FILE; do
    info "Eliminando: $FILE"
    rm -f "$FILE"
    CLEANED=$((CLEANED + 1))
done

echo ""
if [ $CLEANED -gt 0 ]; then
    info "✅ Limpieza completada: $CLEANED archivo(s) eliminado(s)"
else
    info "✅ No se encontraron artefactos para limpiar"
fi

info ""
info "Verificando estado de Git..."
if command -v git &> /dev/null; then
    UNTRACKED=$(git status --porcelain 2>/dev/null | grep "^??" | wc -l || echo "0")
    if [ "$UNTRACKED" -gt 0 ]; then
        warn "Aún hay $UNTRACKED archivo(s) sin seguimiento en Git"
        warn "Ejecuta: git status para verlos"
    else
        info "✅ No hay archivos sin seguimiento"
    fi
fi

info ""
info "Para prevenir esto en el futuro:"
info "  1. Los patrones ya están en .gitignore"
info "  2. Asegúrate de ejecutar builds desde el directorio correcto"
info "  3. No ejecutes 'docker compose build' desde el directorio raíz del proyecto"

