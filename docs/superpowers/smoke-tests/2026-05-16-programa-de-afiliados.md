# Smoke test — Programa de Afiliados

**Fecha plan:** 2026-05-16
**Spec:** [../specs/2026-05-16-programa-de-afiliados.md](../specs/2026-05-16-programa-de-afiliados.md)
**Plan:** [../plans/2026-05-16-programa-de-afiliados.md](../plans/2026-05-16-programa-de-afiliados.md)

Marca cada item al verificarlo en el ambiente correspondiente (`dev` local o `prod`).

## 0. Pre-flight

- [ ] El deploy de GitHub Actions del commit final está en verde.
- [ ] En el servidor (o local), `php artisan migrate --force` aplicó las 3 migraciones nuevas sin error.
- [ ] `SELECT COUNT(*) FROM commerces WHERE referral_code IS NULL` retorna `0`.
- [ ] La tabla `referral_commissions` existe y empieza vacía.

## 1. Suite automatizada

- [ ] Backend: `docker compose --env-file .env exec -T php-fpm php artisan test --filter='Referral|Commissions'` → **39/39 green** (29 Referral + 10 Commissions).
- [ ] Frontend: `npm run type-check` en `apps/web-dashboard` → solo los 13 errores pre-existentes de tests unitarios (`StatCard.test.tsx`, `TabBadge.test.tsx`). Ninguno nuevo del feature.

## 2. Flujo de registro con referido

- [ ] Como Comercio A (admin existente, status `active`), entrar al dashboard → tab "Referidos" → copiar link `/register?ref=<code>`.
- [ ] Abrir el link en una ventana de incógnito → registrarse como nuevo usuario (email + password + phone).
- [ ] Al crear el comercio, ver que el campo "Código de referido (opcional)" viene pre-llenado con el código de A.
- [ ] Crear el comercio.
- [ ] En BD: `SELECT referred_by_commerce_id FROM commerces WHERE name = '<nombre del nuevo>'` debe retornar el `id` del comercio A.

### Casos negativos

- [ ] Registrarse con `?ref=codigo-inexistente` → registro funciona, `referred_by_commerce_id` queda NULL, no aparece error en pantalla.
- [ ] Como Comercio A, intentar crear un segundo comercio usando su propio `referral_code` → se crea sin referido (autorreferencia ignorada silenciosamente). Verificar el log de Laravel: aparece warning con reason `self_referral`.

## 3. Cuenta bancaria

- [ ] Como admin de A: tab "Referidos" → sección "Cuenta para pagos" muestra badge **"Falta configurar"**.
- [ ] Completar los 5 campos (banco, tipo, número, titular, doc) y guardar → mensaje "✓ Guardado" → badge cambia a **"Completa"**.
- [ ] En BD: `SELECT payout_account_number FROM commerces WHERE id=<A>` muestra texto cifrado (no se lee en plano).
- [ ] Recargar la página → el form vuelve a mostrar los datos completos (decryption funciona).
- [ ] Como captador del comercio: navegar a `/dashboard?tab=referrals` → la pestaña no aparece en la barra (el endpoint payout devolvería 403 igual).
- [ ] Como super admin: en la sub-tab "Comisiones", al abrir modal "Pagar" sobre una comisión de A → ver datos de cuenta bancaria de A en read-only.

### Validaciones

- [ ] Intentar guardar con un campo vacío → error 422 inline.
- [ ] Intentar guardar con `payout_account_type='crypto'` (manual via DevTools) → error 422.

## 4. Generación de comisión

- [ ] Super admin aprueba el comercio nuevo (status → `active`).
- [ ] Super admin crea un renewal con `amount_paid = 49.00`.
- [ ] En BD: `SELECT * FROM referral_commissions ORDER BY id DESC LIMIT 1` muestra fila con `referrer_commerce_id = A.id`, `amount = 9.80`, `commission_rate = 0.2000`, `status = 'pending'`.
- [ ] En tab "Referidos" de A: KPI **"Saldo pendiente"** muestra `S/ 9.80`.
- [ ] Tabla "Mis comisiones" lista la comisión.

### Casos donde NO se debe generar

- [ ] Renewal sobre un comercio **sin referidor** → 0 filas nuevas.
- [ ] Renewal sobre un comercio referido pero con `status = 'pending'` → 0 filas.
- [ ] Renewal con `amount_paid = 0` → 0 filas.

## 5. Aprobación / pago / anulación

- [ ] Super admin → tab "Comisiones" → click "Aprobar" en fila pending → status pasa a `approved`. KPI "Saldo pendiente" de A sigue sumando.
- [ ] Crear un comercio referido B' sin cuenta de pago configurada en A'. Renovar B' para generar comisión. Como super admin, aprobarla y luego intentar "Pagar" → ver advertencia en amber dentro del modal "El comercio no tiene cuenta de pago configurada", botón Confirmar deshabilitado.
- [ ] Sobre la comisión de A (que sí tiene cuenta): click "Pagar" → modal muestra los datos bancarios → ingresar `payout_reference = "OP-12345"` → confirmar → status pasa a `paid`, `paid_at` registrado. KPI "Total pagado" sube a S/ 9.80, "Saldo pendiente" baja.
- [ ] Crear otra comisión `pending` y anularla con motivo "test" → status `void`, `voided_reason = 'test'`.

## 6. Anulación automática al borrar renewal

- [ ] Generar un renewal nuevo → confirma que se crea una comisión `pending`.
- [ ] Eliminar el renewal desde la BD: `DELETE FROM commerce_renewals WHERE id = X` (o desde algún endpoint si existe).
- [ ] La comisión sigue existiendo en `referral_commissions` pero su `status` ahora es `void` y `voided_reason = 'Renewal eliminado'`.
- [ ] El FK `commerce_renewal_id` quedó NULL (cascada `nullOnDelete`).

## 7. UI / UX

- [ ] El código de referido se muestra con tipografía mono en la tarjeta superior.
- [ ] Botón "Copiar" cambia a ✓ por 1.5 segundos.
- [ ] Botón "Compartir por WhatsApp" abre `https://wa.me/?text=...` con el link en el mensaje.
- [ ] KPIs muestran moneda PEN (`S/`) con formato `es-PE`.
- [ ] Pills de estado tienen colores correctos: pending=amber, approved=blue, paid=green, void=slate.
- [ ] Paginación "Anterior / Siguiente" funciona en ambas tablas (comisiones del admin + comisiones del super admin).

## 8. Seguridad

- [ ] Como captador autenticado, hacer `GET /api/referrals/stats` directo con curl → 403.
- [ ] Como captador, `PUT /api/commerces/me/payout-account` → 403.
- [ ] Como admin del comercio A, `GET /api/admin/commissions` → 403 (solo super_admin).
- [ ] Sin auth (sin Bearer token), cualquier endpoint nuevo → 401.

## 9. Performance / regresión

- [ ] El dashboard del admin carga las 3 queries iniciales en paralelo (revisar Network tab).
- [ ] Crear/editar un empleado, dispositivo, o notificación sigue funcionando (no regresión por cambios en `Commerce` model).
- [ ] El flujo de creación de comercio sin código de referido sigue funcionando (`POST /api/commerces` con solo `name`).

---

**Notas a registrar al ejecutar:**

- Fecha de smoke test:
- Ejecutado por:
- Ambiente: dev / prod
- Items rojos / blockers:
- Fecha de cierre del checklist:
