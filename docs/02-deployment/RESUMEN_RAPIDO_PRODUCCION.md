# ⚡ Resumen Rápido - Producción

> **Referencia rápida para DevOps**
> 
> Para guía completa: ver [GUIA_PRODUCCION_PASO_A_PASO.md](GUIA_PRODUCCION_PASO_A_PASO.md)

---

## 🆕 Despliegue Inicial (Primera Vez)

```bash
# 1. Conectarse al servidor
ssh deploy@tu-servidor
cd /var/apps/yape-notifier/infra/docker/environments/production

# 2. Configurar .env (si no existe)
./setup.sh
nano .env  # Configurar DB_PASSWORD

# 3. Actualizar código
cd /var/apps/yape-notifier
git pull origin main

# 4. Desplegar
cd infra/docker/environments/production
./deploy.sh --no-cache
```

**Tiempo:** 20-30 minutos

---

## 🔄 Actualización (Sistema Existente)

```bash
# 1. Conectarse al servidor
ssh deploy@tu-servidor
cd /var/apps/yape-notifier

# 2. Actualizar código
git pull origin main

# 3. Actualizar sistema (con backup automático)
cd infra/docker/environments/production
./update.sh
```

**Tiempo:** 15-30 minutos

---

## ✅ Verificación Rápida

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Estado de servicios
docker compose --env-file .env ps

# Healthchecks
docker compose --env-file .env ps --format 'table {{.Name}}\t{{.Health}}'

# API
curl -f https://api.notificaciones.space/up

# Migraciones
docker compose --env-file .env exec php-fpm php artisan migrate:status
```

---

## 🔧 Troubleshooting Rápido

| Problema | Solución |
|----------|----------|
| Migraciones desincronizadas | `./fix-migrations.sh` |
| Servicios unhealthy | `./diagnose-health.sh` → `./fix-healthchecks.sh` |
| Error 302 Redirect | `./fix-302-redirect.sh` |
| Error 419 CSRF | Limpiar caches: `docker compose --env-file .env exec php-fpm php artisan config:clear` |
| Error 502 Bad Gateway | `docker compose --env-file .env restart nginx-api php-fpm` |
| Artefactos BuildKit | `./clean-buildkit-artifacts.sh` |

---

## 📋 Checklist Pre-Despliegue

- [ ] Código actualizado (`git pull`)
- [ ] `.env` configurado (especialmente `DB_PASSWORD`)
- [ ] Backup de BD (si es actualización)
- [ ] DNS configurado
- [ ] Puertos 80/443 disponibles

---

## 📝 Comandos Útiles

```bash
# Ver logs
docker compose --env-file .env logs -f [servicio]

# Reiniciar servicio
docker compose --env-file .env restart [servicio]

# Backup manual
docker compose --env-file .env exec -T db pg_dump -U postgres yape_notifier | gzip > backup_$(date +%Y%m%d).sql.gz

# Conectar a BD
docker compose --env-file .env exec -it db psql -U postgres -d yape_notifier
```

---

**Documentación completa:** [GUIA_PRODUCCION_PASO_A_PASO.md](GUIA_PRODUCCION_PASO_A_PASO.md)




