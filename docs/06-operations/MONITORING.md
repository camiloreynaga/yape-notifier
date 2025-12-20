# 📊 Monitoreo y Observabilidad

Guía completa para monitorear Yape Notifier en producción.

## 📈 Métricas Recomendadas

### Aplicación (Laravel API)

- **Tiempo de respuesta**: p50, p95, p99
- **Tasa de errores**: 4xx, 5xx por endpoint
- **Throughput**: Requests por segundo
- **Uptime**: Disponibilidad del servicio
- **Healthcheck status**: Estado de `/up` endpoint
- **Queue jobs**: Tiempo de procesamiento y fallos

### Infraestructura (Docker)

- **CPU usage**: Por servicio (php-fpm, nginx-api, caddy, db)
- **Memory usage**: Por servicio y total del sistema
- **Disk I/O**: Lectura/escritura de volúmenes
- **Network I/O**: Tráfico entrante/saliente
- **Container health**: Estado de healthchecks
- **Image size**: Tamaño de imágenes Docker

### Base de Datos (PostgreSQL)

- **Conexiones activas**: Conexiones actuales vs máximo
- **Queries lentas**: Queries que toman > 1 segundo
- **Tamaño de base de datos**: Crecimiento y espacio utilizado
- **Cache hit ratio**: Eficiencia del cache de PostgreSQL
- **Locks**: Bloqueos y deadlocks
- **Replication lag**: Si se usa replicación (futuro)

## 🛠️ Herramientas Recomendadas

### 1. Monitoreo Básico con Docker Stats

```bash
# Ver uso de recursos en tiempo real
docker stats

# Ver uso de recursos sin actualización continua
docker stats --no-stream

# Ver uso de un contenedor específico
docker stats yape-notifier-php-fpm-prod --no-stream
```

### 2. Prometheus + Grafana (Recomendado para Producción)

#### Configuración Básica

Agregar a `docker-compose.yml`:

```yaml
  # Prometheus - Métricas
  prometheus:
    image: prom/prometheus:latest
    container_name: yape-notifier-prometheus-prod
    restart: always
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus_data_prod:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
    networks:
      - yape-network-prod
    ports:
      - "9090:9090"
    labels:
      - "com.yape-notifier.service=prometheus"
      - "com.yape-notifier.environment=production"

  # Grafana - Dashboards
  grafana:
    image: grafana/grafana:latest
    container_name: yape-notifier-grafana-prod
    restart: always
    volumes:
      - grafana_data_prod:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards:ro
      - ./monitoring/grafana/datasources:/etc/grafana/provisioning/datasources:ro
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASSWORD:-admin}
      - GF_USERS_ALLOW_SIGN_UP=false
    networks:
      - yape-network-prod
    ports:
      - "3000:3000"
    depends_on:
      - prometheus
    labels:
      - "com.yape-notifier.service=grafana"
      - "com.yape-notifier.environment=production"

  # Node Exporter - Métricas del sistema
  node-exporter:
    image: prom/node-exporter:latest
    container_name: yape-notifier-node-exporter-prod
    restart: always
    command:
      - '--path.procfs=/host/proc'
      - '--path.sysfs=/host/sys'
      - '--collector.filesystem.mount-points-exclude=^/(sys|proc|dev|host|etc)($$|/)'
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro
    networks:
      - yape-network-prod
    ports:
      - "9100:9100"
    labels:
      - "com.yape-notifier.service=node-exporter"
      - "com.yape-notifier.environment=production"

volumes:
  prometheus_data_prod:
    driver: local
  grafana_data_prod:
    driver: local
```

#### Configuración de Prometheus (`monitoring/prometheus.yml`)

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: "node-exporter"
    static_configs:
      - targets: ["node-exporter:9100"]

  - job_name: "caddy"
    static_configs:
      - targets: ["caddy:2019"]
    metrics_path: "/metrics"

  - job_name: "postgres"
    static_configs:
      - targets: ["postgres-exporter:9187"]
```

### 3. Health Checks Externos (Recomendado)

Servicios externos para monitoreo de disponibilidad:

- **UptimeRobot** (https://uptimerobot.com) - Gratis hasta 50 monitores
- **Pingdom** (https://www.pingdom.com) - Planes de pago
- **StatusCake** (https://www.statuscake.com) - Plan gratuito disponible

**Configuración recomendada**:

- Endpoint: `https://api.notificaciones.space/up`
- Intervalo: 5 minutos
- Timeout: 10 segundos
- Alertas: Email/SMS cuando el servicio esté caído > 2 minutos

### 4. Logging con Docker Logs

```bash
# Ver logs de todos los servicios
docker compose --env-file .env logs -f

# Ver logs de un servicio específico
docker compose --env-file .env logs -f php-fpm
docker compose --env-file .env logs -f nginx-api

# Ver últimas 100 líneas
docker compose --env-file .env logs --tail=100 php-fpm

# Ver logs desde una fecha específica
docker compose --env-file .env logs --since 2024-01-01T00:00:00 php-fpm
```

### 5. Monitoreo de Base de Datos

```bash
# Ver conexiones activas
docker compose --env-file .env exec db psql -U postgres -d yape_notifier -c "SELECT count(*) FROM pg_stat_activity;"

# Ver queries lentas
docker compose --env-file .env exec db psql -U postgres -d yape_notifier -c "SELECT pid, now() - pg_stat_activity.query_start AS duration, query FROM pg_stat_activity WHERE (now() - pg_stat_activity.query_start) > interval '1 minute';"

# Ver tamaño de base de datos
docker compose --env-file .env exec db psql -U postgres -d yape_notifier -c "SELECT pg_size_pretty(pg_database_size('yape_notifier'));"

# Ver tamaño de tablas
docker compose --env-file .env exec db psql -U postgres -d yape_notifier -c "SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size FROM pg_tables WHERE schemaname = 'public' ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;"
```

## 🚨 Alertas y Umbrales

### Alertas Críticas (Debe Alertar Inmediatamente)

- **Servicio caído**: Cualquier servicio no responde por > 1 minuto
- **Error rate**: > 5% de requests con error (4xx/5xx)
- **CPU**: > 90% de uso por > 5 minutos consecutivos
- **Memory**: > 90% de uso por > 5 minutos consecutivos
- **Disk**: > 85% de espacio utilizado
- **Base de datos**: No responde o conexiones rechazadas
- **Healthcheck fallando**: Cualquier servicio con healthcheck fallido > 2 minutos
- **Certificado SSL**: Próximo a expirar (< 7 días) o expirado

### Alertas de Advertencia

- **Error rate**: > 1% de requests con error
- **CPU**: > 70% de uso por > 15 minutos
- **Memory**: > 80% de uso por > 15 minutos
- **Response time**: p95 > 1 segundo por > 10 minutos
- **Conexiones BD**: > 80% del máximo configurado
- **Disk**: > 75% de espacio utilizado
- **Backup fallido**: Backup diario no se ejecutó en las últimas 25 horas

### Configuración de Alertas

#### Script de Monitoreo Básico

Crear `/var/apps/yape-notifier/monitor.sh`:

```bash
#!/bin/bash
# Script básico de monitoreo y alertas

ALERT_EMAIL="admin@notificaciones.space"
LOG_FILE="/var/log/yape-notifier-monitor.log"

# Función para logging
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Verificar servicios
check_services() {
    cd /var/apps/yape-notifier/infra/docker/environments/production

    # Verificar que todos los servicios estén corriendo
    DOWN_SERVICES=$(docker compose --env-file .env ps --format json | jq -r '.[] | select(.State != "running") | .Name')

    if [ ! -z "$DOWN_SERVICES" ]; then
        log "ALERTA: Servicios caídos: $DOWN_SERVICES"
        echo "Servicios caídos: $DOWN_SERVICES" | mail -s "ALERTA: Servicios caídos en Yape Notifier" "$ALERT_EMAIL"
    fi

    # Verificar healthchecks
    UNHEALTHY=$(docker compose --env-file .env ps --format json | jq -r '.[] | select(.Health != "healthy" and .Health != "") | .Name')

    if [ ! -z "$UNHEALTHY" ]; then
        log "ALERTA: Servicios no saludables: $UNHEALTHY"
        echo "Servicios no saludables: $UNHEALTHY" | mail -s "ALERTA: Healthchecks fallando en Yape Notifier" "$ALERT_EMAIL"
    fi
}

# Verificar recursos del sistema
check_resources() {
    # CPU
    CPU_USAGE=$(top -bn1 | grep "Cpu(s)" | sed "s/.*, *\([0-9.]*\)%* id.*/\1/" | awk '{print 100 - $1}')

    if (( $(echo "$CPU_USAGE > 90" | bc -l) )); then
        log "ALERTA: CPU usage alto: ${CPU_USAGE}%"
        echo "CPU usage: ${CPU_USAGE}%" | mail -s "ALERTA: CPU alto en Yape Notifier" "$ALERT_EMAIL"
    fi

    # Memory
    MEM_USAGE=$(free | grep Mem | awk '{printf("%.2f", $3/$2 * 100.0)}')

    if (( $(echo "$MEM_USAGE > 90" | bc -l) )); then
        log "ALERTA: Memory usage alto: ${MEM_USAGE}%"
        echo "Memory usage: ${MEM_USAGE}%" | mail -s "ALERTA: Memory alto en Yape Notifier" "$ALERT_EMAIL"
    fi

    # Disk
    DISK_USAGE=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')

    if [ "$DISK_USAGE" -gt 85 ]; then
        log "ALERTA: Disk usage alto: ${DISK_USAGE}%"
        echo "Disk usage: ${DISK_USAGE}%" | mail -s "ALERTA: Disk lleno en Yape Notifier" "$ALERT_EMAIL"
    fi
}

# Verificar endpoint de API
check_api() {
    RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" https://api.notificaciones.space/up --max-time 10)

    if [ "$RESPONSE" != "200" ]; then
        log "ALERTA: API no responde correctamente. HTTP Code: $RESPONSE"
        echo "API HTTP Code: $RESPONSE" | mail -s "ALERTA: API no responde en Yape Notifier" "$ALERT_EMAIL"
    fi
}

# Ejecutar checks
log "Iniciando monitoreo..."
check_services
check_resources
check_api
log "Monitoreo completado"
```

Hacer ejecutable y configurar en cron:

```bash
chmod +x /var/apps/yape-notifier/monitor.sh

# Agregar a crontab para ejecutar cada 5 minutos
crontab -e
# Agregar: */5 * * * * /var/apps/yape-notifier/monitor.sh
```

## 📊 Dashboards Recomendados

### 1. Overview Dashboard (Vista General)

**Métricas principales**:

- Estado de todos los servicios (Up/Down/Unhealthy)
- Uptime de cada servicio
- Requests por segundo (total)
- Error rate (4xx + 5xx)
- Response time promedio (p50, p95, p99)
- CPU y Memory total del sistema

**Configuración en Grafana**:

- Panel de estado de servicios (Stat panels)
- Gráfico de línea para requests/segundo
- Gráfico de área para error rate
- Gráfico de línea para response times

### 2. Application Dashboard (Laravel API)

**Métricas principales**:

- Response times por endpoint (p50, p95, p99)
- Endpoints más lentos (top 10)
- Error rate por endpoint
- Throughput por endpoint
- Status codes (200, 4xx, 5xx)
- Queue jobs procesados/fallidos

**Configuración en Grafana**:

- Tabla de endpoints ordenada por tiempo de respuesta
- Gráfico de barras para error rate por endpoint
- Heatmap de response times
- Panel de status codes

### 3. Infrastructure Dashboard (Docker)

**Métricas principales**:

- CPU usage por servicio (php-fpm, nginx-api, caddy, db, dashboard)
- Memory usage por servicio
- Network I/O (bytes in/out)
- Disk I/O (read/write)
- Container count y estado
- Image sizes

**Configuración en Grafana**:

- Gráficos de línea para CPU/Memory por servicio
- Gráfico de área para Network I/O
- Gráfico de barras para Disk I/O
- Panel de estado de contenedores

### 4. Database Dashboard (PostgreSQL)

**Métricas principales**:

- Conexiones activas vs máximo
- Queries por segundo
- Queries lentas (> 1 segundo)
- Cache hit ratio
- Tamaño de base de datos y tablas
- Locks y deadlocks
- Replication lag (si aplica)

**Configuración en Grafana**:

- Gráfico de línea para conexiones activas
- Tabla de queries lentas
- Gauge para cache hit ratio
- Gráfico de área para tamaño de BD
- Panel de alertas de locks

### 5. Caddy Dashboard (Reverse Proxy)

**Métricas principales**:

- Requests por segundo
- Response times
- Status codes
- Certificados SSL (días hasta expiración)
- Bytes transferidos

## 📝 Logs y Troubleshooting

### Ver Logs en Tiempo Real

```bash
# Todos los servicios
docker compose --env-file .env logs -f

# Servicio específico
docker compose --env-file .env logs -f php-fpm
docker compose --env-file .env logs -f nginx-api
docker compose --env-file .env logs -f caddy
docker compose --env-file .env logs -f db
```

### Filtrar Logs por Nivel

```bash
# Solo errores de PHP-FPM
docker compose --env-file .env logs php-fpm | grep -i error

# Solo errores de Nginx
docker compose --env-file .env logs nginx-api | grep -i error

# Logs de Caddy (certificados SSL)
docker compose --env-file .env logs caddy | grep -i certificate
```

### Exportar Logs para Análisis

```bash
# Exportar logs de las últimas 24 horas
docker compose --env-file .env logs --since 24h > /var/log/yape-notifier-$(date +%Y%m%d).log

# Comprimir logs antiguos
gzip /var/log/yape-notifier-*.log
```

## ✅ Checklist de Monitoreo

- [ ] Healthchecks configurados y funcionando
- [ ] Métricas básicas siendo recolectadas (CPU, Memory, Disk)
- [ ] Alertas configuradas para eventos críticos
- [ ] Dashboards configurados en Grafana (si se usa)
- [ ] Logs siendo monitoreados regularmente
- [ ] Backup de logs configurado
- [ ] Notificaciones de alertas funcionando
- [ ] Documentación de procedimientos de respuesta a alertas
