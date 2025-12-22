#!/bin/bash

# Script para instalar git hooks profesionales
# Ejecutar una vez después de clonar el repositorio

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
GIT_HOOKS_DIR="$PROJECT_ROOT/.git/hooks"

echo "🔧 Instalando git hooks profesionales..."

# Crear directorio de hooks si no existe
mkdir -p "$GIT_HOOKS_DIR"

# Instalar pre-commit hook
cat > "$GIT_HOOKS_DIR/pre-commit" << 'EOF'
#!/bin/bash

# Pre-commit hook para validar composer.lock
# Este hook previene commits de composer.lock incompatible con PHP 8.2 LTS

# Solo validar si composer.lock cambió
if git diff --cached --name-only | grep -q "apps/api/composer.lock"; then
    echo "🔍 Validando composer.lock antes del commit..."
    
    cd apps/api
    
    # Validar usando Docker con PHP 8.2 (mismo que producción)
    VALIDATION_OUTPUT=$(docker run --rm -v "$(pwd):/app" -w /app php:8.2-cli sh -c \
        "curl -sS https://getcomposer.org/installer | php && php composer.phar install --dry-run --no-dev --no-interaction" 2>&1 || true)
    
    if echo "$VALIDATION_OUTPUT" | grep -q "does not satisfy that requirement\|Your lock file does not contain a compatible set\|requires php >=8\.[34]"; then
        echo "❌ ERROR: composer.lock no es compatible con PHP 8.2 LTS"
        echo ""
        echo "El composer.lock fue generado con una versión de PHP diferente a 8.2"
        echo ""
        echo "SOLUCIÓN:"
        echo "  1. cd apps/api"
        echo "  2. ./update-dependencies.sh"
        echo "  3. O: make composer:update"
        echo ""
        echo "Esto regenerará composer.lock usando PHP 8.2 LTS (mismo que Dockerfile)"
        exit 1
    fi
    
    echo "✅ composer.lock es compatible con PHP 8.2 LTS"
    cd - > /dev/null
fi

exit 0
EOF

chmod +x "$GIT_HOOKS_DIR/pre-commit"

echo "✅ Git hooks instalados correctamente"
echo ""
echo "El pre-commit hook validará que composer.lock sea compatible con PHP 8.2 LTS"
echo "antes de permitir commits."

