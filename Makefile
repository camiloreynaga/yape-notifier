.PHONY: help dev test build docker-up docker-down clean install

help: ## Mostrar esta ayuda
	@echo "Comandos disponibles:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

install: ## Instalar dependencias de todas las apps
	@echo "📦 Instalando dependencias..."
	@cd apps/api && composer install
	@cd apps/web-dashboard && npm install
	@echo "✅ Dependencias instaladas"

dev: ## Iniciar entorno de desarrollo
	@echo "🚀 Iniciando entorno de desarrollo..."
	@echo "Backend: http://localhost:8000"
	@echo "Dashboard: http://localhost:3000"
	@cd apps/api && php artisan serve &
	@cd apps/web-dashboard && npm run dev

dev:api: ## Iniciar solo el backend
	@echo "🚀 Iniciando backend..."
	@cd apps/api && php artisan serve

dev:dashboard: ## Iniciar solo el dashboard
	@echo "🚀 Iniciando dashboard..."
	@cd apps/web-dashboard && npm run dev

test: ## Ejecutar todos los tests
	@echo "🧪 Ejecutando tests..."
	@cd apps/api && php artisan test || true
	@cd apps/android-client && ./gradlew test || true
	@echo "✅ Tests completados"

test:api: ## Ejecutar tests del backend
	@cd apps/api && php artisan test

test:android: ## Ejecutar tests de Android
	@cd apps/android-client && ./gradlew test

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

