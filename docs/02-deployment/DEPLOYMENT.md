# 🚀 Guía Completa de Deployment - Yape Notifier

Guía profesional y consolidada para desplegar Yape Notifier en producción usando Digital Ocean Droplets con subdominios.

---

## 📋 Tabla de Contenidos

1. [Resumen de Pasos para Producción](#resumen-de-pasos-para-producción) ⚡
2. [Arquitectura del Sistema](#arquitectura-del-sistema)
3. [Prerrequisitos](#prerrequisitos)
4. [Opción 1: Digital Ocean Droplet con Subdominios (Recomendado)](#opción-1-digital-ocean-droplet-con-subdominios-recomendado)
5. [Opción 2: Digital Ocean App Platform (Alternativa)](#opción-2-digital-ocean-app-platform-alternativa)
6. [Configuración de Variables de Entorno](#configuración-de-variables-de-entorno)
7. [Solución de Problemas](#solución-de-problemas)
8. [Mantenimiento y Actualizaciones](#mantenimiento-y-actualizaciones)
9. [Backup y Recuperación](#backup-y-recuperación)

---

## ⚡ Resumen de Pasos para Producción

**Guía rápida de los pasos esenciales para desplegar en producción:**

### 1️⃣ Preparación del Servidor

```bash
# 1. Crear Droplet en Digital Ocean (Ubuntu 22.04 LTS, mínimo 2GB RAM)
# 2. Configurar DNS (api.notificaciones.space y dashboard.notificaciones.space → IP del Droplet)
# 3. Conectarse al servidor: ssh root@TU_IP_DROPLET
# 4. Actualizar sistema: apt update && apt upgrade -y
# 5. Instalar herramientas: apt install -y curl wget git nano ufw
# 6. Configurar firewall: ufw allow 22/tcp && ufw allow 80/tcp && ufw allow 443/tcp && ufw enable
```

### 2️⃣ Instalar Docker

```bash
# Instalar Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# Instalar Docker Compose Plugin
apt install docker-compose-plugin -y

# Verificar instalación
docker --version
docker compose version
```

### 3️⃣ Clonar Repositorio

```bash
# Crear directorio y clonar
mkdir -p /var/apps
cd /var/apps
git clone https://github.com/TU_USUARIO/yape-notifier.git
cd yape-notifier
```

### 4️⃣ Configurar Variables de Entorno

```bash
# Ir al directorio de producción
cd infra/docker/environments/production

# Crear archivo .env desde plantilla
cp .env.example .env

# Editar y configurar DB_PASSWORD (OBLIGATORIO - es lo único que DEBES cambiar)
nano .env
# Buscar la línea que contiene: DB_PASSWORD=TU_CONTRASEÑA_SEGURA_AQUI
# O simplemente buscar: DB_PASSWORD
# Cambiar por: DB_PASSWORD=tu_contraseña_segura_real
#
# Ejemplo de línea correcta:
# DB_PASSWORD=MiContraseñaSegura123!@#
#
# ⚠️ IMPORTANTE: No dejes DB_PASSWORD vacío ni con el valor placeholder
```

**Nota**: El archivo `.env.example` contiene todas las variables necesarias con valores por defecto. **IMPORTANTE**: Antes del primer despliegue, debes configurar:

- `DB_PASSWORD`: Contraseña segura para PostgreSQL (OBLIGATORIO)
- `APP_KEY`: Clave de aplicación Laravel (OBLIGATORIO - ver sección de generación más abajo)

Las demás variables (APP_URL, DASHBOARD_API_URL, etc.) ya están configuradas correctamente para producción.

### 5️⃣ Generar APP_KEY (OBLIGATORIO)

```bash
# Generar APP_KEY antes del despliegue
# Opción 1: Si tienes PHP local
cd apps/api
php artisan key:generate --show
# Copia la clave generada

# Opción 2: Usar contenedor temporal
cd infra/docker/environments/production
docker run --rm -v $(pwd)/../../../../apps/api:/var/www -w /var/www php:8.2-cli php artisan key:generate --show

# Editar .env y agregar APP_KEY
nano .env
# Busca APP_KEY= y reemplaza con: APP_KEY=base64:tu_clave_generada_aqui
```

### 6️⃣ Desplegar Aplicación

```bash
# Hacer scripts ejecutables
chmod +x setup.sh deploy.sh

# Ejecutar setup (solo primera vez, ya debería estar hecho)
./setup.sh

# Desplegar (esto valida APP_KEY, detiene contenedores, construye, inicia servicios, ejecuta migraciones, etc.)
./deploy.sh

# O si necesitas rebuild completo sin cache:
./deploy.sh --no-cache
```

### 7️⃣ Verificar Despliegue

```bash
# Verificar estado de contenedores
docker compose --env-file .env ps

# Ver logs
docker compose --env-file .env logs -f

# Verificar desde navegador (esperar 2-5 minutos para certificados SSL)
# - https://api.notificaciones.space/up
# - https://dashboard.notificaciones.space
```

### ✅ Checklist Rápido

- [ ] Droplet creado y accesible
- [ ] DNS configurado (api y dashboard apuntan a IP del Droplet)
- [ ] Docker y Docker Compose instalados
- [ ] Repositorio clonado
- [ ] Archivo `.env` creado con `DB_PASSWORD` configurado
- [ ] `APP_KEY` generado y configurado en `.env` (antes de ejecutar deploy.sh)
- [ ] Script `deploy.sh` ejecutado exitosamente
- [ ] Contenedores corriendo y saludables
- [ ] Certificados SSL obtenidos (verificar en navegador)
- [ ] API accesible en `https://api.notificaciones.space/up`
- [ ] Dashboard accesible en `https://dashboard.notificaciones.space`

> 📖 **Para detalles completos de cada paso, consulta las secciones detalladas a continuación.**

---

## 🏗️ Arquitectura del Sistema

```
Internet
   │
   ▼
[ Caddy :80, :443 ]
   │ (HTTPS automático con Let's Encrypt)
   ├─► api.notificaciones.space → [ Nginx API :80 ] → [ PHP-FPM :9000 ] → [ Laravel API ]
   └─► dashboard.notificaciones.space → [ Dashboard :80 ] → [ React App ]
   │
   ▼
[ PostgreSQL :5432 ]
   (interno, no expuesto)
```

### Componentes

- **Caddy**: Reverse proxy con HTTPS automático (Let's Encrypt)
- **Nginx API**: Servidor web para Laravel (PHP-FPM) - Imagen construida con código incluido
- **PHP-FPM**: Aplicación Laravel 11 - Imagen optimizada con dependencias de Composer instaladas
- **Dashboard**: Frontend React servido por Nginx
- **PostgreSQL**: Base de datos (no expuesta públicamente)

### Optimizaciones de Producción

El proyecto utiliza las siguientes optimizaciones para producción:

- ✅ **Imágenes autocontenidas**: El código de la aplicación está incluido en las imágenes Docker, sin dependencias del host
- ✅ **Composer optimizado**: Las dependencias de Composer se instalan durante el build de la imagen
- ✅ **Healthchecks configurados**: Todos los servicios tienen healthchecks para monitoreo automático
- ✅ **Permisos de Laravel**: Los directorios `storage/` y `bootstrap/cache` tienen permisos correctos configurados en la imagen
- ✅ **Sin volúmenes de código**: En producción, no se montan volúmenes del host para evitar sobrescribir archivos generados durante el build

---

## ✅ Prerrequisitos

Antes de comenzar, asegúrate de tener:

- ✅ Cuenta en Digital Ocean
- ✅ Dominio configurado: `notificaciones.space`
- ✅ Acceso SSH al Droplet
- ✅ Repositorio en GitHub/GitLab (o acceso al código)
- ✅ Conocimientos básicos de Linux y Docker

---

## 🐳 Opción 1: Digital Ocean Droplet con Subdominios (Recomendado)

Esta es la opción recomendada para producción, ya que ofrece:

- ✅ Control total sobre la infraestructura
- ✅ HTTPS automático con Caddy
- ✅ Subdominios profesionales (`api.notificaciones.space`, `dashboard.notificaciones.space`)
- ✅ Mejor rendimiento y escalabilidad
- ✅ Costo optimizado

### Paso 1: Crear el Droplet

1. **Inicia sesión** en [Digital Ocean Dashboard](https://cloud.digitalocean.com/)

2. **Crea un nuevo Droplet:**

   - Haz clic en **"Create"** → **"Droplets"**
   - **Imagen**: Ubuntu 22.04 LTS (o la más reciente)
   - **Plan**:
     - Mínimo: **Basic - $12/mes** (2GB RAM, 1 vCPU)
     - Recomendado: **Basic - $24/mes** (4GB RAM, 2 vCPU) para mejor rendimiento
   - **Región**: Elige la más cercana a tus usuarios
   - **Autenticación**:
     - ✅ **SSH keys** (recomendado) - Agrega tu clave pública SSH
     - O contraseña (menos seguro)
   - **Hostname**: `yape-notifier-prod` (o el que prefieras)
   - **Tags**: Opcional (ej: `production`, `yape-notifier`)

3. **Haz clic en "Create Droplet"**

4. **Anota la IP del Droplet** - La necesitarás para configurar DNS

### Paso 2: Configurar DNS (Subdominios)

Antes de continuar, configura los subdominios en tu proveedor de DNS.

#### 2.1. Obtener la IP del Droplet

En Digital Ocean Dashboard, ve a tu Droplet y copia la **IP pública** (ej: `157.230.45.123`)

#### 2.2. Configurar Registros DNS

Ve a tu proveedor de DNS (donde compraste el dominio) y agrega los siguientes registros:

**Opción A: Registros A (Recomendado)**

```
Tipo: A
Nombre: api
Valor: 157.230.45.123  (IP de tu Droplet)
TTL: 3600 (o el mínimo)

Tipo: A
Nombre: dashboard
Valor: 157.230.45.123  (IP de tu Droplet)
TTL: 3600 (o el mínimo)
```

**Opción B: Registro A Wildcard (Alternativa)**

Si prefieres usar un solo registro para todos los subdominios:

```
Tipo: A
Nombre: *
Valor: 157.230.45.123  (IP de tu Droplet)
TTL: 3600
```

#### 2.3. Verificar Propagación DNS

Espera 5-15 minutos y verifica que los DNS se hayan propagado:

```bash
# En tu máquina local
nslookup api.notificaciones.space
nslookup dashboard.notificaciones.space

# Deberías ver la IP de tu Droplet
```

**Nota**: La propagación DNS puede tardar hasta 24 horas, pero generalmente es mucho más rápido.

### Paso 3: Configuración Inicial del Servidor

#### 3.1. Conectarse al Droplet

```bash
# Reemplaza con tu IP y usuario
ssh root@TU_IP_DROPLET

# O si configuraste un usuario:
ssh usuario@TU_IP_DROPLET
```

#### 3.2. Actualizar el Sistema

```bash
# Actualizar paquetes
apt update && apt upgrade -y

# Instalar herramientas básicas
apt install -y curl wget git nano ufw
```

#### 3.3. Configurar Firewall

```bash
# Permitir SSH
ufw allow 22/tcp

# Permitir HTTP y HTTPS (para Caddy)
ufw allow 80/tcp
ufw allow 443/tcp

# Activar firewall
ufw --force enable

# Verificar estado
ufw status
```

### Paso 4: Instalar Docker y Docker Compose

#### 4.1. Instalar Docker

```bash
# Descargar script de instalación
curl -fsSL https://get.docker.com -o get-docker.sh

# Ejecutar instalación
sh get-docker.sh

# Agregar usuario actual al grupo docker (si no eres root)
# usermod -aG docker $USER

# Verificar instalación
docker --version
```

#### 4.2. Instalar Docker Compose Plugin

```bash
# Instalar Docker Compose Plugin
apt install docker-compose-plugin -y

# Verificar instalación
docker compose version
```

#### 4.3. Verificar que Docker Funciona

```bash
# Probar Docker
docker run hello-world

# Si ves "Hello from Docker!", todo está bien
```

**⚠️ Si obtienes error "Cannot connect to the Docker daemon":**

Si estás en el grupo `docker` pero aún no puedes conectarte:

```bash
# Opción 1: Reiniciar sesión SSH (recomendado)
exit
# Luego vuelve a conectarte
ssh deploy@TU_SERVIDOR

# Opción 2: Activar el grupo sin cerrar sesión
newgrp docker

# Opción 3: Verificar que el servicio Docker esté corriendo
sudo systemctl status docker
sudo systemctl start docker  # Si no está corriendo
```

### Paso 5: Clonar el Repositorio

#### 5.1. Crear Directorio para la Aplicación

```bash
# Crear directorio
mkdir -p /var/apps
cd /var/apps
```

#### 5.2. Clonar el Repositorio

```bash
# Si es un repositorio público
git clone https://github.com/TU_USUARIO/yape-notifier.git

# Si es privado, necesitarás configurar SSH keys o usar HTTPS con token
# git clone git@github.com:TU_USUARIO/yape-notifier.git

# Entrar al directorio
cd yape-notifier
```

#### 5.3. Verificar Estructura

```bash
# Verificar que existe la estructura
ls -la infra/docker/environments/production/

# Deberías ver:
# - docker-compose.yml
# - Caddyfile
# - .env.example
# - deploy.sh
# - setup.sh
```

### Paso 6: Configurar Variables de Entorno

**⚠️ IMPORTANTE**: Este paso es **OBLIGATORIO** antes de construir las imágenes Docker.

**📋 Resumen rápido:**

- El archivo `.env` en `infra/docker/environments/production/` se usa para **Docker Compose**
- **OBLIGATORIO antes del primer despliegue**: Configurar `DB_PASSWORD` y `APP_KEY`
- Las demás variables ya tienen valores por defecto adecuados
- ⚠️ **IMPORTANTE**: En producción, `APP_KEY` NO se genera automáticamente. Debe existir en `.env` antes de ejecutar `deploy.sh`

#### 6.1. Entendiendo las Variables de Entorno

**Dos tipos de variables de entorno en este proyecto:**

1. **Variables para Docker Compose** (archivo `.env` en `infra/docker/environments/production/`):

   - Se usan **antes** de crear los contenedores
   - Docker Compose las lee para configurar servicios (PostgreSQL, build args, etc.)
   - Se especifican con `--env-file .env` en los comandos
   - Ejemplo: `DB_PASSWORD`, `APP_URL`, `DASHBOARD_API_URL`

2. **Variables para Laravel** (dentro del contenedor PHP-FPM):
   - Se pasan al contenedor de Laravel
   - Laravel las lee desde su archivo `.env` interno
   - Incluyen todas las variables que Laravel necesita (APP*\*, DB*\_, CACHE\_\_, etc.)

**Cómo Docker Compose resuelve variables:**

Cuando usas `${VARIABLE}` en `docker-compose.yml`, Docker Compose busca la variable en:

1. Archivo especificado con `--env-file .env` (recomendado)
2. Archivo `.env` en el mismo directorio (carga automática si no usas `--env-file`)
3. Variables del shell actual
4. Variables del sistema

**Mejor práctica**: Siempre usar `--env-file .env` explícitamente en todos los comandos de Docker Compose para evitar ambigüedades.

#### 6.2. Crear Archivo .env

```bash
# Ir al directorio de producción
cd /var/apps/yape-notifier/infra/docker/environments/production

# Verificar que existe la plantilla
ls -la .env.example

# Crear archivo .env desde la plantilla
cp .env.example .env

# Editar el archivo con tus valores reales
nano .env
```

**⚠️ IMPORTANTE**: Si no creas este archivo o `DB_PASSWORD` está vacío, obtendrás el error: "The DB_PASSWORD variable is not set" o "Database is uninitialized and superuser password is not specified"

#### 6.3. Entendiendo las Variables de Entorno

El archivo `.env` en `infra/docker/environments/production/` tiene **dos propósitos**:

1. **Variables para Docker Compose** (usadas en `docker-compose.yml`):

   - Estas variables se leen **antes** de crear los contenedores
   - Se usan para configurar servicios Docker (PostgreSQL, build args, etc.)

2. **Variables para Laravel** (necesarias dentro del contenedor PHP-FPM):
   - Estas variables se pasan al contenedor de Laravel
   - Laravel las lee desde su propio archivo `.env` dentro del contenedor

#### 6.4. Variables Requeridas para Docker Compose

**Variables OBLIGATORIAS** que Docker Compose necesita (definidas en `docker-compose.yml`):

```env
# ⚠️ OBLIGATORIO - Sin esta variable, PostgreSQL no iniciará
DB_PASSWORD=tu_contraseña_segura_aqui
```

**Variables OPCIONALES** con valores por defecto (puedes omitirlas si los defaults son correctos):

```env
# Base de Datos (opcionales, tienen defaults)
DB_DATABASE=yape_notifier          # Default: yape_notifier
DB_USERNAME=postgres               # Default: postgres

# URLs (opcionales, tienen defaults)
APP_URL=https://api.notificaciones.space                    # Default: https://api.notificaciones.space
DASHBOARD_API_URL=https://api.notificaciones.space         # Default: https://api.notificaciones.space
```

**Nota**: Si no defines estas variables opcionales, Docker Compose usará los valores por defecto definidos en `docker-compose.yml`.

#### 6.5. Variables para Laravel (dentro del contenedor)

Laravel necesita su propio archivo `.env` dentro del contenedor. El archivo `.env.example` en `infra/docker/environments/production/` contiene **todas** las variables que Laravel necesita, incluyendo:

- Variables de base de datos (DB\_\*)
- Variables de aplicación (APP\_\*)
- Variables de sesión, cache, queue, etc.

**Importante**:

- El archivo `.env.example` ya contiene valores por defecto para todas las variables de Laravel
- **OBLIGATORIO antes del primer despliegue**: Configurar `DB_PASSWORD` y `APP_KEY`
- ⚠️ **En producción, `APP_KEY` NO se genera automáticamente**. Debes generarlo manualmente antes del despliegue (ver sección "Generar APP_KEY" más abajo)
- Las demás variables de Laravel se pueden ajustar después si es necesario

#### 6.6. Resumen: ¿Qué configurar antes del despliegue?

**Mínimo requerido** (antes de ejecutar `./deploy.sh`):

```env
# 1. Configurar contraseña de base de datos (OBLIGATORIO)
DB_PASSWORD=tu_contraseña_segura_real_aqui

# 2. Generar y configurar APP_KEY (OBLIGATORIO)
# Primero genera la clave (ver sección "Generar APP_KEY" más abajo)
APP_KEY=base64:tu_clave_generada_aqui
```

**⚠️ IMPORTANTE**: El script `deploy.sh` validará que `APP_KEY` existe antes de continuar. Si no está configurado, el despliegue fallará con un error claro.

**Opcional** (si quieres personalizar):

```env
# Si tu dominio es diferente:
APP_URL=https://tu-dominio.com
DASHBOARD_API_URL=https://tu-dominio.com

# Si quieres cambiar el nombre de la base de datos:
DB_DATABASE=mi_base_de_datos
DB_USERNAME=mi_usuario
```

**Nota**: Puedes ajustar otras variables de Laravel después del despliegue si es necesario.

#### 6.3. Guardar y Salir

```bash
# En nano: Ctrl+O (guardar), Enter, Ctrl+X (salir)
```

### Paso 7: Configurar Caddy para Subdominios

Caddy manejará automáticamente HTTPS con Let's Encrypt para tus subdominios.

#### 7.1. Verificar Caddyfile

El Caddyfile ya está configurado en la nueva estructura. Si necesitas editarlo:

```bash
# Editar Caddyfile
nano /var/apps/yape-notifier/infra/docker/environments/production/Caddyfile
```

#### 7.2. Verificar Configuración de Subdominios

El Caddyfile ya está configurado correctamente con:

- `api.notificaciones.space` → Nginx API
- `dashboard.notificaciones.space` → Dashboard

**Nota**: El dominio `notificaciones.space` ya está configurado. Si usas otro dominio, edita el Caddyfile y actualiza los valores.

### Paso 8: Generar APP_KEY (OBLIGATORIO antes del despliegue)

**⚠️ IMPORTANTE**: Antes de ejecutar `deploy.sh`, debes generar y configurar `APP_KEY` en el archivo `.env`. El script validará que existe y abortará si no está configurado.

**Procedimiento rápido:**

```bash
# Opción 1: Si tienes PHP localmente
cd /var/apps/yape-notifier/apps/api
php artisan key:generate --show
# Copia la clave generada (ej: base64:xxxxxxxxxxxxx)

# Opción 2: Usar contenedor temporal
cd /var/apps/yape-notifier/infra/docker/environments/production
docker run --rm -v $(pwd)/../../../../apps/api:/var/www -w /var/www php:8.2-cli php artisan key:generate --show
# Copia la clave generada

# Editar .env y agregar la clave
nano .env
# Busca APP_KEY= y reemplaza con: APP_KEY=base64:tu_clave_generada_aqui
```

**Verificar que APP_KEY está configurado:**

```bash
# Verificar que APP_KEY existe y no está vacío
grep "^APP_KEY=base64:" .env

# Debe mostrar algo como: APP_KEY=base64:xxxxxxxxxxxxx
# Si muestra APP_KEY= o APP_KEY=, necesitas generarlo
```

> 📖 **Para más detalles y opciones, consulta la sección [Generar APP_KEY](#generar-app_key) más abajo.**

### Paso 9: Desplegar la Aplicación

#### 9.1. Usar el Script de Despliegue (Recomendado)

```bash
# Asegúrate de estar en el directorio correcto
cd /var/apps/yape-notifier/infra/docker/environments/production

# Hacer el script ejecutable (primera vez)
chmod +x deploy.sh

# Ejecutar el script de despliegue
./deploy.sh

# O si necesitas rebuild completo sin cache:
./deploy.sh --no-cache
```

El script `deploy.sh` automáticamente:

1. **Valida configuración**: Verifica que `DB_PASSWORD` y `APP_KEY` estén configurados en `.env`
2. **Detiene contenedores**: Ejecuta `docker compose down --remove-orphans` para limpiar servicios anteriores
3. **Construye imágenes**: Construye las imágenes Docker (con cache por defecto, o sin cache si usas `--no-cache`)
4. **Inicia servicios**: Levanta todos los contenedores
5. **Espera PostgreSQL**: Usa un wait loop activo con `pg_isready` para asegurar que la base de datos esté lista
6. **Configura permisos**: Crea directorios necesarios y configura permisos de Laravel
7. **Ejecuta migraciones**: Ejecuta las migraciones de base de datos
8. **Optimiza Laravel**: Cachea configuración y rutas para producción

**⚠️ IMPORTANTE**: El script validará que `APP_KEY` existe antes de continuar. Si no está configurado, el despliegue fallará. Debes generar `APP_KEY` manualmente antes del primer despliegue (ver sección "Generar APP_KEY" más abajo).

#### 9.2. Despliegue Manual (Alternativa)

Si prefieres hacerlo manualmente:

```bash
# Ir al directorio de producción
cd /var/apps/yape-notifier/infra/docker/environments/production

# Construir imágenes (esto puede tardar varios minutos)
# Nota: Las imágenes incluyen todo el código y dependencias de Composer
docker compose --env-file .env build

# Iniciar todos los servicios
docker compose --env-file .env up -d

# Verificar que los contenedores estén corriendo y saludables
docker compose --env-file .env ps

# Verificar healthchecks
docker compose --env-file .env ps --format "table {{.Name}}\t{{.Status}}\t{{.Health}}"
```

**Nota importante**: En producción, las imágenes Docker contienen todo el código de la aplicación. No se montan volúmenes del host para el código, lo que asegura que:

- Las dependencias de Composer instaladas durante el build no se sobrescriban
- Los archivos optimizados de Laravel se mantengan intactos
- La aplicación sea completamente autocontenida y portable

Deberías ver algo como:

```
NAME                        STATUS
yape-notifier-php-fpm       Up
yape-notifier-nginx-api     Up
yape-notifier-dashboard     Up
yape-notifier-caddy         Up
yape-notifier-db            Up
```

#### 9.3. Ver Logs (Opcional)

```bash
# Ver logs de todos los servicios
docker compose --env-file .env logs -f

# O ver logs de un servicio específico
docker compose --env-file .env logs -f caddy
docker compose --env-file .env logs -f php-fpm
```

### Paso 10: Verificar el Despliegue

#### 9.1. Verificar Contenedores

```bash
# Ver estado de contenedores
docker compose --env-file .env ps

# Todos deberían estar "Up" y "healthy"
```

**Nota**: Si usaste `deploy.sh`, los siguientes pasos (10.2-10.5) ya fueron ejecutados automáticamente. Solo necesitas verificar que todo funcione.

#### 10.2. Verificar APP_KEY de Laravel

```bash
# Verificar que APP_KEY esté configurado (debería estar desde antes del despliegue)
docker compose --env-file .env exec php-fpm cat /var/www/.env | grep APP_KEY

# Si necesitas generar un nuevo APP_KEY (solo si no existe):
# ⚠️ CUIDADO: Esto regenerará la clave, lo que invalidará sesiones y datos encriptados existentes
docker compose --env-file .env exec php-fpm php artisan key:generate --show

# Luego actualiza el .env en el host con la nueva clave generada
nano .env
# Busca APP_KEY y reemplaza con el valor mostrado
```

**Nota**: En producción, `APP_KEY` debe estar configurado ANTES del despliegue. El script `deploy.sh` validará esto automáticamente.

#### 10.3. Verificar Migraciones

```bash
# Verificar que las migraciones estén ejecutadas
docker compose --env-file .env exec php-fpm php artisan migrate:status
```

#### 10.4. Verificar Health Checks

```bash
# Verificar healthchecks de todos los servicios
docker compose --env-file .env ps --format "table {{.Name}}\t{{.Status}}\t{{.Health}}"

# Todos los servicios deberían mostrar "healthy" después de unos minutos

# Verificar endpoint de healthcheck de Nginx (estático, no requiere PHP-FPM)
curl http://127.0.0.1/up
# Deberías ver: OK

# Verificar API completa (desde el servidor, a través de Caddy)
curl http://localhost/up
# O desde fuera: curl https://api.notificaciones.space/up

# Verificar Dashboard (desde el servidor)
curl http://localhost/
# Deberías ver el HTML del dashboard
```

**Nota sobre Healthchecks**:

- **Nginx**: Usa endpoint estático `/up` que responde directamente sin pasar por PHP-FPM
- **PHP-FPM**: Verifica que PHP esté funcionando correctamente
- **PostgreSQL**: Verifica que la base de datos esté lista para aceptar conexiones
- Todos los healthchecks están configurados para verificar el estado real de cada servicio

#### 10.5. Verificar desde el Navegador

Espera unos minutos para que Caddy obtenga los certificados SSL automáticamente, luego:

1. **Abre en tu navegador:**

   - API: `https://api.notificaciones.space/up`
   - Dashboard: `https://dashboard.notificaciones.space`

2. **Verifica que:**
   - ✅ El certificado SSL esté activo (candado verde)
   - ✅ La API responda correctamente
   - ✅ El Dashboard cargue sin errores

---

## ☁️ Opción 2: Digital Ocean App Platform (Alternativa)

Digital Ocean App Platform es un servicio PaaS que facilita el despliegue automático desde GitHub, ideal si prefieres menos configuración manual.

### Ventajas y Desventajas

**Ventajas:**

- ✅ Despliegue automático desde GitHub
- ✅ Escalado automático
- ✅ SSL/HTTPS incluido
- ✅ Menos configuración manual

**Desventajas:**

- ❌ Menos control sobre la infraestructura
- ❌ Más costoso
- ❌ URLs largas por defecto (a menos que uses dominio personalizado)

### Pasos Rápidos

1. **Crear Base de Datos PostgreSQL** en Digital Ocean Dashboard → Databases
2. **Crear App API** en App Platform:
   - Source Directory: `apps/api`
   - Build Command: `composer install --no-dev --optimize-autoloader --no-interaction`
   - Run Command: `php artisan migrate --force && php artisan config:cache && php artisan route:cache && php artisan serve --host=0.0.0.0 --port=$PORT`
3. **Crear App Dashboard** en App Platform:
   - Source Directory: `apps/web-dashboard`
   - Build Command: `npm ci && npm run build`
   - Run Command: `npx serve -s dist -l $PORT`
4. **Configurar Variables de Entorno** (ver sección correspondiente)
5. **Configurar Dominio Personalizado** (opcional)

> 📖 **Nota**: Para una guía detallada de App Platform, consulta la documentación oficial de Digital Ocean.

---

## ⚙️ Configuración de Variables de Entorno

### Archivos de Plantilla Disponibles

El proyecto incluye archivos de ejemplo con todas las variables necesarias:

- **`infra/docker/environments/production/.env.example`** - Plantilla para producción
- **`infra/docker/environments/staging/.env.example`** - Plantilla para staging
- **`infra/docker/environments/development/.env.example`** - Plantilla para desarrollo

### Crear Archivos .env desde Plantillas

```bash
# Producción
cd infra/docker/environments/production
cp .env.example .env
nano .env  # Configurar valores reales

# Staging
cd infra/docker/environments/staging
cp .env.example .env
nano .env  # Configurar valores reales

# Development
cd infra/docker/environments/development
cp .env.example .env
nano .env  # Configurar valores reales
```

### Variables Requeridas para Producción

#### Variables para Docker Compose (en `infra/docker/environments/production/.env`)

**OBLIGATORIA**:

```env
DB_PASSWORD=tu_contraseña_segura_aqui  # ⚠️ DEBES cambiar esto
```

**Opcionales** (con valores por defecto en `docker-compose.yml`):

```env
DB_DATABASE=yape_notifier              # Default: yape_notifier
DB_USERNAME=postgres                   # Default: postgres
APP_URL=https://api.notificaciones.space  # Default: https://api.notificaciones.space
DASHBOARD_API_URL=https://api.notificaciones.space  # Default: https://api.notificaciones.space
```

#### Variables para Laravel (dentro del contenedor)

El archivo `.env.example` contiene **todas** las variables que Laravel necesita. La mayoría ya tienen valores por defecto adecuados para producción. Las más importantes:

```env
# Base de Datos (se sincronizan con las de Docker Compose)
DB_CONNECTION=pgsql
DB_HOST=db
DB_PORT=5432
DB_DATABASE=yape_notifier
DB_USERNAME=postgres
DB_PASSWORD=tu_contraseña_segura_aqui  # ⚠️ Mismo valor que DB_PASSWORD de Docker Compose

# Aplicación Laravel
APP_NAME="Yape Notifier API"
APP_ENV=production
APP_DEBUG=false
APP_KEY=base64:TU_CLAVE_AQUI          # ⚠️ OBLIGATORIO: Debe generarse manualmente antes del despliegue
APP_URL=https://api.notificaciones.space

# Sesiones, Cache, Queue, etc. (ya configurados con valores por defecto)
SESSION_DRIVER=database
CACHE_DRIVER=file
QUEUE_CONNECTION=database
LOG_CHANNEL=stderr
LOG_LEVEL=error
```

**Nota importante**:

- El archivo `.env.example` ya contiene valores por defecto para todas las variables de Laravel
- **OBLIGATORIO antes del primer despliegue**: Configurar `DB_PASSWORD` y `APP_KEY`
- ⚠️ **En producción, `APP_KEY` NO se genera automáticamente**. Debes generarlo manualmente antes del despliegue
- Las demás variables se pueden ajustar después si es necesario

### Generar APP_KEY

**⚠️ IMPORTANTE**: `APP_KEY` debe generarse ANTES del primer despliegue. El script `deploy.sh` validará que existe y abortará si no está configurado.

**Opción 1: Generar desde tu máquina local (Recomendado)**

Si tienes PHP y Composer instalados localmente:

```bash
# Desde el directorio de la API
cd apps/api

# Generar APP_KEY
php artisan key:generate --show

# Copia la clave generada (ej: base64:xxxxxxxxxxxxx)
# Luego edita el .env de producción y pega el valor
```

**Opción 2: Generar desde contenedor temporal**

Si no tienes PHP local, puedes usar un contenedor temporal:

```bash
# Desde el directorio de producción
cd /var/apps/yape-notifier/infra/docker/environments/production

# Crear contenedor temporal con PHP
docker run --rm -v $(pwd)/../../../../apps/api:/var/www -w /var/www php:8.2-cli php artisan key:generate --show

# Copia la clave generada y actualiza .env
nano .env
# Busca APP_KEY= y reemplaza con el valor generado
```

**Opción 3: Generar manualmente (avanzado)**

Si prefieres generar la clave manualmente:

```bash
# Generar una clave base64 aleatoria
php -r "echo 'base64:' . base64_encode(random_bytes(32)) . PHP_EOL;"
```

**Después de generar APP_KEY:**

1. Edita el archivo `.env` en `infra/docker/environments/production/`
2. Busca la línea `APP_KEY=`
3. Reemplaza con el valor generado (ej: `APP_KEY=base64:xxxxxxxxxxxxx`)
4. Guarda el archivo
5. Ahora puedes ejecutar `./deploy.sh` de forma segura

---

## 🐛 Solución de Problemas

### Error: Healthcheck fallando en Nginx o PostgreSQL

**Causa**: Los healthchecks pueden fallar si no están configurados correctamente.

**Solución**:

```bash
# Verificar estado de healthchecks
docker compose --env-file .env ps --format "table {{.Name}}\t{{.Status}}\t{{.Health}}"

# Ver logs de healthcheck de Nginx
docker inspect yape-notifier-nginx-api-prod | grep -A 10 Health

# Verificar que el endpoint /up responda
docker compose --env-file .env exec nginx-api wget -qO- http://127.0.0.1/up
# Debería mostrar: OK

# Verificar healthcheck de PostgreSQL
docker compose --env-file .env exec db pg_isready -U postgres -d yape_notifier
# Debería mostrar: postgres:5432 - accepting connections
```

**Nota**: Los healthchecks están configurados para:

- **Nginx**: Usar `127.0.0.1` en lugar de `localhost` para evitar problemas con IPv6
- **PostgreSQL**: Usar variables de entorno del contenedor (`$$POSTGRES_USER`, `$$POSTGRES_DB`) para evitar expansión prematura

### Error: "APP_KEY no está configurado en .env"

**Causa**: El script `deploy.sh` valida que `APP_KEY` existe y está configurado correctamente antes de continuar. Si no está configurado, el despliegue abortará.

**Solución paso a paso**:

```bash
# 1. Ir al directorio de producción
cd /var/apps/yape-notifier/infra/docker/environments/production

# 2. Verificar el contenido actual de APP_KEY
grep "^APP_KEY" .env

# 3. Si está vacío o no existe, generar APP_KEY
# Opción A: Si tienes PHP local
cd ../../../../apps/api
php artisan key:generate --show
# Copia la clave generada (ej: base64:xxxxxxxxxxxxx)

# Opción B: Usar contenedor temporal
cd /var/apps/yape-notifier/infra/docker/environments/production
docker run --rm -v $(pwd)/../../../../apps/api:/var/www -w /var/www php:8.2-cli php artisan key:generate --show
# Copia la clave generada

# 4. Editar .env y configurar APP_KEY
nano .env

# Busca la línea que dice:
# APP_KEY=
# O
# APP_KEY=base64:
#
# Cámbiala por:
# APP_KEY=base64:tu_clave_generada_aqui
#
# Ejemplo:
# APP_KEY=base64:2x3y4z5a6b7c8d9e0f1g2h3i4j5k6l7m8n9o0p1q2r3s4t5u6v7w8x9y0z

# 5. Verificar que se guardó correctamente (debe mostrar tu clave)
grep "^APP_KEY" .env

# 6. Verificar que NO está vacío ni tiene el valor placeholder
if grep -q "^APP_KEY=$" .env || ! grep -q "^APP_KEY=base64:" .env; then
    echo "ERROR: APP_KEY aún no está configurado correctamente"
    echo "Por favor, edita .env y configura APP_KEY con un valor válido"
    exit 1
fi

# 7. Ahora intentar de nuevo
./deploy.sh
```

**⚠️ IMPORTANTE**:

- `APP_KEY` es **OBLIGATORIO** y debe tener un valor válido en formato `base64:...`
- No puede estar vacío (`APP_KEY=`)
- No puede tener solo el prefijo (`APP_KEY=base64:`)
- Debe generarse ANTES del primer despliegue
- En producción, NUNCA debe regenerarse automáticamente (invalidaría sesiones y datos encriptados)

### Error: "The DB_PASSWORD variable is not set"

**Causa**: El archivo `.env` no existe o no tiene la variable `DB_PASSWORD` configurada correctamente.

**Solución paso a paso**:

```bash
# 1. Ir al directorio de producción
cd /var/apps/yape-notifier/infra/docker/environments/production

# 2. Verificar si existe .env
ls -la .env

# 3. Si no existe, crearlo desde la plantilla
if [ ! -f ".env" ]; then
    cp .env.example .env
    echo "Archivo .env creado desde .env.example"
fi

# 4. Verificar el contenido actual de DB_PASSWORD
grep "^DB_PASSWORD" .env

# 5. Editar y configurar DB_PASSWORD (OBLIGATORIO)
nano .env

# Busca la línea que dice:
# DB_PASSWORD=TU_CONTRASEÑA_SEGURA_AQUI
# O
# DB_PASSWORD=
#
# Cámbiala por:
# DB_PASSWORD=tu_contraseña_segura_real_aqui
#
# Ejemplo:
# DB_PASSWORD=MiContraseñaSegura123!@#

# 6. Verificar que se guardó correctamente (debe mostrar tu contraseña)
grep "^DB_PASSWORD" .env

# 7. Verificar que NO está vacío ni tiene el valor placeholder
if grep -q "^DB_PASSWORD=$" .env || grep -q "TU_CONTRASEÑA" .env; then
    echo "ERROR: DB_PASSWORD aún no está configurado correctamente"
    echo "Por favor, edita .env y configura DB_PASSWORD con un valor real"
    exit 1
fi

# 8. Ahora intentar de nuevo
docker compose --env-file .env up -d
```

**⚠️ IMPORTANTE**:

- `DB_PASSWORD` es **OBLIGATORIO** y debe tener un valor real
- No puede estar vacío (`DB_PASSWORD=`)
- No puede tener el valor placeholder (`DB_PASSWORD=TU_CONTRASEÑA_SEGURA_AQUI`)
- Debe ser una contraseña segura (mínimo 12 caracteres, con mayúsculas, minúsculas, números y símbolos)

### Error: "Certificate not obtained" (Caddy)

**Causa**: DNS no propagado o subdominios incorrectos

**Solución**:

```bash
# Verificar DNS
nslookup api.notificaciones.space
nslookup dashboard.notificaciones.space

# Ver logs de Caddy
docker compose --env-file .env logs caddy

# Verificar que los subdominios en Caddyfile coincidan con DNS
```

### Error: "502 Bad Gateway"

**Causa**: Nginx o PHP-FPM no están corriendo

**Solución**:

```bash
# Verificar contenedores
docker compose --env-file .env ps

# Reiniciar servicios
docker compose --env-file .env restart nginx-api php-fpm

# Ver logs
docker compose --env-file .env logs nginx-api
docker compose --env-file .env logs php-fpm
```

### Error: "Container yape-notifier-db is unhealthy" o "dependency db failed to start"

**Causa**: La base de datos PostgreSQL no puede iniciar correctamente, generalmente por:

1. `DB_PASSWORD` no configurado o vacío en `.env.production`
2. Variables de entorno incorrectas
3. Volumen de datos corrupto
4. Healthcheck fallando

**Solución paso a paso**:

```bash
# 1. Verificar que .env existe y tiene DB_PASSWORD
cd /var/apps/yape-notifier/infra/docker/environments/production
cat .env | grep DB_PASSWORD

# Si no existe o está vacío, crearlo/editar:
cp .env.example .env
nano .env
# Asegúrate de que DB_PASSWORD tenga un valor (ej: DB_PASSWORD=tu_contraseña_segura_123)

# 2. Ver logs del contenedor de base de datos
docker compose --env-file .env logs db

# 3. Si hay errores de permisos o datos corruptos, eliminar el volumen (CUIDADO: esto borra los datos)
docker compose --env-file .env down -v
# Luego volver a levantar
docker compose --env-file .env up -d db

# 4. Esperar a que la base de datos esté healthy (puede tardar 30-60 segundos)
docker compose --env-file .env ps db
# Debe mostrar "healthy" en el estado

# 5. Si sigue fallando, iniciar solo la base de datos primero
docker compose --env-file .env up -d db
# Esperar 30 segundos
docker compose --env-file .env logs -f db
# Presiona Ctrl+C cuando veas "database system is ready to accept connections"

# 6. Luego iniciar el resto de servicios
docker compose --env-file .env up -d
```

**Diagnóstico avanzado**:

```bash
# Ver el estado detallado del healthcheck
docker inspect yape-notifier-db | grep -A 10 Health

# Probar conexión manual a PostgreSQL
docker compose exec db psql -U postgres -d yape_notifier -c "SELECT version();"

# Verificar variables de entorno del contenedor
docker compose exec db env | grep POSTGRES
```

### Error: "Database connection failed"

**Causa**: Variables de entorno incorrectas o base de datos no iniciada

**Solución**:

```bash
# Verificar que la base de datos esté corriendo
docker compose --env-file .env ps db

# Verificar variables de entorno
docker compose --env-file .env exec php-fpm env | grep DB_

# Probar conexión
docker compose --env-file .env exec php-fpm php artisan tinker
# Luego en tinker: DB::connection()->getPdo();
```

### Error: "Permission denied" en storage

**Causa**: Los permisos de los directorios de Laravel no están configurados correctamente.

**Solución**:

```bash
# Los permisos deberían estar configurados automáticamente en el Dockerfile
# Si aún hay problemas, ejecutar manualmente:

docker compose --env-file .env exec php-fpm chown -R www-data:www-data /var/www/storage
docker compose --env-file .env exec php-fpm chown -R www-data:www-data /var/www/bootstrap/cache
docker compose --env-file .env exec php-fpm chmod -R 775 /var/www/storage
docker compose --env-file .env exec php-fpm chmod -R 775 /var/www/bootstrap/cache
```

**Nota**: En producción, los permisos se configuran automáticamente durante el build de la imagen Docker. Si necesitas ajustarlos, puedes reconstruir la imagen o ejecutar los comandos anteriores.

### Dashboard no se conecta a la API

**Causa**: `VITE_API_BASE_URL` incorrecta o CORS mal configurado

**Solución**:

```bash
# Verificar variable en .env
grep DASHBOARD_API_URL .env

# Reconstruir dashboard con la URL correcta
docker compose --env-file .env build dashboard
docker compose --env-file .env up -d dashboard

# Verificar CORS en Laravel
docker compose --env-file .env exec php-fpm php artisan config:clear
docker compose --env-file .env exec php-fpm php artisan config:cache
```

### Ver Logs Detallados

```bash
# Todos los logs
docker compose --env-file .env logs -f

# Logs específicos
docker compose --env-file .env logs -f caddy
docker compose --env-file .env logs -f php-fpm
docker compose --env-file .env logs -f nginx-api
docker compose --env-file .env logs -f dashboard
docker compose --env-file .env logs -f db
```

---

## 🔄 Mantenimiento y Actualizaciones

### Actualizar Código

**⚠️ IMPORTANTE**: Al actualizar, el script `deploy.sh` ahora:

- Valida que `APP_KEY` existe (no lo regenera)
- Detiene contenedores ANTES de construir imágenes
- Usa espera activa para PostgreSQL (no sleep fijo)
- Usa cache por defecto (usa `--no-cache` solo si es necesario)

```bash
# Ir al directorio del proyecto
cd /var/apps/yape-notifier

# Actualizar código
git pull origin main
# O si estás en otra rama: git pull origin master

# Reconstruir imágenes (importante: esto reinstala dependencias de Composer)
cd infra/docker/environments/production

# Opción 1: Usar el script de despliegue (recomendado)
# Con cache (más rápido, usa cache de Docker)
./deploy.sh

# O sin cache (rebuild completo, más lento pero más seguro)
./deploy.sh --no-cache

# Opción 2: Despliegue manual
docker compose --env-file .env down --remove-orphans
docker compose --env-file .env build  # O con --no-cache si necesitas rebuild completo
docker compose --env-file .env up -d

# Ejecutar migraciones si hay nuevas
docker compose --env-file .env exec php-fpm php artisan migrate --force

# Limpiar y optimizar cache de Laravel
docker compose --env-file .env exec php-fpm php artisan config:clear
docker compose --env-file .env exec php-fpm php artisan config:cache
docker compose --env-file .env exec php-fpm php artisan route:cache
docker compose --env-file .env exec php-fpm php artisan view:cache

# Verificar que todo esté funcionando
docker compose --env-file .env ps
```

**Nota importante**: Al actualizar el código, es necesario reconstruir las imágenes Docker porque el código está incluido en las imágenes, no montado desde el host. Esto asegura que:

- Las nuevas dependencias de Composer se instalen correctamente
- Los cambios en el código se reflejen en la aplicación
- La aplicación mantenga su estado autocontenido

### Reiniciar Servicios

```bash
# Reiniciar todos los servicios
docker compose --env-file .env restart

# Reiniciar un servicio específico
docker compose --env-file .env restart php-fpm
docker compose --env-file .env restart caddy
```

### Ver Uso de Recursos

```bash
# Ver uso de recursos de contenedores
docker stats

# Ver espacio en disco
df -h

# Ver volúmenes Docker
docker volume ls
```

### Script de Actualización Automática

Crea un script `/var/apps/yape-notifier/update.sh`:

```bash
#!/bin/bash
set -e

cd /var/apps/yape-notifier
git pull origin main
cd infra/docker/environments/production

docker compose --env-file .env build
docker compose --env-file .env up -d
docker compose --env-file .env exec php-fpm php artisan migrate --force
docker compose --env-file .env exec php-fpm php artisan config:cache
docker compose --env-file .env exec php-fpm php artisan route:cache

echo "✅ Actualización completada"
```

Hazlo ejecutable:

```bash
chmod +x /var/apps/yape-notifier/update.sh
```

---

## 💾 Backup y Recuperación

Para una guía completa de backup y disaster recovery, consulta:

- **`infra/docker/environments/production/BACKUP.md`** - Estrategia completa de backup, scripts automatizados, disaster recovery plan y almacenamiento remoto

---

## ✅ Checklist de Despliegue

### Pre-Deployment

- [ ] Droplet creado y accesible
- [ ] DNS configurado y propagado
- [ ] Docker y Docker Compose instalados
- [ ] Repositorio clonado
- [ ] `.env` configurado correctamente en `infra/docker/environments/production/` con `DB_PASSWORD` y `APP_KEY`
- [ ] `Caddyfile` configurado con subdominios correctos (ya está configurado)

### Deployment

- [ ] Contenedores corriendo y saludables
- [ ] `APP_KEY` configurado en `.env` (generado manualmente antes del despliegue)
- [ ] Migraciones ejecutadas
- [ ] Permisos de storage configurados
- [ ] Certificados SSL obtenidos por Caddy
- [ ] API accesible en `https://api.notificaciones.space`
- [ ] Dashboard accesible en `https://dashboard.notificaciones.space`
- [ ] Dashboard se conecta correctamente a la API
- [ ] Logs sin errores críticos

### Post-Deployment

- [ ] Backup de base de datos configurado
- [ ] Monitoreo configurado (opcional)
- [ ] Documentación actualizada

---

## 📚 Recursos Adicionales

- [Documentación de Digital Ocean](https://docs.digitalocean.com/)
- [Documentación de Docker](https://docs.docker.com/)
- [Documentación de Caddy](https://caddyserver.com/docs/)
- [Documentación de Laravel](https://laravel.com/docs)
- [Documentación de PostgreSQL](https://www.postgresql.org/docs/)

---

## 🆘 Soporte

Si encuentras problemas:

1. Revisa los logs: `docker compose --env-file .env logs -f` (desde `infra/docker/environments/production/`)
2. Verifica la configuración DNS
3. Verifica que los puertos estén abiertos en el firewall
4. Consulta la sección de [Solución de Problemas](#solución-de-problemas)

---

**¡Felicitaciones! Tu aplicación debería estar funcionando en producción con HTTPS automático.** 🎉
