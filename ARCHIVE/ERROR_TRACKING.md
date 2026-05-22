# Error Tracking - Guía de Implementación

## 📊 Opciones de Error Tracking

### 1. Sentry (Recomendado)

**Plan Free:**
- ✅ 5,000 eventos/mes
- ✅ 1 proyecto
- ✅ 30 días de retención de datos
- ✅ Source maps
- ✅ Performance monitoring básico
- ✅ Integración con React

**Plan Developer ($26/mes):**
- 50,000 eventos/mes
- Proyectos ilimitados
- 90 días de retención
- Performance monitoring avanzado

**Ventajas:**
- ✅ Muy popular y bien documentado
- ✅ Excelente integración con React
- ✅ Source maps automáticos
- ✅ Stack traces detallados
- ✅ Filtrado y agrupación inteligente
- ✅ Alertas por email/Slack

**Desventajas:**
- ❌ Plan free limitado a 5K eventos/mes
- ❌ Puede ser costoso si tienes mucho tráfico

### 2. LogRocket

**Plan Free:**
- ❌ No tiene plan free permanente
- Solo trial de 14 días

**Plan Starter ($99/mes):**
- 1,000 sesiones/mes
- 30 días de retención
- Session replay completo

**Ventajas:**
- ✅ Session replay (ver exactamente qué hizo el usuario)
- ✅ Muy útil para debugging
- ✅ Captura console logs, network requests, Redux state

**Desventajas:**
- ❌ Más caro que Sentry
- ❌ No tiene plan free permanente

### 3. Rollbar

**Plan Free:**
- ✅ 5,000 eventos/mes
- ✅ 1 proyecto
- ✅ 30 días de retención

**Ventajas:**
- ✅ Similar a Sentry
- ✅ Buen soporte para JavaScript/React

**Desventajas:**
- ❌ Menos popular que Sentry
- ❌ Menos documentación

### 4. Bugsnag

**Plan Free:**
- ✅ 7,500 eventos/mes
- ✅ 1 proyecto
- ✅ 30 días de retención

**Ventajas:**
- ✅ Más eventos en plan free que Sentry
- ✅ Buen soporte para React

**Desventajas:**
- ❌ Menos popular que Sentry

### 5. Self-Hosted: GlitchTip

**Plan:**
- ✅ Completamente gratis (self-hosted)
- ✅ Open source
- ✅ Compatible con API de Sentry

**Ventajas:**
- ✅ Gratis completamente
- ✅ Control total de datos
- ✅ Sin límites

**Desventajas:**
- ❌ Requiere servidor propio
- ❌ Mantenimiento necesario
- ❌ Setup más complejo

## 🎯 Recomendación

**Para empezar:** **Sentry Free** (5K eventos/mes es suficiente para empezar)

**Si necesitas más:** **GlitchTip self-hosted** (gratis, sin límites)

**Si necesitas session replay:** **LogRocket** (pero es caro)

## 📦 Implementación de Sentry

### Paso 1: Crear Cuenta y Proyecto

1. Ir a https://sentry.io/signup/
2. Crear cuenta (gratis)
3. Crear nuevo proyecto → "React"
4. Copiar el DSN (Data Source Name)

### Paso 2: Instalar Dependencias

```bash
cd apps/web-dashboard
npm install @sentry/react
```

### Paso 3: Configurar Sentry

**Crear `src/services/sentry.ts`:**

```typescript
import * as Sentry from "@sentry/react";

export function initSentry() {
  const dsn = import.meta.env.VITE_SENTRY_DSN;
  
  if (!dsn) {
    // En desarrollo sin DSN, no inicializar
    if (import.meta.env.DEV) {
      console.warn("Sentry DSN no configurado, error tracking deshabilitado");
      return;
    }
    // En producción sin DSN, es un problema pero no crasheamos
    return;
  }

  Sentry.init({
    dsn,
    environment: import.meta.env.MODE,
    integrations: [
      new Sentry.BrowserTracing(),
      new Sentry.Replay({
        maskAllText: true,
        blockAllMedia: true,
      }),
    ],
    // Performance Monitoring
    tracesSampleRate: import.meta.env.MODE === "production" ? 0.1 : 1.0, // 10% en prod
    // Session Replay
    replaysSessionSampleRate: 0.1, // 10% de sesiones
    replaysOnErrorSampleRate: 1.0, // 100% de sesiones con errores
  });
}
```

**Actualizar `src/main.tsx`:**

```typescript
import { initSentry } from "./services/sentry";

// Inicializar Sentry ANTES de renderizar React
initSentry();

// ... resto del código
```

### Paso 4: Capturar Errores

**En `src/services/logger.ts` (ya actualizado):**

```typescript
private sendToErrorTracking(message: string, context?: LogContext) {
  if (window.Sentry) {
    window.Sentry.captureException(new Error(message), {
      extra: context,
    });
  }
}
```

**Capturar errores de React Query:**

```typescript
// En App.tsx
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      onError: (error) => {
        if (window.Sentry) {
          window.Sentry.captureException(error);
        }
      },
    },
  },
});
```

**Capturar errores de WebSocket:**

```typescript
// En echo.ts (ya implementado)
logger.error("Error de WebSocket", error, { state: "error" });
// Esto automáticamente envía a Sentry si está configurado
```

### Paso 5: Variables de Entorno

**Agregar a `.env.production`:**

```env
VITE_SENTRY_DSN=https://tu-dsn@sentry.io/proyecto-id
```

**O en Docker Compose:**

```yaml
dashboard:
  environment:
    - VITE_SENTRY_DSN=https://tu-dsn@sentry.io/proyecto-id
```

### Paso 6: Source Maps (Opcional pero Recomendado)

**Instalar plugin:**

```bash
npm install --save-dev @sentry/vite-plugin
```

**Actualizar `vite.config.ts`:**

```typescript
import { sentryVitePlugin } from "@sentry/vite-plugin";

export default defineConfig({
  plugins: [
    react(),
    sentryVitePlugin({
      org: "tu-org",
      project: "tu-proyecto",
      authToken: process.env.SENTRY_AUTH_TOKEN, // Token de Sentry
    }),
  ],
  build: {
    sourcemap: true, // Generar source maps
  },
});
```

## 🔧 Implementación de GlitchTip (Self-Hosted)

Si prefieres una solución completamente gratis:

### Paso 1: Instalar GlitchTip

```bash
# Usando Docker
docker run -d \
  --name glitchtip \
  -p 8000:8000 \
  -e SECRET_KEY=tu-secret-key \
  glitchtip/glitchtip
```

### Paso 2: Configurar

1. Acceder a http://localhost:8000
2. Crear cuenta y proyecto
3. Copiar DSN

### Paso 3: Usar en la App

**Es compatible con Sentry SDK:**

```typescript
import * as Sentry from "@sentry/react";

Sentry.init({
  dsn: "https://tu-dsn@glitchtip.tu-dominio.com/1",
  // ... resto igual que Sentry
});
```

## 📊 Comparación Rápida

| Solución | Plan Free | Eventos/mes | Self-Hosted | Session Replay |
|----------|-----------|-------------|-------------|----------------|
| **Sentry** | ✅ | 5,000 | ❌ | ✅ (paid) |
| **GlitchTip** | ✅ | Ilimitado | ✅ | ❌ |
| **LogRocket** | ❌ | Trial 14d | ❌ | ✅ |
| **Rollbar** | ✅ | 5,000 | ❌ | ❌ |
| **Bugsnag** | ✅ | 7,500 | ❌ | ❌ |

## 🎯 Recomendación Final

**Para tu proyecto:**

1. **Corto plazo:** Sentry Free (5K eventos es suficiente para empezar)
2. **Largo plazo:** GlitchTip self-hosted (gratis, sin límites, control total)

**Implementación recomendada:**

1. Implementar logger estructurado (✅ ya hecho)
2. Agregar Sentry cuando necesites (solo agregar DSN)
3. Migrar a GlitchTip cuando superes 5K eventos/mes

## 📝 Notas

- **No implementar ahora:** El código ya está preparado para Sentry
- **Solo falta:** Agregar DSN cuando decidas implementarlo
- **Logger actual:** Ya captura errores, solo falta enviarlos a Sentry

