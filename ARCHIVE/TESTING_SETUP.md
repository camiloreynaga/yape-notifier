# Configuración de Testing y Linting

## ✅ Configuración Completada

### Testing (Vitest)

- ✅ **Vitest** configurado con React Testing Library
- ✅ **Coverage** configurado con umbrales mínimos (70%)
- ✅ **Setup file** con mocks para window.matchMedia, IntersectionObserver, ResizeObserver
- ✅ **Test utilities** con providers (Router, AuthContext)
- ✅ **Tests de ejemplo** para componentes y hooks

### Linting (ESLint)

- ✅ **ESLint** mejorado con reglas adicionales
- ✅ **React plugin** para mejores prácticas de React
- ✅ **JSX A11y** para accesibilidad
- ✅ **TypeScript** con reglas estrictas
- ✅ **Overrides** para archivos de test

### Docker

- ✅ **Dockerfile.test** para ejecutar tests en contenedores
- ✅ **docker-compose.test.yml** para facilitar ejecución
- ✅ **.dockerignore** optimizado

### CI/CD

- ✅ **GitHub Actions** workflow configurado
- ✅ **Codecov** integration preparada

## 📦 Dependencias Instaladas

### Testing
- `vitest` - Framework de testing
- `@vitest/ui` - UI interactiva para tests
- `@vitest/coverage-v8` - Coverage reports
- `@testing-library/react` - Testing utilities para React
- `@testing-library/jest-dom` - Matchers adicionales
- `@testing-library/user-event` - Simulación de eventos de usuario
- `jsdom` - DOM environment para tests

### Linting
- `eslint-plugin-react` - Reglas para React
- `eslint-plugin-jsx-a11y` - Reglas de accesibilidad

## 🚀 Uso

### Ejecutar Tests

```bash
# Tests una vez
npm run test

# Tests en modo watch
npm run test:watch

# Tests con UI
npm run test:ui

# Tests con coverage
npm run test:coverage

# Tests en CI (con coverage)
npm run test:ci
```

### Ejecutar Linting

```bash
# Lint
npm run lint

# Lint y auto-fix
npm run lint:fix

# Type check
npm run type-check

# Validar todo
npm run validate
```

### Docker

```bash
# Build imagen de test
docker build -f Dockerfile.test -t yape-dashboard-test .

# Ejecutar tests
docker run --rm yape-dashboard-test

# O con docker-compose
docker-compose -f docker-compose.test.yml up --build
```

## 📁 Estructura de Archivos

```
apps/web-dashboard/
├── vitest.config.ts          # Configuración de Vitest
├── Dockerfile.test           # Dockerfile para tests
├── docker-compose.test.yml   # Docker Compose para tests
├── .github/workflows/
│   └── ci.yml                # GitHub Actions CI
├── src/
│   ├── test/
│   │   ├── setup.ts          # Setup global para tests
│   │   └── utils.tsx          # Utilidades de testing
│   ├── components/
│   │   └── ComponentName/
│   │       └── ComponentName.test.tsx
│   └── hooks/
│       └── useHookName.test.ts
└── coverage/                 # Reportes de coverage (generado)
```

## 🎯 Cobertura Mínima

- **Lines**: 70%
- **Functions**: 70%
- **Branches**: 70%
- **Statements**: 70%

## 📝 Próximos Pasos

1. **Agregar más tests**:
   - Tests para componentes principales
   - Tests para hooks personalizados
   - Tests de integración

2. **Pre-commit hooks** (opcional):
   ```bash
   npm install -D husky lint-staged
   ```

3. **Mejorar coverage**:
   - Agregar tests para casos edge
   - Tests de error handling
   - Tests de accesibilidad

## 🔧 Troubleshooting

### Error: Cannot find module '@testing-library/jest-dom'

```bash
npm install -D @testing-library/jest-dom
```

### Error: jsdom not found

```bash
npm install -D jsdom
```

### Tests muy lentos

- Usa `test.only()` para ejecutar un test específico
- Verifica que los mocks estén configurados correctamente
- Considera usar `vi.useFakeTimers()` para tests con timers

## 📚 Recursos

- [Vitest Documentation](https://vitest.dev/)
- [React Testing Library](https://testing-library.com/react)
- [ESLint Rules](https://eslint.org/docs/rules/)
- [Testing Best Practices](https://kentcdodds.com/blog/common-mistakes-with-react-testing-library)

