# Especificación de Requerimientos

## Proyecto: Yape & Bank Notification Payment Validator

---

## 1. Objetivo general

Desarrollar una solución compuesta por una app Android y un backend en Laravel que permita:

- Leer notificaciones de pago (Yape, Plin y bancos) desde dispositivos Android.
- Procesar y parsear automáticamente la información relevante (monto, pagador, origen).
- Enviar dichas notificaciones a una API central.
- Registrar y consolidar los pagos en una base de datos (y opcionalmente en Google Sheets).
- Permitir a un "dispositivo padre" (celular central o dashboard) visualizar y validar pagos.

---

## 2. Usuarios del sistema

### 2.1. Administrador / Dueño de cuenta

- Crea la cuenta principal del sistema.
- Registra y administra dispositivos Android.
- Consulta y filtra pagos consolidados.
- Accede al dashboard (web o móvil) con vista global.

### 2.2. Usuario – Dispositivo Android

- Instala la app móvil.
- Concede el permiso de lectura de notificaciones.
- Permite el envío automático de notificaciones de pago hacia la API.

---

## 3. Requerimientos funcionales (RF)

### RF01 – Registro y autenticación de cuenta

- El sistema debe permitir crear cuentas mediante email y contraseña.
- El backend en Laravel debe generar y gestionar tokens de acceso (JWT o Sanctum).
- El usuario debe poder iniciar sesión y recibir un token para acceder a los endpoints protegidos.

### RF02 – Gestión de dispositivos

- El administrador debe poder registrar múltiples dispositivos asociados a su cuenta.
- Cada dispositivo debe tener:
  - ID único (UUID).
  - Nombre descriptivo (ej. "Caja 1 – Yape").
  - Plataforma (Android).
  - Estado (activo / inactivo).
- Las notificaciones recibidas se deben asociar siempre a un dispositivo.

### RF03 – Lectura de notificaciones en Android

- La app Android debe implementar un `NotificationListenerService`.
- Debe leer notificaciones de las siguientes apps (configurable):
  - Yape.
  - BCP.
  - Plin.
  - Interbank.
  - BBVA.
  - Scotiabank.
- Para cada notificación relevante, la app debe obtener:
  - Aplicación origen (package name).
  - Título.
  - Texto / cuerpo completo.
  - Fecha y hora de recepción.

### RF04 – Parseo de contenido de notificación

- La app Android debe incluir lógica (parser) para:
  - Identificar si la notificación corresponde a un "pago recibido".
  - Extraer el monto y moneda desde el texto.
  - Extraer el nombre o alias del pagador si está presente.
  - Extraer referencia de operación cuando sea posible.
- El parser debe estar diseñado para ser extendible por banco / app.

### RF05 – Envío de notificaciones a la API (Laravel)

- La app Android debe enviar las notificaciones relevantes a la API vía HTTPs.
- El payload mínimo debe incluir:
  - `device_id`
  - `source_app`
  - `title`
  - `body`
  - `amount` (opcional si se logra parsear)
  - `currency` (opcional)
  - `payer_name` (opcional)
  - `received_at` (ISO 8601)
  - `raw_json` o datos adicionales si aplica
- La petición debe ir autenticada (token asociado a la cuenta/dispositivo).

### RF06 – Registro de notificaciones en la base de datos

El backend en Laravel debe registrar cada notificación en la BD con, al menos, los siguientes campos:

- `id` (UUID o autoincremental).
- `account_id`.
- `device_id`.
- `source_app`.
- `title`.
- `body`.
- `amount` (nullable).
- `currency` (nullable).
- `payer_name` (nullable).
- `received_at`.
- `raw_json` (nullable).
- `created_at` / `updated_at`.

### RF07 – Detección de duplicados y consistencia

- El sistema debe evitar registrar múltiples veces la misma notificación.
- Se debe definir una regla de unicidad (por ejemplo, combinación de:
  - `device_id`
  - `source_app`
  - `body`
  - `received_at` con tolerancia de segundos.
- Las notificaciones detectadas como duplicadas deben marcarse como tales o ignorarse.

### RF08 – Validación automática de pagos (reglas básicas)

- El sistema debe permitir asociar notificaciones a "órdenes de pago" (en una fase posterior).
- Para el MVP:
  - Se debe marcar el pago como "recibido" cuando llegue una notificación de monto X, para un dispositivo dado, en una ventana de tiempo determinada.
- Debe existir la posibilidad de marcar pagos como:
  - "Validado automáticamente".
  - "Pendiente de revisión".
  - "Inconsistente".

### RF09 – Manejo de notificaciones no recibidas / casos limite

- El sistema debe considerar el escenario donde:
  - La app de Yape/banco está abierta y no genera notificación.
  - Hay fallos de red y la notificación no se envía a la API.
- Debe ser posible registrar manualmente un pago o marcar un pago como "no notificado".

### RF10 – Dashboard (web o vista consolidada)

- El sistema debe ofrecer una vista consolidada donde se pueda:
  - Listar pagos por rango de fechas.
  - Filtrar por dispositivo.
  - Ver totales por día, por dispositivo, por banco.
  - Ver el estado (última actividad) de cada dispositivo.

### RF11 – Exportación / integración externa

- Debe existir un mecanismo para:
  - Exportar los datos a CSV/Excel.
  - Opcionalmente, sincronizar con una hoja de cálculo (Google Sheets o similar) mediante un proceso batch o cron.

---

## 4. Requerimientos no funcionales (RNF)

### RNF01 – Arquitectura

- El proyecto debe estructurarse como un **monorepo** que incluya al menos:
  - `apps/android-client` (app Android).
  - `apps/api` (Laravel).
  - `apps/web-dashboard` (opcional, futuro).
  - `infra/` (Docker, scripts, config).
- El backend se desarrollará con:
  - **PHP 8.2+**
  - **Laravel 10/11** (versión LTS estable).
- El monorepo se gestionará con Git y se trabajará con el editor **Cursor**.

### RNF02 – Backend (Laravel)

- El backend debe seguir arquitectura limpia dentro de Laravel:
  - Uso de **Service classes** para lógica de dominio.
  - Repositorios o Query Builders claros para acceso a datos.
  - Requests con validación (FormRequest).
  - Estructura RESTful para los controladores.
- Autenticación con:
  - **Laravel Sanctum** o **Laravel Passport** (a definir).
- Se utilizará **PostgreSQL** o **MySQL** (según disponibilidad en el proveedor) como base de datos principal.

### RNF03 – Infraestructura y despliegue

- MVP:
  - Deploy de la API en **Railway** (o plataforma similar PaaS).
- Producción real:
  - Deploy de la API en un **Droplet de DigitalOcean** usando Docker.
  - Uso de un reverse proxy (Caddy o Nginx) para HTTPS.
- Debe existir un `Dockerfile` y un `docker-compose.yml` para levantar:
  - API Laravel.
  - Base de datos.
  - (Opcional) servicios adicionales como Redis.

### RNF04 – Rendimiento

- El backend debe ser capaz de procesar al menos 10–20 notificaciones por segundo en un entorno de recursos modestos (Droplet pequeño).
- Las respuestas API deben ser ligeras y optimizadas (evitar carga innecesaria de relaciones pesadas).

### RNF05 – Seguridad

- Todas las comunicaciones entre app Android y API deben realizarse sobre HTTPS.
- Las contraseñas deben almacenarse con hashing seguro (bcrypt o Argon2).
- Las claves y secretos deben gestionarse mediante variables de entorno.
- El sistema debe validar correctamente autenticación y autorización:
  - Un dispositivo solo puede enviar notificaciones para la cuenta a la que pertenece.

### RNF06 – Escalabilidad

- El diseño debe permitir:
  - Añadir más dispositivos sin cambios estructurales.
  - Añadir nuevos orígenes (otros bancos / billeteras) mediante configuración y parsers adicionales.
  - Crear futuros servicios de análisis, reportes y monitoreo.

### RNF07 – Observabilidad

- El backend debe registrar logs de:
  - Errores de parseo.
  - Fallos en llamadas desde Android.
  - Errores de base de datos.
- Se debe poder activar un nivel de log más detallado en entornos de testing.

---

## 5. Requerimientos técnicos del cliente Android

### RT-Android-01

- Desarrollado en **Kotlin**.
- Arquitectura **MVVM**.
- Uso de:
  - `NotificationListenerService` para capturar notificaciones.
  - `Retrofit` o cliente HTTP equivalente para la comunicación con la API Laravel.
  - `Coroutines` para operaciones asíncronas.
  - `DataStore` (preferentemente Encrypted) para almacenar tokens y configuración.

### RT-Android-02

- La app debe implementarse de forma que:
  - Tenga manejo de reconexión si falla el envío a la API.
  - Pueda hacer reintentos de envío (ej. cola interna simple).
  - Sea posible activar/desactivar qué tipos de notificaciones procesar.

---

## 6. Requerimientos de seguridad y privacidad

### RS01 – Permisos

- La app Android debe solicitar explícitamente el permiso de acceso a notificaciones.
- La app debe informar al usuario de forma clara el uso de ese permiso y qué datos se envían.

### RS02 – Datos sensibles

- La app y la API no deben registrar información irrelevante o excesivamente sensible:
  - No guardar contenido de notificaciones que no sean de pagos.
  - Evitar almacenar datos personales no necesarios (por ejemplo, texto de WhatsApp).

### RS03 – Auditoría

- El backend debe permitir rastrear:
  - Qué dispositivo envió cada notificación.
  - Cuándo se recibió.
  - Resultado del procesamiento (éxito / fallo / duplicado).

---

## 7. Requerimientos de MVP

Para la primera versión funcional (MVP) se considera suficiente:

1. Backend Laravel:

   - Autenticación básica (login/registro).
   - Registro de dispositivos.
   - Endpoint `POST /api/notifications` para registrar notificaciones.
   - Persistencia en BD.
   - Endpoint simple para listar notificaciones (`GET /api/notifications` filtrado por cuenta).

2. App Android:

   - Lectura de notificaciones de Yape/Bancos básicos.
   - Parseo mínimo de monto y texto.
   - Envío automático a la API.

3. Infra:
   - API desplegada en Railway.
   - Base de datos en Railway o servicio administrado equivalente.

Las siguientes fases incluirán dashboard web, reglas avanzadas de validación de pagos y exportación a Google Sheets.





