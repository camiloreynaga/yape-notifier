.PHONY: help test test-unit test-feature lint lint-fix coverage

help: ## Mostrar esta ayuda
	@echo "Comandos disponibles:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

test: ## Ejecutar todos los tests
	php artisan test

test-unit: ## Ejecutar solo tests unitarios
	php artisan test --testsuite=Unit

test-feature: ## Ejecutar solo tests de integración
	php artisan test --testsuite=Feature

lint: ## Verificar estilo de código sin modificar
	./vendor/bin/pint --test

lint-fix: ## Corregir estilo de código automáticamente
	./vendor/bin/pint

coverage: ## Generar reporte de cobertura de código
	php artisan test --coverage --min=80


