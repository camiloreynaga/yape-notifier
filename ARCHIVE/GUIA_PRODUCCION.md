# Guía de Deployment a Producción - Dashboard Web

## 📋 Checklist Pre-Deployment

### ✅ 1. Variables de Entorno Configuradas

**Ubicación:** `apps/web-dashboard/.env.production` o variables en Docker Compose

**Variables Requeridas:**

```env
# API Backend
VITE_API_URL=https://api.notificaciones.space

# Laravel Reverb WebSocket Server
VITE_REVERB_APP_KEY=base64:7IFJN3FFdqSdv3nAdxoRmjMxzI5jqDIG33VJ7XC4LOk
VITE_REVERB_HOST=api.notificaciones.space
VITE_REVERB_PORT=8080
VITE_REVERB_SCHEME=https
```

**⚠️ IMPORTANTE:**
- Estas variables se inyectan en **build time**, no en runtime
- Si faltan variables, el build puede fallar o la app no funcionará
- En Docker, configurar en `docker-compose.yml` o `.env` del contenedor

### ✅ 2. Build de Producción

```bash
cd apps/web-dashboard
npm ci  # Instalar dependencias
npm run build  # Build de producción
```

**Verificar:**
- Build exitoso sin errores
- Archivos generados en `dist/`
- Bundle size razonable (< 1MB gzipped)

### ✅ 3. Tests y Linting

```bash
npm run lint        # Debe pasar sin errores
npm run test        # Todos los tests deben pasar
npm run type-check  # Sin errores críticos de TypeScript
```

### ✅ 4. Configuración de Docker

**Para desarrollo:**
```bash
docker-compose -f infra/docker/environments/development/docker-compose.yml build dashboard
docker-compose -f infra/docker/environments/development/docker-compose.yml up dashboard
```

**Para producción:**
- Las variables de entorno deben estar en el `.env` de producción
- O configuradas en el `docker-compose.yml` de producción

## 🚀 Pasos de Deployment

### Paso 1: Preparar Variables de Entorno

1. **Obtener valores de Reverb del backend:**
   ```bash
   # En el servidor de producción, revisar apps/api/.env
   REVERB_APP_KEY=base64:7IFJN3FFdqSdv3nAdxoRmjMxzI5jqDIG33VJ7XC4LOk
   REVERB_APP_SECRET=1771fded8db62696cfa7a92461511e22
   REVERB_HOST=0.0.0.0  # Interno en Docker
   REVERB_PORT=8080
   ```

2. **Configurar variables para el frontend:**
   ```env
   # En apps/web-dashboard/.env.production
   VITE_API_URL=https://api.notificaciones.space
   VITE_REVERB_APP_KEY=base64:7IFJN3FFdqSdv3nAdxoRmjMxzI5jqDIG33VJ7XC4LOk
   VITE_REVERB_HOST=api.notificaciones.space  # Dominio público
   VITE_REVERB_PORT=8080
   VITE_REVERB_SCHEME=https
   ```

### Paso 2: Build en el Servidor

**Opción A: Build en el servidor (recomendado)**

```bash
# 1. Conectarse al servidor
ssh usuario@servidor

# 2. Ir al directorio del proyecto
cd /ruta/a/yape-notifier/apps/web-dashboard

# 3. Instalar dependencias (si no están)
npm ci

# 4. Build con variables de entorno
VITE_API_URL=https://api.notificaciones.space \
VITE_REVERB_APP_KEY=base64:7IFJN3FFdqSdv3nAdxoRmjMxzI5jqDIG33VJ7XC4LOk \
VITE_REVERB_HOST=api.notificaciones.space \
VITE_REVERB_PORT=8080 \
VITE_REVERB_SCHEME=https \
npm run build

# 5. Verificar que dist/ tiene los archivos
ls -la dist/
```

**Opción B: Build en CI/CD**

- Configurar GitHub Actions o similar
- Pasar variables de entorno como secrets
- Build automático en cada push a `main`

### Paso 3: Deployment con Docker

**Si usas Docker Compose:**

```bash
# 1. Asegurar que las variables están en .env o docker-compose.yml
# 2. Reconstruir imagen
docker-compose -f infra/docker/environments/production/docker-compose.yml build dashboard

# 3. Reiniciar contenedor
docker-compose -f infra/docker/environments/production/docker-compose.yml up -d dashboard
```

**Variables en docker-compose.yml:**

```yaml
dashboard:
  environment:
    - VITE_API_URL=https://api.notificaciones.space
    - VITE_REVERB_APP_KEY=base64:7IFJN3FFdqSdv3nAdxoRmjMxzI5jqDIG33VJ7XC4LOk
    - VITE_REVERB_HOST=api.notificaciones.space
    - VITE_REVERB_PORT=8080
    - VITE_REVERB_SCHEME=https
```

### Paso 4: Verificar Deployment

1. **Health Check:**
   ```bash
   curl https://dashboard.notificaciones.space/health
   # Debe responder: healthy
   ```

2. **Verificar WebSocket:**
   - Abrir dashboard en navegador
   - Verificar que el indicador de conexión muestra "Conectado"
   - Abrir DevTools → Network → WS
   - Debe haber conexión WebSocket a `wss://api.notificaciones.space:8080`

3. **Verificar Variables:**
   - Abrir DevTools → Console
   - No debe haber errores sobre variables faltantes
   - Verificar que `env.ts` valida correctamente

## 🔍 Troubleshooting

### Problema: WebSocket no conecta

**Síntomas:**
- Indicador muestra "Desconectado" o "Error"
- No llegan notificaciones en tiempo real

**Soluciones:**
1. Verificar que Reverb está corriendo:
   ```bash
   docker ps | grep reverb
   ```

2. Verificar configuración de Caddy (reverse proxy):
   - Debe tener configuración para WebSocket upgrade
   - Ver `infra/docker/environments/production/REVERB_SETUP.md`

3. Verificar variables de entorno:
   - `VITE_REVERB_HOST` debe ser el dominio público (no `localhost`)
   - `VITE_REVERB_SCHEME` debe ser `https` en producción

### Problema: Variables de entorno no funcionan

**Síntomas:**
- App funciona pero WebSocket no conecta
- Errores en consola sobre variables faltantes

**Soluciones:**
1. Verificar que variables están en build time:
   ```bash
   # Las variables VITE_* se inyectan en build, no runtime
   # Deben estar disponibles durante `npm run build`
   ```

2. Verificar archivo `.env.production`:
   ```bash
   cat apps/web-dashboard/.env.production
   ```

3. Rebuild con variables explícitas:
   ```bash
   VITE_REVERB_APP_KEY=tu-key npm run build
   ```

### Problema: Token expirado

**Síntomas:**
- WebSocket se desconecta después de un tiempo
- Usuario es deslogueado automáticamente

**Solución:**
- Esto es comportamiento esperado
- El sistema detecta token expirado y cierra sesión
- Usuario debe hacer login nuevamente

## 📊 Monitoreo Post-Deployment

### 1. Verificar Logs

```bash
# Logs del contenedor dashboard
docker logs yape-notifier-dashboard-prod -f

# Logs de Reverb
docker logs yape-notifier-reverb-prod -f
```

### 2. Verificar Métricas

- Conexiones WebSocket activas
- Tasa de errores
- Tiempo de respuesta de API

### 3. Health Check Endpoint

```bash
# Verificar estado de servicios
curl https://dashboard.notificaciones.space/health
```

## 🔐 Seguridad

### Variables Sensibles

- **NO** commitear `.env.production` al repositorio
- Usar secrets en CI/CD
- Rotar `REVERB_APP_KEY` y `REVERB_APP_SECRET` periódicamente

### HTTPS

- Asegurar que todo el tráfico es HTTPS
- WebSocket debe usar `wss://` (no `ws://`)
- Configurar HSTS headers

## 📝 Notas Importantes

1. **Variables VITE_***: Se inyectan en build time, no runtime
2. **WebSocket**: Requiere configuración especial en reverse proxy (Caddy)
3. **Token Expiration**: El sistema maneja automáticamente tokens expirados
4. **Error Tracking**: Actualmente deshabilitado, ver sección de Sentry para habilitar

## 🆘 Soporte

Si encuentras problemas:
1. Revisar logs del contenedor
2. Verificar configuración de variables
3. Verificar que Reverb está corriendo
4. Revisar configuración de Caddy para WebSocket

