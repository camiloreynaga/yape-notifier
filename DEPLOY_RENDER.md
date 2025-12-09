# 🚀 Guía de Deploy en Render

Esta guía te ayudará a desplegar el backend Laravel en Render.

## 📋 Prerrequisitos

1. Cuenta en [Render](https://render.com) (gratis)
2. Repositorio en GitHub
3. Git configurado localmente

## 🔧 Pasos para Deploy

### 1. Subir el código a GitHub

Si aún no has subido el código:

```bash
# Verificar que estás en la rama master
git branch

# Agregar el remoto de GitHub (reemplaza con tu URL)
git remote add origin https://github.com/TU_USUARIO/yape-notifier.git

# Subir el código
git push -u origin master
```

### 2. Crear cuenta en Render

1. Ve a [https://render.com](https://render.com)
2. Haz clic en "Get Started for Free"
3. Inicia sesión con tu cuenta de GitHub

### 3. Crear Base de Datos PostgreSQL

1. En el dashboard de Render, haz clic en **"New +"**
2. Selecciona **"PostgreSQL"**
3. Configura:
   - **Name**: `yape-notifier-db`
   - **Database**: `yape_notifier`
   - **User**: `yape_user`
   - **Region**: `Oregon` (o la más cercana a ti)
   - **Plan**: `Free`
4. Haz clic en **"Create Database"**
5. **Guarda las credenciales** que aparecen (las necesitarás después)

### 4. Crear Web Service

1. En el dashboard, haz clic en **"New +"**
2. Selecciona **"Web Service"**
3. Conecta tu repositorio de GitHub:
   - Selecciona el repositorio `yape-notifier`
   - Haz clic en **"Connect"**

### 5. Configurar el Web Service

**Configuración básica:**
- **Name**: `yape-notifier-api`
- **Region**: `Oregon` (o la misma que la base de datos)
- **Branch**: `master`
- **Root Directory**: `apps/api`
- **Runtime**: `PHP`
- **Build Command**: 
  ```bash
  composer install --no-dev --optimize-autoloader && php artisan key:generate --force
  ```
- **Start Command**: 
  ```bash
  php artisan serve --host=0.0.0.0 --port=$PORT
  ```

**Variables de Entorno:**

Agrega las siguientes variables de entorno en la sección "Environment":

```env
APP_NAME=Yape Notifier API
APP_ENV=production
APP_DEBUG=false
APP_URL=https://yape-notifier-api.onrender.com
LOG_CHANNEL=stderr
LOG_LEVEL=error

DB_CONNECTION=pgsql
DB_HOST=<HOST_DE_LA_BASE_DE_DATOS>
DB_PORT=<PUERTO_DE_LA_BASE_DE_DATOS>
DB_DATABASE=<NOMBRE_DE_LA_BASE_DE_DATOS>
DB_USERNAME=<USUARIO_DE_LA_BASE_DE_DATOS>
DB_PASSWORD=<CONTRASEÑA_DE_LA_BASE_DE_DATOS>

CACHE_DRIVER=file
SESSION_DRIVER=file
QUEUE_CONNECTION=sync
```

**Nota:** Los valores de `DB_*` los obtienes de la base de datos que creaste en el paso 3.

### 6. Ejecutar Migraciones

Después del primer deploy, necesitas ejecutar las migraciones:

1. En el dashboard de Render, ve a tu servicio web
2. Haz clic en **"Shell"** (en la barra lateral)
3. Ejecuta:
   ```bash
   php artisan migrate --force
   ```

### 7. Obtener la URL

Una vez que el deploy termine, Render te dará una URL como:
```
https://yape-notifier-api.onrender.com
```

**Nota:** En el plan gratuito, el servicio se "duerme" después de 15 minutos de inactividad. La primera petición puede tardar ~30 segundos en despertar.

## 🔄 Deploy Automático

Render automáticamente hace deploy cada vez que haces push a la rama `master` de tu repositorio.

## 📝 Actualizar la App Android

Una vez que tengas la URL de Render, actualiza `RetrofitClient.kt`:

```kotlin
// apps/android-client/app/src/main/java/com/yapenotifier/android/data/api/RetrofitClient.kt
object RetrofitClient {
    private const val BASE_URL = "https://yape-notifier-api.onrender.com/"
    // ...
}
```

## 🐛 Solución de Problemas

### Error: "Application failed to respond"
- Verifica que el `Start Command` sea correcto
- Revisa los logs en Render Dashboard → Logs

### Error: "Database connection failed"
- Verifica que las variables de entorno `DB_*` sean correctas
- Asegúrate de que la base de datos esté en la misma región que el servicio web

### Error: "500 Internal Server Error"
- Revisa los logs en Render Dashboard → Logs
- Verifica que las migraciones se hayan ejecutado
- Verifica que `APP_KEY` esté configurado (se genera automáticamente con el build command)

### El servicio tarda mucho en responder
- Esto es normal en el plan gratuito (se "duerme" después de 15 min)
- Considera usar un servicio de "ping" para mantenerlo activo
- O actualiza a un plan de pago

## 📚 Recursos

- [Documentación de Render](https://render.com/docs)
- [Deploy Laravel en Render](https://render.com/docs/deploy-laravel)

