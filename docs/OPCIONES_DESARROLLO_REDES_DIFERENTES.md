# Opciones de Desarrollo: Teléfono y Backend en Redes Diferentes

Cuando el teléfono Android y el backend están en redes WiFi diferentes, necesitas una solución para conectar ambos. Este documento compara las opciones disponibles y recomienda la mejor según tu caso de uso.

## 🎯 Problema

**Escenario común:**
- Backend corriendo en tu computadora (red WiFi: `192.168.1.x`)
- Teléfono Android conectado a otra red WiFi (ej: `192.168.0.x` o datos móviles)
- **Resultado:** El teléfono no puede acceder al backend directamente

## 📊 Comparación de Opciones

### Opción 1: Túnel Local (ngrok, localtunnel, cloudflared) ⭐ **RECOMENDADO PARA DESARROLLO RÁPIDO**

#### Descripción
Crea un túnel público que expone tu `localhost` a internet, permitiendo que el teléfono acceda desde cualquier red.

#### Ventajas ✅
- **Rápido de configurar** (5 minutos)
- **Funciona desde cualquier red** (WiFi, datos móviles)
- **Gratis** para desarrollo
- **No requiere cambios en el código** (solo cambiar URL en la app)
- **Perfecto para pruebas rápidas**

#### Desventajas ❌
- **URL cambia** cada vez que reinicias (en versión gratuita)
- **Límites de tráfico** en planes gratuitos
- **Menos seguro** (URL pública, aunque con HTTPS)
- **Requiere conexión a internet** en ambos lados

#### Herramientas Disponibles

**1. ngrok** (Más popular)
```bash
# Instalar
# Windows: choco install ngrok
# Mac: brew install ngrok
# Linux: descargar de https://ngrok.com/download

# Crear túnel
ngrok http 8000

# Salida:
# Forwarding  https://abc123.ngrok.io -> http://localhost:8000
# Usa: https://abc123.ngrok.io en la app Android
```

**2. Cloudflare Tunnel (cloudflared)** (Gratis, sin límites)
```bash
# Instalar
# Windows: choco install cloudflared
# Mac: brew install cloudflared
# Linux: descargar de https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/install-and-setup/installation/

# Crear túnel
cloudflared tunnel --url http://localhost:8000

# Salida:
# https://random-subdomain.trycloudflare.com
# Usa esta URL en la app Android
```

**3. localtunnel** (Gratis, open source)
```bash
# Instalar
npm install -g localtunnel

# Crear túnel
lt --port 8000

# Salida:
# your url is: https://random-name.loca.lt
# Usa esta URL en la app Android
```

#### Configuración en la App Android

```kotlin
// RetrofitClient.kt
object RetrofitClient {
    // Opción 1: ngrok
    private const val BASE_URL = "https://abc123.ngrok.io/"
    
    // Opción 2: Cloudflare Tunnel
    // private const val BASE_URL = "https://random-subdomain.trycloudflare.com/"
    
    // Opción 3: localtunnel
    // private const val BASE_URL = "https://random-name.loca.lt/"
    
    // ...
}
```

#### ⚠️ Importante: HTTPS

**Problema:** Android bloquea conexiones HTTP no seguras por defecto.

**Solución:** Los túneles proporcionan HTTPS automáticamente, pero necesitas configurar:

1. **Permitir certificados no confiables (solo desarrollo):**
```xml
<!-- apps/android-client/app/src/main/AndroidManifest.xml -->
<application
    android:usesCleartextTraffic="true"
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

2. **Crear `network_security_config.xml`:**
```xml
<!-- apps/android-client/app/src/main/res/xml/network_security_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">localhost</domain>
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">192.168.1.0</domain>
    </domain-config>
    <!-- Para túneles, confiar en todos los certificados (solo desarrollo) -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

---

### Opción 2: Desplegar en Servidor (Railway, Render, Heroku) ⭐ **RECOMENDADO PARA DESARROLLO CONTINUO**

#### Descripción
Despliega el backend en un servicio PaaS (Platform as a Service) que proporciona una URL pública permanente.

#### Ventajas ✅
- **URL permanente** (no cambia)
- **HTTPS incluido** automáticamente
- **Más seguro** (infraestructura profesional)
- **Disponible 24/7** (no depende de tu PC)
- **Mejor para pruebas con múltiples dispositivos**
- **Simula mejor el entorno de producción**

#### Desventajas ❌
- **Requiere configuración inicial** (15-30 minutos)
- **Puede tener costos** (aunque muchos tienen planes gratuitos)
- **Cambios requieren deploy** (aunque puedes usar hot-reload con algunos servicios)

#### Servicios Recomendados

**1. Railway** (Recomendado - Plan gratuito generoso)
```bash
# 1. Instalar CLI
npm install -g @railway/cli

# 2. Login
railway login

# 3. Inicializar proyecto
cd yape-notifier/apps/api
railway init

# 4. Desplegar
railway up

# 5. Obtener URL
railway domain
# Salida: https://tu-proyecto.railway.app
```

**2. Render** (Gratis, fácil de usar)
- Conecta tu repositorio GitHub
- Render detecta automáticamente Laravel
- Configura variables de entorno
- Deploy automático en cada push

**3. Fly.io** (Gratis, rápido)
```bash
# Instalar CLI
curl -L https://fly.io/install.sh | sh

# Login
fly auth login

# Configurar
cd yape-notifier/apps/api
fly launch

# Desplegar
fly deploy
```

#### Configuración en la App Android

```kotlin
// RetrofitClient.kt
object RetrofitClient {
    // URL permanente del servidor
    private const val BASE_URL = "https://tu-proyecto.railway.app/"
    
    // O para desarrollo local con túnel
    // private const val BASE_URL = BuildConfig.DEBUG 
    //     ? "https://abc123.ngrok.io/"
    //     : "https://tu-proyecto.railway.app/"
    
    // ...
}
```

#### Variables de Entorno en Railway/Render

```env
APP_NAME="Yape Notifier API"
APP_ENV=production
APP_DEBUG=false
APP_URL=https://tu-proyecto.railway.app

DB_CONNECTION=pgsql
DB_HOST=tu-host-postgres
DB_PORT=5432
DB_DATABASE=yape_notifier
DB_USERNAME=postgres
DB_PASSWORD=tu-password

# Sanctum
SANCTUM_STATEFUL_DOMAINS=tu-proyecto.railway.app
```

---

### Opción 3: Hotspot Móvil (Solución Temporal)

#### Descripción
Convierte tu teléfono en hotspot y conecta tu PC a esa red.

#### Ventajas ✅
- **No requiere configuración adicional**
- **Funciona inmediatamente**
- **Gratis** (usa datos móviles)

#### Desventajas ❌
- **Consume datos móviles** (puede ser costoso)
- **Lento** (depende de la velocidad de datos)
- **No práctico** para desarrollo continuo
- **El teléfono debe estar cerca**

#### Pasos

1. **Activar hotspot en el teléfono:**
   - Configuración → Hotspot y anclaje a red
   - Activar "Hotspot portátil"
   - Anotar nombre de red y contraseña

2. **Conectar PC al hotspot:**
   - Buscar la red WiFi del hotspot
   - Conectar con la contraseña

3. **Obtener IP del PC en el hotspot:**
   ```bash
   # Windows
   ipconfig
   # Busca la IP en la interfaz del hotspot
   
   # Mac/Linux
   ifconfig
   ```

4. **Configurar en la app:**
   ```kotlin
   private const val BASE_URL = "http://192.168.43.XXX:8000/"
   ```

---

### Opción 4: VPN (No Recomendado para Desarrollo)

#### Descripción
Configurar una VPN para conectar ambas redes.

#### Ventajas ✅
- **Seguro**
- **Permanente** una vez configurado

#### Desventajas ❌
- **Complejo de configurar**
- **Requiere hardware adicional** (router con VPN o servidor)
- **No práctico** para desarrollo rápido
- **Overkill** para este caso de uso

#### Conclusión
❌ **No recomendado** para desarrollo. Mejor para producción.

---

## 🎯 Recomendación por Escenario

### Escenario 1: Desarrollo Rápido / Pruebas Esporádicas
**Recomendación:** **Túnel Local (ngrok o Cloudflare Tunnel)**

**Razón:**
- Configuración en 5 minutos
- Funciona inmediatamente
- No requiere cambios en infraestructura

**Pasos:**
```bash
# 1. Instalar ngrok
# 2. Iniciar backend
php artisan serve

# 3. Crear túnel
ngrok http 8000

# 4. Copiar URL (ej: https://abc123.ngrok.io)
# 5. Actualizar RetrofitClient.kt con esa URL
# 6. Probar en el teléfono
```

### Escenario 2: Desarrollo Continuo / Múltiples Dispositivos
**Recomendación:** **Desplegar en Railway/Render**

**Razón:**
- URL permanente (no cambia)
- Disponible siempre
- Mejor para pruebas con múltiples dispositivos
- Simula mejor producción

**Pasos:**
```bash
# 1. Crear cuenta en Railway
# 2. Conectar repositorio GitHub
# 3. Configurar variables de entorno
# 4. Deploy automático
# 5. Usar URL permanente en la app
```

### Escenario 3: Desarrollo Local con Misma Red
**Recomendación:** **IP Local (solución actual)**

**Razón:**
- Más rápido (sin latencia de internet)
- No consume datos
- Mejor para debugging

**Pasos:**
```bash
# 1. Asegurar que PC y teléfono estén en misma WiFi
# 2. Obtener IP local de la PC
ipconfig  # Windows
ifconfig  # Mac/Linux

# 3. Configurar en RetrofitClient.kt
private const val BASE_URL = "http://192.168.1.XXX:8000/"

# 4. Iniciar backend con --host=0.0.0.0
php artisan serve --host=0.0.0.0 --port=8000
```

---

## 🚀 Guía Paso a Paso: Opción Recomendada (Túnel + Servidor)

### Fase 1: Desarrollo Inicial (Túnel)

**Para empezar rápido:**

1. **Instalar Cloudflare Tunnel:**
   ```bash
   # Windows (con Chocolatey)
   choco install cloudflared
   
   # Mac
   brew install cloudflared
   
   # Linux
   wget https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64
   chmod +x cloudflared-linux-amd64
   sudo mv cloudflared-linux-amd64 /usr/local/bin/cloudflared
   ```

2. **Iniciar backend:**
   ```bash
   cd yape-notifier/apps/api
   php artisan serve
   ```

3. **Crear túnel:**
   ```bash
   cloudflared tunnel --url http://localhost:8000
   ```

4. **Actualizar app Android:**
   ```kotlin
   // RetrofitClient.kt
   private const val BASE_URL = "https://TU-URL-DE-CLOUDFLARE.trycloudflare.com/"
   ```

### Fase 2: Desarrollo Continuo (Servidor)

**Para desarrollo más estable:**

1. **Crear cuenta en Railway:**
   - Visita: https://railway.app
   - Login con GitHub

2. **Crear nuevo proyecto:**
   - New Project → Deploy from GitHub repo
   - Selecciona tu repositorio
   - Selecciona el directorio `apps/api`

3. **Configurar variables de entorno:**
   ```env
   APP_NAME="Yape Notifier API"
   APP_ENV=local  # O 'production' según prefieras
   APP_DEBUG=true
   APP_URL=https://tu-proyecto.railway.app
   
   DB_CONNECTION=pgsql
   DB_HOST=${{Postgres.DATABASE_HOST}}
   DB_PORT=${{Postgres.DATABASE_PORT}}
   DB_DATABASE=${{Postgres.DATABASE_NAME}}
   DB_USERNAME=${{Postgres.DATABASE_USER}}
   DB_PASSWORD=${{Postgres.DATABASE_PASSWORD}}
   ```

4. **Agregar base de datos PostgreSQL:**
   - En Railway, click "New" → Database → PostgreSQL
   - Railway automáticamente inyecta las variables de entorno

5. **Ejecutar migraciones:**
   - En Railway, ve a tu servicio
   - Click en "Deploy Logs"
   - Agrega un comando de deploy:
     ```bash
     php artisan migrate --force
     ```

6. **Obtener URL:**
   - Railway proporciona una URL automáticamente
   - Ejemplo: `https://yape-notifier-api-production.up.railway.app`

7. **Actualizar app Android:**
   ```kotlin
   // RetrofitClient.kt
   private const val BASE_URL = "https://yape-notifier-api-production.up.railway.app/"
   ```

---

## 🔧 Configuración Avanzada: Build Variants

Para tener diferentes URLs según el entorno (desarrollo/producción):

### 1. Configurar Build Variants en Android

**`apps/android-client/app/build.gradle.kts`:**
```kotlin
android {
    buildTypes {
        getByName("debug") {
            buildConfigField("String", "API_BASE_URL", "\"https://abc123.ngrok.io/\"")
        }
        getByName("release") {
            buildConfigField("String", "API_BASE_URL", "\"https://tu-api.railway.app/\"")
        }
    }
}
```

**`RetrofitClient.kt`:**
```kotlin
object RetrofitClient {
    private val BASE_URL = BuildConfig.API_BASE_URL
    
    // ...
}
```

### 2. Usar Variables de Entorno (Más Seguro)

**Crear `local.properties` (no versionar):**
```properties
# apps/android-client/local.properties
API_BASE_URL_DEBUG=https://abc123.ngrok.io/
API_BASE_URL_RELEASE=https://tu-api.railway.app/
```

**`build.gradle.kts`:**
```kotlin
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    buildTypes {
        getByName("debug") {
            val debugUrl = localProperties.getProperty("API_BASE_URL_DEBUG", "http://10.0.2.2:8000/")
            buildConfigField("String", "API_BASE_URL", "\"$debugUrl\"")
        }
        getByName("release") {
            val releaseUrl = localProperties.getProperty("API_BASE_URL_RELEASE", "https://tu-api.railway.app/")
            buildConfigField("String", "API_BASE_URL", "\"$releaseUrl\"")
        }
    }
}
```

---

## 📋 Checklist de Configuración

### Para Túnel (Desarrollo Rápido)
- [ ] Instalar ngrok/cloudflared/localtunnel
- [ ] Iniciar backend local
- [ ] Crear túnel y obtener URL
- [ ] Actualizar `RetrofitClient.kt` con URL del túnel
- [ ] Configurar `network_security_config.xml` para HTTPS
- [ ] Probar conexión desde el teléfono

### Para Servidor (Desarrollo Continuo)
- [ ] Crear cuenta en Railway/Render
- [ ] Conectar repositorio GitHub
- [ ] Configurar variables de entorno
- [ ] Agregar base de datos PostgreSQL
- [ ] Ejecutar migraciones
- [ ] Obtener URL permanente
- [ ] Actualizar `RetrofitClient.kt` con URL del servidor
- [ ] Probar conexión desde el teléfono

---

## 🎓 Resumen y Recomendación Final

### Para tu caso (redes diferentes):

**Recomendación:** **Usar Túnel para desarrollo rápido + Servidor para desarrollo continuo**

**Flujo sugerido:**

1. **Día 1-2 (Configuración inicial):**
   - Usa **Cloudflare Tunnel** para pruebas rápidas
   - Configura en 5 minutos
   - Prueba que todo funciona

2. **Día 3+ (Desarrollo continuo):**
   - Despliega en **Railway** (gratis, fácil)
   - URL permanente
   - Deploy automático desde GitHub
   - Mejor para desarrollo continuo

3. **Producción:**
   - Usa Railway o DigitalOcean
   - Configuración profesional
   - HTTPS, backups, monitoreo

### Ventajas de esta estrategia:

✅ **Flexibilidad:** Puedes cambiar entre túnel y servidor fácilmente  
✅ **Rapidez:** Túnel para pruebas inmediatas  
✅ **Estabilidad:** Servidor para desarrollo continuo  
✅ **Costo:** Ambos tienen planes gratuitos generosos  
✅ **Escalabilidad:** Fácil migrar a producción  

---

## 📚 Recursos Adicionales

- [ngrok Documentation](https://ngrok.com/docs)
- [Cloudflare Tunnel Documentation](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/)
- [Railway Documentation](https://docs.railway.app/)
- [Render Documentation](https://render.com/docs)
- [Android Network Security Config](https://developer.android.com/training/articles/security-config)

