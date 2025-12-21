# Testing - Web Dashboard

Guía rápida de testing para el dashboard web. Para documentación completa, ver `../../docs/04-development/TESTING.md`.

## 🚀 Inicio Rápido

```bash
# Instalar dependencias
npm install

# Ejecutar tests
npm run test

# Tests en modo watch (desarrollo)
npm run test:watch

# Tests con coverage
npm run test:coverage
```

## 📋 Scripts Disponibles

### Testing

```bash
npm run test          # Ejecutar tests una vez
npm run test:watch    # Modo watch (desarrollo)
npm run test:ui       # UI interactiva
npm run test:coverage # Con reporte de coverage
npm run test:ci       # Para CI/CD (con coverage)
```

### Linting

```bash
npm run lint          # Lint (max 50 warnings)
npm run lint:fix      # Lint y auto-fix
npm run lint:strict   # Lint estricto (0 warnings)
npm run type-check    # Verificar tipos TypeScript
npm run validate      # Validar todo (types + lint + test)
```

## 📊 Coverage

Umbrales mínimos: 70% (Lines, Functions, Branches, Statements)

```bash
npm run test:coverage
# Abre coverage/index.html en el navegador
```

## 🐳 Docker

```bash
# Build imagen de test
docker build -f Dockerfile.test -t yape-dashboard-test .

# Ejecutar tests
docker run --rm yape-dashboard-test

# O con docker-compose
docker-compose -f docker-compose.test.yml up --build
```

## 📁 Estructura

```
src/
├── test/
│   ├── setup.ts          # Setup global para tests
│   └── utils.tsx          # Utilidades de testing
├── components/
│   └── ComponentName/
│       └── ComponentName.test.tsx
└── hooks/
    └── useHookName.test.ts
```

## ✅ Estado Actual

- ✅ **13 tests pasando** (3 archivos)
- ✅ **0 errores de linting** (46 warnings aceptables)
- ✅ **Coverage configurado** (70% mínimo)
- ✅ **CI/CD configurado** (GitHub Actions)

## 📚 Documentación Completa

- **Guía general de testing**: Ver `../../docs/04-development/TESTING.md`
- **Configuración detallada**: Ver `TESTING_SETUP.md` (en este directorio)
- **Resumen de configuración**: Ver `RESUMEN_CONFIGURACION.md` (en este directorio)

## 🔧 Troubleshooting

Ver `../../docs/04-development/TESTING.md` para troubleshooting completo.
