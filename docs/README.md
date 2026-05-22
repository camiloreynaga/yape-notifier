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
- **[GUIA_PRODUCCION_PASO_A_PASO.md](02-deployment/GUIA_PRODUCCION_PASO_A_PASO.md)** - Guía detallada paso a paso para despliegue en producción
- **[GUIA_ACTUALIZACION.md](02-deployment/GUIA_ACTUALIZACION.md)** - Guía de actualización de API y Dashboard
- **[ACTUALIZACION_PARCIAL.md](02-deployment/ACTUALIZACION_PARCIAL.md)** - Actualización parcial (solo Dashboard o solo API)
- **[DEPLOYMENT_COMMANDS.md](02-deployment/DEPLOYMENT_COMMANDS.md)** - Comandos rápidos de despliegue
- **[SERVER_COMMANDS.md](02-deployment/SERVER_COMMANDS.md)** - Comandos para ejecutar en el servidor
- **[DEPLOYMENT_CHECKLIST.md](02-deployment/DEPLOYMENT_CHECKLIST.md)** - Checklist de despliegue en DigitalOcean
- **[DEPLOY_GUIDE_PRODUCTION.md](02-deployment/DEPLOY_GUIDE_PRODUCTION.md)** - Guía detallada paso a paso para despliegue en producción
- **[DOCKER.md](02-deployment/DOCKER.md)** - Documentación técnica de Docker
- **[DEVICE_FEATURES.md](02-deployment/DEVICE_FEATURES.md)** - Sistema de vinculación y salud de dispositivos
- **[DASHBOARD_DEPLOYMENT.md](02-deployment/DASHBOARD_DEPLOYMENT.md)** - Guía de deployment específica del dashboard web
- **[DASHBOARD_CHECKLIST.md](02-deployment/DASHBOARD_CHECKLIST.md)** - Checklist de producción del dashboard web
- **[DIGITAL_OCEAN_DEPLOYMENT.md](02-deployment/DIGITAL_OCEAN_DEPLOYMENT.md)** - Guía de despliegue en DigitalOcean
- **[RESUMEN_RAPIDO_PRODUCCION.md](02-deployment/RESUMEN_RAPIDO_PRODUCCION.md)** - Resumen rápido de producción
- **PRODUCTION.md** - Checklist y mejores prácticas de producción (pendiente)

---

### 03. Architecture

Documentación técnica de la arquitectura del sistema.

- **[DUAL_APPS.md](03-architecture/DUAL_APPS.md)** - Sistema de apps duales
- **[MULTI_TENANT.md](03-architecture/MULTI_TENANT.md)** - Sistema multi-tenant
- **[TRACEABILITY_ARCHITECTURE.md](03-architecture/TRACEABILITY_ARCHITECTURE.md)** - Arquitectura de trazabilidad y login
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
- **[DOCUMENTATION_PROCESS.md](04-development/DOCUMENTATION_PROCESS.md)** - Proceso de documentación (comando "documentar")
- **[TESTING.md](04-development/TESTING.md)** - Guía completa de testing
- **[TESTING_QR_LINKING.md](04-development/TESTING_QR_LINKING.md)** - Guía de pruebas del sistema de vinculación QR
- **[ERROR_TRACKING.md](04-development/ERROR_TRACKING.md)** - Guía de implementación de error tracking
- **[DEPENDENCIES.md](04-development/DEPENDENCIES.md)** - Gestión profesional de dependencias (composer)
- **[PROMPTS_DESARROLLO.md](04-development/PROMPTS_DESARROLLO.md)** - Prompts listos para desarrollo con IA
- **CONTRIBUTING.md** - Guía de contribución (pendiente)

---

### 04. Security

Documentación de seguridad y multi-tenancy.

- **[MULTI_TENANT_SECURITY.md](04-security/MULTI_TENANT_SECURITY.md)** - Seguridad multi-tenant
- **[AUDIT_DEVICE_LINKING.md](04-security/AUDIT_DEVICE_LINKING.md)** - Auditoría de vinculación de dispositivos sin autenticación

---

### 05. Features

Documentación de funcionalidades y features del sistema.

- **[NOTIFICATION_FILTERING.md](05-features/NOTIFICATION_FILTERING.md)** - Filtrado inteligente de notificaciones (Fase 2 implementada)
- **[WEBSOCKETS.md](05-features/WEBSOCKETS.md)** - WebSockets para notificaciones en tiempo real (configuración disponible)
- **[DEVICE_LINKING.md](05-features/DEVICE_LINKING.md)** - Vinculación de dispositivos mediante QR/código (implementado)
- **[DEVICE_LINKING_GUIDE.md](05-features/DEVICE_LINKING_GUIDE.md)** - Guía profesional: Cómo vincular adecuadamente los dispositivos
- **[DEVICE_LINKING_ARCHITECTURE.md](03-architecture/DEVICE_LINKING_ARCHITECTURE.md)** - Arquitectura profesional de vinculación de dispositivos
- **[DEVICE_LINKING_FLOW.md](05-features/DEVICE_LINKING_FLOW.md)** - Flujo detallado paso a paso de vinculación de dispositivos
- **[DEVICE_LINKING_METHODS_COMPARISON.md](05-features/DEVICE_LINKING_METHODS_COMPARISON.md)** - Comparación de métodos de vinculación (QR vs Dashboard)
- **[CAPTURER_MODE_EXPLANATION.md](05-features/CAPTURER_MODE_EXPLANATION.md)** - Explicación del modo capturer (sin login)
- **[PIN_SYSTEM_HOW_IT_WORKS.md](05-features/PIN_SYSTEM_HOW_IT_WORKS.md)** - Cómo funciona el sistema con PIN
- **[PIN_SYSTEM_IMPLEMENTATION_SUMMARY.md](05-features/PIN_SYSTEM_IMPLEMENTATION_SUMMARY.md)** - Resumen de implementación del sistema PIN
- **[PIN_SYSTEM_FINAL_IMPLEMENTATION.md](05-features/PIN_SYSTEM_FINAL_IMPLEMENTATION.md)** - Implementación completa del sistema PIN
- **[PIN_SYSTEM_IMPLEMENTATION_PLAN.md](05-features/PIN_SYSTEM_IMPLEMENTATION_PLAN.md)** - Plan de implementación del sistema PIN
- **[PIN_SYSTEM_UX_IMPACT.md](05-features/PIN_SYSTEM_UX_IMPACT.md)** - Impacto UX del sistema PIN en Android
- **[DASHBOARD_OPTIMIZATION_PHASE_1.md](05-features/DASHBOARD_OPTIMIZATION_PHASE_1.md)** - Optimización Fase 1: React Query
- **[DASHBOARD_OPTIMIZATION_PHASE_2.md](05-features/DASHBOARD_OPTIMIZATION_PHASE_2.md)** - Optimización Fase 2: Error Handling y Circuit Breaker
- **[DASHBOARD_OPTIMIZATION_PHASE_3.md](05-features/DASHBOARD_OPTIMIZATION_PHASE_3.md)** - Optimización Fase 3: WebSocket Optimization
- **[DASHBOARD_OPTIMIZATION_PHASE_4.md](05-features/DASHBOARD_OPTIMIZATION_PHASE_4.md)** - Optimización Fase 4: Lazy Loading y Code Splitting
- **[DASHBOARD_OPTIMIZATION_PHASE_5.md](05-features/DASHBOARD_OPTIMIZATION_PHASE_5.md)** - Optimización Fase 5: Debouncing y Optimizaciones Finales
- **[ANDROID_ADMIN_MODULE.md](05-features/ANDROID_ADMIN_MODULE.md)** - Módulo Admin móvil - App Android (implementado)
- **[MONITOR_PACKAGES.md](05-features/MONITOR_PACKAGES.md)** - Sistema de gestión de paquetes monitoreados
- **[DEVICE_CONNECTION_STATUS_EXPLANATION.md](05-features/DEVICE_CONNECTION_STATUS_EXPLANATION.md)** - Explicación del estado de conexión de dispositivos
- **[ADMIN_APP_FEATURE_GAP_ANALYSIS.md](05-features/ADMIN_APP_FEATURE_GAP_ANALYSIS.md)** - Análisis de brecha: App Android Admin vs Dashboard Web
- **COMMERCE.md** - Sistema de comercios (pendiente)

---

### 06. Operations

Documentación para operaciones y mantenimiento.

- **[MONITORING.md](06-operations/MONITORING.md)** - Monitoreo del sistema
- **[BACKUP.md](06-operations/BACKUP.md)** - Backup y recuperación
- **[UPDATE_CHECKLIST.md](06-operations/UPDATE_CHECKLIST.md)** - Checklist de actualización del servidor
- **[TROUBLESHOOTING.md](06-operations/TROUBLESHOOTING.md)** - Diagnóstico y solución de problemas
- **[TROUBLESHOOTING_NOTIFICATIONS.md](06-operations/TROUBLESHOOTING_NOTIFICATIONS.md)** - Solución: Notificaciones no capturadas
- **[TROUBLESHOOTING_ERROR_IS_ACTIVE_CAUSE.md](06-operations/TROUBLESHOOTING_ERROR_IS_ACTIVE_CAUSE.md)** - Causa raíz: Error is_active column does not exist
- **[TROUBLESHOOTING_ERROR_IS_ACTIVE_SOLUTION.md](06-operations/TROUBLESHOOTING_ERROR_IS_ACTIVE_SOLUTION.md)** - Solución: Error is_active column does not exist
- **[TROUBLESHOOTING_ERROR_IS_ACTIVE_FIXED.md](06-operations/TROUBLESHOOTING_ERROR_IS_ACTIVE_FIXED.md)** - Solución corregida: Error is_active column does not exist
- **[CLEAN_DEVICES.md](06-operations/CLEAN_DEVICES.md)** - Guía profesional: Limpieza de dispositivos en base de datos
- **[DEPLOY_CLEAN_DEVICES_COMMAND.md](06-operations/DEPLOY_CLEAN_DEVICES_COMMAND.md)** - Comando de despliegue para limpiar dispositivos
- **MAINTENANCE.md** - Mantenimiento y actualizaciones (pendiente)

---

### 07. Reference

Referencia técnica: bugs, estado de implementación, roadmap, changelog.

- **[KNOWN_ISSUES.md](07-reference/KNOWN_ISSUES.md)** - Bugs conocidos del proyecto
- **[IMPLEMENTATION_STATUS.md](07-reference/IMPLEMENTATION_STATUS.md)** - Estado actual de implementación
- **[ROADMAP.md](07-reference/ROADMAP.md)** - Pendientes y mejoras planificadas
- **[CHANGELOG.md](07-reference/CHANGELOG.md)** - Historial de cambios y mejoras implementadas
- **[ARCHITECTURE_REVIEW.md](07-reference/ARCHITECTURE_REVIEW.md)** - Revisión arquitectónica senior completa
- **[ARCHITECTURE_REVIEW_SUMMARY.md](07-reference/ARCHITECTURE_REVIEW_SUMMARY.md)** - Resumen ejecutivo de revisión arquitectónica
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
- **¿Cómo probar la vinculación QR?** → [TESTING_QR_LINKING.md](04-development/TESTING_QR_LINKING.md)
- **¿Problemas técnicos?** → [TROUBLESHOOTING.md](06-operations/TROUBLESHOOTING.md)

---

## 📝 Notas

- Los documentos marcados como "(pendiente)" están en proceso de creación o consolidación
- Los documentos históricos se encuentran en `../ARCHIVE/`
- Para reportar problemas o sugerir mejoras, consulta la guía de contribución (cuando esté disponible)

---

**Última actualización:** 2025-01-27

---

## 📊 Estadísticas de Documentación

- **Total de documentos:** 85+
- **Categorías:** 7 (Getting Started, Deployment, Architecture, Development, Security, Features, Operations, Reference)
- **Archivos en ARCHIVE:** 30+ (documentación histórica)

---

## 📋 Proceso de Documentación

Para mantener la documentación actualizada, ejecuta el comando **"documentar"** que:

1. Identifica nuevos archivos `.md` en la raíz
2. Clasifica y mueve archivos a ubicaciones correctas
3. Consolida contenido duplicado
4. Actualiza referencias cruzadas
5. Mantiene `docs/README.md` actualizado

Ver **[DOCUMENTATION_PROCESS.md](04-development/DOCUMENTATION_PROCESS.md)** para detalles del proceso.
