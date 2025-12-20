# Yape & Bank Notification Payment Validator

Sistema para leer, procesar y validar notificaciones de pagos desde aplicaciones móviles (Yape, Plin, bancos) en dispositivos Android, consolidando la información en un backend centralizado.

## 🎯 Objetivo

Solución compuesta por una app Android y un backend en Laravel que permite:

- Leer notificaciones de pago (Yape, Plin y bancos) desde dispositivos Android
- Procesar y parsear automáticamente la información relevante (monto, pagador, origen)
- Enviar notificaciones a una API central
- Registrar y consolidar los pagos en una base de datos
- Visualizar y validar pagos desde un dashboard central

## 📁 Estructura del Proyecto

```
yape-notifier/
├── apps/
│   ├── api/              # Backend Laravel (PHP 8.2+, Laravel 11)
│   ├── android-client/   # App Android (Kotlin, MVVM)
│   └── web-dashboard/    # Dashboard web (React + TypeScript)
├── infra/
│   └── docker/           # Dockerfiles y configuraciones
├── docs/                 # Documentación centralizada
└── ARCHIVE/              # Documentación histórica
```

## 🛠️ Stack Tecnológico

### Backend

- PHP 8.2+, Laravel 11, PostgreSQL, Laravel Sanctum

### Frontend Móvil

- Kotlin, Android SDK (API 24+), MVVM, Retrofit, Coroutines, Room Database

### Dashboard Web

- React 18, TypeScript, Vite, Tailwind CSS

### Infraestructura

- Docker, Docker Compose, Caddy (HTTPS automático)

---

## 🚀 Inicio Rápido

### Opción 1: Docker (Recomendado)

```bash
cd infra/docker/environments/development
./setup.sh
./deploy.sh
```

**Acceso:**

- API: `http://localhost:8000`
- Dashboard: `http://localhost:3000`

### Opción 2: Instalación Manual

#### Backend

```bash
cd apps/api
composer install
cp .env.example .env
php artisan key:generate
php artisan migrate
php artisan serve
```

#### Android App

1. Abrir `apps/android-client` en Android Studio
2. Configurar URL de API en `RetrofitClient.kt`
3. Ejecutar en dispositivo físico o emulador

#### Dashboard Web

```bash
cd apps/web-dashboard
npm install
npm run dev
```

---

## 📚 Documentación

### Para Nuevos Usuarios

- **[Quick Start](docs/01-getting-started/QUICKSTART.md)** - Inicio rápido
- **[Estado de Implementación](docs/07-reference/IMPLEMENTATION_STATUS.md)** - Qué está implementado

### Para Desarrolladores

- **[Flujo de Trabajo](docs/04-development/WORKFLOW.md)** - Desarrollo con Docker
- **[Arquitectura](docs/03-architecture/)** - Arquitectura del sistema
- **[Bugs Conocidos](docs/07-reference/KNOWN_ISSUES.md)** - Bugs y soluciones
- **[Roadmap](docs/07-reference/ROADMAP.md)** - Pendientes y mejoras

### Para DevOps

- **[Deployment](docs/02-deployment/DEPLOYMENT.md)** - Guía completa de despliegue
- **[Docker](docs/02-deployment/DOCKER.md)** - Infraestructura Docker

### Por Componente

- **API**: Ver [apps/api/README.md](apps/api/README.md)
- **Android**: Ver [apps/android-client/README.md](apps/android-client/README.md)
- **Dashboard**: Ver [apps/web-dashboard/README.md](apps/web-dashboard/README.md)

### Índice Completo

Ver [docs/README.md](docs/README.md) para índice completo de documentación.

---

## 🔧 Comandos Útiles

### Usando Makefile

```bash
make help          # Ver todos los comandos
make install       # Instalar dependencias
make dev           # Iniciar entorno de desarrollo
make test          # Ejecutar tests
make docker-up     # Iniciar contenedores Docker
```

### Docker

```bash
# Development
cd infra/docker/environments/development
docker compose --env-file .env up -d

# Production
cd infra/docker/environments/production
./deploy.sh
```

---

## ⚠️ Estado del Proyecto

### ✅ Implementado

- Multi-tenancy con Commerce
- Apps duales (con bug conocido)
- Sistema de vinculación QR/código
- Dashboard web completo
- Backend robusto con todos los endpoints

### 🔴 Bug Crítico

- **androidUserId**: Usa `hashCode()` en lugar de `identifier`
- **Ubicación**: `apps/android-client/.../PaymentNotificationListenerService.kt:67`
- **Ver**: [Bugs Conocidos](docs/07-reference/KNOWN_ISSUES.md)

### 🟡 Pendiente

- UI Android para gestionar instancias duales
- Wizard completo de permisos
- Selector de apps en Android
- App Android para administrador

Ver [Roadmap](docs/07-reference/ROADMAP.md) para lista completa.

---

## 🐛 Solución de Problemas

### Error: "Device not found"

- Verificar que el dispositivo esté registrado con el UUID correcto

### Error: "Commerce not found"

- El usuario debe tener un commerce asociado
- Crear commerce con `POST /api/commerces`

### La app Android no captura notificaciones

- Verificar que el servicio de notificaciones esté activado
- Verificar permisos de la app de origen
- Revisar logs: `adb logcat | grep PaymentNotificationService`

Ver [docs/07-reference/KNOWN_ISSUES.md](docs/07-reference/KNOWN_ISSUES.md) para más problemas conocidos.

---

## 📝 Licencia

[Especificar licencia]

---

## 👥 Autores

[Especificar autores]

---

## 🆘 Soporte

Para problemas o preguntas:

1. Revisar [documentación](docs/README.md)
2. Consultar [bugs conocidos](docs/07-reference/KNOWN_ISSUES.md)
3. Verificar [estado de implementación](docs/07-reference/IMPLEMENTATION_STATUS.md)
4. Contactar al equipo de desarrollo

---

**Nota**: Este proyecto está en desarrollo activo. La documentación se actualiza regularmente.
