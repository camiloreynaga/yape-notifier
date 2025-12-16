# Endpoints Consolidados (Android Client)

Base URL (prod): `https://api.notificaciones.space/`  
Base URL (dev): `http://10.0.2.2:8000/` (emulador) / `http://192.168.x.x:8000/` (dispositivo)

Headers comunes (protegidos):
```
Authorization: Bearer {token}
Content-Type: application/json
Accept: application/json
```

---

## 1) Autenticación
- `POST /api/register` — Registrar usuario
- `POST /api/login` — Iniciar sesión
- `POST /api/logout` — Cerrar sesión (token requerido)
- `GET  /api/me` — Datos del usuario autenticado (token requerido)

---

## 2) Dispositivos (token requerido)
- `POST   /api/devices` — Crear dispositivo (UUID, nombre, fcm opcional)
- `GET    /api/devices` — Listar (query: `active_only`)
- `GET    /api/devices/{id}` — Obtener detalle
- `PUT/PATCH /api/devices/{id}` — Actualizar (nombre, fcm)
- `DELETE /api/devices/{id}` — Eliminar
- `POST   /api/devices/{id}/toggle-status` — Activar/desactivar (`is_active` opcional)

---

## 3) Notificaciones (token requerido)
- `POST /api/notifications` — Crear notificación capturada
  - Body esperado (backend):
    ```json
    {
      "device_id": "uuid",                 // requerido
      "source_app": "yape|plin|bcp|interbank|bbva|scotiabank", // requerido
      "title": "string|null",
      "body": "string",                    // requerido
      "amount": 50.0,                      // opcional
      "currency": "PEN",                   // opcional, 3 letras
      "payer_name": "string|null",
      "received_at": "2024-01-15T10:30:00Z", // opcional, ISO 8601
      "raw_json": { "key": "value" },      // opcional, objeto/array
      "status": "pending|validated|inconsistent" // opcional
    }
    ```
- `GET /api/notifications` — Listar (query opcionales: `device_id`, `source_app`, `start_date`, `end_date`, `status`, `exclude_duplicates`, `per_page`)
- `GET /api/notifications/{id}` — Obtener detalle
- `PATCH /api/notifications/{id}/status` — Actualizar estado (`pending|validated|inconsistent`)
- `GET /api/notifications/statistics` — Métricas (query: `start_date`, `end_date`)

Notas:
- Duplicados: misma `device_id` + `source_app` + `body` dentro de ±5s → responde 201 con mensaje `"Notification received (duplicate detected)"` y `is_duplicate=true`.
- `device_id` debe ser el UUID registrado del dispositivo.

---

## 4) Paquetes a monitorear (público)
- `GET /api/settings/monitored-packages` — Lista de paquetes a monitorear (sin token)

---

## 5) Monitor Packages (gestión, token requerido)
- `GET    /api/monitor-packages` — Listar (query: `active_only`)
- `POST   /api/monitor-packages` — Crear
- `GET    /api/monitor-packages/{id}` — Detalle
- `PUT/PATCH /api/monitor-packages/{id}` — Actualizar
- `DELETE /api/monitor-packages/{id}` — Eliminar
- `POST   /api/monitor-packages/{id}/toggle-status` — Activar/desactivar (`is_active` opcional)
- `POST   /api/monitor-packages/bulk-create` — Crear en lote

---

## 6) Respuestas típicas
- 200 OK / 201 Created en éxito.
- 400 Validación: campos faltantes o inválidos.
- 401 Token faltante/incorrecto.
- 403 Dispositivo inactivo u otros permisos.
- 404 No encontrado.

---

## 7) Referencias rápidas en el repo
- Rutas backend: `apps/api/routes/api.php`
- Validación notificaciones: `apps/api/app/Http/Requests/Notification/CreateNotificationRequest.php`
- Modelo Android (actual): `app/src/main/java/com/yapenotifier/android/data/model/NotificationData.kt`
- Mapper propuesto: `app/src/main/java/com/yapenotifier/android/data/mapper/NotificationMapper.kt`

