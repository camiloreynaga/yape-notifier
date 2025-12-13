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

up: ## Iniciar los contenedores (staging)
	docker-compose -f docker-compose.staging.yml up -d

down: ## Detener los contenedores
	docker-compose -f docker-compose.staging.yml down

restart: ## Reiniciar los contenedores
	docker-compose -f docker-compose.staging.yml restart

logs: ## Ver logs de los contenedores
	docker-compose -f docker-compose.staging.yml logs -f

shell: ## Acceder al shell del contenedor de la app
	docker-compose -f docker-compose.staging.yml exec php-fpm sh

artisan: ## Ejecutar comando artisan (uso: make artisan CMD="migrate")
	docker-compose -f docker-compose.staging.yml exec php-fpm php artisan $(CMD)

migrate: ## Ejecutar migraciones
	docker-compose -f docker-compose.staging.yml exec php-fpm php artisan migrate

fresh: ## Ejecutar migraciones desde cero (elimina datos)
	docker-compose -f docker-compose.staging.yml exec php-fpm php artisan migrate:fresh

seed: ## Ejecutar seeders
	docker-compose -f docker-compose.staging.yml exec php-fpm php artisan db:seed

test: ## Ejecutar tests
	docker-compose -f docker-compose.staging.yml exec php-fpm php artisan test

clean: ## Limpiar contenedores y volúmenes (staging)
	docker-compose -f docker-compose.staging.yml down -v
	docker system prune -f

build: ## Construir las imágenes (staging)
	docker-compose -f docker-compose.staging.yml build

ps: ## Ver estado de los contenedores
	docker-compose -f docker-compose.staging.yml ps

dashboard-logs: ## Ver logs del dashboard
	docker-compose -f docker-compose.staging.yml logs -f dashboard

dashboard-shell: ## Acceder al shell del contenedor del dashboard
	docker-compose -f docker-compose.staging.yml exec dashboard sh

dashboard-build: ## Reconstruir el dashboard
	docker-compose -f docker-compose.staging.yml build dashboard

# ============================================
# Comandos para PRODUCCIÓN
# ============================================

prod-build: ## Construir las imágenes de producción
	docker-compose -f docker-compose.yml build --no-cache

prod-up: ## Iniciar los contenedores de producción
	docker-compose -f docker-compose.yml up -d

prod-down: ## Detener los contenedores de producción
	docker-compose -f docker-compose.yml down

prod-restart: ## Reiniciar los contenedores de producción
	docker-compose -f docker-compose.yml restart

prod-logs: ## Ver logs de producción
	docker-compose -f docker-compose.yml logs -f

prod-shell: ## Acceder al shell del contenedor de producción
	docker-compose -f docker-compose.yml exec php-fpm sh

prod-migrate: ## Ejecutar migraciones en producción
	docker-compose -f docker-compose.yml exec php-fpm php artisan migrate --force

prod-optimize: ## Optimizar Laravel en producción
	docker-compose -f docker-compose.yml exec php-fpm php artisan config:cache
	docker-compose -f docker-compose.yml exec php-fpm php artisan route:cache
	docker-compose -f docker-compose.yml exec php-fpm php artisan view:cache

prod-deploy: ## Desplegar en producción (build + up + migrate + optimize)
	./deploy.sh

prod-ps: ## Ver estado de los contenedores de producción
	docker-compose -f docker-compose.yml ps

# ============================================
# Comandos para PRUEBAS
# ============================================

test-up: ## Iniciar contenedores para pruebas
	docker-compose -f docker-compose.test.yml up -d

test-down: ## Detener contenedores de pruebas
	docker-compose -f docker-compose.test.yml down

test-run: ## Ejecutar tests
	docker-compose -f docker-compose.test.yml exec php-fpm php artisan test

test-logs: ## Ver logs de pruebas
	docker-compose -f docker-compose.test.yml logs -f

