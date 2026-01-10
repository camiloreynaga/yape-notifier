# 🌐 Configuración DNS para Staging

Guía para configurar subdominios de staging en tu proveedor DNS.

---

## 📋 Opciones de Configuración

### Opción 1: Subdominios (Recomendado) ⭐

**Ventajas:**
- ✅ URLs profesionales: `staging-api.notificaciones.space`
- ✅ Fácil de recordar
- ✅ Separación clara entre staging y producción
- ✅ Mejor para testing con clientes

**Configuración DNS:**

```
Tipo    Nombre                          Valor                    TTL
A       staging-api                     TU_IP_DEL_SERVIDOR       3600
A       staging-dashboard                TU_IP_DEL_SERVIDOR       3600
```

**Ejemplo con DigitalOcean DNS:**

1. Ve a tu panel de DigitalOcean → Networking → Domains
2. Selecciona `notificaciones.space`
3. Agrega registros A:
   - **Name:** `staging-api`
   - **Value:** `TU_IP_DEL_SERVIDOR` (ej: `157.230.123.45`)
   - **TTL:** `3600`
   
   - **Name:** `staging-dashboard`
   - **Value:** `TU_IP_DEL_SERVIDOR` (misma IP)
   - **TTL:** `3600`

4. Espera 5-10 minutos para propagación DNS

**Verificar DNS:**

```bash
# Verificar que los subdominios apuntan correctamente
dig staging-api.notificaciones.space
dig staging-dashboard.notificaciones.space

# O desde el servidor
nslookup staging-api.notificaciones.space
```

**Actualizar Caddyfile:**

1. Edita `infra/docker/environments/staging/Caddyfile`
2. Descomenta los bloques de subdominios
3. Comenta o elimina el bloque `:80` (puerto directo)
4. Reinicia Caddy: `docker compose --env-file .env restart caddy`

**Actualizar .env:**

```bash
# En infra/docker/environments/staging/.env
APP_URL=http://staging-api.notificaciones.space
DASHBOARD_API_URL=http://staging-api.notificaciones.space
```

---

### Opción 2: Puerto Directo (Actual)

**Ventajas:**
- ✅ No requiere configuración DNS
- ✅ Funciona inmediatamente
- ✅ Útil para testing local

**Desventajas:**
- ❌ URLs menos profesionales: `TU_IP:8080`
- ❌ Requiere recordar la IP
- ❌ No funciona con HTTPS fácilmente

**Configuración:**

Ya está configurado en el Caddyfile actual. Solo necesitas:

```bash
# Acceder a:
http://TU_IP:8080/api/up          # API
http://TU_IP:8080                 # Dashboard
```

**Variables de entorno:**

```bash
# En infra/docker/environments/staging/.env
APP_URL=http://localhost:8080
DASHBOARD_API_URL=http://localhost:8080
```

---

## 🔄 Migrar de Puerto Directo a Subdominios

Si ya tienes staging funcionando con puerto directo y quieres migrar a subdominios:

### Paso 1: Configurar DNS

Sigue las instrucciones de "Opción 1" arriba.

### Paso 2: Actualizar Caddyfile

```bash
cd /var/apps/yape-notifier/infra/docker/environments/staging

# Editar Caddyfile
nano Caddyfile

# Descomentar bloques de subdominios
# Comentar bloque :80
```

### Paso 3: Actualizar .env

```bash
# Editar .env
nano .env

# Cambiar:
APP_URL=http://staging-api.notificaciones.space
DASHBOARD_API_URL=http://staging-api.notificaciones.space
```

### Paso 4: Reiniciar Servicios

```bash
# Reiniciar Caddy para cargar nueva configuración
docker compose --env-file .env restart caddy

# Verificar logs
docker compose --env-file .env logs caddy

# Verificar que funciona
curl http://staging-api.notificaciones.space/up
```

---

## 🔍 Verificación

### Verificar DNS

```bash
# Desde tu máquina local
dig staging-api.notificaciones.space
dig staging-dashboard.notificaciones.space

# Debe mostrar la IP de tu servidor
```

### Verificar Caddy

```bash
# Ver logs de Caddy
docker compose --env-file .env logs caddy

# Verificar que Caddy está escuchando
docker compose --env-file .env exec caddy caddy validate --config /etc/caddy/Caddyfile
```

### Verificar API

```bash
# Probar API
curl http://staging-api.notificaciones.space/up
# O si usas puerto directo:
curl http://TU_IP:8080/api/up
```

### Verificar Dashboard

```bash
# Abrir en navegador
http://staging-dashboard.notificaciones.space
# O si usas puerto directo:
http://TU_IP:8080
```

---

## 🐛 Troubleshooting

### DNS no resuelve

**Síntoma:** `dig staging-api.notificaciones.space` no muestra la IP correcta

**Solución:**
1. Verifica que los registros DNS están configurados correctamente
2. Espera 10-15 minutos para propagación
3. Limpia cache DNS local: `sudo systemd-resolve --flush-caches` (Linux) o reinicia navegador

### Caddy no inicia

**Síntoma:** `docker compose logs caddy` muestra errores

**Solución:**
1. Verifica sintaxis del Caddyfile: `docker compose exec caddy caddy validate --config /etc/caddy/Caddyfile`
2. Verifica que los servicios upstream (nginx-api, dashboard) están corriendo
3. Revisa logs: `docker compose logs caddy`

### 502 Bad Gateway

**Síntoma:** Caddy responde pero muestra 502

**Solución:**
1. Verifica que nginx-api está corriendo: `docker compose ps`
2. Verifica healthcheck: `docker compose exec nginx-api wget -q -O- http://localhost/up`
3. Revisa logs de nginx: `docker compose logs nginx-api`

---

## 📚 Referencias

- [Caddy Documentation](https://caddyserver.com/docs/)
- [DigitalOcean DNS Setup](https://docs.digitalocean.com/products/networking/dns/)
- [DNS Propagation Check](https://www.whatsmydns.net/)

---

**Última actualización:** 2025-01-15

