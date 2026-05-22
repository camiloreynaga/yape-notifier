# 🔧 Configuración del Dashboard para Producción

> **Nota**: Esta es la documentación específica del entorno de producción. Para la guía completa de deployment del dashboard, ver `../../../../docs/02-deployment/DASHBOARD_DEPLOYMENT.md`.

## ⚠️ IMPORTANTE: Variables de Entorno Requeridas

El dashboard necesita variables de entorno que se inyectan en **build time** (no runtime). Estas deben estar configuradas en el archivo `.env` de producción antes de hacer el build.

## 📋 Variables Requeridas en `.env`

Agrega estas variables a tu archivo `.env` en `infra/docker/environments/production/.env`:

```env
# ============================================
# Dashboard - Variables de Entorno para Build
# ============================================

# URL de la API (debe coincidir con APP_URL del backend)
DASHBOARD_API_URL=https://api.notificaciones.space

# Variables de Reverb para WebSocket
# IMPORTANTE: Deben coincidir con las del backend
REVERB_APP_KEY=base64:7IFJN3FFdqSdv3nAdxoRmjMxzI5jqDIG33VJ7XC4LOk
REVERB_HOST_PUBLIC=api.notificaciones.space  # ⚠️ Dominio público, NO localhost
REVERB_PORT=8080
REVERB_SCHEME_PUBLIC=https  # ⚠️ HTTPS en producción
```

## 🔍 Explicación de Variables

### `DASHBOARD_API_URL`
- **Descripción**: URL base de la API Laravel
- **Valor**: Debe coincidir con `APP_URL` del backend
- **Ejemplo**: `https://api.notificaciones.space`

### `REVERB_APP_KEY`
- **Descripción**: Key pública de Reverb para autenticación WebSocket
- **Valor**: Debe ser **exactamente igual** a `REVERB_APP_KEY` del backend
- **Ejemplo**: `base64:7IFJN3FFdqSdv3nAdxoRmjMxzI5jqDIG33VJ7XC4LOk`
- **⚠️ CRÍTICO**: Si no coincide, WebSocket no conectará

### `REVERB_HOST_PUBLIC`
- **Descripción**: Dominio público donde está accesible Reverb
- **Valor**: Dominio público (NO `localhost` ni `0.0.0.0`)
- **Ejemplo**: `api.notificaciones.space`
- **⚠️ IMPORTANTE**: 
  - En producción debe ser el dominio público
  - El cliente (navegador) se conectará a este dominio
  - Caddy hace el proxy interno a `reverb:8080`

### `REVERB_PORT`
- **Descripción**: Puerto donde Reverb escucha (interno)
- **Valor**: Generalmente `8080`
- **Nota**: Caddy maneja el proxy, el cliente no necesita especificar el puerto

### `REVERB_SCHEME_PUBLIC`
- **Descripción**: Esquema (protocolo) para conexiones WebSocket
- **Valor**: `https` en producción (para `wss://`)
- **⚠️ CRÍTICO**: Debe ser `https` en producción para usar `wss://`

## 🚀 Pasos para Configurar

### Paso 1: Verificar Variables del Backend

Primero, verifica que el backend tiene las variables de Reverb configuradas:

```bash
# En el servidor, revisar apps/api/.env o infra/docker/environments/production/.env
cat infra/docker/environments/production/.env | grep REVERB
```

Debes ver:
```env
REVERB_APP_ID=yape-notifier
REVERB_APP_KEY=base64:7IFJN3FFdqSdv3nAdxoRmjMxzI5jqDIG33VJ7XC4LOk
REVERB_APP_SECRET=1771fded8db62696cfa7a92461511e22
REVERB_HOST=0.0.0.0
REVERB_PORT=8080
REVERB_SCHEME=http
```

### Paso 2: Agregar Variables al .env de Producción

Edita el archivo `.env` en `infra/docker/environments/production/.env`:

```bash
cd infra/docker/environments/production
nano .env
```

Agrega estas líneas (o actualiza si ya existen):

```env
# Dashboard Configuration
DASHBOARD_API_URL=https://api.notificaciones.space

# Reverb Public Configuration (for frontend)
REVERB_APP_KEY=base64:7IFJN3FFdqSdv3nAdxoRmjMxzI5jqDIG33VJ7XC4LOk
REVERB_HOST_PUBLIC=api.notificaciones.space
REVERB_PORT=8080
REVERB_SCHEME_PUBLIC=https
```

**⚠️ IMPORTANTE:**
- `REVERB_APP_KEY` debe ser **exactamente igual** al del backend
- `REVERB_HOST_PUBLIC` debe ser el dominio público (no `localhost`)
- `REVERB_SCHEME_PUBLIC` debe ser `https` en producción

### Paso 3: Reconstruir el Dashboard

Después de agregar las variables, reconstruye el contenedor del dashboard:

```bash
cd infra/docker/environments/production

# Reconstruir con las nuevas variables
docker compose --env-file .env build dashboard

# Reiniciar el contenedor
docker compose --env-file .env up -d dashboard

# Verificar logs
docker logs yape-notifier-dashboard-prod -f
```

### Paso 4: Verificar que Funciona

1. **Abrir el dashboard en el navegador:**
   ```
   https://dashboard.notificaciones.space
   ```

2. **Verificar WebSocket:**
   - Abrir DevTools → Console
   - Debe mostrar: `✅ WebSocket conectado`
   - No debe haber errores sobre variables faltantes

3. **Verificar indicador de conexión:**
   - En la interfaz debe mostrar "Conectado" (verde)
   - No debe mostrar "Desconectado" o "Error"

4. **Probar notificaciones en tiempo real:**
   - Crear una notificación desde el backend
   - Debe aparecer automáticamente en el dashboard
   - Debe aparecer un toast

## 🐛 Troubleshooting

### Error: Variables faltantes en build

**Síntoma:**
```
⚠️ Faltan variables de entorno requeridas: VITE_REVERB_APP_KEY, VITE_REVERB_HOST
```

**Solución:**
1. Verificar que las variables están en `.env`
2. Verificar que se pasan como build args en `docker-compose.yml`
3. Reconstruir el contenedor: `docker compose build dashboard`

### Error: WebSocket no conecta

**Síntoma:**
- Indicador muestra "Desconectado"
- Errores en consola sobre conexión WebSocket

**Soluciones:**
1. **Verificar Reverb está corriendo:**
   ```bash
   docker ps | grep reverb
   docker logs yape-notifier-reverb-prod
   ```

2. **Verificar Caddy está configurado:**
   - Verificar `Caddyfile` tiene configuración para WebSocket
   - Verificar que proxy funciona: `curl https://api.notificaciones.space/app/test`

3. **Verificar variables:**
   - `REVERB_HOST_PUBLIC` debe ser dominio público (no `localhost`)
   - `REVERB_SCHEME_PUBLIC` debe ser `https`
   - `REVERB_APP_KEY` debe coincidir con el backend

4. **Verificar en navegador:**
   - DevTools → Network → WS
   - Debe haber conexión a `wss://api.notificaciones.space/app/{key}`
   - Si no hay conexión, revisar errores en Console

### Error: Token expirado

**Síntoma:**
- WebSocket se desconecta después de un tiempo
- Usuario es deslogueado automáticamente

**Solución:**
- Esto es comportamiento esperado
- El sistema detecta token expirado y cierra sesión
- Usuario debe hacer login nuevamente

## 📊 Verificación Post-Deployment

### Checklist de Verificación

- [ ] Variables configuradas en `.env`
- [ ] Dashboard reconstruido con nuevas variables
- [ ] Dashboard accesible en `https://dashboard.notificaciones.space`
- [ ] WebSocket conecta (indicador verde)
- [ ] No hay errores en consola del navegador
- [ ] Notificaciones llegan en tiempo real
- [ ] Toasts aparecen cuando llegan notificaciones
- [ ] Logs del dashboard no muestran errores

### Comandos de Verificación

```bash
# Verificar contenedores corriendo
docker ps | grep -E "dashboard|reverb"

# Verificar logs del dashboard
docker logs yape-notifier-dashboard-prod --tail 50

# Verificar logs de Reverb
docker logs yape-notifier-reverb-prod --tail 50

# Verificar health check
curl https://dashboard.notificaciones.space/health
```

## 🔐 Seguridad

1. **NO commitear `.env`** al repositorio
2. **Usar secrets** en CI/CD si aplica
3. **Rotar keys** periódicamente:
   - `REVERB_APP_KEY`
   - `REVERB_APP_SECRET`
4. **HTTPS obligatorio** en producción
5. **Verificar CORS** en backend permite origen del dashboard

## 📝 Notas Importantes

1. **Variables VITE_***: Se inyectan en build time, no runtime
2. **WebSocket**: Requiere configuración especial en Caddy (ya configurado)
3. **Token Expiration**: Manejo automático implementado
4. **Error Tracking**: Preparado, solo falta agregar DSN de Sentry (opcional)

## 🆘 Soporte

Si encuentras problemas:
1. Revisar logs del contenedor: `docker logs yape-notifier-dashboard-prod`
2. Verificar configuración de variables en `.env`
3. Verificar que Reverb está corriendo: `docker ps | grep reverb`
4. Revisar configuración de Caddy para WebSocket
5. Ver `GUIA_PRODUCCION.md` y `PASOS_PRODUCCION.md` en `apps/web-dashboard/`

