#!/bin/bash

# ============================================
# Staging Setup Script
# ============================================
# Uso: ./setup.sh
# Configura el entorno de staging por primera vez
# ============================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

section() {
    echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
}

section "🔧 Configuración Inicial de Staging"

# Verificar que estamos en el directorio correcto
if [ ! -f "docker-compose.yml" ]; then
    error "Este script debe ejecutarse desde infra/docker/environments/staging"
    exit 1
fi

# Crear .env desde ejemplo
if [ ! -f ".env" ]; then
    if [ -f ".env.example" ]; then
        cp .env.example .env
        info "✅ Archivo .env creado desde .env.example"
    else
        warn "⚠️  Archivo .env.example no encontrado"
        warn "💡 Creando .env básico..."
        
        # Crear .env básico
        cat > .env << 'EOF'
# ============================================
# Yape Notifier - Staging Environment
# ============================================
# ⚠️ IMPORTANTE: Configura DB_PASSWORD antes de desplegar
# ============================================

# Base de Datos
DB_PASSWORD=TU_CONTRASEÑA_STAGING_SEGURA_AQUI
DB_DATABASE=yape_notifier_staging
DB_USERNAME=postgres

# URLs
APP_URL=http://localhost:8080
DASHBOARD_API_URL=http://localhost:8080

# Laravel
APP_NAME="Yape Notifier API (Staging)"
APP_ENV=staging
APP_DEBUG=true
APP_KEY=

# Base de Datos Laravel
DB_CONNECTION=pgsql
DB_HOST=db
DB_PORT=5432

# Sesiones y Cache
SESSION_DRIVER=database
SESSION_LIFETIME=120
CACHE_DRIVER=file
QUEUE_CONNECTION=database

# Logging
LOG_CHANNEL=stderr
LOG_LEVEL=debug
EOF
        info "✅ Archivo .env básico creado"
    fi
else
    warn "⚠️  Archivo .env ya existe"
fi

section "📝 Configuración Requerida"

warn "⚠️  IMPORTANTE: Debes configurar las siguientes variables en .env:"
echo ""
echo "  1. DB_PASSWORD - Contraseña segura para PostgreSQL (OBLIGATORIO)"
echo "  2. APP_URL - URL de la API (opcional, default: http://localhost:8080)"
echo "  3. DASHBOARD_API_URL - URL de la API para el dashboard (opcional)"
echo ""

# Verificar si DB_PASSWORD está configurado
if grep -q "^DB_PASSWORD=TU_CONTRASEÑA" .env || grep -q "^DB_PASSWORD=$" .env; then
    error "❌ DB_PASSWORD no está configurado"
    echo ""
    info "💡 Para configurar:"
    echo "   nano .env"
    echo "   # Busca DB_PASSWORD= y cambia por una contraseña segura"
    echo ""
    read -p "¿Deseas editar .env ahora? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        ${EDITOR:-nano} .env
    fi
else
    info "✅ DB_PASSWORD parece estar configurado"
fi

section "🌐 Configuración DNS (Opcional)"

echo "¿Quieres usar subdominios para staging?"
echo "  - staging-api.notificaciones.space"
echo "  - staging-dashboard.notificaciones.space"
echo ""
read -p "¿Configurar subdominios? (y/N): " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
    info "📖 Sigue la guía en CONFIGURACION_DNS.md para configurar DNS"
    info "   Luego actualiza .env con:"
    echo "     APP_URL=http://staging-api.notificaciones.space"
    echo "     DASHBOARD_API_URL=http://staging-api.notificaciones.space"
else
    info "✅ Usarás puerto directo (http://TU_IP:8080)"
fi

section "✅ Configuración Inicial Completada"

info "Próximos pasos:"
echo ""
echo "  1. ✅ Verifica que .env tiene DB_PASSWORD configurado"
echo "  2. 📖 Lee SETUP_COMPLETO.md para guía detallada"
echo "  3. 🚀 Ejecuta ./deploy.sh para desplegar"
echo ""
echo "📚 Documentación disponible:"
echo "   - SETUP_COMPLETO.md - Guía completa paso a paso"
echo "   - CONFIGURACION_DNS.md - Configurar subdominios"
echo "   - ../../../../docs/02-deployment/STAGING_ENVIRONMENT_STRATEGY.md - Estrategia general"
echo ""

