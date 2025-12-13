# 🚀 Guía Rápida: Despliegue en Digital Ocean

## Resumen de Pasos

### 1️⃣ Crear Base de Datos PostgreSQL

- Ve a **Databases** → **Create Database**
- Selecciona PostgreSQL 15
- Anota las credenciales (host, puerto, usuario, contraseña)

### 2️⃣ Desplegar API (Laravel)

**Opción A: Sin Docker (Buildpack)**

1. **App Platform** → **Create App** → Conecta tu repositorio
2. Configuración:
   - **Name**: `yape-notifier-api`
   - **Type**: Web Service
   - **Source Directory**: `apps/api`
   - **Build Command**: `composer install --no-dev --optimize-autoloader --no-interaction`
   - **Run Command**: `php artisan migrate --force && php artisan config:cache && php artisan route:cache && php artisan serve --host=0.0.0.0 --port=$PORT`

**Opción B: Con Docker**

1. Mismo proceso, pero selecciona **Dockerfile**
2. **Dockerfile Path**: `apps/api/Dockerfile.do`

**Variables de Entorno:**

```
APP_ENV=production
APP_DEBUG=false
APP_KEY=base64:TU_CLAVE_AQUI
APP_URL=https://yape-notifier-api-xxxxx.ondigitalocean.app
DB_CONNECTION=pgsql
DB_HOST=TU_DB_HOST
DB_PORT=25060
DB_DATABASE=defaultdb
DB_USERNAME=doadmin
DB_PASSWORD=TU_PASSWORD
```

**⚠️ IMPORTANTE - Obtener la URL del API:**

- Después del primer deploy, Digital Ocean asignará una URL automática
- Formato: `https://yape-notifier-api-xxxxx.ondigitalocean.app`
- Ve a tu app → **Settings** → **App Details** para ver la URL
- Actualiza `APP_URL` con la URL real después del primer deploy

### 3️⃣ Desplegar Dashboard Web

1. **App Platform** → **Create App** → Mismo repositorio
2. Configuración:
   - **Name**: `yape-notifier-dashboard`
   - **Type**: Web Service
   - **Source Directory**: `apps/web-dashboard`
   - **Build Command**: `npm ci && npm run build`
   - **Run Command**: `npx serve -s dist -l $PORT`

**O con Docker:**

- **Dockerfile Path**: `apps/web-dashboard/Dockerfile.do`

**Variables de Entorno:**

```
VITE_API_BASE_URL=https://yape-notifier-api-xxxxx.ondigitalocean.app
NODE_ENV=production
```

**⚠️ IMPORTANTE:**

- `VITE_API_BASE_URL` debe ser la **URL real** de tu API (obtenida en el paso 2)
- Esta variable se usa durante el build, así que debe estar correcta desde el inicio
- Si cambias la URL después, necesitarás hacer un nuevo deploy

### 4️⃣ Configurar Dominio Personalizado (Opcional pero Recomendado)

**¿Por qué?**

- URLs más profesionales: `api.tudominio.com` vs `yape-notifier-api-xxxxx.ondigitalocean.app`
- Más fácil de recordar y compartir

**Pasos:**

1. En cada app → **Settings** → **Domains** → **Add Domain**
2. Ingresa tu dominio (ej: `api.tudominio.com`)
3. Configura el registro CNAME en tu proveedor DNS
4. Espera la verificación (puede tardar minutos)
5. Actualiza `APP_URL` y `VITE_API_BASE_URL` con el nuevo dominio
6. **Reconstruye el dashboard** (porque `VITE_API_BASE_URL` se usa en el build)

### 5️⃣ Ejecutar Migraciones

Después del primer deploy del API:

1. Ve a tu app API → **Console**
2. Ejecuta: `php artisan migrate --force`

## ⚠️ Puntos Importantes

### URLs y Dominios

- ✅ Digital Ocean asigna automáticamente una URL: `https://nombre-app-xxxxx.ondigitalocean.app`
- ✅ Puedes usar esta URL directamente O configurar un dominio personalizado
- ✅ Si usas dominio personalizado, actualiza `APP_URL` y `VITE_API_BASE_URL`
- ✅ `VITE_API_BASE_URL` se usa en el build, así que si la cambias, necesitas reconstruir

### Variables de Entorno

- ✅ `APP_KEY`: Genera con `php artisan key:generate` localmente
- ✅ `APP_URL`: Actualiza con la URL real después del primer deploy
- ✅ `VITE_API_BASE_URL`: Debe ser la URL completa de tu API (obtenida después del deploy del API)
- ✅ `DB_HOST`, `DB_PASSWORD`: De la base de datos creada en paso 1
- ✅ El puerto se inyecta automáticamente como `$PORT` en Digital Ocean

### Orden de Despliegue

1. Primero despliega el **API**
2. Obtén la URL del API
3. Luego despliega el **Dashboard** con `VITE_API_BASE_URL` configurada

## 📚 Documentación Completa

Para más detalles, consulta: [DEPLOY_DIGITALOCEAN.md](./DEPLOY_DIGITALOCEAN.md)
