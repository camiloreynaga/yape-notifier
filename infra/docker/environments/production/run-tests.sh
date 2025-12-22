#!/bin/bash
# Script para ejecutar pruebas usando Docker en producción
# Uso: ./run-tests.sh [api|dashboard|all]

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Directorio del script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Verificar que existe el archivo .env
if [ ! -f .env ]; then
    echo -e "${RED}Error: Archivo .env no encontrado${NC}"
    echo "Por favor, crea un archivo .env basado en .env.example"
    exit 1
fi

# Función para ejecutar tests de API
run_api_tests() {
    echo -e "${GREEN}🧪 Ejecutando tests de API...${NC}"
    docker compose -f docker-compose.test.yml --env-file .env run --rm api-test php artisan test "$@"
}

# Función para ejecutar tests de Dashboard
run_dashboard_tests() {
    echo -e "${GREEN}🧪 Ejecutando tests de Dashboard...${NC}"
    docker compose -f docker-compose.test.yml --env-file .env run --rm dashboard-test npm run test:ci
}

# Función para ejecutar todos los tests
run_all_tests() {
    echo -e "${GREEN}🧪 Ejecutando todos los tests...${NC}"
    
    echo -e "${YELLOW}Iniciando servicios de prueba...${NC}"
    docker compose -f docker-compose.test.yml --env-file .env up -d db-test
    
    echo -e "${YELLOW}Esperando a que la base de datos esté lista...${NC}"
    sleep 5
    
    echo -e "${GREEN}Ejecutando tests de API...${NC}"
    run_api_tests || {
        echo -e "${RED}❌ Tests de API fallaron${NC}"
        docker compose -f docker-compose.test.yml --env-file .env down
        exit 1
    }
    
    echo -e "${GREEN}Ejecutando tests de Dashboard...${NC}"
    run_dashboard_tests || {
        echo -e "${RED}❌ Tests de Dashboard fallaron${NC}"
        docker compose -f docker-compose.test.yml --env-file .env down
        exit 1
    }
    
    echo -e "${GREEN}✅ Todos los tests pasaron${NC}"
}

# Función para construir las imágenes
build_images() {
    echo -e "${GREEN}🔨 Construyendo imágenes de prueba (con BuildKit para cache optimizado)...${NC}"
    export DOCKER_BUILDKIT=1
    export COMPOSE_DOCKER_CLI_BUILD=1
    docker compose -f docker-compose.test.yml --env-file .env build
}

# Función para limpiar
cleanup() {
    echo -e "${YELLOW}🧹 Limpiando contenedores de prueba...${NC}"
    docker compose -f docker-compose.test.yml --env-file .env down -v
}

# Función para mostrar ayuda
show_help() {
    echo "Uso: $0 [comando] [opciones]"
    echo ""
    echo "Comandos:"
    echo "  build          Construir imágenes de prueba"
    echo "  api            Ejecutar tests de API"
    echo "  dashboard      Ejecutar tests de Dashboard"
    echo "  all            Ejecutar todos los tests"
    echo "  cleanup        Limpiar contenedores y volúmenes"
    echo "  help           Mostrar esta ayuda"
    echo ""
    echo "Ejemplos:"
    echo "  $0 build                    # Construir imágenes"
    echo "  $0 api                      # Ejecutar tests de API"
    echo "  $0 api --filter=Unit        # Ejecutar solo tests unitarios"
    echo "  $0 dashboard                # Ejecutar tests de Dashboard"
    echo "  $0 all                      # Ejecutar todos los tests"
    echo "  $0 cleanup                  # Limpiar contenedores"
}

# Procesar argumentos
case "${1:-help}" in
    build)
        build_images
        ;;
    api)
        shift
        run_api_tests "$@"
        ;;
    dashboard)
        run_dashboard_tests
        ;;
    all)
        run_all_tests
        ;;
    cleanup)
        cleanup
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        echo -e "${RED}Comando desconocido: $1${NC}"
        show_help
        exit 1
        ;;
esac

