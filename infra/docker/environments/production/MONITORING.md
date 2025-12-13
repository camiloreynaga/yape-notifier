# Monitoreo y Observabilidad

## Métricas Recomendadas

### Aplicación

- Tiempo de respuesta (p50, p95, p99)
- Tasa de errores (4xx, 5xx)
- Throughput (requests/segundo)
- Uptime

### Infraestructura

- CPU usage por servicio
- Memory usage por servicio
- Disk I/O
- Network I/O

### Base de Datos

- Conexiones activas
- Queries lentas
- Tamaño de base de datos
- Cache hit ratio

## Herramientas Recomendadas

### 1. Prometheus + Grafana

```yaml
# Agregar a docker-compose.yml
prometheus:
  image: prom/prometheus
  volumes:
    - ./prometheus.yml:/etc/prometheus/prometheus.yml
  ports:
    - "9090:9090"

grafana:
  image: grafana/grafana
  ports:
    - "3000:3000"
```

### 2. ELK Stack (Logging)

```yaml
elasticsearch:
  image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
  environment:
    - discovery.type=single-node

logstash:
  image: docker.elastic.co/logstash/logstash:8.11.0

kibana:
  image: docker.elastic.co/kibana/kibana:8.11.0
  ports:
    - "5601:5601"
```

### 3. Health Checks Externos

- UptimeRobot
- Pingdom
- StatusCake

## Alertas Críticas

### Debe Alertar Inmediatamente:

- Servicio caído (> 1 minuto)
- Error rate > 5%
- CPU > 90% por > 5 minutos
- Memory > 90% por > 5 minutos
- Disk > 85%
- Base de datos no responde

### Alertas de Advertencia:

- Error rate > 1%
- CPU > 70% por > 15 minutos
- Response time p95 > 1 segundo
- Conexiones de BD > 80% del máximo

## Dashboards Recomendados

1. **Overview Dashboard**

   - Estado de todos los servicios
   - Uptime
   - Requests/segundo
   - Error rate

2. **Application Dashboard**

   - Response times
   - Endpoints más lentos
   - Error por endpoint
   - Throughput

3. **Infrastructure Dashboard**

   - CPU/Memory por servicio
   - Network I/O
   - Disk usage

4. **Database Dashboard**
   - Conexiones activas
   - Queries lentas
   - Cache performance
   - Tamaño de tablas
