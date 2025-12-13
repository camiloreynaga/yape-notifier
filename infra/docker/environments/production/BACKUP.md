# Backup y Disaster Recovery

## Estrategia de Backup

### Base de Datos

#### Backup Manual

```bash
# Backup de base de datos
docker compose --env-file .env exec db pg_dump -U postgres yape_notifier > backup_$(date +%Y%m%d_%H%M%S).sql

# Restaurar
docker compose --env-file .env exec -T db psql -U postgres yape_notifier < backup_20241213_120000.sql
```

#### Backup Automatizado (Recomendado)

```bash
# Script de backup diario
#!/bin/bash
BACKUP_DIR="/backups/postgres"
DATE=$(date +%Y%m%d_%H%M%S)
docker compose --env-file .env exec -T db pg_dump -U postgres yape_notifier | gzip > "$BACKUP_DIR/backup_$DATE.sql.gz"

# Retener últimos 30 días
find "$BACKUP_DIR" -name "backup_*.sql.gz" -mtime +30 -delete
```

### Volúmenes Docker

```bash
# Backup de volúmenes
docker run --rm -v yape-notifier_postgres_data_prod:/data -v $(pwd):/backup alpine tar czf /backup/postgres_data_$(date +%Y%m%d).tar.gz /data

# Restaurar volumen
docker run --rm -v yape-notifier_postgres_data_prod:/data -v $(pwd):/backup alpine tar xzf /backup/postgres_data_20241213.tar.gz -C /
```

## Disaster Recovery Plan

### 1. Identificar el Problema

- Revisar logs: `docker compose --env-file .env logs`
- Verificar estado: `docker compose --env-file .env ps`
- Verificar recursos: `docker stats`

### 2. Recuperación de Base de Datos

```bash
# Detener servicios
docker compose --env-file .env down

# Restaurar backup más reciente
docker compose --env-file .env up -d db
# Esperar a que esté healthy
docker compose --env-file .env exec -T db psql -U postgres yape_notifier < backup.sql

# Reiniciar servicios
docker compose --env-file .env up -d
```

### 3. Recuperación Completa

```bash
# Restaurar volúmenes
docker volume restore yape-notifier_postgres_data_prod < backup_volume.tar

# Restaurar base de datos
# (ver paso 2)

# Verificar servicios
docker compose --env-file .env ps
curl https://api.notificaciones.space/up
```

## Frecuencia de Backups Recomendada

- **Base de datos**: Diario (retener 30 días)
- **Volúmenes**: Semanal (retener 4 semanas)
- **Configuraciones**: En cada cambio (Git)

## Almacenamiento

- **Local**: Para recuperación rápida
- **Remoto**: S3, Google Cloud Storage, o similar (para disaster recovery)
