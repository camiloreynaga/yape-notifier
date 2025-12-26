# 🚀 Deploy de WebSockets con Laravel Reverb

## 📋 Resumen Ejecutivo

Esta guía detalla el impacto de la implementación de WebSockets usando Laravel Reverb y el proceso de deploy en producción usando Docker.

## 🔍 Impacto de las Nuevas Implementaciones

### 1. **Nuevos Componentes del Sistema**

#### **Servicio Reverb (WebSocket Server)**
- **Qué es**: Servidor WebSocket nativo de Laravel que maneja conexiones en tiempo real
- **Impacto**: Nuevo servicio que debe ejecutarse de forma continua
- **Recursos**: Consume CPU y memoria para mantener conexiones WebSocket activas
- **Puerto**: Requiere un puerto adicional (por defecto 8080) expuesto internamente

#### **Broadcasting de Eventos**
- **Qué es**: Sistema que transmite eventos de Laravel a clientes conectados vía WebSocket
- **Impacto**: Cada notificación creada ahora dispara un evento broadcast
- **Rendimiento**: Mínimo impacto en la creación de notificaciones (operación asíncrona)
- **Escalabilidad**: Requiere considerar el número de conexiones simultáneas

#### **Autenticación de Canales Privados**
- **Qué es**: Sistema que valida que usuarios solo escuchen canales de su commerce
- **Impacto**: Endpoint adicional `/api/broadcasting/auth` que valida tokens Sanctum
- **Seguridad**: Asegura aislamiento multi-tenant a nivel de WebSocket

### 2. **Cambios en la Infraestructura**

#### **Antes (Sin WebSockets)**
```
Cliente → Caddy → Nginx → PHP-FPM → PostgreSQL
         (HTTPS)   (HTTP)   (FastCGI)
```

#### **Después (Con WebSockets)**
```
Cliente → Caddy → Nginx → PHP-FPM → PostgreSQL
         (HTTPS)   (HTTP)   (FastCGI)
         
Cliente → Caddy → Reverb → PHP-FPM (para auth)
         (WSS)     (WS)      (HTTP)
```

### 3. **Impacto en Recursos**

| Recurso | Impacto | Notas |
|---------|---------|-------|
| **CPU** | +5-10% | Reverb consume CPU para mantener conexiones |
| **Memoria** | +50-100MB | Por cada conexión WebSocket activa (~1-2MB) |
| **Red** | Variable | Depende del número de conexiones simultáneas |
| **Puertos** | +1 puerto | Puerto 8080 (interno) para Reverb |
| **Disco** | Mínimo | Logs adicionales de Reverb |

### 4. **Impacto en el Código**

#### **Archivos Modificados**
- ✅ `app/Events/NotificationCreated.php` - Evento de broadcasting
- ✅ `app/Services/NotificationService.php` - Dispara evento al crear notificación
- ✅ `routes/channels.php` - Autorización de canales privados
- ✅ `config/reverb.php` - Configuración de Reverb (nuevo)
- ✅ `config/broadcasting.php` - Ya tenía configuración de Reverb

#### **Archivos Nuevos**
- ✅ `tests/Feature/NotificationBroadcastingTest.php` - Tests de broadcasting

#### **Sin Cambios Necesarios**
- ✅ `bootstrap/app.php` - Laravel registra rutas de broadcasting automáticamente
- ✅ `routes/api.php` - No requiere cambios (auth manejado por Laravel)

### 5. **Impacto en el Frontend**

El frontend necesita:
- Instalar `laravel-echo` y `pusher-js`
- Configurar conexión WebSocket
- Suscribirse a canales privados
- Manejar reconexión automática

**Nota**: Esto se implementará en el frontend, no afecta el deploy del backend.

---

## 🐳 Deploy en Docker - Guía Completa

### **Paso 1: Actualizar Variables de Entorno**

Agregar al archivo `.env` en `infra/docker/environments/production/.env`:

```env
# ============================================
# Laravel Reverb WebSocket Server
# ============================================
REVERB_APP_ID=yape-notifier
REVERB_APP_KEY=base64:tu-key-generada-aqui
REVERB_APP_SECRET=tu-secret-generado-aqui
REVERB_HOST=0.0.0.0
REVERB_PORT=8080
REVERB_SCHEME=http

# Broadcasting
BROADCAST_CONNECTION=reverb
```

**⚠️ IMPORTANTE**: 
- `REVERB_APP_KEY` y `REVERB_APP_SECRET` deben generarse con `php artisan reverb:install`
- En producción, `REVERB_SCHEME` puede ser `https` si se configura SSL para WebSockets
- `REVERB_HOST=0.0.0.0` permite conexiones desde cualquier interfaz (necesario en Docker)

### **Paso 2: Generar Keys de Reverb**

**Opción A: Generar en desarrollo y copiar**
```bash
cd apps/api
php artisan reverb:install
# Copiar REVERB_APP_KEY y REVERB_APP_SECRET al .env de producción
```

**Opción B: Generar en contenedor después del deploy**
```bash
# Después del primer deploy, ejecutar:
docker compose --env-file .env exec php-fpm php artisan reverb:install
# Copiar las keys generadas al .env y hacer redeploy
```

### **Paso 3: Actualizar docker-compose.yml**

Agregar el servicio Reverb al archivo `infra/docker/environments/production/docker-compose.yml`:

```yaml
services:
  # ... servicios existentes ...

  # ============================================
  # Reverb WebSocket Server
  # ============================================
  reverb:
    build:
      context: ../../../../apps/api
      dockerfile: ../../infra/docker/dockerfiles/Dockerfile.php-fpm
    container_name: yape-notifier-reverb-prod
    restart: always
    working_dir: /var/www
    command: php artisan reverb:start --host=0.0.0.0 --port=8080
    volumes:
      - ../../configs/php/production.ini:/usr/local/etc/php/conf.d/production.ini:ro
    env_file:
      - .env
    environment:
      - APP_ENV=production
      - APP_DEBUG=false
      - APP_URL=${APP_URL:-https://api.notificaciones.space}
    networks:
      - yape-network-prod
    depends_on:
      db:
        condition: service_healthy
      php-fpm:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "php -r 'exit(0);'"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    labels:
      - "com.yape-notifier.service=reverb"
      - "com.yape-notifier.environment=production"
    deploy:
      resources:
        limits:
          cpus: "1"
          memory: 512M
        reservations:
          cpus: "0.25"
          memory: 128M
```

**Notas importantes**:
- Usa el mismo Dockerfile que `php-fpm` (comparte código y dependencias)
- El comando `reverb:start` reemplaza `php-fpm -F`
- Depende de `php-fpm` para asegurar que las migraciones estén ejecutadas
- Healthcheck simple (Reverb no expone endpoint HTTP para healthcheck)

### **Paso 4: Actualizar Caddyfile para WebSockets**

Modificar `infra/docker/environments/production/Caddyfile`:

```caddy
# API - api.notificaciones.space
api.notificaciones.space {
    # Reverse proxy a Nginx API
    reverse_proxy nginx-api:80 {
        header_up X-Real-IP {remote_host}
        header_up Host {host}
        
        health_uri /up
        health_interval 30s
        health_timeout 5s
    }

    # WebSocket proxy para Reverb
    # Ruta específica para WebSocket connections
    handle /app/* {
        reverse_proxy reverb:8080 {
            # Headers necesarios para WebSocket
            header_up Connection {>Connection}
            header_up Upgrade {>Upgrade}
            header_up X-Real-IP {remote_host}
            header_up Host {host}
            
            # Timeout largo para mantener conexiones WebSocket
            transport http {
                dial_timeout 30s
                response_header_timeout 30s
            }
        }
    }

    # Logging
    log {
        output file /var/log/caddy/api.log
        format json
    }

    # Compression
    encode gzip zstd
}

# Dashboard - dashboard.notificaciones.space
dashboard.notificaciones.space {
    # Reverse proxy a Dashboard
    reverse_proxy dashboard:80 {
        header_up X-Real-IP {remote_host}
        header_up Host {host}
        
        health_uri /
        health_interval 30s
        health_timeout 5s
    }

    # Logging
    log {
        output file /var/log/caddy/dashboard.log
        format json
    }

    # Compression
    encode gzip zstd
}
```

**Explicación**:
- La ruta `/app/*` es el endpoint estándar de Laravel Reverb para conexiones WebSocket
- Caddy maneja automáticamente la actualización de HTTP a WebSocket (Upgrade header)
- Timeouts largos permiten mantener conexiones WebSocket persistentes

### **Paso 5: Actualizar Script de Deploy**

Modificar `infra/docker/environments/production/deploy.sh` para incluir validación de Reverb:

Agregar después del PASO 11 (antes de "Verificar estado"):

```bash
# PASO 12: Verificar que Reverb puede iniciar (validación)
info "Verificando configuración de Reverb..."
if docker compose --env-file .env exec -T php-fpm php artisan reverb:install --check 2>/dev/null; then
    info "✅ Configuración de Reverb válida"
else
    warn "⚠️  Reverb no está completamente configurado"
    warn "Ejecuta: docker compose --env-file .env exec php-fpm php artisan reverb:install"
    warn "Luego actualiza REVERB_APP_KEY y REVERB_APP_SECRET en .env"
fi

# PASO 13: Verificar estado de Reverb
info "Verificando estado del servicio Reverb..."
if docker compose --env-file .env ps reverb | grep -q "Up"; then
    info "✅ Servicio Reverb está corriendo"
else
    warn "⚠️  Servicio Reverb no está corriendo"
    warn "Revisa los logs: docker compose --env-file .env logs reverb"
fi
```

### **Paso 6: Proceso de Deploy Completo**

```bash
# 1. Ir al directorio de producción
cd infra/docker/environments/production

# 2. Verificar que .env tiene las variables de Reverb
grep -q "REVERB_APP_KEY" .env || echo "⚠️  REVERB_APP_KEY no configurado"

# 3. Si es el primer deploy con Reverb, generar keys
if ! grep -q "REVERB_APP_KEY=base64:" .env; then
    echo "Generando keys de Reverb..."
    # Opción: generar en contenedor temporal
    docker run --rm -v $(pwd)/../../../../apps/api:/app -w /app \
        php:8.2-cli sh -c "curl -sS https://getcomposer.org/installer | php && \
        php composer.phar install --no-dev --optimize-autoloader && \
        php artisan reverb:install"
    # Luego copiar las keys al .env
fi

# 4. Ejecutar deploy normal
./deploy.sh

# 5. Verificar que Reverb está corriendo
docker compose --env-file .env ps reverb

# 6. Ver logs de Reverb
docker compose --env-file .env logs -f reverb

# 7. Probar conexión WebSocket (desde el servidor)
# Instalar wscat: npm install -g wscat
# wscat -c "wss://api.notificaciones.space/app/ws?protocol=7&client=js&version=8.4.0&flash=false"
```

### **Paso 7: Verificación Post-Deploy**

#### **Verificar que Reverb está corriendo**:
```bash
docker compose --env-file .env ps reverb
# Debe mostrar "Up" y puerto 8080

docker compose --env-file .env logs reverb
# Debe mostrar logs de inicio sin errores
```

#### **Verificar que Caddy está proxyando WebSockets**:
```bash
# Ver logs de Caddy
docker compose --env-file .env logs caddy | grep -i websocket

# Probar conexión WebSocket (requiere herramienta externa)
# Desde el frontend, verificar que puede conectarse
```

#### **Verificar que las notificaciones se broadcast**:
```bash
# Crear una notificación de prueba y verificar logs
docker compose --env-file .env logs reverb -f
# En otra terminal, crear notificación vía API
# Debe aparecer evento broadcast en logs
```

---

## 🔧 Configuración Avanzada

### **Usar Redis para Broadcasting (Recomendado para Producción)**

Si tienes muchos eventos o necesitas mejor rendimiento:

1. **Agregar servicio Redis a docker-compose.yml**:
```yaml
redis:
  image: redis:7-alpine
  container_name: yape-notifier-redis-prod
  restart: always
  networks:
    - yape-network-prod
  volumes:
    - redis_data_prod:/data
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
    interval: 10s
    timeout: 5s
    retries: 5
```

2. **Actualizar .env**:
```env
BROADCAST_CONNECTION=redis
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=null
```

3. **Instalar predis** (si no está):
```bash
cd apps/api
composer require predis/predis
```

### **Configurar SSL para WebSockets (WSS)**

Para usar `wss://` en lugar de `ws://`:

1. **Actualizar Caddyfile**:
```caddy
handle /app/* {
    reverse_proxy reverb:8080 {
        # ... configuración existente ...
        
        # Forzar TLS (opcional, Caddy lo maneja automáticamente)
        header_up X-Forwarded-Proto https
    }
}
```

2. **Actualizar .env**:
```env
REVERB_SCHEME=https
REVERB_PORT=443
```

**Nota**: Caddy maneja SSL automáticamente, pero Reverb internamente usa HTTP.

### **Monitoreo y Logs**

#### **Logs de Reverb**:
```bash
# Ver logs en tiempo real
docker compose --env-file .env logs -f reverb

# Ver últimas 100 líneas
docker compose --env-file .env logs --tail=100 reverb

# Exportar logs
docker compose --env-file .env logs reverb > reverb-$(date +%Y%m%d).log
```

#### **Métricas de Conexiones**:
Reverb no expone métricas HTTP por defecto. Para monitorear:
- Ver logs de conexiones/desconexiones
- Usar `docker stats` para recursos del contenedor
- Implementar endpoint de métricas personalizado si es necesario

---

## 🚨 Troubleshooting

### **Problema: Reverb no inicia**

**Síntomas**: Contenedor se reinicia constantemente

**Solución**:
```bash
# Ver logs detallados
docker compose --env-file .env logs reverb

# Verificar variables de entorno
docker compose --env-file .env exec reverb env | grep REVERB

# Verificar que las keys están configuradas
docker compose --env-file .env exec reverb php artisan reverb:install --check
```

### **Problema: WebSockets no conectan desde el frontend**

**Síntomas**: Frontend no puede establecer conexión WebSocket

**Solución**:
1. Verificar que Caddy está proxyando correctamente:
```bash
docker compose --env-file .env logs caddy | grep -i websocket
```

2. Verificar que Reverb está escuchando:
```bash
docker compose --env-file .env exec reverb netstat -tuln | grep 8080
```

3. Verificar configuración de Caddyfile (ruta `/app/*`)

4. Verificar que el frontend usa la URL correcta:
```javascript
// Debe ser: wss://api.notificaciones.space/app/ws
// NO: ws://api.notificaciones.space:8080
```

### **Problema: Eventos no se broadcast**

**Síntomas**: Notificaciones se crean pero no llegan vía WebSocket

**Solución**:
1. Verificar que `BROADCAST_CONNECTION=reverb` en .env
2. Verificar logs de Reverb al crear notificación:
```bash
docker compose --env-file .env logs reverb -f
# Crear notificación y ver si aparece evento
```

3. Verificar que el evento se dispara:
```bash
docker compose --env-file .env exec php-fpm php artisan tinker
# En tinker:
broadcast(new App\Events\NotificationCreated(App\Models\Notification::first()));
```

### **Problema: Alto uso de memoria**

**Síntomas**: Contenedor Reverb consume mucha memoria

**Solución**:
1. Reducir límite de conexiones en configuración de Reverb
2. Implementar desconexión automática de clientes inactivos
3. Considerar usar Redis para broadcasting (reduce carga en Reverb)

---

## ✅ Checklist de Deploy

- [ ] Variables de entorno de Reverb agregadas al `.env`
- [ ] Keys de Reverb generadas (`REVERB_APP_KEY` y `REVERB_APP_SECRET`)
- [ ] Servicio `reverb` agregado a `docker-compose.yml`
- [ ] `Caddyfile` actualizado con proxy para `/app/*`
- [ ] Script de deploy actualizado (opcional, para validación)
- [ ] Deploy ejecutado: `./deploy.sh`
- [ ] Contenedor Reverb está corriendo: `docker compose ps reverb`
- [ ] Logs de Reverb sin errores: `docker compose logs reverb`
- [ ] Caddy está proxyando WebSockets (verificar logs)
- [ ] Frontend puede conectarse (probar desde navegador)
- [ ] Notificaciones se broadcast correctamente (crear notificación de prueba)
- [ ] Monitoreo configurado (logs, métricas)

---

## 📊 Estimación de Recursos Adicionales

Para un deployment típico:

| Métrica | Valor Estimado |
|---------|----------------|
| **Memoria adicional** | 128-512 MB (depende de conexiones) |
| **CPU adicional** | 0.25-1 core |
| **Puerto adicional** | 8080 (interno) |
| **Conexiones simultáneas** | 50-200 usuarios activos |
| **Ancho de banda** | ~1-5 KB/s por conexión activa |

---

## 🔄 Rollback Plan

Si necesitas hacer rollback:

1. **Remover servicio Reverb**:
```bash
docker compose --env-file .env stop reverb
docker compose --env-file .env rm reverb
```

2. **Revertir Caddyfile** (quitar sección `/app/*`)

3. **Revertir docker-compose.yml** (quitar servicio `reverb`)

4. **Redeploy**:
```bash
./deploy.sh
```

**Nota**: El código de broadcasting seguirá funcionando pero los eventos no se transmitirán (fallan silenciosamente gracias al try-catch en NotificationService).

---

## 📚 Referencias

- [Laravel Reverb Documentation](https://laravel.com/docs/reverb)
- [Laravel Broadcasting Documentation](https://laravel.com/docs/broadcasting)
- [Caddy WebSocket Proxy](https://caddyserver.com/docs/quick-starts/reverse-proxy)
- Documentación interna: `docs/05-features/WEBSOCKETS.md`



