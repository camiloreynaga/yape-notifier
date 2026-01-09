# Script de verificación de migraciones y estructura de base de datos
# Para el sistema de vinculación QR sin autenticación
# Windows PowerShell

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Verificación de Migraciones y BD" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

function Check-Command {
    param (
        [string]$Message,
        [int]$ExitCode
    )
    if ($ExitCode -eq 0) {
        Write-Host "✓ $Message" -ForegroundColor Green
        return $true
    } else {
        Write-Host "✗ $Message" -ForegroundColor Red
        return $false
    }
}

Write-Host "1. Verificando estado de migraciones..." -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
docker-compose exec app php artisan migrate:status
Check-Command "Migraciones listadas" $LASTEXITCODE
Write-Host ""

Write-Host "2. Ejecutando migraciones pendientes..." -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
docker-compose exec app php artisan migrate --force
Check-Command "Migraciones ejecutadas" $LASTEXITCODE
Write-Host ""

Write-Host "3. Verificando estructura de tabla 'devices'..." -ForegroundColor Yellow
Write-Host "================================================" -ForegroundColor Yellow
docker-compose exec app php artisan tinker --execute="`$columns = DB::select('DESCRIBE devices'); foreach (`$columns as `$column) { echo sprintf('%-30s %-15s %-10s', `$column->Field, `$column->Type, `$column->Null) . PHP_EOL; }"
Check-Command "Estructura de tabla mostrada" $LASTEXITCODE
Write-Host ""

Write-Host "4. Verificando que user_id sea nullable..." -ForegroundColor Yellow
Write-Host "===========================================" -ForegroundColor Yellow
docker-compose exec app php artisan tinker --execute="`$column = DB::select(\`"SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'devices' AND COLUMN_NAME = 'user_id' AND TABLE_SCHEMA = DATABASE()\`"); if (!empty(`$column) && `$column[0]->IS_NULLABLE === 'YES') { echo 'user_id es NULLABLE ✓' . PHP_EOL; } else { echo 'user_id NO es nullable ✗' . PHP_EOL; exit(1); }"
Check-Command "user_id es nullable" $LASTEXITCODE
Write-Host ""

Write-Host "5. Verificando que commerce_id exista..." -ForegroundColor Yellow
Write-Host "=========================================" -ForegroundColor Yellow
docker-compose exec app php artisan tinker --execute="`$column = DB::select(\`"SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'devices' AND COLUMN_NAME = 'commerce_id' AND TABLE_SCHEMA = DATABASE()\`"); if (!empty(`$column)) { echo 'commerce_id existe ✓' . PHP_EOL; } else { echo 'commerce_id NO existe ✗' . PHP_EOL; exit(1); }"
Check-Command "commerce_id existe" $LASTEXITCODE
Write-Host ""

Write-Host "6. Verificando tabla device_link_codes..." -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Yellow
docker-compose exec app php artisan tinker --execute="`$table = DB::select(\`"SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'device_link_codes' AND TABLE_SCHEMA = DATABASE()\`"); if (!empty(`$table)) { echo 'Tabla device_link_codes existe ✓' . PHP_EOL; } else { echo 'Tabla device_link_codes NO existe ✗' . PHP_EOL; exit(1); }"
Check-Command "Tabla device_link_codes existe" $LASTEXITCODE
Write-Host ""

Write-Host "7. Verificando rutas públicas..." -ForegroundColor Yellow
Write-Host "=================================" -ForegroundColor Yellow
docker-compose exec app php artisan route:list --path=devices/link
Check-Command "Rutas de vinculación listadas" $LASTEXITCODE
Write-Host ""

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Verificación completada" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Si todos los checks son ✓, la base de datos está correctamente configurada." -ForegroundColor Green
Write-Host "Si hay ✗, revisa los errores arriba y ejecuta las migraciones faltantes." -ForegroundColor Yellow
Write-Host ""




