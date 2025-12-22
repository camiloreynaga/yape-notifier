# Script PowerShell para ejecutar pruebas usando Docker en producción
# Uso: .\run-tests.ps1 [api|dashboard|all]

param(
    [Parameter(Position=0)]
    [ValidateSet("build", "api", "dashboard", "all", "cleanup", "help")]
    [string]$Command = "help",
    
    [Parameter(ValueFromRemainingArguments=$true)]
    [string[]]$TestArgs
)

$ErrorActionPreference = "Stop"

# Directorio del script
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

# Verificar que existe el archivo .env
if (-not (Test-Path .env)) {
    Write-Host "Error: Archivo .env no encontrado" -ForegroundColor Red
    Write-Host "Por favor, crea un archivo .env basado en .env.example"
    exit 1
}

# Función para ejecutar tests de API
function Run-ApiTests {
    Write-Host "🧪 Ejecutando tests de API..." -ForegroundColor Green
    $argsString = if ($TestArgs) { $TestArgs -join " " } else { "" }
    docker compose -f docker-compose.test.yml --env-file .env run --rm api-test php artisan test $argsString
}

# Función para ejecutar tests de Dashboard
function Run-DashboardTests {
    Write-Host "🧪 Ejecutando tests de Dashboard..." -ForegroundColor Green
    docker compose -f docker-compose.test.yml --env-file .env run --rm dashboard-test npm run test:ci
}

# Función para ejecutar todos los tests
function Run-AllTests {
    Write-Host "🧪 Ejecutando todos los tests..." -ForegroundColor Green
    
    Write-Host "Iniciando servicios de prueba..." -ForegroundColor Yellow
    docker compose -f docker-compose.test.yml --env-file .env up -d db-test
    
    Write-Host "Esperando a que la base de datos esté lista..." -ForegroundColor Yellow
    Start-Sleep -Seconds 5
    
    Write-Host "Ejecutando tests de API..." -ForegroundColor Green
    try {
        Run-ApiTests
        if ($LASTEXITCODE -ne 0) {
            throw "Tests de API fallaron"
        }
    } catch {
        Write-Host "❌ Tests de API fallaron" -ForegroundColor Red
        docker compose -f docker-compose.test.yml --env-file .env down
        exit 1
    }
    
    Write-Host "Ejecutando tests de Dashboard..." -ForegroundColor Green
    try {
        Run-DashboardTests
        if ($LASTEXITCODE -ne 0) {
            throw "Tests de Dashboard fallaron"
        }
    } catch {
        Write-Host "❌ Tests de Dashboard fallaron" -ForegroundColor Red
        docker compose -f docker-compose.test.yml --env-file .env down
        exit 1
    }
    
    Write-Host "✅ Todos los tests pasaron" -ForegroundColor Green
}

# Función para construir las imágenes
function Build-Images {
    Write-Host "🔨 Construyendo imágenes de prueba (con BuildKit para cache optimizado)..." -ForegroundColor Green
    $env:DOCKER_BUILDKIT = "1"
    $env:COMPOSE_DOCKER_CLI_BUILD = "1"
    docker compose -f docker-compose.test.yml --env-file .env build
}

# Función para limpiar
function Cleanup {
    Write-Host "🧹 Limpiando contenedores de prueba..." -ForegroundColor Yellow
    docker compose -f docker-compose.test.yml --env-file .env down -v
}

# Función para mostrar ayuda
function Show-Help {
    Write-Host "Uso: .\run-tests.ps1 [comando] [opciones]"
    Write-Host ""
    Write-Host "Comandos:"
    Write-Host "  build          Construir imágenes de prueba"
    Write-Host "  api            Ejecutar tests de API"
    Write-Host "  dashboard      Ejecutar tests de Dashboard"
    Write-Host "  all            Ejecutar todos los tests"
    Write-Host "  cleanup        Limpiar contenedores y volúmenes"
    Write-Host "  help           Mostrar esta ayuda"
    Write-Host ""
    Write-Host "Ejemplos:"
    Write-Host "  .\run-tests.ps1 build                    # Construir imágenes"
    Write-Host "  .\run-tests.ps1 api                      # Ejecutar tests de API"
    Write-Host "  .\run-tests.ps1 api --filter=Unit        # Ejecutar solo tests unitarios"
    Write-Host "  .\run-tests.ps1 dashboard                # Ejecutar tests de Dashboard"
    Write-Host "  .\run-tests.ps1 all                      # Ejecutar todos los tests"
    Write-Host "  .\run-tests.ps1 cleanup                  # Limpiar contenedores"
}

# Procesar comando
switch ($Command) {
    "build" {
        Build-Images
    }
    "api" {
        Run-ApiTests
    }
    "dashboard" {
        Run-DashboardTests
    }
    "all" {
        Run-AllTests
    }
    "cleanup" {
        Cleanup
    }
    "help" {
        Show-Help
    }
    default {
        Write-Host "Comando desconocido: $Command" -ForegroundColor Red
        Show-Help
        exit 1
    }
}

