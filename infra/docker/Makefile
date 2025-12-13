.PHONY: help setup up down restart logs shell artisan migrate fresh seed test clean prod-build prod-up prod-down prod-deploy

help: ## Mostrar esta ayuda
	@echo "Comandos disponibles para DESARROLLO:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | grep -v '^prod-' | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'
	@echo ""
	@echo "Comandos disponibles para PRODUCCIÓN:"
	@grep -E '^prod-[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sed 's/prod-//' | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

setup: ## Configurar el entorno por primera vez
	@chmod +x setup.sh
	@./setup.sh

up: ## Iniciar los contenedores
	docker-compose up -d

down: ## Detener los contenedores
	docker-compose down

restart: ## Reiniciar los contenedores
	docker-compose restart

logs: ## Ver logs de los contenedores
	docker-compose logs -f

shell: ## Acceder al shell del contenedor de la app
	docker-compose exec app bash

artisan: ## Ejecutar comando artisan (uso: make artisan CMD="migrate")
	docker-compose exec app php artisan $(CMD)

migrate: ## Ejecutar migraciones
	docker-compose exec app php artisan migrate

fresh: ## Ejecutar migraciones desde cero (elimina datos)
	docker-compose exec app php artisan migrate:fresh

seed: ## Ejecutar seeders
	docker-compose exec app php artisan db:seed

test: ## Ejecutar tests
	docker-compose exec app php artisan test

clean: ## Limpiar contenedores y volúmenes
	docker-compose down -v
	docker system prune -f

build: ## Construir las imágenes
	docker-compose build

ps: ## Ver estado de los contenedores
	docker-compose ps

up-dev: ## Iniciar contenedores en modo desarrollo (incluye dashboard-dev)
	docker-compose --profile dev up -d

dashboard-logs: ## Ver logs del dashboard
	docker-compose logs -f dashboard

dashboard-dev-logs: ## Ver logs del dashboard en desarrollo
	docker-compose logs -f dashboard-dev

dashboard-shell: ## Acceder al shell del contenedor del dashboard
	docker-compose exec dashboard sh

dashboard-build: ## Reconstruir el dashboard
	docker-compose build dashboard

# ============================================
# Comandos para PRODUCCIÓN
# ============================================

prod-build: ## Construir las imágenes de producción
	docker-compose -f docker-compose.prod.yml build --no-cache

prod-up: ## Iniciar los contenedores de producción
	docker-compose -f docker-compose.prod.yml up -d

prod-down: ## Detener los contenedores de producción
	docker-compose -f docker-compose.prod.yml down

prod-restart: ## Reiniciar los contenedores de producción
	docker-compose -f docker-compose.prod.yml restart

prod-logs: ## Ver logs de producción
	docker-compose -f docker-compose.prod.yml logs -f

prod-shell: ## Acceder al shell del contenedor de producción
	docker-compose -f docker-compose.prod.yml exec app bash

prod-migrate: ## Ejecutar migraciones en producción
	docker-compose -f docker-compose.prod.yml exec app php artisan migrate --force

prod-optimize: ## Optimizar Laravel en producción
	docker-compose -f docker-compose.prod.yml exec app php artisan config:cache
	docker-compose -f docker-compose.prod.yml exec app php artisan route:cache
	docker-compose -f docker-compose.prod.yml exec app php artisan view:cache

prod-deploy: ## Desplegar en producción (build + up + migrate + optimize)
	./deploy-production.sh

prod-ps: ## Ver estado de los contenedores de producción
	docker-compose -f docker-compose.prod.yml ps

# ============================================
# Comandos para PRUEBAS
# ============================================

test-up: ## Iniciar contenedores para pruebas
	docker-compose -f docker-compose.test.yml up -d

test-down: ## Detener contenedores de pruebas
	docker-compose -f docker-compose.test.yml down

test-run: ## Ejecutar tests
	docker-compose -f docker-compose.test.yml exec app php artisan test

test-logs: ## Ver logs de pruebas
	docker-compose -f docker-compose.test.yml logs -f

