# 🔧 Guía de Flujo de Desarrollo con Docker

Esta guía explica cómo trabajar con el proyecto usando Docker en todos los entornos, manteniendo consistencia de versiones y evitando problemas de dependencias.

## 📋 Principios

1. **Todo en Docker**: Desarrollo, Staging y Producción usan Docker para garantizar consistencia
2. **Hot Reload**: El código se monta como volumen para desarrollo rápido
3. **Versionado**: Node.js y npm versionados en Dockerfiles
4. **Reproducibilidad**: Mismo entorno en todas las máquinas

## 🚀 Desarrollo Local

### Primera Configuración

```bash
# 1. Ir al directorio de desarrollo
cd infra/docker/environments/development

# 2. Configuración inicial (si es primera vez)
./setup.sh

# 3. Configurar .env si es necesario
cp .env.example .env
nano .env  # Asegurar que DB_PASSWORD esté configurado
```

### Iniciar el Entorno

```bash
# Construir y levantar todos los servicios
docker compose --env-file .env up -d

# Ver logs de todos los servicios
docker compose --env-file .env logs -f

# Ver logs solo del dashboard
docker compose --env-file .env logs -f dashboard
```

### Agregar Nueva Dependencia

```bash
# 1. Agregar dependencia al package.json
cd apps/web-dashboard
# Editar package.json y agregar la dependencia

# 2. Reconstruir el contenedor para instalar dependencias
cd ../../infra/docker/environments/development
docker compose --env-file .env build dashboard

# 3. Reiniciar el servicio
docker compose --env-file .env up -d dashboard

# 4. Verificar que funciona
docker compose --env-file .env logs -f dashboard
```

**Nota**: Las dependencias se instalan en el contenedor, pero el código fuente se monta como volumen para hot reload.

### Trabajar con el Código

```bash
# El código está montado como volumen, así que:
# - Los cambios se reflejan automáticamente (hot reload)
# - No necesitas reconstruir para cambios de código
# - Solo reconstruye cuando agregas/eliminas dependencias

# Ver logs en tiempo real
docker compose --env-file .env logs -f dashboard

# Acceder al contenedor si necesitas
docker compose --env-file .env exec dashboard sh

# Dentro del contenedor puedes ejecutar:
npm install nueva-dependencia  # Instalar dependencia adicional
npm run build                  # Build manual
npm run lint                   # Linting
```

### Comandos Útiles

```bash
# Ver estado de servicios
docker compose --env-file .env ps

# Detener servicios
docker compose --env-file .env down

# Detener y eliminar volúmenes (⚠️ elimina datos de DB)
docker compose --env-file .env down -v

# Reconstruir un servicio específico
docker compose --env-file .env build --no-cache dashboard

# Reiniciar un servicio
docker compose --env-file .env restart dashboard

# Ver logs de un servicio específico
docker compose --env-file .env logs -f dashboard

# Ejecutar comandos en el contenedor
docker compose --env-file .env exec dashboard npm install
docker compose --env-file .env exec dashboard sh
```

## 📦 Agregar Dependencias: Flujo Completo

### Paso 1: Desarrollo

```bash
# 1. Editar package.json
cd apps/web-dashboard
# Agregar dependencia manualmente o usar:
# (Nota: npm install se ejecutará en el contenedor)

# 2. Reconstruir contenedor para instalar dependencia
cd ../../infra/docker/environments/development
docker compose --env-file .env build dashboard

# 3. Reiniciar
docker compose --env-file .env up -d dashboard

# 4. Verificar instalación
docker compose --env-file .env exec dashboard npm list | grep nueva-dependencia

# 5. Probar funcionalidad
# Abrir http://localhost:3000 y verificar
```

### Paso 2: Actualizar package-lock.json

```bash
# Dentro del contenedor, actualizar package-lock.json
docker compose --env-file .env exec dashboard npm install

# Copiar package-lock.json actualizado al host
docker compose --env-file .env exec dashboard cat package-lock.json > ../../../../apps/web-dashboard/package-lock.json

# O más simple: el package-lock.json se actualiza automáticamente
# porque el directorio está montado como volumen
```

### Paso 3: Commit

```bash
# Desde la raíz del proyecto
git add apps/web-dashboard/package.json
git add apps/web-dashboard/package-lock.json
git commit -m "feat: agregar nueva-dependencia"
```

### Paso 4: Staging

```bash
cd infra/docker/environments/staging

# Reconstruir dashboard
docker compose --env-file .env build dashboard

# Desplegar
docker compose --env-file .env up -d dashboard

# Verificar
docker compose --env-file .env logs -f dashboard
```

### Paso 5: Producción

```bash
cd infra/docker/environments/production

# Reconstruir dashboard (sin cache para asegurar instalación fresca)
docker compose --env-file .env build --no-cache dashboard

# Desplegar
docker compose --env-file .env up -d dashboard

# Verificar
docker compose --env-file .env logs -f dashboard
```

## 🔍 Troubleshooting

### Problema: Cambios no se reflejan (hot reload no funciona)

```bash
# Verificar que el volumen está montado correctamente
docker compose --env-file .env exec dashboard ls -la /app

# Verificar que Vite está corriendo
docker compose --env-file .env logs dashboard | grep "VITE"

# Reiniciar el servicio
docker compose --env-file .env restart dashboard
```

### Problema: Dependencia no se instala

```bash
# Reconstruir sin cache
docker compose --env-file .env build --no-cache dashboard

# Verificar package.json está correcto
docker compose --env-file .env exec dashboard cat package.json

# Instalar manualmente dentro del contenedor
docker compose --env-file .env exec dashboard npm install
```

### Problema: Puerto 3000 ya en uso

```bash
# Cambiar puerto en .env
echo "DASHBOARD_PORT=3001" >> .env

# O detener proceso que usa el puerto
# En Linux/Mac:
lsof -ti:3000 | xargs kill -9
# En Windows:
netstat -ano | findstr :3000
# Luego usar el PID para matar el proceso
```

### Problema: node_modules no se sincroniza

```bash
# El volumen /app/node_modules está excluido intencionalmente
# Las dependencias deben estar en el contenedor, no en el host
# Si necesitas node_modules en el host para tu IDE:

# Opción 1: Instalar localmente también (solo para IDE)
cd apps/web-dashboard
npm install

# Opción 2: Usar el contenedor para todo
docker compose --env-file .env exec dashboard npm install
```

## 📝 Notas Importantes

1. **Volúmenes en Desarrollo**:
   - El código fuente (`/app`) está montado como volumen para hot reload
   - `node_modules` está excluido del volumen para usar los del contenedor
   - Esto garantiza que las versiones sean consistentes

2. **Dependencias**:
   - Siempre agregar al `package.json` primero
   - Reconstruir el contenedor para instalar
   - Actualizar `package-lock.json` y commitear

3. **Hot Reload**:
   - Funciona automáticamente con Vite
   - No necesitas reconstruir para cambios de código
   - Solo reconstruye para cambios de dependencias

4. **Versionado**:
   - Node.js 18 está fijado en el Dockerfile
   - npm viene con Node.js
   - Todas las máquinas usan las mismas versiones

## 🎯 Checklist de Desarrollo

- [ ] Entorno Docker configurado y corriendo
- [ ] Dependencias agregadas a `package.json`
- [ ] Contenedor reconstruido después de agregar dependencias
- [ ] Funcionalidad probada en desarrollo
- [ ] `package-lock.json` actualizado
- [ ] Cambios commiteados
- [ ] Staging desplegado y probado
- [ ] Producción desplegada y verificada

## 🔗 Referencias

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Vite Documentation](https://vitejs.dev/)
- [Node.js Docker Best Practices](https://github.com/nodejs/docker-node/blob/main/docs/BEST_PRACTICES.md)

