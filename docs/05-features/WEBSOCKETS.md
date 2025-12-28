# WebSockets para Notificaciones en Tiempo Real

## Estado: ✅ CONFIGURACIÓN DISPONIBLE

**Prioridad:** Media  
**Componentes afectados:** API (Laravel), Web Dashboard (React)

**Descripción:** Implementación de WebSockets usando Laravel Reverb para notificaciones en tiempo real en el dashboard web. La configuración de producción con Docker está disponible.

---

## 📋 Resumen

Este documento describe la implementación de WebSockets usando Laravel Reverb para notificaciones en tiempo real en Yape Notifier. Permite que el dashboard web reciba notificaciones nuevas sin necesidad de polling constante.

---

## 🎯 Objetivo

- Actualizar el dashboard web en tiempo real cuando se crea una nueva notificación
- Eliminar la necesidad de polling constante
- Mejorar la experiencia de usuario con actualizaciones instantáneas

---

## 🚀 Instalación

### 1. Instalar Laravel Reverb

```bash
cd apps/api
composer require laravel/reverb
```

### 2. Publicar configuración de Reverb

```bash
php artisan reverb:install
```

Esto creará el archivo `config/reverb.php` con la configuración necesaria.

### 3. Configurar variables de entorno

Agregar al archivo `.env`:

```env
BROADCAST_CONNECTION=reverb

REVERB_APP_ID=yape-notifier
REVERB_APP_KEY=your-app-key
REVERB_APP_SECRET=your-app-secret
REVERB_HOST=127.0.0.1
REVERB_PORT=8080
REVERB_SCHEME=http
```

**Para producción (HTTPS):**
```env
REVERB_SCHEME=https
REVERB_HOST=your-domain.com
REVERB_PORT=443
```

### 4. Generar clave de aplicación

```bash
php artisan reverb:install
```

Esto generará automáticamente las claves necesarias.

---

## 🔧 Configuración

### Archivos a Crear/Modificar

1. **`config/broadcasting.php`** - Configuración de broadcasting
2. **`app/Events/NotificationCreated.php`** - Evento de broadcasting
3. **`routes/channels.php`** - Autorización de canales privados
4. **`app/Services/NotificationService.php`** - Dispara evento al crear notificación
5. **`bootstrap/app.php`** - Registra rutas de broadcasting

### Canales Privados

Las notificaciones se transmiten en canales privados por `commerce_id`:

- **Canal:** `private-commerce.{commerce_id}`
- **Autorización:** Solo usuarios del mismo `commerce_id` pueden escuchar

---

## 🏃 Ejecutar Servidor WebSocket

### Desarrollo

```bash
php artisan reverb:start
```

El servidor se iniciará en `http://127.0.0.1:8080` (o el puerto configurado).

### Producción con Docker

La configuración de producción usa Docker Compose con Caddy como reverse proxy.

#### Arquitectura

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

#### Variables de Entorno para Producción

```env
# Broadcasting (Reverb) - WebSocket Server
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

#### Generar Keys de Reverb

**Opción A: Script automatizado (Recomendado)**

```bash
cd infra/docker/environments/production
chmod +x generate-reverb-keys.sh
./generate-reverb-keys.sh
```

**Opción B: Generar manualmente**

```bash
cd infra/docker/environments/production

# Iniciar contenedor PHP-FPM temporalmente
docker compose --env-file .env up -d php-fpm

# Generar keys
docker compose --env-file .env exec php-fpm php artisan reverb:install --show

# Copiar las keys mostradas al .env
```

#### Servicio Reverb en Docker Compose

El servicio Reverb está integrado en `docker-compose.yml`:

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

#### Configuración de Caddy (WebSocket Proxy)

El `Caddyfile` tiene la configuración WebSocket:

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

#### Despliegue

```bash
cd infra/docker/environments/production

# Paso 1: Generar keys
./generate-reverb-keys.sh

# Paso 2: Agregar keys al .env (ver sección anterior)

# Paso 3: Reconstruir y desplegar
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1
docker compose --env-file .env build --no-cache
docker compose --env-file .env up -d

# Paso 4: Verificar que Reverb está corriendo
docker compose --env-file .env ps reverb
docker compose --env-file .env logs -f reverb
```

#### Verificar Funcionamiento

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

#### Monitoreo

```bash
# Estado del contenedor
docker compose --env-file .env ps reverb

# Uso de recursos
docker stats yape-notifier-reverb-prod

# Logs en tiempo real
docker compose --env-file .env logs -f reverb
```

### Producción (con Supervisor - Alternativa)

Si no usas Docker, puedes usar Supervisor:

Crear archivo `/etc/supervisor/conf.d/reverb.conf`:

```ini
[program:reverb]
process_name=%(program_name)s_%(process_num)02d
command=php /path/to/your/project/artisan reverb:start --host=0.0.0.0 --port=8080
autostart=true
autorestart=true
stopasgroup=true
killasgroup=true
user=www-data
numprocs=1
redirect_stderr=true
stdout_logfile=/path/to/your/project/storage/logs/reverb.log
stopwaitsecs=3600
```

Luego:

```bash
sudo supervisorctl reread
sudo supervisorctl update
sudo supervisorctl start reverb:*
```

---

## 📡 Uso en el Frontend

### Instalación (React/TypeScript)

```bash
cd apps/web-dashboard
npm install laravel-echo pusher-js
```

### Configuración

```typescript
import Echo from 'laravel-echo';
import Pusher from 'pusher-js';

window.Pusher = Pusher;

const echo = new Echo({
  broadcaster: 'reverb',
  key: import.meta.env.VITE_REVERB_APP_KEY,
  wsHost: import.meta.env.VITE_REVERB_HOST,
  wsPort: import.meta.env.VITE_REVERB_PORT,
  wssPort: import.meta.env.VITE_REVERB_PORT,
  forceTLS: (import.meta.env.VITE_REVERB_SCHEME ?? 'https') === 'https',
  enabledTransports: ['ws', 'wss'],
  authEndpoint: '/api/broadcasting/auth',
  auth: {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  },
});
```

### Escuchar Notificaciones

```typescript
// Obtener commerce_id del usuario autenticado
const commerceId = user.commerce_id;

// Suscribirse al canal privado
const channel = echo.private(`commerce.${commerceId}`);

// Escuchar evento de notificación creada
channel.listen('.notification.created', (data: any) => {
  console.log('Nueva notificación recibida:', data);
  
  // Actualizar UI con la nueva notificación
  addNotificationToList(data);
  showNotificationToast(data);
});
```

### Variables de Entorno del Frontend

Agregar a `.env` del frontend:

```env
VITE_REVERB_APP_KEY="${REVERB_APP_KEY}"
VITE_REVERB_HOST="${REVERB_HOST}"
VITE_REVERB_PORT="${REVERB_PORT}"
VITE_REVERB_SCHEME="${REVERB_SCHEME}"
```

---

## 🔐 Autenticación de Canales

La autenticación de canales privados se maneja automáticamente mediante:

1. **Sanctum Token:** El frontend envía el token en el header `Authorization`
2. **Autorización:** `routes/channels.php` verifica que el usuario pertenezca al commerce

### Endpoint de Autenticación

Laravel automáticamente expone `/api/broadcasting/auth` para autenticar suscripciones a canales privados.

---

## 📊 Flujo de Datos

1. **Android App** envía notificación → `POST /api/notifications`
2. **NotificationController** → `NotificationService::createNotification()`
3. **NotificationService** crea notificación y dispara `NotificationCreated` event
4. **Laravel Reverb** transmite evento a clientes conectados
5. **Frontend** recibe evento en tiempo real y actualiza UI

---

## 🧪 Testing

### Probar Broadcasting Localmente

1. Iniciar servidor Reverb:
   ```bash
   php artisan reverb:start
   ```

2. En otra terminal, iniciar aplicación Laravel:
   ```bash
   php artisan serve
   ```

3. Crear notificación de prueba:
   ```bash
   php artisan tinker
   ```
   ```php
   $device = App\Models\Device::first();
   $service = app(App\Services\NotificationService::class);
   $service->createNotification([
       'body' => 'Test notification',
       'source_app' => 'yape',
       'amount' => 50.00,
   ], $device);
   ```

4. Verificar en frontend que se recibe el evento

### Verificar Conexión WebSocket

Usar herramienta como [WebSocket King](https://websocketking.com/) o [Postman](https://www.postman.com/) para conectarse a:

```
ws://127.0.0.1:8080/app/your-app-key
```

---

## 🐛 Troubleshooting

### El servidor Reverb no inicia

- Verificar que el puerto no esté en uso: `netstat -an | grep 8080`
- Verificar permisos del archivo de log
- Revisar configuración en `config/reverb.php`

### El frontend no recibe eventos

1. Verificar que el servidor Reverb esté corriendo
2. Verificar variables de entorno en frontend
3. Verificar token de autenticación
4. Verificar que el usuario tenga `commerce_id`
5. Revisar consola del navegador para errores

### Error de autenticación

- Verificar que `routes/channels.php` esté registrado en `bootstrap/app.php`
- Verificar que el token Sanctum sea válido
- Verificar que el usuario tenga `commerce_id` y coincida con el canal

### Error: "Connection refused" (Docker)

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

### Error: "WebSocket connection failed" (Docker)

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

### Reverb se reinicia constantemente (Docker)

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

---

## 📝 Notas Importantes

1. **Rate Limiting:** Considerar implementar rate limiting para eventos si hay muchos clientes
2. **Redis:** Para producción con múltiples servidores, usar Redis como driver de broadcasting
3. **SSL/TLS:** En producción, usar HTTPS/WSS para seguridad (Caddy maneja esto automáticamente en Docker)
4. **Escalabilidad:** Reverb puede manejar miles de conexiones simultáneas
5. **Reconexión:** Laravel Echo maneja automáticamente la reconexión
6. **Docker (Producción):**
   - Reverb corre en HTTP internamente, Caddy maneja HTTPS externo
   - Puerto 8080 es interno, no se expone directamente, solo a través de Caddy
   - Keys deben coincidir entre servidor y cliente
   - Restart automático si falla
   - Healthcheck para monitoreo

---

## 🔄 Actualización de Configuración

Si necesitas cambiar la configuración:

1. Actualizar `.env`
2. Reiniciar servidor Reverb: `php artisan reverb:restart` (si está en supervisor)
3. Recargar frontend para obtener nuevas variables de entorno

---

## 🔐 Configuración de Scheme: HTTP vs HTTPS

### ⚠️ Importante: Diferencia entre Backend y Frontend

**Backend (Laravel API):**
- `REVERB_SCHEME=http` ✅ **CORRECTO**

**Dashboard (Frontend):**
- `VITE_REVERB_SCHEME=https` ✅ **CORRECTO**

**¿Por qué son diferentes?** Porque el backend y el frontend se comunican con Reverb desde contextos diferentes.

### Arquitectura de Comunicación

```
Navegador (Cliente)
    ↓ HTTPS (wss://)
    ↓ VITE_REVERB_SCHEME=https
Caddy (Reverse Proxy)
    ↓ HTTP (ws://) interno
    ↓ REVERB_SCHEME=http
Reverb Container (Puerto 8080)
    ↓ HTTP (interno)
Laravel API (PHP-FPM)
```

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

### Errores Comunes

**❌ Error 1: Usar `REVERB_SCHEME=https` en el backend**
- Problema: Reverb intentará usar HTTPS internamente, pero no hay certificado SSL en el contenedor
- Solución: Usar `REVERB_SCHEME=http` (comunicación interna)

**❌ Error 2: Usar `VITE_REVERB_SCHEME=http` en el dashboard**
- Problema: El navegador intentará conectarse con `ws://` (no seguro), causando errores de Mixed Content
- Solución: Usar `VITE_REVERB_SCHEME=https` (comunicación externa)

**❌ Error 3: Usar `localhost` o `0.0.0.0` en el dashboard**
- Problema: El navegador no puede conectarse a `localhost` o `0.0.0.0` desde Internet
- Solución: Usar el dominio público `api.notificaciones.space`

### Configuración Correcta

**Backend (.env):**
```env
REVERB_HOST=0.0.0.0          # ✅ Interno en Docker
REVERB_PORT=8080
REVERB_SCHEME=http          # ✅ HTTP interno
```

**Dashboard (Variables de Build):**
```env
REVERB_HOST_PUBLIC=api.notificaciones.space  # ✅ Dominio público
REVERB_SCHEME_PUBLIC=https                  # ✅ HTTPS externo
```

---

## 🚀 Impacto del Deployment

### Nuevos Componentes del Sistema

#### Servicio Reverb (WebSocket Server)
- **Qué es**: Servidor WebSocket nativo de Laravel que maneja conexiones en tiempo real
- **Impacto**: Nuevo servicio que debe ejecutarse de forma continua
- **Recursos**: Consume CPU y memoria para mantener conexiones WebSocket activas
- **Puerto**: Requiere un puerto adicional (por defecto 8080) expuesto internamente

#### Broadcasting de Eventos
- **Qué es**: Sistema que transmite eventos de Laravel a clientes conectados vía WebSocket
- **Impacto**: Cada notificación creada ahora dispara un evento broadcast
- **Rendimiento**: Mínimo impacto en la creación de notificaciones (operación asíncrona)
- **Escalabilidad**: Requiere considerar el número de conexiones simultáneas

#### Autenticación de Canales Privados
- **Qué es**: Sistema que valida que usuarios solo escuchen canales de su commerce
- **Impacto**: Endpoint adicional `/api/broadcasting/auth` que valida tokens Sanctum
- **Seguridad**: Asegura aislamiento multi-tenant a nivel de WebSocket

### Cambios en la Infraestructura

#### Antes (Sin WebSockets)
```
Cliente → Caddy → Nginx → PHP-FPM → PostgreSQL
         (HTTPS)   (HTTP)   (FastCGI)
```

#### Después (Con WebSockets)
```
Cliente → Caddy → Nginx → PHP-FPM → PostgreSQL
         (HTTPS)   (HTTP)   (FastCGI)
         
Cliente → Caddy → Reverb → PHP-FPM (para auth)
         (WSS)     (WS)      (HTTP)
```

### Impacto en Recursos

| Recurso | Impacto | Notas |
|---------|---------|-------|
| **CPU** | +5-10% | Reverb consume CPU para mantener conexiones |
| **Memoria** | +50-100MB | Por cada conexión WebSocket activa (~1-2MB) |
| **Red** | Variable | Depende del número de conexiones simultáneas |
| **Puertos** | +1 puerto | Puerto 8080 (interno) para Reverb |
| **Disco** | Mínimo | Logs adicionales de Reverb |

### Archivos Modificados en el Deployment

- ✅ `app/Events/NotificationCreated.php` - Evento de broadcasting
- ✅ `app/Services/NotificationService.php` - Dispara evento al crear notificación
- ✅ `routes/channels.php` - Autorización de canales privados
- ✅ `config/reverb.php` - Configuración de Reverb (nuevo)
- ✅ `config/broadcasting.php` - Ya tenía configuración de Reverb
- ✅ `docker-compose.yml` - Servicio Reverb agregado
- ✅ `Caddyfile` - Proxy WebSocket configurado

---

## 📚 Referencias

- [Laravel Reverb Documentation](https://laravel.com/docs/reverb)
- [Laravel Broadcasting Documentation](https://laravel.com/docs/broadcasting)
- [Laravel Echo Documentation](https://laravel.com/docs/echo)
- **Arquitectura**: Ver `docs/03-architecture/` para más detalles
- **Estado de implementación**: Ver `docs/07-reference/IMPLEMENTATION_STATUS.md`
- **Configuración de producción**: Ver `infra/docker/environments/production/REVERB_SETUP.md` para detalles específicos del entorno de producción
- **Docker**: Ver `docs/02-deployment/DOCKER.md` para infraestructura Docker
- **Deployment**: Ver `docs/02-deployment/DEPLOYMENT.md` para guía completa de deployment

---

**Última actualización:** 2025-01-21





