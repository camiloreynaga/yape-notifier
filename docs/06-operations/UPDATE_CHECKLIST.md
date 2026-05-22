# ✅ Checklist de Actualización - Cambios Pendientes

## 📋 Resumen de Cambios en Infra

### 1. ✅ Caddyfile - Headers para HTTPS
**Archivo:** `Caddyfile`
**Cambio:** Agregados headers `X-Forwarded-Proto` y `X-Forwarded-Host` para resolver problema del 302 redirect.

**Estado:** ✅ Ya aplicado en el código

---

### 2. ✅ Nginx API Config - Headers para PHP-FPM
**Archivo:** `infra/docker/configs/nginx/api.conf`
**Cambio:** Agregados headers `X-Forwarded-Proto`, `X-Forwarded-For`, `X-Forwarded-Host` para que Laravel detecte HTTPS correctamente.

**Estado:** ✅ Ya aplicado en el código

---

### 3. ✅ Docker Compose - Variables de Reverb para Dashboard
**Archivo:** `docker-compose.yml`
**Cambio:** El dashboard ahora usa variables `REVERB_HOST_PUBLIC` y `REVERB_SCHEME_PUBLIC` del `.env`.

**Estado:** ✅ Ya aplicado en el código

---

### 4. ✅ Scripts de Utilidad
**Archivos nuevos:**
- `fix-302-redirect.sh` - Resuelve problema del 302 redirect
- `fix-migrations.sh` - Sincroniza migraciones desincronizadas
- `diagnose-health.sh` - Diagnostica servicios unhealthy
- `fix-healthchecks.sh` - Intenta reparar servicios unhealthy
- `clean-buildkit-artifacts.sh` - Limpia artefactos de BuildKit
- `quick-deploy.sh` - Despliegue automatizado

**Estado:** ✅ Ya creados en el código

---

## 🔄 Pasos para Actualizar el Servidor

### Paso 1: Actualizar Código

```bash
# Conectarse al servidor
ssh deploy@Server-notifier

# Ir al directorio del proyecto
cd /var/apps/yape-notifier

# Actualizar código
git pull origin tenant-version
# O si estás en main:
# git pull origin main
```

---

### Paso 2: Actualizar Variables de Entorno

**Editar `.env` en el servidor:**

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Editar .env
nano .env
```

**Agregar estas variables (si no existen):**

```env
# ============================================
# Dashboard - Variables de Entorno para Build
# ============================================
DASHBOARD_API_URL=https://api.notificaciones.space

# Variables de Reverb para WebSocket (Dashboard)
# IMPORTANTE: REVERB_HOST_PUBLIC y REVERB_SCHEME_PUBLIC son para el Dashboard
# REVERB_HOST y REVERB_SCHEME son para el Backend (ya están configurados)
REVERB_HOST_PUBLIC=api.notificaciones.space
REVERB_SCHEME_PUBLIC=https
```

**Verificar que estas variables estén correctas:**

```env
# Backend (ya deberían estar)
REVERB_HOST=0.0.0.0
REVERB_PORT=8080
REVERB_SCHEME=http
REVERB_APP_KEY=base64:7IFJN3FFdqSdv3nAdxoRmjMxzI5jqDIG33VJ7XC4LOk
REVERB_APP_SECRET=1771fded8db62696cfa7a92461511e22
BROADCAST_CONNECTION=reverb

# Dashboard (nuevas)
REVERB_HOST_PUBLIC=api.notificaciones.space
REVERB_SCHEME_PUBLIC=https
DASHBOARD_API_URL=https://api.notificaciones.space
```

---

### Paso 3: Hacer Ejecutables los Scripts

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

chmod +x fix-302-redirect.sh
chmod +x fix-migrations.sh
chmod +x diagnose-health.sh
chmod +x fix-healthchecks.sh
chmod +x clean-buildkit-artifacts.sh
chmod +x quick-deploy.sh
chmod +x deploy.sh
chmod +x update.sh
```

---

### Paso 4: Actualizar Servicios

**Opción A: Usar script de actualización (recomendado - con backup)**

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# El script hace backup automático antes de actualizar
./update.sh
```

**Opción B: Despliegue completo (si prefieres rebuild completo)**

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Limpiar artefactos
./clean-buildkit-artifacts.sh

# Desplegar
./deploy.sh --no-cache
```

---

### Paso 5: Resolver Problemas Comunes

**Si hay error 302 redirect:**

```bash
./fix-302-redirect.sh
```

**Si hay migraciones desincronizadas:**

```bash
./fix-migrations.sh
```

**Si hay servicios unhealthy:**

```bash
./diagnose-health.sh
./fix-healthchecks.sh
```

---

### Paso 6: Verificar

```bash
# Ver estado de servicios
docker compose --env-file .env ps

# Verificar healthchecks
docker compose --env-file .env ps --format 'table {{.Name}}\t{{.Status}}\t{{.Health}}'

# Verificar API
curl -f https://api.notificaciones.space/up && echo "✅ API OK" || echo "❌ API Error"

# Probar login (no debe devolver 302)
curl -X POST https://api.notificaciones.space/api/login \
  -H "Content-Type: application/json" \
  -H "Origin: https://dashboard.notificaciones.space" \
  -d '{"email":"test@example.com","password":"test123"}' \
  -w "\nHTTP Status: %{http_code}\n"
```

---

## 📝 Resumen de Variables de Entorno

### Backend (Laravel API)

```env
# Comunicación INTERNA en Docker
REVERB_HOST=0.0.0.0          # Escuchar en todos los interfaces
REVERB_PORT=8080
REVERB_SCHEME=http           # HTTP interno (Caddy maneja HTTPS)
REVERB_APP_KEY=base64:...
REVERB_APP_SECRET=...
BROADCAST_CONNECTION=reverb
```

### Dashboard (Frontend)

```env
# Comunicación EXTERNA desde navegador
REVERB_HOST_PUBLIC=api.notificaciones.space  # Dominio público
REVERB_SCHEME_PUBLIC=https                  # HTTPS externo
DASHBOARD_API_URL=https://api.notificaciones.space
```

**Estas variables se mapean automáticamente en `docker-compose.yml`:**
- `REVERB_HOST_PUBLIC` → `VITE_REVERB_HOST`
- `REVERB_SCHEME_PUBLIC` → `VITE_REVERB_SCHEME`

---

## ⚠️ Importante

1. **No cambiar `REVERB_SCHEME=http` en el backend** - Es correcto para comunicación interna
2. **No cambiar `REVERB_SCHEME_PUBLIC=https` en el dashboard** - Es correcto para comunicación externa
3. **Las variables `REVERB_HOST_PUBLIC` y `REVERB_SCHEME_PUBLIC` son solo para el dashboard** - No afectan al backend
4. **Después de actualizar `.env`, debes reconstruir el dashboard** - Las variables se inyectan en build time

---

## ✅ Checklist Final

- [ ] Código actualizado (`git pull`)
- [ ] Variables de entorno actualizadas (`.env`)
- [ ] Scripts ejecutables (`chmod +x`)
- [ ] Servicios actualizados (`./update.sh` o `./deploy.sh`)
- [ ] Problema 302 resuelto (`./fix-302-redirect.sh` si es necesario)
- [ ] Migraciones sincronizadas (`./fix-migrations.sh` si es necesario)
- [ ] Servicios verificados (`docker compose ps`)
- [ ] API responde correctamente (`curl https://api.notificaciones.space/up`)

---

## 🆘 Si Algo Sale Mal

1. **Ver logs:**
   ```bash
   docker compose --env-file .env logs -f
   ```

2. **Diagnosticar:**
   ```bash
   ./diagnose-health.sh
   ```

3. **Rollback (si usaste update.sh):**
   ```bash
   # El script de rollback está en backups/
   ./backups/rollback_YYYYMMDD_HHMMSS.sh
   ```

4. **Reconstruir desde cero:**
   ```bash
   docker compose --env-file .env down
   ./deploy.sh --no-cache
   ```

