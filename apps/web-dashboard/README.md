# Yape Notifier Web Dashboard

Dashboard web React + TypeScript para administrar notificaciones, dispositivos e instancias de apps duales.

## 📋 Stack Tecnológico

- **React 18**
- **TypeScript**
- **Vite**
- **Tailwind CSS**

## 🏗️ Estructura

```
src/
├── components/         # Componentes reutilizables
├── pages/              # Páginas principales
├── contexts/           # Contextos React (Auth)
├── hooks/              # Custom hooks
├── services/           # Cliente API
└── types/              # Tipos TypeScript
```

## 🚀 Comandos Básicos

### Desarrollo

```bash
# Instalar dependencias
npm install

# Servidor de desarrollo
npm run dev
```

El dashboard estará disponible en `http://localhost:3000`

### Build

```bash
# Build de producción
npm run build
```

Los archivos se generan en `dist/`

### Linting

```bash
npm run lint
```

## 🔧 Configuración

### URL de la API

Editar `src/config/api.ts`:

```typescript
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000';
```

O variable de entorno:

```bash
VITE_API_BASE_URL=https://api.notificaciones.space npm run dev
```

## 📱 Páginas Principales

- **Dashboard Overview**: KPIs, gráficos, resumen
- **Notificaciones**: Feed con filtros avanzados
- **Dispositivos**: Lista con salud y estado
- **App Instances**: Gestión de instancias duales
- **Crear Comercio**: Formulario de creación

## 🔌 Integración con API

El dashboard consume endpoints de:
- Autenticación (`/api/login`, `/api/register`)
- Notificaciones (`/api/notifications`)
- Dispositivos (`/api/devices`)
- App Instances (`/api/app-instances`)
- Commerce (`/api/commerces`)

## 📚 Documentación

- **Testing**: Ver `README_TESTING.md` (guía rápida) o `../../docs/04-development/TESTING.md` (completa)
- **Producción**: Ver `PRODUCTION_CHECKLIST.md`
- **Deployment**: Ver `../../docs/02-deployment/DEPLOYMENT.md`
- **Arquitectura**: Ver `../../docs/03-architecture/`
- **Estado de implementación**: Ver `../../docs/07-reference/IMPLEMENTATION_STATUS.md`

## 🐛 Solución de Problemas

### Error: "Network Error" o "CORS Error"
1. Verificar que la API esté corriendo
2. Verificar `VITE_API_BASE_URL`
3. Verificar CORS en Laravel

### Error: "Unauthorized"
1. Verificar token en localStorage
2. Hacer logout y login nuevamente
