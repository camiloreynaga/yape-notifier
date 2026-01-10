# 🔄 Actualización Parcial - Solo Dashboard o Solo API

> **Referencias relacionadas:**
> - [GUIA_ACTUALIZACION.md](GUIA_ACTUALIZACION.md) - Guía completa de actualización
> - [DEPLOYMENT.md](DEPLOYMENT.md) - Guía completa de despliegue
> - [DEPLOYMENT_COMMANDS.md](DEPLOYMENT_COMMANDS.md) - Comandos rápidos de despliegue

Guía para actualizar solo un servicio específico sin reconstruir todo el proyecto.

---

## 📋 ¿Cuándo reconstruir solo el Dashboard?

**Reconstruye SOLO el dashboard cuando:**
- ✅ Solo cambiaste código del frontend (React/TypeScript)
- ✅ Solo corregiste errores de TypeScript en el dashboard
- ✅ Solo actualizaste estilos o componentes del dashboard
- ✅ No hay cambios en la API (Laravel)
- ✅ No hay cambios en dependencias de la API (composer.json)
- ✅ No hay nuevas migraciones de base de datos

**Ejemplo de tu caso actual:**
- Corregiste errores de TypeScript en el dashboard
- No hay cambios en la API
- ✅ **Puedes reconstruir solo el dashboard**

---

## 📋 ¿Cuándo reconstruir TODO el proyecto?

**Reconstruye TODO cuando:**
- ✅ Cambiaste código de la API (Laravel)
- ✅ Cambiaste dependencias de la API (composer.json)
- ✅ Hay nuevas migraciones de base de datos
- ✅ Cambiaste configuración de Docker (Dockerfile, docker-compose.yml)
- ✅ Cambiaste variables de entorno críticas
- ✅ Cambiaste código de Reverb (WebSocket server)
- ✅ Quieres asegurar consistencia total

---

## 🎯 Opción 1: Reconstruir SOLO el Dashboard (Recomendado para tu caso)

### Paso 1: Actualizar código en el servidor

```bash
# Conectarse al servidor
ssh deploy@tu-servidor

# Ir al directorio del proyecto
cd /var/apps/yape-notifier

# Actualizar código desde el repositorio
git pull origin tenant-version
```

### Paso 2: Ir al directorio de producción

```bash
cd infra/docker/environments/production
```

### Paso 3: Reconstruir solo el servicio dashboard

```bash
# Habilitar BuildKit para cache optimizado
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1

# Reconstruir solo el servicio dashboard (sin cache para asegurar cambios)
docker compose --env-file .env build --no-cache dashboard

# Reiniciar solo el servicio dashboard
docker compose --env-file .env up -d --no-deps dashboard
```

**Explicación:**
- `--no-cache`: Fuerza reconstrucción completa (asegura que los cambios se apliquen)
- `--no-deps`: No reinicia dependencias (nginx-api, caddy, etc.)
- Solo reconstruye y reinicia el contenedor del dashboard

### Paso 4: Verificar que funciona

```bash
# Ver estado del dashboard
docker compose --env-file .env ps dashboard

# Ver logs del dashboard
docker compose --env-file .env logs -f dashboard

# Probar que el dashboard responde
curl -f https://dashboard.notificaciones.space/health
```

**Tiempo estimado:** 2-5 minutos (vs 10-15 minutos reconstruyendo todo)

---

## 🎯 Opción 2: Reconstruir TODO el proyecto (Usar update.sh)

Si prefieres usar el script completo (con backup automático):

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# El script reconstruye TODO, pero es más seguro
./update.sh
```

**Ventajas:**
- ✅ Backup automático de la base de datos
- ✅ Limpieza de caches de Laravel
- ✅ Verificación de healthchecks
- ✅ Script de rollback automático

**Desventajas:**
- ⏱️ Más lento (reconstruye todos los servicios)
- ⚠️ Reinicia todos los servicios (puede causar downtime breve)

---

## 🔧 Opción 3: Reconstruir solo Dashboard con Script Personalizado

Puedes crear un script rápido para actualizar solo el dashboard:

```bash
# Crear script: update-dashboard.sh
cat > update-dashboard.sh << 'EOF'
#!/bin/bash
set -e

export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1

echo "🔄 Reconstruyendo dashboard..."
docker compose --env-file .env build --no-cache dashboard

echo "🚀 Reiniciando dashboard..."
docker compose --env-file .env up -d --no-deps dashboard

echo "✅ Dashboard actualizado"
echo "📊 Verificando estado..."
docker compose --env-file .env ps dashboard
EOF

chmod +x update-dashboard.sh

# Ejecutar
./update-dashboard.sh
```

---

## ⚡ Comparación de Tiempos

| Método | Tiempo | Servicios afectados | Downtime |
|--------|--------|---------------------|----------|
| Solo dashboard | 2-5 min | Solo dashboard | Mínimo (< 10s) |
| update.sh (todo) | 10-15 min | Todos | Mínimo (< 30s) |
| deploy.sh (todo) | 15-20 min | Todos | Mayor (1-2 min) |

---

## 🚨 ¿Qué pasa si solo reconstruyo el dashboard?

**Si solo reconstruyes el dashboard:**
- ✅ El dashboard se actualiza con los nuevos cambios
- ✅ La API sigue funcionando normalmente
- ✅ La base de datos no se toca
- ✅ Reverb (WebSocket) sigue funcionando
- ✅ No hay downtime en la API

**IMPORTANTE:** Si los cambios del dashboard requieren cambios en la API (nuevos endpoints, cambios en autenticación, etc.), entonces SÍ debes reconstruir todo.

---

## ✅ Recomendación para tu caso actual

**Como solo corregiste errores de TypeScript en el dashboard:**

```bash
# En el servidor
cd /var/apps/yape-notifier
git pull origin tenant-version

cd infra/docker/environments/production

# Reconstruir solo dashboard
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1
docker compose --env-file .env build --no-cache dashboard
docker compose --env-file .env up -d --no-deps dashboard

# Verificar
docker compose --env-file .env ps dashboard
docker compose --env-file .env logs dashboard
```

**Tiempo total:** ~3-5 minutos  
**Downtime:** < 10 segundos (solo el dashboard)

---

## 📝 Checklist para Actualización Parcial del Dashboard

- [ ] Código actualizado en el servidor (`git pull`)
- [ ] No hay cambios en la API que afecten al dashboard
- [ ] No hay nuevas migraciones de base de datos
- [ ] Reconstruir solo dashboard (`build --no-cache dashboard`)
- [ ] Reiniciar solo dashboard (`up -d --no-deps dashboard`)
- [ ] Verificar que el dashboard responde
- [ ] Verificar que no hay errores en logs

---

## 🔄 Si algo sale mal

Si el dashboard no funciona después de la actualización parcial:

```bash
# Ver logs detallados
docker compose --env-file .env logs dashboard

# Reconstruir con más información
docker compose --env-file .env build --progress=plain dashboard

# Si persiste el problema, reconstruir todo con update.sh
./update.sh
```

---

## 📚 Referencias

- **Guía completa de actualización**: `docs/02-deployment/GUIA_ACTUALIZACION.md`
- **Script de actualización completo**: `infra/docker/environments/production/update.sh`
- **Docker Compose**: `infra/docker/environments/production/docker-compose.yml`


