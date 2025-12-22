# Guía de Testing

> Esta guía cubre testing para todos los componentes del proyecto. Para detalles específicos del dashboard web, ver `apps/web-dashboard/README.md`.

## 📋 Resumen

El proyecto utiliza diferentes frameworks de testing según el componente:

- **Web Dashboard**: Vitest + React Testing Library
- **API (Laravel)**: PHPUnit + Pest
- **Android App**: JUnit + Espresso

---

## 🎨 Web Dashboard (React + TypeScript)

### Stack Tecnológico

- **Vitest** - Framework de testing
- **React Testing Library** - Testing utilities para React
- **jsdom** - DOM environment para tests
- **@testing-library/jest-dom** - Matchers adicionales

### Scripts Disponibles

```bash
cd apps/web-dashboard

# Ejecutar tests una vez
npm run test

# Ejecutar tests en modo watch (desarrollo)
npm run test:watch

# Ejecutar tests con UI interactiva
npm run test:ui

# Ejecutar tests con coverage
npm run test:coverage

# Ejecutar tests en CI/CD
npm run test:ci
```

### Estructura de Tests

```
src/
  components/
    ComponentName/
      ComponentName.tsx
      ComponentName.test.tsx  # Tests del componente
  hooks/
    useHookName.ts
    useHookName.test.ts      # Tests del hook
```

### Ejemplo: Test de Componente

```tsx
import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/utils';
import MyComponent from './MyComponent';

describe('MyComponent', () => {
  it('renders correctly', () => {
    render(<MyComponent title="Test" />);
    expect(screen.getByText('Test')).toBeInTheDocument();
  });
});
```

### Coverage

Umbrales mínimos:
- **Lines**: 70%
- **Functions**: 70%
- **Branches**: 70%
- **Statements**: 70%

### Testing en Docker

```bash
cd apps/web-dashboard

# Build imagen de testing
docker build -f Dockerfile.test -t yape-dashboard-test .

# Ejecutar tests
docker run --rm yape-dashboard-test

# O con docker-compose
docker-compose -f docker-compose.test.yml up --build
```

### CI/CD

El proyecto incluye un workflow de GitHub Actions (`.github/workflows/ci.yml`) que:
1. Ejecuta type-check
2. Ejecuta linter
3. Ejecuta tests con coverage
4. Sube reportes de coverage a Codecov

---

## 🔧 API (Laravel)

### Stack Tecnológico

- **PHPUnit** / **Pest** - Framework de testing
- **Laravel Testing** - Utilidades de Laravel

### Scripts Disponibles

```bash
cd apps/api

# Todos los tests
php artisan test

# Tests unitarios
php artisan test --testsuite=Unit

# Tests de integración
php artisan test --testsuite=Feature

# Con cobertura
php artisan test --coverage
```

### Estructura de Tests

```
tests/
├── Feature/          # Tests de integración (API, endpoints)
│   ├── AuthTest.php
│   ├── DeviceTest.php
│   └── NotificationTest.php
└── Unit/             # Tests unitarios (servicios, modelos)
    └── NotificationServiceTest.php
```

### Ejemplo: Test de Feature

```php
<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class MyFeatureTest extends TestCase
{
    use RefreshDatabase;

    public function test_user_can_do_something(): void
    {
        $user = User::factory()->create();
        $token = $user->createToken('test-token')->plainTextToken;

        $response = $this->withHeader('Authorization', "Bearer $token")
            ->getJson('/api/endpoint');

        $response->assertStatus(200);
    }
}
```

---

## 📱 Android App

### Stack Tecnológico

- **JUnit** - Framework de testing
- **Espresso** - Testing de UI
- **Mockito** - Mocking

### Scripts Disponibles

```bash
cd apps/android-client

# Tests unitarios
./gradlew test

# Tests de instrumentación
./gradlew connectedAndroidTest

# Todos los tests
./gradlew check
```

### Estructura de Tests

```
app/src/
├── test/              # Tests unitarios
│   └── java/.../
└── androidTest/       # Tests de instrumentación
    └── java/.../
```

---

## 🎯 Buenas Prácticas

### 1. Nombres Descriptivos

Usa nombres claros para tests:

```tsx
// ✅ Bueno
it('should render title when provided', () => { ... });

// ❌ Malo
it('test 1', () => { ... });
```

### 2. AAA Pattern (Arrange, Act, Assert)

```tsx
it('should do something', () => {
  // Arrange
  const props = { title: 'Test' };
  
  // Act
  render(<Component {...props} />);
  
  // Assert
  expect(screen.getByText('Test')).toBeInTheDocument();
});
```

### 3. Tests Aislados

Cada test debe ser independiente y poder ejecutarse de forma aislada.

### 4. Mocking

Usa mocks para dependencias externas (API, servicios):

```tsx
vi.mock('@/services/api', () => ({
  fetchData: vi.fn(() => Promise.resolve({ data: 'test' }))
}));
```

### 5. Accesibilidad

Usa queries accesibles (`getByRole`, `getByLabelText`) en lugar de `getByTestId` cuando sea posible.

---

## 🐳 Testing con Docker en Producción

### Descripción

Configuración para ejecutar pruebas usando las imágenes de producción de Docker. Permite validar que las imágenes de producción funcionan correctamente antes del despliegue.

### Ubicación

Configuración en: `infra/docker/environments/production/`

### Requisitos

- Docker y Docker Compose instalados
- Archivo `.env` configurado en `infra/docker/environments/production/`

### Uso Rápido

#### Linux/macOS

```bash
cd infra/docker/environments/production

# Construir imágenes de prueba
make test:prod:build

# Ejecutar todos los tests
make test:prod

# Ejecutar solo tests de API
make test:prod:api

# Ejecutar solo tests de Dashboard
make test:prod:dashboard

# Limpiar contenedores y volúmenes
make test:prod:cleanup
```

#### Windows (PowerShell)

```powershell
cd infra/docker/environments/production

# Construir imágenes de prueba
.\run-tests.ps1 build

# Ejecutar todos los tests
.\run-tests.ps1 all

# Ejecutar solo tests de API
.\run-tests.ps1 api

# Ejecutar solo tests de Dashboard
.\run-tests.ps1 dashboard

# Limpiar contenedores y volúmenes
.\run-tests.ps1 cleanup
```

### Comandos Docker Compose Directos

```bash
cd infra/docker/environments/production

# Construir imágenes
docker compose -f docker-compose.test.yml --env-file .env build

# Ejecutar tests de API
docker compose -f docker-compose.test.yml --env-file .env run --rm api-test php artisan test

# Ejecutar tests de Dashboard
docker compose -f docker-compose.test.yml --env-file .env run --rm dashboard-test npm run test:ci

# Ejecutar tests con filtros
docker compose -f docker-compose.test.yml --env-file .env run --rm api-test php artisan test --filter=Unit

# Limpiar todo
docker compose -f docker-compose.test.yml --env-file .env down -v
```

### Servicios de Prueba

#### API Tests (`api-test`)
- Basado en la imagen de producción pero con dependencias de desarrollo
- Ejecuta PHPUnit/Pest tests
- Requiere base de datos PostgreSQL (`db-test`)
- Genera reportes de cobertura en `/var/www/coverage`

#### Dashboard Tests (`dashboard-test`)
- Basado en Node.js Alpine
- Ejecuta Vitest tests
- Genera reportes de cobertura en `/app/coverage`

#### Base de Datos (`db-test`)
- PostgreSQL 15 Alpine
- Base de datos separada para pruebas (`yape_notifier_test`)
- Se limpia automáticamente antes de cada ejecución

### Cobertura de Código

Los reportes de cobertura se generan en volúmenes Docker:

- **API**: `api_test_coverage_prod` → `/var/www/coverage/html`
- **Dashboard**: `dashboard_test_coverage_prod` → `/app/coverage`

**Acceder a los reportes:**

```bash
# Copiar reporte de cobertura de API a local
docker run --rm -v yape-notifier_api_test_coverage_prod:/data -v $(pwd):/output alpine cp -r /data/html /output/api-coverage

# Copiar reporte de cobertura de Dashboard a local
docker run --rm -v yape-notifier_dashboard_test_coverage_prod:/data -v $(pwd):/output alpine cp -r /data /output/dashboard-coverage
```

### Opciones Avanzadas

#### Ejecutar tests específicos

```bash
# Solo tests unitarios
docker compose -f docker-compose.test.yml --env-file .env run --rm api-test php artisan test --filter=Unit

# Solo tests de feature
docker compose -f docker-compose.test.yml --env-file .env run --rm api-test php artisan test --filter=Feature

# Test específico
docker compose -f docker-compose.test.yml --env-file .env run --rm api-test php artisan test tests/Unit/PaymentNotificationValidatorTest.php
```

#### Modo interactivo

```bash
# Acceder al shell del contenedor de API
docker compose -f docker-compose.test.yml --env-file .env run --rm api-test sh

# Acceder al shell del contenedor de Dashboard
docker compose -f docker-compose.test.yml --env-file .env run --rm dashboard-test sh
```

### Variables de Entorno

Las pruebas usan las siguientes variables de entorno (configuradas en `.env`):

- `DB_DATABASE`: Base de datos para pruebas (default: `yape_notifier_test`)
- `DB_USERNAME`: Usuario de PostgreSQL (default: `postgres`)
- `DB_PASSWORD`: Contraseña de PostgreSQL
- `APP_ENV`: Entorno de la aplicación (forzado a `testing`)

### Troubleshooting

**Error: "Database connection failed"**
- Verifica que el servicio `db-test` esté corriendo y saludable
- Verifica las credenciales en `.env`
- Espera unos segundos después de iniciar `db-test` para que esté listo

**Error: "Container already exists"**
```bash
docker compose -f docker-compose.test.yml --env-file .env down
```

**Limpiar todo y empezar de nuevo**
```bash
docker compose -f docker-compose.test.yml --env-file .env down -v --remove-orphans
docker image prune -f
```

### Notas Importantes

- Las pruebas usan una base de datos separada para no afectar datos de producción
- Los volúmenes de cobertura persisten entre ejecuciones
- Las imágenes se construyen basándose en los Dockerfiles de producción
- Los tests se ejecutan en modo `testing` con `APP_DEBUG=true` para mejor debugging

---

## 🔧 Troubleshooting

### Web Dashboard

**Error: Cannot find module '@testing-library/jest-dom'**
```bash
npm install -D @testing-library/jest-dom
```

**Error: jsdom not found**
```bash
npm install -D jsdom
```

**Tests muy lentos**
- Usa `test.only()` para ejecutar un test específico
- Verifica que los mocks estén configurados correctamente
- Considera usar `vi.useFakeTimers()` para tests con timers

### API (Laravel)

**Error: "Class 'App\Models\User' not found"**
```bash
composer dump-autoload
```

**Error: Database connection failed**
- Verifica que la base de datos de testing esté configurada
- Usa `RefreshDatabase` trait en tests de Feature

---

## 📚 Recursos

### Web Dashboard
- [Vitest Documentation](https://vitest.dev/)
- [React Testing Library](https://testing-library.com/react)
- [Testing Library Queries](https://testing-library.com/docs/queries/about/)

### API (Laravel)
- [Laravel Testing](https://laravel.com/docs/testing)
- [PHPUnit Documentation](https://phpunit.de/)
- [Pest Documentation](https://pestphp.com/)

### Android
- [Android Testing Guide](https://developer.android.com/training/testing)
- [JUnit Documentation](https://junit.org/junit5/)
- [Espresso Documentation](https://developer.android.com/training/testing/espresso)

---

## Referencias

- **Web Dashboard**: Ver `apps/web-dashboard/README.md` para detalles específicos
- **API**: Ver `apps/api/README.md` para detalles específicos
- **Android**: Ver `apps/android-client/README.md` para detalles específicos
- **Workflow**: Ver `docs/04-development/WORKFLOW.md` para flujo de desarrollo
- **Docker**: Ver `docs/02-deployment/DOCKER.md` para infraestructura Docker
- **Testing en Producción**: Ver `infra/docker/environments/production/README_TESTING.md` para detalles específicos de testing con Docker

