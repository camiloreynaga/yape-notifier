# 🔐 Explicación: REVERB_SCHEME vs REVERB_SCHEME_PUBLIC

## 📋 Resumen

**Backend (Laravel API):**
- `REVERB_SCHEME=http` ✅ **CORRECTO**

**Dashboard (Frontend):**
- `VITE_REVERB_SCHEME=https` ✅ **CORRECTO**

**¿Por qué son diferentes?** Porque el backend y el frontend se comunican con Reverb desde contextos diferentes.

---

## 🏗️ Arquitectura de Comunicación

```
┌─────────────────────────────────────────────────────────────┐
│                    Navegador (Cliente)                       │
│  Dashboard Web (https://dashboard.notificaciones.space)    │
└───────────────────────┬─────────────────────────────────────┘
                        │ HTTPS (wss://)
                        │ VITE_REVERB_SCHEME=https
                        │ VITE_REVERB_HOST=api.notificaciones.space
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                    Caddy (Reverse Proxy)                      │
│  - Recibe conexiones HTTPS desde el navegador                │
│  - Hace upgrade a WebSocket (wss://)                          │
│  - Proxy a Reverb en HTTP interno                            │
└───────────────────────┬─────────────────────────────────────┘
                        │ HTTP (ws://)
                        │ REVERB_SCHEME=http
                        │ REVERB_HOST=0.0.0.0
                        ▼
┌─────────────────────────────────────────────────────────────┐
│              Reverb Container (Puerto 8080)                  │
│  - Escucha en 0.0.0.0:8080 (HTTP interno)                   │
│  - REVERB_SCHEME=http (comunicación interna)                 │
└───────────────────────┬─────────────────────────────────────┘
                        │ HTTP (interno)
                        ▼
┌─────────────────────────────────────────────────────────────┐
│              Laravel API (PHP-FPM)                            │
│  - Broadcasting events                                       │
│  - Autenticación de canales                                  │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔍 Explicación Detallada

### Backend: `REVERB_SCHEME=http`

**Ubicación:** `.env` del backend (Laravel API)

```env
REVERB_SCHEME=http
REVERB_HOST=0.0.0.0
REVERB_PORT=8080
```

**Razón:**
- Reverb corre **dentro de Docker** en la red interna
- Se comunica con Laravel API en **HTTP** (comunicación interna)
- Caddy hace el proxy de HTTPS externo → HTTP interno
- `0.0.0.0` permite que otros contenedores se conecten

**Ejemplo de uso:**
```php
// Laravel envía eventos a Reverb
broadcast(new PaymentNotification($notification))
    ->toOthers();

// Laravel se conecta a Reverb usando:
// http://0.0.0.0:8080 (interno)
```

---

### Frontend: `VITE_REVERB_SCHEME=https`

**Ubicación:** Variables de entorno para build del Dashboard

```env
VITE_REVERB_SCHEME=https
VITE_REVERB_HOST=api.notificaciones.space
VITE_REVERB_PORT=8080
```

**Razón:**
- El navegador se conecta **desde Internet** (no desde Docker)
- Debe usar **HTTPS** (wss://) porque la página está en HTTPS
- El navegador se conecta a `wss://api.notificaciones.space:8080/app/{key}`
- Caddy intercepta esta conexión y hace proxy a Reverb interno

**Ejemplo de uso:**
```typescript
// Dashboard se conecta desde el navegador
Echo.connector.pusher = new Pusher(env.REVERB_APP_KEY, {
  wsHost: env.REVERB_HOST,        // api.notificaciones.space
  wsPort: env.REVERB_PORT,        // 8080
  wssPort: env.REVERB_PORT,       // 8080
  forceTLS: env.REVERB_SCHEME === "https",  // true
});

// El navegador intenta conectarse a:
// wss://api.notificaciones.space:8080/app/{key}
```

---

## ⚠️ Errores Comunes

### ❌ Error 1: Usar `REVERB_SCHEME=https` en el backend

```env
# ❌ INCORRECTO
REVERB_SCHEME=https
REVERB_HOST=0.0.0.0
```

**Problema:** Reverb intentará usar HTTPS internamente, pero no hay certificado SSL en el contenedor.

**Solución:** Usar `REVERB_SCHEME=http` (comunicación interna).

---

### ❌ Error 2: Usar `VITE_REVERB_SCHEME=http` en el dashboard

```env
# ❌ INCORRECTO
VITE_REVERB_SCHEME=http
VITE_REVERB_HOST=api.notificaciones.space
```

**Problema:** El navegador intentará conectarse con `ws://` (no seguro), pero la página está en HTTPS, causando errores de Mixed Content.

**Solución:** Usar `VITE_REVERB_SCHEME=https` (comunicación externa).

---

### ❌ Error 3: Usar `localhost` o `0.0.0.0` en el dashboard

```env
# ❌ INCORRECTO
VITE_REVERB_HOST=localhost
# o
VITE_REVERB_HOST=0.0.0.0
```

**Problema:** El navegador no puede conectarse a `localhost` o `0.0.0.0` desde Internet.

**Solución:** Usar el dominio público `api.notificaciones.space`.

---

## ✅ Configuración Correcta

### Backend (.env)

```env
# ============================================
# Broadcasting (Reverb) - WebSocket Server
# ============================================
BROADCAST_CONNECTION=reverb

REVERB_APP_ID=yape-notifier
REVERB_APP_KEY=base64:7IFJN3FFdqSdv3nAdxoRmjMxzI5jqDIG33VJ7XC4LOk
REVERB_APP_SECRET=1771fded8db62696cfa7a92461511e22
REVERB_HOST=0.0.0.0          # ✅ Interno en Docker
REVERB_PORT=8080
REVERB_SCHEME=http          # ✅ HTTP interno
```

### Dashboard (Variables de Build)

```env
# ============================================
# Dashboard - Variables de Entorno para Build
# ============================================
DASHBOARD_API_URL=https://api.notificaciones.space

# Variables de Reverb para WebSocket
REVERB_APP_KEY=base64:7IFJN3FFdqSdv3nAdxoRmjMxzI5jqDIG33VJ7XC4LOk
REVERB_HOST_PUBLIC=api.notificaciones.space  # ✅ Dominio público
REVERB_PORT=8080
REVERB_SCHEME_PUBLIC=https  # ✅ HTTPS externo
```

**Nota:** En el código del dashboard, estas variables se mapean a:
- `VITE_REVERB_HOST=${REVERB_HOST_PUBLIC}`
- `VITE_REVERB_SCHEME=${REVERB_SCHEME_PUBLIC}`

---

## 🔄 Flujo de Conexión Completo

1. **Navegador → Caddy:**
   ```
   wss://api.notificaciones.space:8080/app/{key}
   (HTTPS desde Internet)
   ```

2. **Caddy → Reverb:**
   ```
   ws://reverb:8080/app/{key}
   (HTTP interno en Docker)
   ```

3. **Reverb → Laravel:**
   ```
   http://php-fpm:9000
   (HTTP interno para broadcasting)
   ```

---

## 📝 Resumen de Variables

| Variable | Backend | Dashboard | Valor | Razón |
|----------|---------|-----------|-------|-------|
| `REVERB_SCHEME` | ✅ | ❌ | `http` | Comunicación interna en Docker |
| `VITE_REVERB_SCHEME` | ❌ | ✅ | `https` | Comunicación externa desde navegador |
| `REVERB_HOST` | ✅ | ❌ | `0.0.0.0` | Escuchar en todos los interfaces (Docker) |
| `VITE_REVERB_HOST` | ❌ | ✅ | `api.notificaciones.space` | Dominio público accesible desde Internet |
| `REVERB_PORT` | ✅ | ✅ | `8080` | Mismo puerto (Caddy hace el proxy) |

---

## ✅ Conclusión

**Tu configuración actual es CORRECTA:**

- ✅ Backend: `REVERB_SCHEME=http` (interno)
- ✅ Dashboard: `REVERB_SCHEME_PUBLIC=https` (externo)

**No necesitas cambiar nada.** La diferencia es intencional y correcta.

