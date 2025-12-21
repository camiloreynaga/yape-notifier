# Configuración de WebSockets para Notificaciones en Tiempo Real

## 📋 Resumen

Este documento describe la implementación de WebSockets usando Laravel Reverb para notificaciones en tiempo real en Yape Notifier.

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

## 🔧 Configuración

### Archivos Creados/Modificados

1. **`config/broadcasting.php`** - Configuración de broadcasting
2. **`app/Events/NotificationCreated.php`** - Evento de broadcasting
3. **`routes/channels.php`** - Autorización de canales privados
4. **`app/Services/NotificationService.php`** - Dispara evento al crear notificación
5. **`bootstrap/app.php`** - Registra rutas de broadcasting

### Canales Privados

Las notificaciones se transmiten en canales privados por `commerce_id`:

- **Canal:** `private-commerce.{commerce_id}`
- **Autorización:** Solo usuarios del mismo `commerce_id` pueden escuchar

## 🏃 Ejecutar Servidor WebSocket

### Desarrollo

```bash
php artisan reverb:start
```

El servidor se iniciará en `http://127.0.0.1:8080` (o el puerto configurado).

### Producción (con Supervisor)

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

## 📡 Uso en el Frontend

### Instalación (React/TypeScript)

```bash
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
  // Ejemplo: agregar a la lista, mostrar notificación, etc.
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

## 🔐 Autenticación de Canales

La autenticación de canales privados se maneja automáticamente mediante:

1. **Sanctum Token:** El frontend envía el token en el header `Authorization`
2. **Autorización:** `routes/channels.php` verifica que el usuario pertenezca al commerce

### Endpoint de Autenticación

Laravel automáticamente expone `/api/broadcasting/auth` para autenticar suscripciones a canales privados.

## 📊 Flujo de Datos

1. **Android App** envía notificación → `POST /api/notifications`
2. **NotificationController** → `NotificationService::createNotification()`
3. **NotificationService** crea notificación y dispara `NotificationCreated` event
4. **Laravel Reverb** transmite evento a clientes conectados
5. **Frontend** recibe evento en tiempo real y actualiza UI

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

## 📝 Notas Importantes

1. **Rate Limiting:** Considerar implementar rate limiting para eventos si hay muchos clientes
2. **Redis:** Para producción con múltiples servidores, usar Redis como driver de broadcasting
3. **SSL/TLS:** En producción, usar HTTPS/WSS para seguridad
4. **Escalabilidad:** Reverb puede manejar miles de conexiones simultáneas
5. **Reconexión:** Laravel Echo maneja automáticamente la reconexión

## 🔄 Actualización de Configuración

Si necesitas cambiar la configuración:

1. Actualizar `.env`
2. Reiniciar servidor Reverb: `php artisan reverb:restart` (si está en supervisor)
3. Recargar frontend para obtener nuevas variables de entorno

## 📚 Referencias

- [Laravel Reverb Documentation](https://laravel.com/docs/reverb)
- [Laravel Broadcasting Documentation](https://laravel.com/docs/broadcasting)
- [Laravel Echo Documentation](https://laravel.com/docs/echo)

---

**Última actualización:** 2025-01-21

