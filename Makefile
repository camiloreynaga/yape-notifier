.PHONY: help dev test build docker-up docker-down clean install

help: ## Mostrar esta ayuda
	@echo "Comandos disponibles:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

install: ## Instalar dependencias (para desarrollo local sin Docker)
	@echo "📦 Instalando dependencias localmente..."
	@echo "⚠️  Nota: En desarrollo se recomienda usar Docker. Ver docs/DEVELOPMENT_WORKFLOW.md"
	@cd apps/api && composer install
	@cd apps/web-dashboard && npm install
	@echo "✅ Dependencias instaladas"

install:docker: ## Instalar dependencias en contenedores Docker
	@echo "📦 Instalando dependencias en contenedores Docker..."
	@cd infra/docker/environments/development && docker compose --env-file .env build
	@echo "✅ Dependencias instaladas en contenedores"

dev: ## Iniciar entorno de desarrollo con Docker
	@echo "🚀 Iniciando entorno de desarrollo con Docker..."
	@echo "Backend: http://localhost:8000"
	@echo "Dashboard: http://localhost:3000"
	@cd infra/docker/environments/development && docker compose --env-file .env up -d
	@echo "✅ Servicios iniciados. Ver logs con: docker compose --env-file .env logs -f"

dev:api: ## Iniciar solo el backend (Docker)
	@echo "🚀 Iniciando backend con Docker..."
	@cd infra/docker/environments/development && docker compose --env-file .env up -d php-fpm nginx-api db

dev:dashboard: ## Iniciar solo el dashboard (Docker)
	@echo "🚀 Iniciando dashboard con Docker..."
	@cd infra/docker/environments/development && docker compose --env-file .env up -d dashboard

dev:logs: ## Ver logs del entorno de desarrollo
	@cd infra/docker/environments/development && docker compose --env-file .env logs -f

dev:down: ## Detener entorno de desarrollo
	@cd infra/docker/environments/development && docker compose --env-file .env down

test: ## Ejecutar todos los tests
	@echo "🧪 Ejecutando tests..."
	@cd apps/api && php artisan test || true
	@cd apps/android-client && ./gradlew test || true
	@echo "✅ Tests completados"

test:api: ## Ejecutar tests del backend
	@cd apps/api && php artisan test

test:android: ## Ejecutar tests de Android
	@cd apps/android-client && ./gradlew test

test:prod: ## Ejecutar todos los tests usando Docker de producción
	@echo "🧪 Ejecutando tests con Docker de producción..."
	@cd infra/docker/environments/production && bash run-tests.sh all

test:prod:api: ## Ejecutar tests de API usando Docker de producción
	@echo "🧪 Ejecutando tests de API con Docker de producción..."
	@cd infra/docker/environments/production && bash run-tests.sh api

test:prod:dashboard: ## Ejecutar tests de Dashboard usando Docker de producción
	@echo "🧪 Ejecutando tests de Dashboard con Docker de producción..."
	@cd infra/docker/environments/production && bash run-tests.sh dashboard

test:prod:build: ## Construir imágenes de prueba de producción
	@echo "🔨 Construyendo imágenes de prueba de producción..."
	@cd infra/docker/environments/production && bash run-tests.sh build

test:prod:cleanup: ## Limpiar contenedores y volúmenes de pruebas de producción
	@echo "🧹 Limpiando contenedores de prueba de producción..."
	@cd infra/docker/environments/production && bash run-tests.sh cleanup

build: ## Build de todas las apps
	@echo "🔨 Building apps..."
	@cd apps/api && composer install --no-dev --optimize-autoloader
	@cd apps/web-dashboard && npm run build
	@echo "✅ Build completado"

build:api: ## Build del backend
	@cd apps/api && composer install --no-dev --optimize-autoloader

build:dashboard: ## Build del dashboard
	@cd apps/web-dashboard && npm run build

docker-up: ## Iniciar contenedores Docker
	@echo "🐳 Iniciando contenedores Docker..."
	@cd infra/docker && docker-compose up -d
	@echo "✅ Contenedores iniciados"
	@echo "API: http://localhost:8000"
	@echo "Dashboard: http://localhost:3000"

docker-down: ## Detener contenedores Docker
	@cd infra/docker && docker-compose down
	@echo "✅ Contenedores detenidos"

docker-logs: ## Ver logs de Docker
	@cd infra/docker && docker-compose logs -f

docker-shell: ## Acceder al shell del contenedor de la API
	@cd infra/docker && docker-compose exec app bash

migrate: ## Ejecutar migraciones
	@cd apps/api && php artisan migrate

migrate:fresh: ## Resetear base de datos y ejecutar migraciones
	@cd apps/api && php artisan migrate:fresh

lint: ## Ejecutar linters
	@echo "🔍 Ejecutando linters..."
	@cd apps/api && ./vendor/bin/pint --test || true
	@cd apps/android-client && ./gradlew ktlint || true
	@echo "✅ Linting completado"

lint:fix: ## Corregir problemas de linting
	@echo "🔧 Corrigiendo problemas de linting..."
	@cd apps/api && ./vendor/bin/pint
	@cd apps/android-client && ./gradlew ktlintFormat
	@echo "✅ Linting corregido"

clean: ## Limpiar builds y caches
	@echo "🧹 Limpiando..."
	@cd apps/api && rm -rf vendor bootstrap/cache/*.php storage/framework/cache/* storage/framework/sessions/* storage/framework/views/*
	@cd apps/web-dashboard && rm -rf node_modules dist
	@cd apps/android-client && ./gradlew clean
	@echo "✅ Limpieza completada"

