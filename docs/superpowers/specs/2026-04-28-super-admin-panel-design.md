# Super Admin Panel — Diseño

**Fecha**: 2026-04-28
**Estado**: Aprobado por usuario, pendiente de plan de implementacion
**Alcance**: Backend (Laravel) + Frontend (React dashboard)

## 1. Objetivo

Construir el panel del super admin para gestionar comercios y planes de la plataforma Yape Notifier. El super admin es el unico rol con visibilidad y control sobre todos los comercios, su ciclo de vida (suscripcion mensual con auto-suspension) y los planes disponibles.

## 2. Reglas de negocio

### 2.1 Ciclo de vida de un comercio

| Estado    | Cuando                                   | Acepta notificaciones |
|-----------|------------------------------------------|------------------------|
| pending   | Recien registrado, no aprobado           | No                     |
| active    | Aprobado y dentro de los 30 dias         | Si                     |
| active (en gracia) | Vencido pero dentro de 3 dias post-vencimiento | Si (sigue funcionando, pero rojo en panel) |
| suspended | 3 dias post-vencimiento sin renovar, o suspendido manualmente | No |

### 2.2 Renovacion

- Renovar suma 30 dias al `plan_expires_at` actual.
- Si el comercio esta `suspended`, se reactiva (`status='active'`) y los 30 dias cuentan desde la fecha de renovacion.
- Si esta `active` (incluyendo gracia), los 30 dias se suman al `plan_expires_at` actual (no se "pierden" dias por renovar antes).
- Cada renovacion queda registrada en `commerce_renewals` con: quien renovo, fecha, plan asignado, monto opcional, nota opcional.

### 2.3 Auto-suspension

- Job programado corre 1 vez al dia (artisan schedule).
- Busca comercios `status='active'` con `plan_expires_at + 3 dias < HOY` y los pasa a `status='suspended'`.
- Loggea cada cambio para auditoria.

### 2.4 Aprobacion inicial

- Comercio nuevo entra en `pending` (logica ya existente en `CommerceService`).
- Super admin lo aprueba asignandole un plan. Al aprobar:
  - `status='active'`
  - `plan_id` = plan elegido
  - `approved_at` = ahora
  - `approved_by` = super admin
  - `plan_expires_at` = ahora + 30 dias

## 3. Cambios en backend (Laravel)

### 3.1 Migraciones

**Migracion 1**: agregar `phone` a `users`
```sql
ALTER TABLE users ADD COLUMN phone VARCHAR(20) NULL;
```
Solo aplicara a admins (dueños de comercio). Captadores no necesitan.

**Migracion 2**: campo `grace_until` en `commerces` — opcional pero recomendable
- Alternativa: calcular `plan_expires_at + 3 days` on-the-fly en queries y vistas.
- **Decision**: calcular on-the-fly (no agregar columna), evita inconsistencias si se cambia la regla de gracia.

**Migracion 3**: tabla `commerce_renewals`
```sql
CREATE TABLE commerce_renewals (
    id BIGSERIAL PRIMARY KEY,
    commerce_id BIGINT NOT NULL REFERENCES commerces(id) ON DELETE CASCADE,
    plan_id BIGINT NOT NULL REFERENCES plans(id),
    renewed_by_user_id BIGINT NOT NULL REFERENCES users(id),
    previous_expires_at TIMESTAMP NULL,
    new_expires_at TIMESTAMP NOT NULL,
    amount_paid DECIMAL(10,2) NULL,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_commerce_renewals_commerce_id ON commerce_renewals(commerce_id, created_at DESC);
```

### 3.2 Job: SuspendExpiredCommercesJob

- Ubicacion: `app/Jobs/SuspendExpiredCommercesJob.php`
- Programado en `routes/console.php` o `App\Console\Kernel.php`: `daily()` (corre 1 vez al dia, ej. 02:00 AM).
- Logica:
  ```php
  Commerce::where('status', 'active')
      ->whereNotNull('plan_expires_at')
      ->where('plan_expires_at', '<', now()->subDays(3))
      ->each(function ($commerce) {
          $commerce->update(['status' => 'suspended']);
          Log::info("Auto-suspended commerce {$commerce->id} ({$commerce->name})");
      });
  ```

### 3.3 Endpoints

#### Existentes (modificar)

**GET `/api/admin/commerces`**
- Aceptar filtros: `status`, `q` (busqueda por nombre o email del dueño)
- Agregar a cada commerce de la respuesta:
  - `captadores_count` (count de users con role='captador' linkeados)
  - `expiry_status`: enum calculado on-the-fly: `active`, `expiring_soon` (queda <7 dias), `in_grace` (vencido pero <3 dias post), `expired` (post-gracia, todavia 'active' en BD esperando job), `pending`, `suspended`
  - `days_until_expiry` (numero, negativo si ya vencio)

**GET `/api/admin/commerces/{id}`**
- Agregar a respuesta:
  - `renewals` (ultimos 10, ordenados por fecha desc)
  - `captadores` (lista con id, name, pin)
  - `devices_active_count` (count de dispositivos activos)
  - `notifications_this_month_count`

**PATCH `/api/admin/commerces/{id}/approve`** — modificar comportamiento existente
- Ya existe; al aprobar setear ademas `plan_expires_at = now() + 30 days`. Logica encapsulada en `CommerceService::approve()`.

**PATCH `/api/admin/plans/{id}`** — restringir
- Solo permitir editar: `name`, `max_devices`, `max_notifications_per_day`, `price`, `is_active`
- **NO** permitir editar: `slug` (ya esta en validacion: `'slug' => 'sometimes|string|...'` aceptaria, hay que sacarlo)

#### Nuevos

**PATCH `/api/admin/commerces/{id}/renew`**
- Body:
  ```json
  {
    "plan_slug": "basic",
    "amount_paid": 49.00,    // opcional
    "notes": "Yape Juan"     // opcional
  }
  ```
- Logica:
  - Calcular `new_expires_at`:
    - Si `status='suspended'` o `plan_expires_at < now()` → `now() + 30 days`
    - Si `status='active'` y aun no vence → `plan_expires_at + 30 days`
  - Update commerce: `status='active'`, `plan_id`, `plan_expires_at = new_expires_at`
  - Insert en `commerce_renewals`
- Response: `{message, commerce: {...full reload...}}`

**GET `/api/admin/dashboard/kpis`**
- Response:
  ```json
  {
    "total": 23,
    "pending": 5,
    "active": 12,
    "expiring_soon": 2,    // <7 dias para vencer
    "in_grace": 1,         // vencido en gracia
    "suspended": 3
  }
  ```

### 3.4 Cambios en `CommerceService`

- En `createCommerce()`: ya respeta super_admin desde la fix anterior.
- Agregar metodo `renew(Commerce $commerce, Plan $plan, ?float $amount, ?string $notes, User $renewedBy): Commerce` que encapsula la logica de renovacion + insert en `commerce_renewals`.
- Agregar metodo `approve(Commerce $commerce, Plan $plan, User $approvedBy): Commerce` que setea `plan_expires_at = now()->addDays(30)` ademas de lo que ya hace.

## 4. Cambios en frontend (React)

### 4.1 Estructura de archivos

```
src/pages/SuperAdminPage.tsx              wrapper con tabs
src/components/SuperAdmin/
  KpiCards.tsx                            4 widgets de totales
  CommercesTable.tsx                      tabla compacta
  CommerceDetailDrawer.tsx                drawer derecho con detalle
  RenewCommerceModal.tsx                  modal de renovacion
  ApproveCommerceModal.tsx                modal de aprobacion (pending)
  ChangePlanModal.tsx                     modal de cambio de plan (sin renovar)
  SuspendConfirmDialog.tsx                confirmacion de suspension manual
  PlansTable.tsx                          tabla de planes
  EditPlanModal.tsx                       modal editar plan (precio + limites)
src/services/superAdminApi.ts             cliente API tipado para endpoints /admin/*
src/hooks/useCommerces.ts                 React Query hook
src/hooks/usePlans.ts                     React Query hook
```

### 4.2 Rutas

```
/super-admin                  → redirige a /super-admin/commerces
/super-admin/commerces        → tab Comercios
/super-admin/plans            → tab Planes
```

El drawer de detalle es un overlay sobre la lista, sincronizado con query string `?commerce={id}` para deep-linking y navegacion atras.

### 4.3 Tab "Comercios" — vista detallada

#### KPI Cards (top)

4 cards en una grid responsive (4 columnas en desktop, 2 en tablet, 1 en mobile):

| Card           | Color de fondo  | Contenido               | Click action            |
|----------------|-----------------|-------------------------|-------------------------|
| Total          | gris claro      | numero, label "Total"   | filtra "todos"          |
| Pendientes    | amarillo claro  | numero pendientes       | filtra `pending`        |
| Por vencer    | naranja claro   | numero `expiring_soon` + `in_grace` | filtra ambos |
| Suspendidos   | rojo claro      | numero suspendidos       | filtra `suspended`      |

Click en card aplica el filtro a la tabla de abajo.

#### Filtros + busqueda

Fila con:
- Botones radio: `[Todos] [Pendientes] [Activos] [Por vencer] [Suspendidos]`
- Input de busqueda a la derecha (busca nombre comercio o email dueño)
- Estado del filtro/busqueda se mantiene en URL (`?status=pending&q=juan`)

#### Tabla

5 columnas:

| Columna             | Contenido                                                |
|---------------------|----------------------------------------------------------|
| Comercio · Dueño    | linea 1: nombre comercio (bold). linea 2: nombre dueño (muted) |
| Plan                | badge con nombre + tooltip con precio/limites            |
| Estado              | badge con punto de color + label (Pendiente, Activo, Por vencer, Vencido, Suspendido) |
| Vencimiento         | linea 1: relativa ("en 12 dias"). linea 2: absoluta ("15 May 2026"). Si pendiente: "—" |
| Accion              | boton contextual segun estado (ver tabla abajo)          |

**Botones de accion contextual** (columna 5):

| Estado calculado | Boton principal               |
|------------------|-------------------------------|
| pending          | `[Aprobar]` (primario)        |
| active           | `[Cambiar plan ▼]` (menu con: Cambiar plan, Suspender) |
| expiring_soon    | `[Renovar]` (naranja)         |
| in_grace         | `[Renovar]` (rojo, urgente)   |
| expired          | `[Renovar]` (rojo, urgente — mismo que in_grace, vencido sin gracia, esperando job)   |
| suspended        | `[Reactivar]` (azul, abre modal de renovacion)         |

Mapeo del enum `expiry_status` a etiqueta visible (badges en columna "Estado"):
- `pending` → "Pendiente" (amarillo)
- `active` → "Activo" (verde)
- `expiring_soon` → "Por vencer" (naranja)
- `in_grace` → "En gracia" (rojo claro)
- `expired` → "Vencido" (rojo)
- `suspended` → "Suspendido" (gris)

Click en cualquier parte de la fila (excepto el boton) → abre drawer de detalle.

### 4.4 Drawer de detalle

Panel lateral derecho de ~480px (en desktop). En mobile cubre toda la pantalla. Secciones:

1. **Header**: nombre comercio + badge estado + plan actual
2. **Info del dueño**: nombre, email (con boton copiar), telefono (con boton copiar)
3. **Plan actual**: nombre + precio + limites desplegados + fecha de vencimiento + dias restantes
4. **Captadores**: lista (nombre + PIN)
5. **Uso actual**: dispositivos activos, notif. del mes
6. **Historial de renovaciones**: ultimas 10, formato `[fecha previa] → [fecha nueva] · [plan] · [monto] · por [admin]`
7. **Acciones**: botones agrupados al fondo
   - `[Renovar 30 dias]` (primario, contextual al estado)
   - `[Cambiar plan]` (secundario)
   - `[Suspender]` (rojo, secundario, oculto si ya esta suspendido)
   - `[Reactivar]` (verde, solo si suspended — abre modal de renovacion)

### 4.5 Modal de renovacion

Modal centrado, ~500px ancho, fondo overlay:

```
Renovar Comercio: [nombre]
─────────────────────────
Vence: [fecha] (en X dias) | o "Suspendido hace X dias"

Plan a renovar:
[Dropdown con planes activos] (default: plan actual)

Monto recibido (opcional):
[Input numerico, default vacio]

Nota (opcional):
[Textarea pequeño]

[ ] Confirmo que el cliente pago

Nuevo vencimiento: [calculado en vivo]

[Cancelar]   [Confirmar y renovar] (deshabilitado hasta marcar checkbox)
```

Al confirmar:
- POST al endpoint `/admin/commerces/{id}/renew`
- Toast de exito
- Cierra modal
- Refresca tabla y drawer (si esta abierto)

### 4.6 Modal de aprobacion

Similar al de renovacion pero mas simple:
- Titulo: "Aprobar Comercio: [nombre]"
- Solo selector de plan (no monto ni nota)
- Sin checkbox de pago (puede agregarse despues)
- Boton: `[Aprobar]`

### 4.7 Modal de cambio de plan

Similar a aprobacion pero el titulo dice "Cambiar plan: [nombre]" y muestra el plan actual:
- "Plan actual: Basic (S/49)"
- Selector con otros planes
- Boton: `[Cambiar plan]` — no afecta `plan_expires_at`

### 4.8 Tab "Planes"

Tabla simple de 4 filas (los 4 planes seedeados):

| Plan       | Precio    | Dispositivos | Notif/dia    | Estado          | Accion |
|------------|-----------|--------------|---------------|-----------------|--------|
| Starter    | S/0       | 1            | 10            | Activo          | [Editar] |
| Basic      | S/49      | 3            | Ilimitado     | Activo          | [Editar] |
| Pro        | S/129     | 10           | Ilimitado     | Activo          | [Editar] |
| Enterprise | S/299     | Ilimitado    | Ilimitado     | Activo          | [Editar] |

Click en `[Editar]` abre modal con campos editables:
- precio (number)
- max_devices (number, vacio = ilimitado)
- max_notifications_per_day (number, vacio = ilimitado)
- is_active (toggle)

Bloqueados (read-only): nombre, slug.

### 4.9 Manejo de estados

- React Query para cache + invalidacion. Mutations (renew, suspend, approve, edit plan) invalidan las queries afectadas.
- Toast notifications en exito/error (componente `ToastContainer` ya existente).
- Loading states en botones (spinner inline).
- Confirmacion antes de suspender (`SuspendConfirmDialog`).

## 5. Casos borde y validaciones

| Caso                                              | Comportamiento                                  |
|---------------------------------------------------|-------------------------------------------------|
| Renovar comercio pending                          | No permitido — usar "Aprobar" en su lugar       |
| Cambiar plan a comercio pending                   | No permitido — el plan se asigna al aprobar     |
| Suspender comercio ya suspended                   | Backend devuelve 400 (ya implementado)          |
| Approve commerce sin plan elegido                 | Validacion frontend: dropdown requerido         |
| Renovar sin marcar checkbox de pago               | Boton deshabilitado                             |
| Telefono vacio en commerce                        | Mostrar "Sin telefono" + link "[Agregar]" (futuro) |
| Plan eliminado/desactivado pero asignado a comercio | El comercio mantiene el plan, pero al renovar/cambiar no aparece como opcion |

## 6. Telemetria y logs

- Todas las acciones (aprobar, renovar, suspender, cambiar plan, editar plan) deben loggearse via `Log::info` con: super_admin_id, commerce_id, accion, valores antes/despues.
- El job de auto-suspension loggea cada commerce suspendido.

## 7. Fuera de alcance (para iteraciones futuras)

- Notificaciones por email/WhatsApp automaticas al cliente cuando su plan vence
- Dashboard de estadisticas globales (revenue, MRR, churn)
- Integracion con pasarela de pago (Yape, Plin, transferencia)
- Crear/eliminar planes desde la UI (por ahora solo edicion)
- Editar slug de plan
- Logs de auditoria visibles en UI (solo en backend por ahora)
- Roles intermedios entre super_admin y admin (ej. "soporte")
- Telefono editable desde el panel del super admin (por ahora solo lectura via UI; el campo se llena al registrarse o por backend)

## 8. Decisiones tomadas

- **Auto-suspension automatica** despues de 3 dias de gracia post-vencimiento (no solo aviso visual)
- **Renovacion manual con confirmacion de pago externo** — sin pasarela aun
- **4 planes fijos**, solo edicion (no creacion) para evitar romper relaciones
- **Drawer lateral** para detalle (no pagina dedicada)
- **Tabs en /super-admin** para Comercios y Planes (no menu lateral)
- **Click en telefono copia al portapapeles** — sin abrir WhatsApp ni mensaje pre-cargado
- **`grace_until` calculado on-the-fly** (no columna), regla = `plan_expires_at + 3 dias`
