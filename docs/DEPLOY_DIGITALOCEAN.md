# 🚀 Guía de Despliegue en Digital Ocean

Esta guía te ayudará a desplegar el API y Dashboard Web en Digital Ocean usando **App Platform** (recomendado) o **Droplets con Docker**.

---

## 📋 Tabla de Contenidos

1. [Opción 1: Digital Ocean App Platform (Recomendado)](#opción-1-digital-ocean-app-platform)
2. [Opción 2: Droplets con Docker](#opción-2-droplets-con-docker)
3. [Configuración de Base de Datos](#configuración-de-base-de-datos)
4. [Variables de Entorno](#variables-de-entorno)
5. [Solución de Problemas](#solución-de-problemas)

---

## 🎯 Opción 1: Digital Ocean App Platform

Digital Ocean App Platform es un servicio PaaS (Platform as a Service) similar a Render, que facilita el despliegue automático desde GitHub.

### 📍 ¿Cómo funcionan las URLs?

**Digital Ocean asigna automáticamente una URL** cuando despliegas una aplicación:

- **Formato**: `https://nombre-app-xxxxx.ondigitalocean.app`
- **Ejemplo API**: `https://yape-notifier-api-abc123.ondigitalocean.app`
- **Ejemplo Dashboard**: `https://yape-notifier-dashboard-xyz789.ondigitalocean.app`

**Opciones de URL:**

1. **Usar la URL automática** (gratis, funciona inmediatamente)

   - ✅ No requiere configuración adicional
   - ✅ SSL/HTTPS incluido automáticamente
   - ❌ URL larga y difícil de recordar

2. **Usar dominio personalizado** (recomendado para producción)
   - ✅ URLs profesionales: `api.tudominio.com`
   - ✅ Más fácil de recordar
   - ✅ Requiere configurar DNS en tu proveedor de dominios
   - ✅ SSL/HTTPS se configura automáticamente

**Flujo recomendado:**

1. Despliega primero con la URL automática
2. Obtén la URL asignada
3. Configura el dominio personalizado (opcional pero recomendado)
4. Actualiza las variables de entorno con la nueva URL

### Prerrequisitos

- ✅ Cuenta en Digital Ocean
- ✅ Repositorio en GitHub/GitLab/Bitbucket
- ✅ Tarjeta de crédito (necesaria para crear recursos)
- ✅ Dominio personalizado (opcional, pero recomendado)

### Paso 1: Crear Base de Datos PostgreSQL

1. Ve a [Digital Ocean Dashboard](https://cloud.digitalocean.com/)
2. Navega a **Databases** → **Create Database**
3. Selecciona:
   - **Engine**: PostgreSQL
   - **Version**: 15 (o la más reciente)
   - **Plan**: Basic ($15/mes) o el que prefieras
   - **Region**: Elige la región más cercana a tus usuarios
   - **Database Name**: `yape_notifier` (o el que prefieras)
4. Haz clic en **Create Database Cluster**
5. **Anota las credenciales** que se muestran (las necesitarás después)

### Paso 2: Crear la Aplicación API

1. En Digital Ocean Dashboard, ve a **App Platform** → **Create App**
2. Conecta tu repositorio de GitHub/GitLab
3. Selecciona el repositorio `yape-notifier`
4. En la configuración del servicio:

   **Configuración General:**

   - **Name**: `yape-notifier-api`
   - **Type**: Web Service
   - **Source Directory**: `apps/api`
   - **Branch**: `main` o `master`

   **Build Settings:**

   - **Build Command**:
     ```bash
     composer install --no-dev --optimize-autoloader --no-interaction
     ```
   - **Run Command**:
     ```bash
     php artisan migrate --force && php artisan config:cache && php artisan route:cache && php artisan serve --host=0.0.0.0 --port=$PORT
     ```

   **Dockerfile (Alternativa):**

   - Si prefieres usar Docker, selecciona **Dockerfile** como tipo de build
   - **Dockerfile Path**: `apps/api/Dockerfile.do` (crearemos este archivo)

   **Environment Variables:**

   ```
   APP_NAME=Yape Notifier API
   APP_ENV=production
   APP_DEBUG=false
   APP_KEY=base64:TU_CLAVE_GENERADA_AQUI
   APP_URL=https://tu-api.ondigitalocean.app
   APP_TIMEZONE=UTC

   DB_CONNECTION=pgsql
   DB_HOST=TU_DB_HOST
   DB_PORT=25060
   DB_DATABASE=defaultdb
   DB_USERNAME=doadmin
   DB_PASSWORD=TU_PASSWORD

   SESSION_DRIVER=database
   SESSION_LIFETIME=120

   CACHE_DRIVER=file
   QUEUE_CONNECTION=database

   LOG_CHANNEL=stderr
   LOG_LEVEL=error
   ```

   **Nota**:

   - `APP_KEY`: Genera una clave con `php artisan key:generate` localmente o usa la que ya tienes
   - `DB_HOST`, `DB_PASSWORD`: Obtén estos valores de la base de datos creada en el Paso 1
   - `APP_URL`: **Deja este valor temporalmente** (lo actualizarás después del primer deploy)

5. Haz clic en **Next** → **Add Resource** → **Database**
6. Selecciona la base de datos creada en el Paso 1
7. Haz clic en **Next** → **Review** → **Create Resources**

### Paso 2.1: Obtener la URL del API

Después de crear la aplicación, Digital Ocean asignará automáticamente una URL con el formato:

```
https://yape-notifier-api-xxxxx.ondigitalocean.app
```

**Para obtener la URL:**

1. Ve a tu aplicación en **App Platform**
2. La URL aparece en la parte superior de la página, o en **Settings** → **App Details**
3. **Copia esta URL** - la necesitarás para:
   - Actualizar la variable `APP_URL` en las variables de entorno
   - Configurar `VITE_API_BASE_URL` en el dashboard

**Actualizar APP_URL:**

1. Ve a **Settings** → **App-Level Environment Variables**
2. Actualiza `APP_URL` con la URL real que obtuviste:
   ```
   APP_URL=https://yape-notifier-api-xxxxx.ondigitalocean.app
   ```
3. Guarda los cambios (esto reiniciará la aplicación)

### Paso 3: Crear la Aplicación Dashboard Web

1. En Digital Ocean Dashboard, ve a **App Platform** → **Create App**
2. Conecta el mismo repositorio
3. En la configuración:

   **Configuración General:**

   - **Name**: `yape-notifier-dashboard`
   - **Type**: Web Service
   - **Source Directory**: `apps/web-dashboard`
   - **Branch**: `main` o `master`

   **Build Settings:**

   - **Build Command**:
     ```bash
     npm ci && npm run build
     ```
   - **Run Command**:
     ```bash
     npx serve -s dist -l $PORT
     ```

   **O usando Dockerfile:**

   - **Dockerfile Path**: `apps/web-dashboard/Dockerfile`

   **Environment Variables:**

   ```
   VITE_API_BASE_URL=https://yape-notifier-api-xxxxx.ondigitalocean.app
   NODE_ENV=production
   ```

   **Nota**:

   - `VITE_API_BASE_URL`: **DEBE ser la URL real de tu API** obtenida en el Paso 2.1 (formato: `https://yape-notifier-api-xxxxx.ondigitalocean.app`)
   - ⚠️ **IMPORTANTE**: Esta variable se usa durante el build, así que debe estar correcta desde el inicio
   - Si usas Dockerfile, el build argument se pasa automáticamente

4. Haz clic en **Next** → **Review** → **Create Resources**

### Paso 4: Configurar Dominio Personalizado (Recomendado)

**¿Por qué usar un dominio personalizado?**

- ✅ URLs más profesionales: `api.tudominio.com` en lugar de `yape-notifier-api-xxxxx.ondigitalocean.app`
- ✅ Más fácil de recordar y compartir
- ✅ Mejor para producción
- ✅ Puedes usar subdominios: `api.tudominio.com` y `dashboard.tudominio.com`

**Pasos para configurar dominio personalizado:**

#### Para el API:

1. En tu aplicación API, ve a **Settings** → **Domains**
2. Haz clic en **Add Domain**
3. Ingresa tu dominio (ej: `api.tudominio.com`)
4. Digital Ocean te mostrará los registros DNS que debes configurar:
   - **Tipo**: CNAME
   - **Nombre**: `api` (o el subdominio que prefieras)
   - **Valor**: `yape-notifier-api-xxxxx.ondigitalocean.app` (la URL asignada por Digital Ocean)
5. Ve a tu proveedor de DNS (donde compraste el dominio) y agrega el registro CNAME
6. Espera a que se propague el DNS (puede tardar unos minutos hasta 24 horas)
7. Una vez verificado, Digital Ocean configurará automáticamente SSL/HTTPS

#### Para el Dashboard:

1. Repite el mismo proceso con un subdominio diferente (ej: `dashboard.tudominio.com`)

#### Actualizar Variables de Entorno:

Después de configurar el dominio personalizado:

1. **API**: Actualiza `APP_URL`:

   ```
   APP_URL=https://api.tudominio.com
   ```

2. **Dashboard**: Actualiza `VITE_API_BASE_URL` y **reconstruye** la aplicación:
   ```
   VITE_API_BASE_URL=https://api.tudominio.com
   ```
   ⚠️ **Importante**: Como `VITE_API_BASE_URL` se usa en el build, necesitas hacer un nuevo deploy después de cambiar esta variable.

**Ejemplo de configuración DNS:**

```
Tipo    Nombre      Valor
CNAME   api         yape-notifier-api-xxxxx.ondigitalocean.app
CNAME   dashboard   yape-notifier-dashboard-xxxxx.ondigitalocean.app
```

### Paso 5: Ejecutar Migraciones

Después del primer deploy del API:

1. Ve a tu aplicación API en App Platform
2. Haz clic en **Console** (en la barra lateral)
3. Ejecuta:
   ```bash
   php artisan migrate --force
   ```

---

## 🐳 Opción 2: Droplets con Docker

Si prefieres más control sobre la infraestructura, puedes usar Droplets con Docker.

### Prerrequisitos

- ✅ Droplet de Digital Ocean (mínimo 2GB RAM, $12/mes)
- ✅ Docker y Docker Compose instalados
- ✅ Dominio configurado (opcional)

### Paso 1: Crear Droplet

1. Ve a **Droplets** → **Create Droplet**
2. Selecciona:
   - **Image**: Ubuntu 22.04 LTS
   - **Plan**: Basic ($12/mes mínimo, recomendado $24/mes)
   - **Region**: Elige la región más cercana
   - **Authentication**: SSH keys (recomendado) o Password
3. Haz clic en **Create Droplet**

### Paso 2: Configurar Droplet

Conéctate a tu Droplet vía SSH:

```bash
ssh root@TU_IP_DROPLET
```

Instala Docker y Docker Compose:

```bash
# Actualizar sistema
apt update && apt upgrade -y

# Instalar Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# Instalar Docker Compose
apt install docker-compose-plugin -y

# Verificar instalación
docker --version
docker compose version
```

### Paso 3: Clonar Repositorio

```bash
# Instalar Git
apt install git -y

# Clonar repositorio
cd /opt
git clone https://github.com/TU_USUARIO/yape-notifier.git
cd yape-notifier
```

### Paso 4: Configurar Base de Datos

**Opción A: Base de Datos Externa (Recomendado)**

Usa la base de datos de Digital Ocean creada en la Opción 1, o crea una nueva.

**Opción B: Base de Datos en el Droplet**

Edita `infra/docker/docker-compose.yml` y asegúrate de que el servicio `db` esté configurado.

### Paso 5: Configurar Variables de Entorno

Crea archivos `.env`:

```bash
# En apps/api
cd apps/api
cp .env.example .env
nano .env
```

Configura las variables (ver sección [Variables de Entorno](#variables-de-entorno))

```bash
# En infra/docker
cd ../../infra/docker
cp .env.example .env
nano .env
```

### Paso 6: Construir y Desplegar

```bash
cd /opt/yape-notifier/infra/docker

# Construir imágenes
docker compose build

# Iniciar servicios
docker compose up -d

# Ver logs
docker compose logs -f

# Ejecutar migraciones
docker compose exec app php artisan migrate --force
```

### Paso 7: Configurar Nginx Reverse Proxy (Opcional)

Para usar dominios personalizados y SSL:

```bash
# Instalar Nginx
apt install nginx certbot python3-certbot-nginx -y

# Configurar Nginx
nano /etc/nginx/sites-available/yape-notifier
```

Configuración de ejemplo:

```nginx
server {
    listen 80;
    server_name api.tudominio.com;

    location / {
        proxy_pass http://localhost:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

server {
    listen 80;
    server_name dashboard.tudominio.com;

    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
# Habilitar sitio
ln -s /etc/nginx/sites-available/yape-notifier /etc/nginx/sites-enabled/
nginx -t
systemctl reload nginx

# Obtener certificado SSL
certbot --nginx -d api.tudominio.com -d dashboard.tudominio.com
```

---

## 🗄️ Configuración de Base de Datos

### Variables de Conexión

Para App Platform, las variables se configuran automáticamente cuando conectas la base de datos.

Para Droplets, usa estas variables:

```env
DB_CONNECTION=pgsql
DB_HOST=TU_DB_HOST
DB_PORT=25060
DB_DATABASE=defaultdb
DB_USERNAME=doadmin
DB_PASSWORD=TU_PASSWORD
```

**Nota**:

- El puerto para bases de datos externas de Digital Ocean suele ser `25060`
- Para bases de datos locales en Docker, usa `5432`

---

## 🔐 Variables de Entorno

### API (Laravel)

```env
# Aplicación
APP_NAME="Yape Notifier API"
APP_ENV=production
APP_DEBUG=false
APP_KEY=base64:TU_CLAVE_AQUI
APP_URL=https://tu-api.ondigitalocean.app
APP_TIMEZONE=UTC

# Base de Datos
DB_CONNECTION=pgsql
DB_HOST=TU_DB_HOST
DB_PORT=25060
DB_DATABASE=defaultdb
DB_USERNAME=doadmin
DB_PASSWORD=TU_PASSWORD

# Sesiones
SESSION_DRIVER=database
SESSION_LIFETIME=120
SESSION_ENCRYPT=false

# Cache y Queue
CACHE_DRIVER=file
QUEUE_CONNECTION=database

# Logs
LOG_CHANNEL=stderr
LOG_LEVEL=error
```

### Dashboard Web

```env
VITE_API_BASE_URL=https://tu-api.ondigitalocean.app
NODE_ENV=production
```

**Importante**:

- `VITE_API_BASE_URL` debe ser la URL completa de tu API
- Esta variable se usa durante el build, no en runtime

---

## 🔧 Solución de Problemas

### Error: "Application failed to respond"

**Causa**: El comando de inicio no es correcto o el puerto no coincide.

**Solución**:

- Verifica que el `Run Command` use `$PORT` (variable de entorno de Digital Ocean)
- Revisa los logs en **Runtime Logs**

### Error: "Database connection failed"

**Causa**: Variables de entorno incorrectas o base de datos no accesible.

**Solución**:

- Verifica que las variables `DB_*` sean correctas
- Asegúrate de que la base de datos esté en la misma región
- Para bases de datos externas, verifica el firewall/trusted sources

### Error: "Build failed"

**Causa**: Dependencias no instaladas o comando de build incorrecto.

**Solución**:

- Verifica que el `Build Command` sea correcto
- Revisa los logs de build
- Asegúrate de que el `Source Directory` apunte al directorio correcto

### Dashboard no se conecta a la API

**Causa**: `VITE_API_BASE_URL` incorrecta o CORS no configurado.

**Solución**:

- Verifica que `VITE_API_BASE_URL` sea la URL correcta de la API
- Configura CORS en Laravel (`config/cors.php`)
- Reconstruye el dashboard después de cambiar la variable

### Migraciones fallan

**Causa**: Permisos o conexión a base de datos.

**Solución**:

- Ejecuta migraciones manualmente desde la consola
- Verifica permisos de la base de datos
- Asegúrate de que el usuario tenga permisos para crear tablas

---

## 📊 Monitoreo y Logs

### App Platform

- **Logs**: Ve a tu aplicación → **Runtime Logs**
- **Métricas**: Ve a **Insights** para ver CPU, memoria, requests, etc.

### Droplets

```bash
# Ver logs de Docker
docker compose logs -f

# Ver logs de Laravel
docker compose exec app tail -f storage/logs/laravel.log

# Ver uso de recursos
htop
df -h
```

---

## 🔄 Actualizaciones Automáticas

### App Platform

Por defecto, App Platform hace deploy automático cuando haces push a la rama configurada.

Para desactivar:

1. Ve a **Settings** → **App-Level Settings**
2. Desactiva **Auto Deploy**

### Droplets

Crea un script de actualización:

```bash
#!/bin/bash
cd /opt/yape-notifier
git pull origin main
cd infra/docker
docker compose build
docker compose up -d
docker compose exec app php artisan migrate --force
docker compose exec app php artisan config:cache
docker compose exec app php artisan route:cache
```

Guarda como `/opt/yape-notifier/update.sh` y hazlo ejecutable:

```bash
chmod +x /opt/yape-notifier/update.sh
```

---

## 💰 Costos Estimados

### App Platform

- **API**: ~$5-12/mes (Basic plan)
- **Dashboard**: ~$5-12/mes (Basic plan)
- **Base de Datos**: ~$15/mes (Basic PostgreSQL)
- **Total**: ~$25-39/mes

### Droplets

- **Droplet**: ~$12-24/mes
- **Base de Datos**: ~$15/mes (opcional, puede estar en el Droplet)
- **Total**: ~$12-39/mes

---

## ✅ Checklist de Despliegue

- [ ] Base de datos PostgreSQL creada
- [ ] API desplegada y funcionando
- [ ] Dashboard desplegado y funcionando
- [ ] Variables de entorno configuradas
- [ ] Migraciones ejecutadas
- [ ] Dominio personalizado configurado (opcional)
- [ ] SSL/HTTPS configurado
- [ ] CORS configurado correctamente
- [ ] Logs monitoreados
- [ ] Backup de base de datos configurado

---

## 📚 Recursos Adicionales

- [Documentación de Digital Ocean App Platform](https://docs.digitalocean.com/products/app-platform/)
- [Documentación de Laravel Deployment](https://laravel.com/docs/deployment)
- [Guía de Docker en Digital Ocean](https://docs.digitalocean.com/products/droplets/how-to/install-docker/)

---

¿Necesitas ayuda? Revisa los logs o consulta la documentación oficial de Digital Ocean.
