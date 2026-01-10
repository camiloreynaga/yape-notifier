# 🏗️ Estrategia de Staging Environment - Análisis DevOps

Análisis profesional sobre implementar staging en el mismo servidor que producción.

---

## 📊 Respuesta Rápida

**¿Es posible?** ✅ **SÍ**, técnicamente es totalmente viable con Docker.

**¿Es recomendable?** ⚠️ **DEPENDE** del contexto, pero para tu caso actual: **SÍ, es una buena opción**.

**Mejor práctica general:** 🏆 **Servidor separado**, pero para proyectos pequeños/medianos, staging en el mismo servidor es aceptable y común.

---

## 🎯 Análisis de Tu Situación Actual

### ✅ Ventajas de Staging en el Mismo Servidor

1. **Costo reducido**: No necesitas un servidor adicional
2. **Ya está configurado**: Tienes `infra/docker/environments/staging` listo
3. **Aislamiento con Docker**: Redes, volúmenes y contenedores separados
4. **Misma infraestructura**: Pruebas más realistas (mismo hardware, OS, versión de Docker)
5. **Fácil sincronización**: Mismo código, misma configuración base

### ⚠️ Desventajas y Riesgos

1. **Recursos compartidos**: CPU, RAM y disco compartidos
2. **Riesgo de afectar producción**: Si staging consume muchos recursos
3. **Seguridad**: Si staging es comprometido, podría afectar producción (aunque Docker mitiga esto)
4. **Escalabilidad limitada**: Si creces, necesitarás separar

---

## 🏆 Mejores Prácticas por Escenario

### Escenario 1: Proyecto Pequeño/Mediano (Tu Caso Actual) ⭐

**Recomendación: Staging en el mismo servidor es ACEPTABLE**

**Configuración recomendada:**

```yaml
# Recursos limitados para staging
staging:
  deploy:
    resources:
      limits:
        cpus: "1"      # Menos CPU que producción
        memory: 512M   # Menos RAM que producción
      reservations:
        cpus: "0.25"
        memory: 128M
```

**Ventajas:**
- ✅ Costo eficiente
- ✅ Fácil de mantener
- ✅ Suficiente para testing

**Cuándo migrar a servidor separado:**
- Cuando tengas >100 usuarios activos
- Cuando staging necesite pruebas de carga
- Cuando el presupuesto lo permita

---

### Escenario 2: Proyecto Grande/Enterprise

**Recomendación: Servidor separado OBLIGATORIO**

**Arquitectura recomendada:**

```
┌─────────────────────────────────────┐
│  Servidor Staging (Separado)       │
│  - staging-api.example.com         │
│  - staging-dashboard.example.com   │
│  - BD: yape_notifier_staging       │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  Servidor Producción                │
│  - api.example.com                  │
│  - dashboard.example.com            │
│  - BD: yape_notifier_production     │
└─────────────────────────────────────┘
```

**Ventajas:**
- ✅ Aislamiento total
- ✅ Pruebas de carga sin afectar producción
- ✅ Rollback más seguro
- ✅ Mejor para compliance/auditorías

---

## 🔒 Seguridad y Aislamiento

### Con Docker (Tu Configuración Actual)

**Aislamiento a nivel de:**
- ✅ **Redes**: `yape-network-staging` vs `yape-network-prod` (separadas)
- ✅ **Volúmenes**: `postgres_data_staging` vs `postgres_data_prod` (separados)
- ✅ **Contenedores**: Nombres únicos con sufijo `-staging` vs `-prod`
- ✅ **Puertos**: 8080/8443 (staging) vs 80/443 (producción)

**Riesgos residuales:**
- ⚠️ **Mismo kernel**: Si el kernel tiene vulnerabilidades, afecta ambos
- ⚠️ **Recursos compartidos**: Si staging consume todo el CPU/RAM
- ⚠️ **Acceso SSH**: Mismo servidor = mismo acceso

**Mitigaciones:**
- ✅ Usar límites de recursos (`deploy.resources.limits`)
- ✅ Monitoreo de recursos
- ✅ Usuarios separados para staging (opcional)

---

## 📋 Configuración Recomendada para Tu Caso

### 1. Actualizar docker-compose.yml de Staging

```yaml
# infra/docker/environments/staging/docker-compose.yml

services:
  php-fpm:
    # ... configuración existente ...
    deploy:
      resources:
        limits:
          cpus: "1"           # Máximo 1 CPU
          memory: 512M        # Máximo 512MB RAM
        reservations:
          cpus: "0.25"        # Mínimo 0.25 CPU
          memory: 128M        # Mínimo 128MB RAM

  db:
    # ... configuración existente ...
    deploy:
      resources:
        limits:
          cpus: "0.5"
          memory: 256M
        reservations:
          cpus: "0.1"
          memory: 64M
```

### 2. Configurar DNS para Staging

**Opción A: Subdominios (Recomendado)**

```
staging-api.notificaciones.space     → Servidor (puerto 8080)
staging-dashboard.notificaciones.space → Servidor (puerto 8080)
```

**Opción B: Puerto directo (Actual)**

```
TU_IP:8080/api/up                    → API Staging
TU_IP:8080                           → Dashboard Staging
```

### 3. Variables de Entorno Separadas

```bash
# infra/docker/environments/staging/.env
APP_ENV=staging
APP_DEBUG=true
APP_URL=http://staging-api.notificaciones.space
DB_DATABASE=yape_notifier_staging
DB_PASSWORD=password_staging_diferente
```

---

## 🚀 Workflow Recomendado

### Flujo de Deployment

```
1. Desarrollo Local
   ↓
2. Push a rama `develop` o `staging`
   ↓
3. Deploy a Staging (mismo servidor, puerto 8080)
   ↓
4. Testing en Staging
   ↓
5. Si OK → Merge a `main` o `production`
   ↓
6. Deploy a Producción (mismo servidor, puerto 80/443)
```

### Scripts de Deployment

```bash
# Deploy a staging
cd /var/apps/yape-notifier/infra/docker/environments/staging
./deploy.sh

# Deploy a producción (después de validar staging)
cd /var/apps/yape-notifier/infra/docker/environments/production
./deploy.sh
```

---

## 📊 Monitoreo de Recursos

### Verificar Uso de Recursos

```bash
# Ver uso de recursos de todos los contenedores
docker stats

# Ver uso específico de staging
docker stats yape-notifier-php-fpm-staging yape-notifier-db-staging

# Ver uso específico de producción
docker stats yape-notifier-php-fpm-prod yape-notifier-db-prod
```

### Alertas Recomendadas

Configurar alertas si:
- CPU > 80% en producción
- RAM > 80% en producción
- Staging consume > 50% de recursos totales

---

## 🔄 Alternativas y Cuándo Usarlas

### Opción 1: Staging en el Mismo Servidor (Actual) ⭐

**Cuándo usar:**
- ✅ Proyecto pequeño/mediano
- ✅ Presupuesto limitado
- ✅ Equipo pequeño
- ✅ Testing básico

**Configuración:** Ya la tienes ✅

---

### Opción 2: Staging en Servidor Separado

**Cuándo usar:**
- ✅ Proyecto grande (>100 usuarios)
- ✅ Necesitas pruebas de carga
- ✅ Compliance/auditorías requeridas
- ✅ Presupuesto disponible

**Costo adicional:** ~$12-24/mes (Droplet básico)

**Configuración:**
```bash
# Mismo proceso que producción, pero en servidor diferente
# Usar mismo código, pero .env diferente
```

---

### Opción 3: Staging en Cloud (DigitalOcean App Platform)

**Cuándo usar:**
- ✅ Quieres CI/CD automático
- ✅ No quieres gestionar servidor
- ✅ Presupuesto medio

**Costo:** ~$12-25/mes

**Ventajas:**
- ✅ Deploy automático desde Git
- ✅ Escalado automático
- ✅ SSL automático

---

### Opción 4: Staging Local + Producción en Servidor

**Cuándo usar:**
- ✅ Desarrollo activo
- ✅ Testing rápido
- ✅ No necesitas staging 24/7

**Configuración:**
```bash
# Staging en tu máquina local
cd infra/docker/environments/staging
docker compose up -d

# Producción en servidor
ssh deploy@servidor
cd infra/docker/environments/production
./deploy.sh
```

---

## ✅ Checklist de Implementación

### Para Staging en el Mismo Servidor

- [ ] ✅ Configurar límites de recursos en `docker-compose.yml`
- [ ] ✅ Configurar DNS para staging (subdominios)
- [ ] ✅ Variables de entorno separadas (`.env` diferente)
- [ ] ✅ Base de datos separada (`yape_notifier_staging`)
- [ ] ✅ Scripts de deployment separados
- [ ] ✅ Monitoreo de recursos configurado
- [ ] ✅ Documentación del workflow de deployment
- [ ] ✅ Backup de staging (opcional, menos crítico)

---

## 🎯 Recomendación Final para Tu Proyecto

### Fase Actual (MVP/Inicial)

**✅ RECOMENDADO: Staging en el mismo servidor**

**Razones:**
1. Ya está configurado
2. Costo eficiente
3. Suficiente para testing básico
4. Aislamiento con Docker es adecuado

**Acciones:**
1. ✅ Configurar límites de recursos
2. ✅ Configurar DNS para staging (subdominios)
3. ✅ Documentar workflow
4. ✅ Monitorear recursos

---

### Fase de Crecimiento (Futuro)

**🔄 MIGRAR A: Servidor separado cuando:**
- Tengas >100 usuarios activos
- Necesites pruebas de carga
- El presupuesto lo permita
- Tengas equipo dedicado a DevOps

---

## 📚 Referencias y Mejores Prácticas

### Estándares de la Industria

1. **12-Factor App**: Separación de config por entorno ✅ (ya lo tienes)
2. **Docker Best Practices**: Aislamiento con contenedores ✅ (ya lo tienes)
3. **Infrastructure as Code**: Docker Compose ✅ (ya lo tienes)

### Recursos Adicionales

- [Docker Resource Limits](https://docs.docker.com/compose/compose-file/deploy/#resources)
- [12-Factor App](https://12factor.net/)
- [DigitalOcean Staging Environments](https://www.digitalocean.com/community/tutorials/how-to-set-up-staging-environments)

---

## 🛠️ Scripts Útiles

### Verificar Recursos

```bash
#!/bin/bash
# check-resources.sh

echo "=== Recursos de Producción ==="
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" | grep prod

echo ""
echo "=== Recursos de Staging ==="
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" | grep staging
```

### Deploy a Staging

```bash
#!/bin/bash
# deploy-staging.sh

cd /var/apps/yape-notifier/infra/docker/environments/staging
git pull origin staging  # o la rama que uses
./deploy.sh
```

---

## 📞 Conclusión

**Para tu proyecto actual: Staging en el mismo servidor es una EXCELENTE opción.**

**Ventajas:**
- ✅ Costo eficiente
- ✅ Ya está configurado
- ✅ Aislamiento adecuado con Docker
- ✅ Suficiente para testing

**Solo migra a servidor separado cuando:**
- El proyecto crezca significativamente
- Necesites pruebas de carga
- Tengas presupuesto disponible

**Tu configuración actual es profesional y sigue mejores prácticas.** 🎉

---

**Última actualización:** 2025-01-15

