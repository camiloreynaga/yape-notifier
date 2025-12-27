# 🚀 Pasos para Deployment a Producción

> **Nota**: Esta es la documentación específica del dashboard web. Para la versión consolidada, ver `../../docs/02-deployment/DASHBOARD_DEPLOYMENT.md`.

## ✅ Estado Actual del Código

**Implementado:**

- ✅ WebSockets con Laravel Echo
- ✅ Reconexión automática con backoff exponencial
- ✅ Manejo de token expirado
- ✅ Logging estructurado
- ✅ Health check
- ✅ Validación de variables de entorno
- ✅ Tests pasando (13/13)
- ✅ Linting sin errores

**Listo para producción:** ✅ **SÍ** (con configuración correcta)

## 📋 Checklist Pre-Deployment

### 1. Variables de Entorno

**Ubicación:** Configurar en Docker Compose o `.env.production`

```env
# API Backend
VITE_API_URL=https://api.notificaciones.space

# Laravel Reverb (valores de tu .env de producción)
VITE_REVERB_APP_KEY=base64:7IFJN3FFdqSdv3nAdxoRmjMxzI5jqDIG33VJ7XC4LOk
VITE_REVERB_HOST=api.notificaciones.space  # ⚠️ Dominio público, NO localhost
VITE_REVERB_PORT=8080
VITE_REVERB_SCHEME=https  # ⚠️ HTTPS en producción
```

**⚠️ CRÍTICO:**

- `VITE_REVERB_HOST` debe ser el dominio público donde está Reverb
- NO usar `localhost` o `0.0.0.0` en producción
- Estas variables se inyectan en **build time**, no runtime

### 2. Build de Producción

**En el servidor o CI/CD:**

```bash
cd apps/web-dashboard

# Instalar dependencias
npm ci

# Build con variables de entorno
VITE_API_URL=https://api.notificaciones.space \
VITE_REVERB_APP_KEY=base64:7IFJN3FFdqSdv3nAdxoRmjMxzI5jqDIG33VJ7XC4LOk \
VITE_REVERB_HOST=api.notificaciones.space \
VITE_REVERB_PORT=8080 \
VITE_REVERB_SCHEME=https \
npm run build

# Verificar build
ls -la dist/  # Debe tener index.html y assets/
```

### 3. Docker Compose (Producción)

**Configurar en `infra/docker/environments/production/docker-compose.yml`:**

```yaml
dashboard:
  build:
    context: ../../../../apps/web-dashboard
    dockerfile: Dockerfile
    args:
      - VITE_API_URL=https://api.notificaciones.space
      - VITE_REVERB_APP_KEY=base64:7IFJN3FFdqSdv3nAdxoRmjMxzI5jqDIG33VJ7XC4LOk
      - VITE_REVERB_HOST=api.notificaciones.space
      - VITE_REVERB_PORT=8080
      - VITE_REVERB_SCHEME=https
  environment:
    - VITE_API_URL=https://api.notificaciones.space
    - VITE_REVERB_APP_KEY=base64:7IFJN3FFdqSdv3nAdxoRmjMxzI5jqDIG33VJ7XC4LOk
    - VITE_REVERB_HOST=api.notificaciones.space
    - VITE_REVERB_PORT=8080
    - VITE_REVERB_SCHEME=https
```

**O usar archivo `.env` en el contenedor:**

```bash
# Crear .env.production en apps/web-dashboard/
cp .env.production.example .env.production
nano .env.production  # Editar con valores reales
```

### 4. Verificar Reverb en Backend

**Asegurar que Reverb está corriendo:**

```bash
# En el servidor
docker ps | grep reverb
# Debe mostrar contenedor reverb corriendo

# Verificar logs
docker logs yape-notifier-reverb-prod -f
```

**Verificar configuración de Caddy:**

- Caddy debe tener configuración para WebSocket upgrade
- Ver `infra/docker/environments/production/REVERB_SETUP.md`

### 5. Deployment

**Opción A: Docker Compose**

```bash
# 1. Reconstruir imagen con nuevas variables
docker-compose -f infra/docker/environments/production/docker-compose.yml build dashboard

# 2. Reiniciar contenedor
docker-compose -f infra/docker/environments/production/docker-compose.yml up -d dashboard

# 3. Verificar logs
docker logs yape-notifier-dashboard-prod -f
```

**Opción B: Build Manual**

```bash
# 1. Build en servidor
cd /ruta/a/yape-notifier/apps/web-dashboard
npm ci
npm run build

# 2. Copiar dist/ a nginx
cp -r dist/* /var/www/dashboard/

# 3. Reiniciar nginx
sudo systemctl reload nginx
```

## 🔍 Verificación Post-Deployment

### 1. Health Check

```bash
curl https://dashboard.notificaciones.space/health
# Debe responder: healthy
```

### 2. Verificar WebSocket

1. Abrir dashboard en navegador
2. Abrir DevTools → Console
3. Verificar que no hay errores sobre variables faltantes
4. Verificar indicador de conexión muestra "Conectado"
5. DevTools → Network → WS
6. Debe haber conexión a `wss://api.notificaciones.space:8080`

### 3. Verificar Funcionalidad

1. Login funciona
2. Notificaciones se cargan
3. WebSocket conecta (indicador verde)
4. Nueva notificación aparece en tiempo real
5. Toast aparece cuando llega notificación

## 🐛 Troubleshooting

### WebSocket no conecta

**Síntomas:**

- Indicador muestra "Desconectado"
- No llegan notificaciones en tiempo real

**Soluciones:**

1. **Verificar Reverb:**

   ```bash
   docker ps | grep reverb
   docker logs yape-notifier-reverb-prod
   ```

2. **Verificar Caddy:**

   - Debe tener configuración para WebSocket upgrade
   - Verificar que proxy funciona: `curl https://api.notificaciones.space:8080`

3. **Verificar Variables:**

   - `VITE_REVERB_HOST` debe ser dominio público (no localhost)
   - `VITE_REVERB_SCHEME` debe ser `https` en producción
   - Verificar en DevTools → Application → Local Storage → inspeccionar variables

4. **Verificar CORS:**
   - Backend debe permitir origen `https://dashboard.notificaciones.space`
   - Verificar `apps/api/config/cors.php`

### Variables no funcionan

**Síntomas:**

- Errores en consola sobre variables faltantes
- WebSocket no conecta

**Soluciones:**

1. **Verificar Build:**

   ```bash
   # Las variables VITE_* se inyectan en build time
   # Deben estar disponibles durante npm run build
   ```

2. **Rebuild con variables explícitas:**

   ```bash
   VITE_REVERB_APP_KEY=tu-key npm run build
   ```

3. **Verificar Dockerfile:**
   - Debe pasar variables como ARG y ENV
   - Ver `apps/web-dashboard/Dockerfile`

## 📊 Monitoreo

### Logs

```bash
# Dashboard
docker logs yape-notifier-dashboard-prod -f

# Reverb
docker logs yape-notifier-reverb-prod -f

# API
docker logs yape-notifier-api-prod -f
```

### Métricas

- Conexiones WebSocket activas
- Tasa de errores
- Tiempo de respuesta

## 🔐 Seguridad

1. **NO commitear `.env.production`** al repositorio
2. **Usar secrets** en CI/CD
3. **Rotar keys** periódicamente:
   - `REVERB_APP_KEY`
   - `REVERB_APP_SECRET`
4. **HTTPS obligatorio** en producción
5. **CSP headers** configurados en nginx

## 📝 Notas Finales

1. **Variables VITE\_\***: Se inyectan en build time
2. **WebSocket**: Requiere configuración especial en Caddy
3. **Token Expiration**: Manejo automático implementado
4. **Error Tracking**: Preparado, solo falta agregar DSN de Sentry (opcional)

## 🆘 Soporte

Si encuentras problemas:

1. Revisar logs del contenedor
2. Verificar configuración de variables
3. Verificar que Reverb está corriendo
4. Revisar configuración de Caddy
5. Ver `GUIA_PRODUCCION.md` para más detalles
