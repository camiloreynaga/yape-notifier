# 💾 Backup y Disaster Recovery

Guía completa para realizar backups y recuperación de desastres en Yape Notifier.

## 📋 Estrategia de Backup

### Base de Datos PostgreSQL

#### Backup Manual

```bash
# Ir al directorio de producción
cd /var/apps/yape-notifier/infra/docker/environments/production

# Backup de base de datos (sin comprimir)
docker compose --env-file .env exec -T db pg_dump -U postgres yape_notifier > backup_$(date +%Y%m%d_%H%M%S).sql

# Backup comprimido (recomendado para bases de datos grandes)
docker compose --env-file .env exec -T db pg_dump -U postgres yape_notifier | gzip > backup_$(date +%Y%m%d_%H%M%S).sql.gz

# Verificar tamaño del backup
ls -lh backup_*.sql*
```

#### Restaurar Backup

```bash
# Restaurar backup sin comprimir
docker compose --env-file .env exec -T db psql -U postgres yape_notifier < backup_20241213_120000.sql

# Restaurar backup comprimido
gunzip < backup_20241213_120000.sql.gz | docker compose --env-file .env exec -T db psql -U postgres yape_notifier

# Nota: La restauración sobrescribirá los datos existentes. Usar con precaución.
```

#### Backup Automatizado (Recomendado)

Crear script `/var/apps/yape-notifier/backup.sh`:

```bash
#!/bin/bash
set -e

# Configuración
BACKUP_DIR="/var/backups/yape-notifier"
PROJECT_DIR="/var/apps/yape-notifier/infra/docker/environments/production"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

# Crear directorio de backups si no existe
mkdir -p "$BACKUP_DIR"

# Cambiar al directorio del proyecto
cd "$PROJECT_DIR"

# Realizar backup comprimido
echo "Iniciando backup de base de datos..."
docker compose --env-file .env exec -T db pg_dump -U postgres yape_notifier | gzip > "$BACKUP_DIR/backup_$DATE.sql.gz"

# Verificar que el backup se creó correctamente
if [ -f "$BACKUP_DIR/backup_$DATE.sql.gz" ]; then
    echo "✅ Backup creado: backup_$DATE.sql.gz"
    echo "Tamaño: $(du -h "$BACKUP_DIR/backup_$DATE.sql.gz" | cut -f1)"
else
    echo "❌ Error: No se pudo crear el backup"
    exit 1
fi

# Eliminar backups más antiguos que RETENTION_DAYS
echo "Eliminando backups más antiguos de $RETENTION_DAYS días..."
find "$BACKUP_DIR" -name "backup_*.sql.gz" -mtime +$RETENTION_DAYS -delete

# Mostrar backups restantes
echo "Backups restantes:"
ls -lh "$BACKUP_DIR"/backup_*.sql.gz 2>/dev/null || echo "No hay backups anteriores"

echo "✅ Proceso de backup completado"
```

Hacer el script ejecutable:

```bash
chmod +x /var/apps/yape-notifier/backup.sh
```

#### Configurar Backup Automático con Cron

```bash
# Editar crontab
crontab -e

# Agregar línea para backup diario a las 2:00 AM
0 2 * * * /var/apps/yape-notifier/backup.sh >> /var/log/yape-notifier-backup.log 2>&1
```

### Volúmenes Docker

**Nota**: En producción, el código está incluido en las imágenes Docker, por lo que no es necesario hacer backup de volúmenes de código. Solo se necesita backup del volumen de PostgreSQL.

#### Backup del Volumen de PostgreSQL

```bash
# Backup del volumen completo (útil para disaster recovery completo)
docker run --rm \
  -v postgres_data_prod:/data \
  -v /var/backups/yape-notifier:/backup \
  alpine tar czf /backup/postgres_volume_$(date +%Y%m%d).tar.gz -C /data .

# Restaurar volumen
docker run --rm \
  -v postgres_data_prod:/data \
  -v /var/backups/yape-notifier:/backup \
  alpine sh -c "cd /data && rm -rf * && tar xzf /backup/postgres_volume_20241213.tar.gz"
```

**⚠️ Advertencia**: Restaurar un volumen completo requiere detener el contenedor de PostgreSQL primero.

## 🔄 Disaster Recovery Plan

### 1. Identificar el Problema

```bash
# Revisar logs de todos los servicios
cd /var/apps/yape-notifier/infra/docker/environments/production
docker compose --env-file .env logs --tail=100

# Verificar estado de contenedores
docker compose --env-file .env ps

# Verificar recursos del sistema
docker stats --no-stream

# Verificar espacio en disco
df -h
```

### 2. Recuperación de Base de Datos

```bash
# Ir al directorio de producción
cd /var/apps/yape-notifier/infra/docker/environments/production

# Detener servicios (opcional, puede hacerlo sin detener)
# docker compose --env-file .env stop php-fpm nginx-api dashboard caddy

# Asegurar que la base de datos esté corriendo
docker compose --env-file .env up -d db

# Esperar a que esté healthy (verificar con ps)
docker compose --env-file .env ps db
# Debe mostrar "healthy"

# Restaurar backup más reciente
# Opción A: Backup sin comprimir
docker compose --env-file .env exec -T db psql -U postgres yape_notifier < /var/backups/yape-notifier/backup_YYYYMMDD_HHMMSS.sql

# Opción B: Backup comprimido
gunzip < /var/backups/yape-notifier/backup_YYYYMMDD_HHMMSS.sql.gz | docker compose --env-file .env exec -T db psql -U postgres yape_notifier

# Verificar que la restauración fue exitosa
docker compose --env-file .env exec db psql -U postgres -d yape_notifier -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';"

# Reiniciar servicios si los detuviste
# docker compose --env-file .env up -d
```

### 3. Recuperación Completa del Sistema

```bash
# 1. Detener todos los servicios
cd /var/apps/yape-notifier/infra/docker/environments/production
docker compose --env-file .env down

# 2. Restaurar volumen de PostgreSQL (si tienes backup del volumen)
docker run --rm \
  -v postgres_data_prod:/data \
  -v /var/backups/yape-notifier:/backup \
  alpine sh -c "cd /data && rm -rf * && tar xzf /backup/postgres_volume_YYYYMMDD.tar.gz"

# 3. O restaurar desde dump SQL (más común)
docker compose --env-file .env up -d db
# Esperar a que esté healthy
gunzip < /var/backups/yape-notifier/backup_YYYYMMDD_HHMMSS.sql.gz | docker compose --env-file .env exec -T db psql -U postgres yape_notifier

# 4. Reconstruir imágenes si es necesario (después de actualizar código)
docker compose --env-file .env build

# 5. Iniciar todos los servicios
docker compose --env-file .env up -d

# 6. Verificar que todo esté funcionando
docker compose --env-file .env ps
curl https://api.notificaciones.space/up
```

### 4. Verificación Post-Recuperación

```bash
# Verificar estado de todos los servicios
docker compose --env-file .env ps --format "table {{.Name}}\t{{.Status}}\t{{.Health}}"

# Verificar conectividad de la base de datos
docker compose --env-file .env exec php-fpm php artisan migrate:status

# Verificar API
curl https://api.notificaciones.space/up

# Verificar Dashboard
curl -I https://dashboard.notificaciones.space

# Revisar logs para errores
docker compose --env-file .env logs --tail=50
```

## 📅 Frecuencia de Backups Recomendada

- **Base de datos**: Diario a las 2:00 AM (retener 30 días)
- **Volúmenes de PostgreSQL**: Semanal (retener 4 semanas) - Opcional
- **Configuraciones (.env)**: En cada cambio (versionado en Git con precaución)
- **Código**: Versionado en Git (no requiere backup adicional)

## 💿 Almacenamiento de Backups

### Almacenamiento Local

```bash
# Directorio recomendado
/var/backups/yape-notifier/

# Verificar espacio disponible
df -h /var/backups
```

### Almacenamiento Remoto (Recomendado para Disaster Recovery)

Para mayor seguridad, sincroniza los backups a almacenamiento remoto:

#### Opción 1: S3 Compatible (DigitalOcean Spaces, AWS S3)

```bash
# Instalar herramienta de sincronización
apt install s3cmd -y

# Configurar (primera vez)
s3cmd --configure

# Script de sincronización
#!/bin/bash
BACKUP_DIR="/var/backups/yape-notifier"
s3cmd sync "$BACKUP_DIR/" s3://tu-bucket/yape-notifier/backups/
```

#### Opción 2: rsync a servidor remoto

```bash
# Sincronizar backups a servidor remoto
rsync -avz /var/backups/yape-notifier/ usuario@servidor-remoto:/backups/yape-notifier/
```

#### Opción 3: Google Cloud Storage

```bash
# Instalar gsutil
# Configurar y sincronizar
gsutil -m rsync -r /var/backups/yape-notifier gs://tu-bucket/yape-notifier/backups
```

## ✅ Checklist de Backup

- [ ] Script de backup creado y ejecutable
- [ ] Cron configurado para backups automáticos
- [ ] Verificación de que los backups se crean correctamente
- [ ] Prueba de restauración realizada (al menos una vez)
- [ ] Almacenamiento remoto configurado (recomendado)
- [ ] Documentación de procedimientos de recuperación actualizada
- [ ] Notificaciones configuradas para fallos de backup (opcional)

## 🚨 Alertas y Monitoreo

Considera configurar alertas para:

- Fallos en la ejecución del backup
- Espacio en disco bajo (< 20% libre)
- Backups que no se han ejecutado en más de 25 horas
- Tamaño de backup anormalmente pequeño (posible error)
