.PHONY: help setup up down restart logs shell artisan migrate fresh seed test clean

help: ## Mostrar esta ayuda
	@echo "Comandos disponibles:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

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

