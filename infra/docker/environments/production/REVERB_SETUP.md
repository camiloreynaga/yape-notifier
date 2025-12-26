# Configuración Profesional de Reverb WebSocket Server

> **Nota**: Esta es la documentación específica del entorno de producción. Para la guía completa de WebSockets, ver `../../../../docs/05-features/WEBSOCKETS.md`.

## 📋 Resumen

Reverb es el servidor WebSocket nativo de Laravel que permite comunicación en tiempo real bidireccional entre el servidor y los clientes (dashboard web, apps móviles).

## 🏗️ Arquitectura

```
Cliente (Dashboard/App)
    ↓ WebSocket (wss://api.notificaciones.space/app/{key})
Caddy (Reverse Proxy con HTTPS)
    ↓ WebSocket Proxy
Reverb Container (Puerto 8080)
    ↓ Broadcasting
Laravel API (PHP-FPM)
    ↓ Eventos
PostgreSQL Database
```

## ✅ Configuración Completa

### 1. Variables de Entorno (.env)

Agrega estas variables a tu `.env`:

```env
# ============================================
# Broadcasting (Reverb) - WebSocket Server
# ============================================
BROADCAST_CONNECTION=reverb

# Reverb Configuration
REVERB_APP_ID=yape-notifier
REVERB_APP_KEY=base64:TU_KEY_GENERADA_AQUI
REVERB_APP_SECRET=TU_SECRET_GENERADO_AQUI
REVERB_HOST=0.0.0.0
REVERB_PORT=8080
REVERB_SCHEME=http  # Reverb corre en HTTP internamente, Caddy maneja HTTPS
```

**⚠️ IMPORTANTE:**

- `REVERB_SCHEME=http` porque Reverb corre dentro de Docker en HTTP
- Caddy maneja el HTTPS externo y hace proxy al Reverb interno
- `REVERB_HOST=0.0.0.0` permite conexiones desde otros contenedores

### 2. Generar Keys de Reverb

**Opción A: Usar el script automatizado (Recomendado)**

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production
chmod +x generate-reverb-keys.sh
./generate-reverb-keys.sh
```

El script:

1. Crea un contenedor temporal
2. Genera las keys usando `php artisan reverb:install`
3. Muestra las keys para copiar al `.env`
4. Opcionalmente actualiza el `.env` automáticamente

**Opción B: Generar manualmente**

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Iniciar contenedor PHP-FPM temporalmente
docker compose --env-file .env up -d php-fpm

# Generar keys
docker compose --env-file .env exec php-fpm php artisan reverb:install --show

# Copiar las keys mostradas al .env
```

### 3. Servicio Reverb en Docker Compose

El servicio Reverb ya está integrado en `docker-compose.yml`:

```yaml
reverb:
  build:
    context: ../../../../apps/api
    dockerfile: ../../infra/docker/dockerfiles/Dockerfile.php-fpm
  container_name: yape-notifier-reverb-prod
  restart: always
  command: php artisan reverb:start --host=0.0.0.0 --port=8080
  # ... configuración completa
```

**Características:**

- Usa la misma imagen que PHP-FPM (comparte código)
- Corre en puerto 8080 interno
- Se reinicia automáticamente si falla
- Healthcheck para monitoreo
- Límites de recursos configurados

### 4. Configuración de Caddy (WebSocket Proxy)

El `Caddyfile` ya tiene la configuración WebSocket:

```caddyfile
api.notificaciones.space {
    # ... reverse_proxy a nginx-api ...

    # WebSocket proxy para Reverb
    handle /app/* {
        reverse_proxy reverb:8080 {
            header_up Connection {>Connection}
            header_up Upgrade {>Upgrade}
            # ... configuración completa
        }
    }
}
```

**Explicación:**

- `/app/*` es la ruta que Laravel Echo usa para WebSocket
- Caddy hace upgrade de HTTP a WebSocket automáticamente
- Headers `Connection` y `Upgrade` son necesarios para WebSocket
- Timeouts largos permiten mantener conexiones activas

### 5. Configuración de Laravel

#### `config/broadcasting.php`

```php
'default' => env('BROADCAST_CONNECTION', 'null'), // 'reverb' cuando está configurado
```

#### `config/reverb.php`

Ya está configurado correctamente, lee las variables de `.env`.

#### `routes/channels.php`

Ya tiene verificación condicional:

- Solo registra canales si Reverb está configurado
- Previene errores cuando `BROADCAST_CONNECTION=null`

## 🚀 Despliegue

### Paso 1: Generar Keys

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production
./generate-reverb-keys.sh
```

### Paso 2: Actualizar .env

Agregar las keys generadas al `.env`:

```env
BROADCAST_CONNECTION=reverb
REVERB_APP_ID=yape-notifier
REVERB_APP_KEY=base64:...
REVERB_APP_SECRET=...
REVERB_HOST=0.0.0.0
REVERB_PORT=8080
REVERB_SCHEME=http
```

### Paso 3: Reconstruir y Desplegar

```bash
# Reconstruir imágenes (si hay cambios en código)
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1
docker compose --env-file .env build --no-cache

# Iniciar servicios (incluye Reverb)
docker compose --env-file .env up -d

# Verificar que Reverb está corriendo
docker compose --env-file .env ps reverb

# Ver logs de Reverb
docker compose --env-file .env logs -f reverb
```

### Paso 4: Verificar Funcionamiento

```bash
# Verificar que Reverb está escuchando
docker compose --env-file .env exec reverb netstat -tuln | grep 8080

# Verificar logs
docker compose --env-file .env logs reverb --tail=50

# Probar conexión WebSocket (desde el servidor)
curl -i -N \
  -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Version: 13" \
  -H "Sec-WebSocket-Key: test" \
  http://localhost:8080/app/test
```

## 🔍 Verificación en el Cliente

### Dashboard Web (Laravel Echo)

```javascript
// En el dashboard, configurar Echo
import Echo from "laravel-echo";
import Pusher from "pusher-js";

window.Pusher = Pusher;

window.Echo = new Echo({
  broadcaster: "reverb",
  key: import.meta.env.VITE_REVERB_APP_KEY,
  wsHost: import.meta.env.VITE_REVERB_HOST,
  wsPort: import.meta.env.VITE_REVERB_PORT ?? 80,
  wssPort: import.meta.env.VITE_REVERB_PORT ?? 443,
  forceTLS: (import.meta.env.VITE_REVERB_SCHEME ?? "https") === "https",
  enabledTransports: ["ws", "wss"],
});

// Escuchar canal
Echo.private(`commerce.${commerceId}`).listen(".notification.created", (e) => {
  console.log("Notification received:", e);
});
```

### Variables de Entorno del Dashboard

```env
VITE_REVERB_APP_KEY=base64:TU_KEY_GENERADA
VITE_REVERB_HOST=api.notificaciones.space
VITE_REVERB_PORT=443
VITE_REVERB_SCHEME=https
```

## 🐛 Solución de Problemas

### Error: "Connection refused"

**Causa:** Reverb no está corriendo o no está accesible.

**Solución:**

```bash
# Verificar que Reverb está corriendo
docker compose --env-file .env ps reverb

# Ver logs
docker compose --env-file .env logs reverb

# Reiniciar Reverb
docker compose --env-file .env restart reverb
```

### Error: "WebSocket connection failed"

**Causa:** Caddy no está haciendo proxy correctamente o falta configuración.

**Solución:**

1. Verificar que Caddyfile tiene la sección `/app/*`
2. Verificar logs de Caddy: `docker compose --env-file .env logs caddy`
3. Reiniciar Caddy: `docker compose --env-file .env restart caddy`

### Error: "Invalid key" o "Authentication failed"

**Causa:** Las keys de Reverb no coinciden entre servidor y cliente.

**Solución:**

1. Verificar que `REVERB_APP_KEY` en `.env` del servidor coincide con `VITE_REVERB_APP_KEY` en el dashboard
2. Regenerar keys si es necesario: `./generate-reverb-keys.sh`
3. Reconstruir dashboard con las nuevas variables

### Reverb se reinicia constantemente

**Causa:** Error en la configuración o falta de recursos.

**Solución:**

```bash
# Ver logs detallados
docker compose --env-file .env logs reverb

# Verificar recursos
docker stats yape-notifier-reverb-prod

# Verificar configuración
docker compose --env-file .env exec reverb php artisan config:show broadcasting
```

## 📊 Monitoreo

### Ver Estado de Reverb

```bash
# Estado del contenedor
docker compose --env-file .env ps reverb

# Uso de recursos
docker stats yape-notifier-reverb-prod

# Conexiones activas (desde dentro del contenedor)
docker compose --env-file .env exec reverb netstat -an | grep 8080
```

### Logs

```bash
# Logs en tiempo real
docker compose --env-file .env logs -f reverb

# Últimas 100 líneas
docker compose --env-file .env logs --tail=100 reverb
```

## 🔒 Seguridad

1. **HTTPS Obligatorio:** Caddy maneja HTTPS automáticamente
2. **Autenticación:** Los canales privados requieren autenticación Laravel
3. **CORS:** Configurado en `config/cors.php`
4. **Rate Limiting:** Considerar agregar rate limiting para WebSocket connections

## 📝 Notas Importantes

1. **Reverb corre en HTTP internamente:** Caddy maneja HTTPS externo
2. **Puerto 8080 es interno:** No se expone directamente, solo a través de Caddy
3. **Keys deben coincidir:** Servidor y cliente deben usar las mismas keys
4. **Restart automático:** Reverb se reinicia automáticamente si falla
5. **Healthcheck:** Docker monitorea la salud del servicio

## 🎯 Resumen de Archivos Modificados

- ✅ `docker-compose.yml` - Servicio Reverb agregado
- ✅ `Caddyfile` - Proxy WebSocket configurado
- ✅ `config/broadcasting.php` - Default a 'null' (seguro)
- ✅ `routes/channels.php` - Verificación condicional
- ✅ `generate-reverb-keys.sh` - Script para generar keys

## ✅ Checklist de Configuración

- [ ] Keys de Reverb generadas
- [ ] Variables agregadas al `.env`
- [ ] Servicio Reverb en `docker-compose.yml`
- [ ] Proxy WebSocket en `Caddyfile`
- [ ] Servicios desplegados y corriendo
- [ ] Logs verificados sin errores
- [ ] Conexión WebSocket probada desde cliente

---

## 📚 Referencias

- **Guía completa de WebSockets**: Ver `../../../../docs/05-features/WEBSOCKETS.md`
- **Docker**: Ver `../../../../docs/02-deployment/DOCKER.md`
- **Deployment**: Ver `../../../../docs/02-deployment/DEPLOYMENT.md`
