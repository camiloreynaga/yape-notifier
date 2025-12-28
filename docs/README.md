# Documentación - Yape Notifier

Índice completo de la documentación del proyecto.

---

## 📚 Estructura de Documentación

### 01. Getting Started

Documentación para nuevos usuarios y desarrolladores que se inician en el proyecto.

- **[QUICKSTART.md](01-getting-started/QUICKSTART.md)** - Inicio rápido del proyecto
- **INSTALLATION.md** - Guía detallada de instalación (pendiente)
- **DEVELOPMENT_SETUP.md** - Configuración del entorno de desarrollo (pendiente)

---

### 02. Deployment

Guías de despliegue y operaciones en diferentes entornos.

- **[DEPLOYMENT.md](02-deployment/DEPLOYMENT.md)** - Guía completa de despliegue en producción
- **[DEPLOY_GUIDE_PRODUCTION.md](02-deployment/DEPLOY_GUIDE_PRODUCTION.md)** - Guía detallada paso a paso para despliegue en producción
- **[DOCKER.md](02-deployment/DOCKER.md)** - Documentación técnica de Docker
- **[DEVICE_FEATURES.md](02-deployment/DEVICE_FEATURES.md)** - Sistema de vinculación y salud de dispositivos
- **[DASHBOARD_DEPLOYMENT.md](02-deployment/DASHBOARD_DEPLOYMENT.md)** - Guía de deployment específica del dashboard web
- **[DASHBOARD_CHECKLIST.md](02-deployment/DASHBOARD_CHECKLIST.md)** - Checklist de producción del dashboard web
- **PRODUCTION.md** - Checklist y mejores prácticas de producción (pendiente)

---

### 03. Architecture

Documentación técnica de la arquitectura del sistema.

- **[DUAL_APPS.md](03-architecture/DUAL_APPS.md)** - Sistema de apps duales
- **[MULTI_TENANT.md](03-architecture/MULTI_TENANT.md)** - Sistema multi-tenant
- **[ANDROID_USER_ID.md](03-architecture/ANDROID_USER_ID.md)** - Análisis técnico: Identificador de usuario Android
- **[ANDROID_IMPLEMENTATION.md](03-architecture/ANDROID_IMPLEMENTATION.md)** - Análisis completo de implementación de la app Android
- **[ANDROID_HILT.md](03-architecture/ANDROID_HILT.md)** - Dependency Injection con Hilt en Android
- **[API_ENDPOINTS.md](03-architecture/API_ENDPOINTS.md)** - Referencia consolidada de endpoints API
- **OVERVIEW.md** - Arquitectura general del sistema (pendiente)
- **API.md** - Documentación completa de API (pendiente)
- **DATABASE.md** - Esquema de base de datos y migraciones (pendiente)

---

### 04. Development

Guías para desarrolladores.

- **[WORKFLOW.md](04-development/WORKFLOW.md)** - Flujo de trabajo de desarrollo
- **[TESTING.md](04-development/TESTING.md)** - Guía completa de testing
- **[ERROR_TRACKING.md](04-development/ERROR_TRACKING.md)** - Guía de implementación de error tracking
- **[PROMPTS_DESARROLLO.md](04-development/PROMPTS_DESARROLLO.md)** - Prompts listos para desarrollo con IA
- **CONTRIBUTING.md** - Guía de contribución (pendiente)

---

### 05. Features

Documentación de funcionalidades y features del sistema.

- **[NOTIFICATION_FILTERING.md](05-features/NOTIFICATION_FILTERING.md)** - Filtrado inteligente de notificaciones (Fase 2 implementada)
- **[WEBSOCKETS.md](05-features/WEBSOCKETS.md)** - WebSockets para notificaciones en tiempo real (configuración disponible)
- **[DEVICE_LINKING.md](05-features/DEVICE_LINKING.md)** - Vinculación de dispositivos mediante QR/código (implementado)
- **[DEVICE_LINKING_GUIDE.md](05-features/DEVICE_LINKING_GUIDE.md)** - Guía profesional: Cómo vincular adecuadamente los dispositivos
- **[ANDROID_ADMIN_MODULE.md](05-features/ANDROID_ADMIN_MODULE.md)** - Módulo Admin móvil - App Android (implementado)
- **COMMERCE.md** - Sistema de comercios (pendiente)

---

### 06. Operations

Documentación para operaciones y mantenimiento.

- **[MONITORING.md](06-operations/MONITORING.md)** - Monitoreo del sistema
- **[BACKUP.md](06-operations/BACKUP.md)** - Backup y recuperación
- **[UPDATE_CHECKLIST.md](06-operations/UPDATE_CHECKLIST.md)** - Checklist de actualización del servidor
- **MAINTENANCE.md** - Mantenimiento y actualizaciones (pendiente)

---

### 07. Reference

Referencia técnica: bugs, estado de implementación, roadmap, changelog.

- **[KNOWN_ISSUES.md](07-reference/KNOWN_ISSUES.md)** - Bugs conocidos del proyecto
- **[IMPLEMENTATION_STATUS.md](07-reference/IMPLEMENTATION_STATUS.md)** - Estado actual de implementación
- **[ROADMAP.md](07-reference/ROADMAP.md)** - Pendientes y mejoras planificadas
- **[CHANGELOG.md](07-reference/CHANGELOG.md)** - Historial de cambios y mejoras implementadas
- **[CODE_QUALITY_API.md](07-reference/CODE_QUALITY_API.md)** - Análisis de buenas prácticas y calidad de código (API Laravel)
- **[CODE_QUALITY_ANDROID.md](07-reference/CODE_QUALITY_ANDROID.md)** - Revisión de código y calidad (App Android)
- **API_REFERENCE.md** - Referencia completa de API (pendiente)

---

## 🚀 Inicio Rápido

Si eres nuevo en el proyecto:

1. Lee **[QUICKSTART.md](01-getting-started/QUICKSTART.md)** para levantar el sistema rápidamente
2. Revisa **[IMPLEMENTATION_STATUS.md](07-reference/IMPLEMENTATION_STATUS.md)** para entender qué está implementado
3. Consulta **[KNOWN_ISSUES.md](07-reference/KNOWN_ISSUES.md)** para conocer bugs conocidos
4. Revisa **[ROADMAP.md](07-reference/ROADMAP.md)** para ver qué está pendiente

---

## 📖 Por Audiencia

### Para Nuevos Desarrolladores

1. [QUICKSTART.md](01-getting-started/QUICKSTART.md)
2. [WORKFLOW.md](04-development/WORKFLOW.md)
3. [IMPLEMENTATION_STATUS.md](07-reference/IMPLEMENTATION_STATUS.md)

### Para DevOps/Deployment

1. [DEPLOYMENT.md](02-deployment/DEPLOYMENT.md)
2. [WORKFLOW.md](04-development/WORKFLOW.md)

### Para Desarrolladores

1. [WORKFLOW.md](04-development/WORKFLOW.md)
2. [IMPLEMENTATION_STATUS.md](07-reference/IMPLEMENTATION_STATUS.md)
3. [KNOWN_ISSUES.md](07-reference/KNOWN_ISSUES.md)
4. [ROADMAP.md](07-reference/ROADMAP.md)

---

## 🔍 Búsqueda Rápida

- **¿Cómo inicio el proyecto?** → [QUICKSTART.md](01-getting-started/QUICKSTART.md)
- **¿Cómo despliego en producción?** → [DEPLOYMENT.md](02-deployment/DEPLOYMENT.md)
- **¿Qué está implementado?** → [IMPLEMENTATION_STATUS.md](07-reference/IMPLEMENTATION_STATUS.md)
- **¿Hay bugs conocidos?** → [KNOWN_ISSUES.md](07-reference/KNOWN_ISSUES.md)
- **¿Qué falta por hacer?** → [ROADMAP.md](07-reference/ROADMAP.md)
- **¿Cómo trabajo con el código?** → [WORKFLOW.md](04-development/WORKFLOW.md)
- **¿Cómo vincular dispositivos correctamente?** → [DEVICE_LINKING_GUIDE.md](05-features/DEVICE_LINKING_GUIDE.md)

---

## 📝 Notas

- Los documentos marcados como "(pendiente)" están en proceso de creación o consolidación
- Los documentos históricos se encuentran en `../ARCHIVE/`
- Para reportar problemas o sugerir mejoras, consulta la guía de contribución (cuando esté disponible)

---

**Última actualización:** 2025-01-21
