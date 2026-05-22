# Credenciales de empleados + seguridad de roles — Diseño

**Fecha**: 2026-05-14
**Estado**: Pendiente de aprobación del usuario
**Alcance**: Backend (Laravel) + Frontend (web-dashboard). Android sin cambios.

---

## 1. Problema

Dos problemas conectados, detectados en la auditoría de roles:

### 1.1 El admin creado no puede entrar al dashboard web
`UserController::store` asigna a cada empleado un `password` aleatorio (bcrypt) que **nadie conoce**. El dashboard web exige email + contraseña. Resultado: un empleado con rol "administrador" no puede iniciar sesión en el dashboard — queda funcionalmente como captador (solo PIN en la app Android).

### 1.2 Huecos de seguridad en la gestión de roles
- Las rutas `users` solo validan `commerce_id`, no el rol → **un captador puede crear/editar/borrar usuarios**, incluso crear admins.
- `CreateUserRequest` acepta `['admin', 'captador', 'system']` → el rol interno `system` es creable vía API cruda.
- `PinAuthController` no restringe por rol → cualquier rol con PIN puede entrar por el endpoint pensado solo para captadores.

---

## 2. Modelo de credenciales (decisión de diseño)

**Cada rol tiene UNA credencial, alineada a dónde trabaja:**

| Rol | Credencial | Dónde inicia sesión |
|-----|-----------|---------------------|
| `captador` | PIN de 4 dígitos | App Android (`/api/auth/login-pin`) |
| `admin` | Contraseña | Dashboard web (`/api/login`) |
| `super_admin` | Contraseña | Dashboard web |
| `system` | — | No se crea desde la UI |

- Captador: **PIN, sin contraseña visible.**
- Admin: **contraseña visible, sin PIN.** (PIN queda `null`.)
- El dueño del comercio administra ambas credenciales — patrón "managed credentials".

---

## 3. Contraseña visible — cómo se guarda

El hash bcrypt es irreversible: no se puede "mostrar". Se agrega una segunda columna con la contraseña **encriptada de forma reversible** (AES-256 vía `Crypt::encryptString()` de Laravel, atada al `APP_KEY`).

| Columna | Uso | Forma |
|---------|-----|-------|
| `password` (existe) | Autenticación / login | Hash bcrypt — **no se toca** |
| `password_visible` (nueva) | Que el dueño la vea/copie | `Crypt::encryptString()` — reversible solo con APP_KEY |

**Riesgo aceptado conscientemente:** si el `APP_KEY` se filtra, las contraseñas en `password_visible` son descifrables. Es un riesgo acotado y *menor* que el actual: hoy el PIN ya se guarda en texto plano directo.

---

## 4. Cambios en backend

### 4.1 Migración
- `users.password_visible` — `text`, `nullable`.

### 4.2 Nuevo middleware `require_admin`
- Crea `app/Http/Middleware/RequireAdmin.php`.
- Permite pasar solo si `user->isAdmin()` o `user->isSuperAdmin()`. Si no → 403.
- Se registra con alias `require_admin` en `bootstrap/app.php`.

### 4.3 Rutas — aplicar `require_admin`
En `routes/api.php`, envolver con `require_admin`:
- `apiResource('users', ...)` (todo el CRUD de empleados)
- `POST /users/{id}/regenerate-pin`
- `POST /users/{id}/regenerate-password` (nueva)

Resultado: un captador autenticado ya **no** puede tocar el módulo de empleados.

### 4.4 `CreateUserRequest` / `UpdateUserRequest`
- `role` validation → `Rule::in(['admin', 'captador'])`. Se elimina `system`.

### 4.5 `UserController::store` — credencial según rol
```
$plainPassword = null;
$pin = null;

if ($role === 'captador') {
    $pin = User::generateUniquePin(4);
} else { // admin
    $plainPassword = Str::random(12); // legible, 12 chars
}

User::create([
    ...,
    'password' => Hash::make($plainPassword ?? bin2hex(random_bytes(16))),
    'password_visible' => $plainPassword ? Crypt::encryptString($plainPassword) : null,
    'pin' => $pin,
    'role' => $role,
]);
```
Respuesta incluye `pin` (si captador) o `password` en texto plano (si admin) — para mostrarlo una vez al crear.

### 4.6 `UserController::index` — devolver contraseña desencriptada
- Para cada usuario con `password_visible != null`, agregar al payload `password_visible` ya **desencriptado** con `Crypt::decryptString()`.
- Solo se devuelve porque la ruta ya está protegida por `require_admin` + `commerce.active` + scope de comercio.

### 4.7 `UserController` — nuevo método `regeneratePassword`
- `POST /api/users/{id}/regenerate-password`
- Genera nueva contraseña legible, actualiza `password` (bcrypt) y `password_visible` (Crypt).
- Solo aplica a usuarios con rol `admin`. Si el target es `captador` → 400 "Los captadores usan PIN".
- Devuelve la contraseña en texto plano en la respuesta.
- Scope: el target debe pertenecer al mismo `commerce_id` que el solicitante (salvo super_admin).

### 4.8 `regeneratePin` — restringir a captador
- Si el target es `admin` → 400 "Los administradores usan contraseña".

### 4.9 `PinAuthController::loginWithPin` — restringir por rol
Tras encontrar el usuario por PIN, validar:
```
if ($user->role !== 'captador') {
    return 403 "Este acceso es solo para captadores";
}
```
Cierra el hueco de que un admin/super_admin entre por el endpoint de PIN.

### 4.10 Modelo `User`
- Agregar `password_visible` a `$fillable`.
- Agregarlo a `$hidden` (para que no se serialice crudo por accidente — el controller lo agrega desencriptado explícitamente).

---

## 5. Cambios en frontend (web-dashboard)

### 5.1 Tipo `User`
- Agregar `password_visible?: string | null` (ya desencriptado por el backend).

### 5.2 `EmployeesPage` — columna "Contraseña"
Nueva columna entre PIN y Estado:
- **Captador**: muestra "—" (no aplica).
- **Admin**: muestra `••••••••` con botón 👁 (ver/ocultar) + 📋 (copiar).
- Botón 🔄 "Regenerar" → llama a `regenerate-password`, muestra la nueva en un toast/modal una vez.

Columna PIN existente:
- **Captador**: muestra el PIN + botón regenerar (como hoy).
- **Admin**: muestra "—".

### 5.3 Formulario "Agregar empleado"
- Selector de rol: `admin` / `captador` (sin cambios — ya está así).
- Al crear:
  - Si `captador` → modal/toast muestra el **PIN** generado.
  - Si `admin` → modal/toast muestra la **contraseña** generada (con copiar).
- Quitar cualquier opción de `system` si existiera.

### 5.4 Servicio API
- `regeneratePassword(id)` → `POST /api/users/{id}/regenerate-password`.

---

## 6. Migración de datos existentes

Los empleados ya creados tienen `password` con hash aleatorio irrecuperable y `password_visible = null`.

- **Admins existentes**: el dueño usa el botón "Regenerar contraseña" para darles una credencial usable. No se hace backfill automático.
- **Captadores existentes**: sin cambios, su PIN sigue funcionando.

No se requiere seeder ni comando de migración de datos — el botón regenerar cubre el caso.

---

## 7. Casos borde

| Caso | Comportamiento |
|------|----------------|
| Crear admin | Genera contraseña, `pin = null` |
| Crear captador | Genera PIN, `password_visible = null` |
| Regenerar contraseña de un captador | 400 — "Los captadores usan PIN" |
| Regenerar PIN de un admin | 400 — "Los administradores usan contraseña" |
| Captador intenta entrar a `/api/users` | 403 (middleware `require_admin`) |
| Admin intenta `login-pin` | 403 — "Acceso solo para captadores" |
| `role: 'system'` en API cruda | 422 validación |
| `Crypt::decryptString` falla (APP_KEY cambió) | El controller atrapa la excepción y devuelve `password_visible: null` para ese usuario, no rompe el listado |
| super_admin gestiona empleados de cualquier comercio | Permitido (no se le aplica el scope de commerce_id) |

---

## 8. Fuera de alcance

- Enlace de invitación / "establece tu contraseña" por email.
- Login por PIN en el dashboard web.
- Política de fuerza de contraseña configurable.
- Auditoría/historial de regeneración de credenciales.
- Rotación de `APP_KEY`.
- 2FA.

---

## 9. Criterios de aceptación

- [ ] Un captador autenticado recibe 403 al llamar cualquier endpoint de `users`.
- [ ] Crear un empleado `admin` devuelve una contraseña en texto plano en la respuesta.
- [ ] Crear un empleado `captador` devuelve un PIN, sin `password_visible`.
- [ ] `GET /api/users` devuelve `password_visible` desencriptado para los admins del comercio.
- [ ] La tabla de Empleados muestra la columna Contraseña con ver/copiar para admins y "—" para captadores.
- [ ] "Regenerar contraseña" produce una nueva contraseña usable para login web.
- [ ] Un admin no puede iniciar sesión por `/api/auth/login-pin`.
- [ ] `role: 'system'` es rechazado por validación.
- [ ] Los super admin tests existentes siguen verdes.
