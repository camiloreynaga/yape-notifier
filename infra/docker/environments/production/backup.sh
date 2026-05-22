#!/bin/bash

# ============================================
# Production Backup Script
# ============================================
# Script para crear backup de base de datos
# Uso: ./backup.sh
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

# Configuración
PROJECT_DIR="$(pwd)"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

# Verificar directorio
if [ ! -f "docker-compose.yml" ] || [ ! -f ".env" ]; then
    error "Este script debe ejecutarse desde infra/docker/environments/production"
    exit 1
fi

# Detectar ruta de backup escribible
# Prioridad: 1) Variable BACKUP_DIR, 2) ./backups, 3) ~/backups/yape-notifier, 4) /tmp/yape-notifier-backups
if [ -n "$BACKUP_DIR" ]; then
    # Usar la variable de entorno si está configurada
    TARGET_DIR="$BACKUP_DIR"
elif [ -w "$PROJECT_DIR" ]; then
    # Usar directorio dentro del proyecto si es escribible
    TARGET_DIR="$PROJECT_DIR/backups"
elif [ -w "$HOME" ]; then
    # Usar directorio en el home del usuario
    TARGET_DIR="$HOME/backups/yape-notifier"
else
    # Último recurso: /tmp
    TARGET_DIR="/tmp/yape-notifier-backups"
fi

BACKUP_DIR="$TARGET_DIR"

# Intentar crear el directorio
if mkdir -p "$BACKUP_DIR" 2>/dev/null; then
    # Verificar que realmente podemos escribir
    if [ -w "$BACKUP_DIR" ]; then
        info "Usando directorio de backup: $BACKUP_DIR"
    else
        error "No se puede escribir en: $BACKUP_DIR"
        error "Configura BACKUP_DIR con una ruta escribible:"
        error "  export BACKUP_DIR=/ruta/escribible && ./backup.sh"
        exit 1
    fi
else
    error "No se pudo crear el directorio de backup: $BACKUP_DIR"
    error "Configura BACKUP_DIR con una ruta escribible:"
    error "  export BACKUP_DIR=/ruta/escribible && ./backup.sh"
    exit 1
fi

# Verificar que la BD está corriendo
if ! docker compose --env-file .env ps db | grep -q "Up"; then
    error "Base de datos no está corriendo"
    exit 1
fi

# Realizar backup
info "Iniciando backup de base de datos..."
BACKUP_FILE="$BACKUP_DIR/backup_$DATE.sql.gz"

if docker compose --env-file .env exec -T db pg_dump -U postgres yape_notifier | gzip > "$BACKUP_FILE"; then
    BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    info "✅ Backup creado: $BACKUP_FILE ($BACKUP_SIZE)"
    
    # Verificar integridad
    if gunzip -t "$BACKUP_FILE" 2>/dev/null; then
        info "✅ Backup verificado correctamente"
    else
        error "❌ El backup está corrupto"
        rm -f "$BACKUP_FILE"
        exit 1
    fi
else
    error "❌ Error al crear backup"
    exit 1
fi

# Eliminar backups antiguos
info "Eliminando backups más antiguos de $RETENTION_DAYS días..."
find "$BACKUP_DIR" -name "backup_*.sql.gz" -mtime +$RETENTION_DAYS -delete

# Mostrar backups restantes
info "Backups restantes:"
ls -lh "$BACKUP_DIR"/backup_*.sql.gz 2>/dev/null || warn "No hay backups anteriores"

info "✅ Proceso de backup completado"


