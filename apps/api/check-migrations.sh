#!/bin/bash

# Script de verificación de migraciones y estructura de base de datos
# Para el sistema de vinculación QR sin autenticación

echo "=================================="
echo "Verificación de Migraciones y BD"
echo "=================================="
echo ""

# Colores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Función para verificar comandos
check_command() {
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $1"
    else
        echo -e "${RED}✗${NC} $1"
        return 1
    fi
}

echo "1. Verificando estado de migraciones..."
echo "========================================"
php artisan migrate:status
check_command "Migraciones listadas"
echo ""

echo "2. Ejecutando migraciones pendientes..."
echo "========================================"
php artisan migrate --force
check_command "Migraciones ejecutadas"
echo ""

echo "3. Verificando estructura de tabla 'devices'..."
echo "================================================"
php artisan tinker --execute="
\$columns = DB::select('DESCRIBE devices');
foreach (\$columns as \$column) {
    echo sprintf('%-30s %-15s %-10s', \$column->Field, \$column->Type, \$column->Null) . PHP_EOL;
}
"
check_command "Estructura de tabla mostrada"
echo ""

echo "4. Verificando que user_id sea nullable..."
echo "==========================================="
php artisan tinker --execute="
\$column = DB::select(\"SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'devices' AND COLUMN_NAME = 'user_id' AND TABLE_SCHEMA = DATABASE()\");
if (!empty(\$column) && \$column[0]->IS_NULLABLE === 'YES') {
    echo 'user_id es NULLABLE ✓' . PHP_EOL;
} else {
    echo 'user_id NO es nullable ✗' . PHP_EOL;
    exit(1);
}
"
check_command "user_id es nullable"
echo ""

echo "5. Verificando que commerce_id exista..."
echo "========================================="
php artisan tinker --execute="
\$column = DB::select(\"SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'devices' AND COLUMN_NAME = 'commerce_id' AND TABLE_SCHEMA = DATABASE()\");
if (!empty(\$column)) {
    echo 'commerce_id existe ✓' . PHP_EOL;
} else {
    echo 'commerce_id NO existe ✗' . PHP_EOL;
    exit(1);
}
"
check_command "commerce_id existe"
echo ""

echo "6. Verificando tabla device_link_codes..."
echo "=========================================="
php artisan tinker --execute="
\$table = DB::select(\"SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'device_link_codes' AND TABLE_SCHEMA = DATABASE()\");
if (!empty(\$table)) {
    echo 'Tabla device_link_codes existe ✓' . PHP_EOL;
} else {
    echo 'Tabla device_link_codes NO existe ✗' . PHP_EOL;
    exit(1);
}
"
check_command "Tabla device_link_codes existe"
echo ""

echo "7. Verificando rutas públicas..."
echo "================================="
php artisan route:list --path=devices/link
check_command "Rutas de vinculación listadas"
echo ""

echo "=================================="
echo "Verificación completada"
echo "=================================="
echo ""
echo "Si todos los checks son ✓, la base de datos está correctamente configurada."
echo "Si hay ✗, revisa los errores arriba y ejecuta las migraciones faltantes."
echo ""




