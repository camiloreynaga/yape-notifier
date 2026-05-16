# Programa de Afiliados — Spec

**Fecha:** 2026-05-16
**Goal:** Permitir que cada comercio refiera la plataforma a otros y reciba 20% de comisión recurrente sobre las renovaciones de sus referidos, mientras el referido permanezca `active`. Cada comercio registra su cuenta bancaria para recibir las transferencias.
**Arquitectura:** Cada `commerce` tiene un `referral_code` único y un `referred_by_commerce_id` inmutable (write-once). Cuando un comercio referido renueva, un Observer sobre `CommerceRenewal::created` genera una fila en `referral_commissions` (status `pending` → `approved` → `paid` / `void`). Idempotencia garantizada por `UNIQUE(commerce_renewal_id)`. Cada comercio puede registrar su cuenta bancaria (encriptada con `Crypt`) para recibir los pagos; el super admin la consulta al ejecutar la transferencia y la marca como `paid` con un `payout_reference`.
**Tech Stack:** Laravel 11 + Sanctum + PostgreSQL + Eloquent Observers; React 18 + TS + Tailwind + TanStack Query. Tests: PHPUnit (TDD backend), `npm run type-check` (frontend).

---

## 1. Modelo de datos

### 1.1 Cambios a `commerces`

| Columna | Tipo | Constraint | Notas |
|---|---|---|---|
| `referral_code` | VARCHAR(20) | UNIQUE NOT NULL | Generado en `creating`; slug del nombre + sufijo aleatorio. |
| `referred_by_commerce_id` | BIGINT | FK commerces ON DELETE SET NULL, NULL | Inmutable post-creación. Indexado. |
| `payout_bank` | VARCHAR(80) | NULL | Texto libre (BCP, BBVA, Interbank, etc.). |
| `payout_account_type` | VARCHAR(20) | NULL | `corriente` \| `ahorros` \| `cci`. |
| `payout_account_number` | TEXT | NULL | **Encriptado** (`Crypt::encryptString`). |
| `payout_account_holder` | VARCHAR(150) | NULL | Nombre del titular. |
| `payout_account_holder_doc` | VARCHAR(20) | NULL | DNI / RUC del titular. |

Backfill: al correr la migración, recorrer comercios existentes y generar `referral_code` para cada uno (la columna es NOT NULL UNIQUE).

### 1.2 Tabla nueva `referral_commissions`

| Campo | Tipo | Constraint |
|---|---|---|
| `id` | bigint | PK |
| `referrer_commerce_id` | bigint | FK commerces NOT NULL, indexed |
| `referred_commerce_id` | bigint | FK commerces NOT NULL, indexed |
| `commerce_renewal_id` | bigint | FK commerce_renewals NOT NULL, **UNIQUE** |
| `base_amount` | decimal(10,2) | NOT NULL |
| `commission_rate` | decimal(5,4) | NOT NULL, default 0.2000 |
| `amount` | decimal(10,2) | NOT NULL (= base × rate, redondeado 2 dec) |
| `status` | varchar(16) | `pending` \| `approved` \| `paid` \| `void`, default `pending` |
| `paid_at` | timestamp | nullable |
| `payout_reference` | varchar(100) | nullable (nº operación de transferencia) |
| `voided_reason` | text | nullable |
| `approved_by_user_id` | bigint | FK users nullable |
| `paid_by_user_id` | bigint | FK users nullable |
| `created_at` / `updated_at` | | |

`UNIQUE(commerce_renewal_id)` garantiza idempotencia (un renewal = a lo más una comisión).

---

## 2. Flujos

### 2.1 Generación del código de referido

Observer `CommerceObserver::creating`:
1. Si `referral_code` ya está seteado → no tocar.
2. Generar slug: tomar `name`, normalizar (sin tildes, espacios → guiones, minúsculas), tomar primeros 8 chars. Si queda vacío → usar `com`.
3. Concatenar `-` + 4 chars random alfanuméricos en minúscula.
4. Verificar que no exista en `commerces.referral_code`. Reintentar hasta 5 veces.
5. Si tras 5 intentos sigue colisionando → agregar timestamp epoch al final.

### 2.2 Registro con referido

1. Usuario abre `/register?ref=karol-7x2z`.
2. Frontend lee `ref` del query string y lo persiste hasta el momento de crear el comercio.
3. El form de creación de comercio también acepta entrada manual del código.
4. `CommerceController::store` recibe `referral_code` opcional en body.
5. Validaciones (rechazo silencioso, log warning):
   - Si código no existe → ignorar.
   - Si pertenece al mismo `owner_user_id` (autorreferencia) → ignorar.
   - Si válido → setear `referred_by_commerce_id` en el comercio recién creado.
6. **`referred_by_commerce_id` es write-once**: ninguna ruta lo actualiza después.

### 2.3 Generación de comisión

Observer `CommerceRenewalObserver::created`:
1. Cargar el `Commerce` referido (`renewal->commerce`).
2. Return si: `referred_by_commerce_id` null **o** `commerce.status !== 'active'` **o** `renewal.amount_paid <= 0`.
3. Crear `referral_commission` con:
   - `referrer_commerce_id = commerce.referred_by_commerce_id`
   - `referred_commerce_id = commerce.id`
   - `commerce_renewal_id = renewal.id`
   - `base_amount = renewal.amount_paid`
   - `commission_rate = 0.20`
   - `amount = round(base * rate, 2)`
   - `status = 'pending'`
4. La UNIQUE constraint en `commerce_renewal_id` previene duplicados en condiciones de carrera.

### 2.4 Anulación al borrar renewal

Observer `CommerceRenewalObserver::deleting`:
1. Buscar commission con ese `commerce_renewal_id`.
2. Si existe → marcarla `void` con `voided_reason = 'Renewal eliminado'`.
3. No borrar físicamente (audit trail).

### 2.5 Cuenta bancaria del referidor

- Admin del comercio registra/edita su cuenta desde el panel.
- Al guardar: si cualquier campo (banco, tipo, número, titular, doc) está presente, **todos** son requeridos. Validar en `UpdatePayoutAccountRequest`.
- `payout_account_number` se cifra con `Crypt::encryptString` en mutator del modelo, se descifra en accessor.
- Visible solo para: owner del comercio y super admin.
- **Bloqueo al pagar**: super admin no puede marcar `paid` una commission si el `referrer_commerce` no tiene cuenta completa → 422 "El comercio referidor no tiene cuenta de pago configurada".

### 2.6 Vista del comercio admin (`/dashboard?tab=referrals`)

Tab "Programa de Referidos":

- **Mi código + link**: muestra `referral_code`, link copiable `<APP_URL>/register?ref=<code>`, botón "Compartir por WhatsApp".
- **KPIs**: comisiones del mes actual (suma de `pending`+`approved`), saldo aprobado pendiente de pago, total histórico pagado, # ahijados activos.
- **Cuenta para pagos**: form con banco, tipo, número, titular, doc. Indicador "Completa" / "Falta configurar".
- **Mis ahijados**: tabla — nombre, plan, status, fecha alta, última renovación, comisiones totales generadas.
- **Mis comisiones**: tabla paginada — fecha, ahijado, monto base, %, comisión, status, fecha pago, referencia.

### 2.7 Vista super admin (`/super-admin/commissions`)

Nueva sub-tab:
- Listado paginado con filtros: status, mes (YYYY-MM), `referrer_id`, `referred_id`.
- Por fila: ver detalle (datos del ahijado, del renewal, cuenta bancaria del referidor).
- Acciones:
  - **Aprobar** (`pending` → `approved`): registra `approved_by_user_id`, `updated_at`.
  - **Marcar pagada** (`approved` → `paid`): requiere `payout_reference` no vacío + cuenta de pago del referidor completa. Registra `paid_by_user_id` y `paid_at`.
  - **Anular** (cualquier → `void`): requiere `reason` no vacío.
- KPIs arriba: totales `pending`, `approved`, `paid` del mes filtrado.

---

## 3. API Endpoints

### Comercio admin (auth + `require_admin`)
- `GET  /api/referrals/stats` → `{ month_earnings, pending_balance, lifetime_paid, active_referrals_count }`
- `GET  /api/referrals/referrals` → lista de comercios ahijados con plan/status/última renovación.
- `GET  /api/referrals/commissions?status=&from=&to=&page=` → comisiones propias paginadas.
- `GET  /api/commerces/me/payout-account` → cuenta de pago (desencriptada).
- `PUT  /api/commerces/me/payout-account` → actualiza cuenta de pago. Body: `{ payout_bank, payout_account_type, payout_account_number, payout_account_holder, payout_account_holder_doc }`.

### Super admin (auth + `super_admin`)
- `GET  /api/super-admin/commissions?status=&month=&referrer_id=&referred_id=&page=`
- `POST /api/super-admin/commissions/{id}/approve`
- `POST /api/super-admin/commissions/{id}/pay` body: `{ payout_reference }`
- `POST /api/super-admin/commissions/{id}/void` body: `{ reason }`

### Registro / creación de comercio (modificado)
- `POST /api/commerces` — body agrega campo opcional `referral_code` (string).

---

## 4. Seguridad

- `payout_account_number` cifrado en BD vía `Crypt::encryptString`.
- Acceso a cuenta de pago: solo owner del comercio (admin del propio commerce) y super admin.
- `referred_by_commerce_id`: write-once (no expuesto en ningún `update`).
- Endpoints `super-admin/commissions/*` protegidos por verificación de rol `super_admin`.
- Validación de autorreferencia por `owner_user_id`.
- Log de auditoría: cada cambio de status (`approved`/`paid`/`void`) registra el `user_id` del actor.

---

## 5. Decisiones cerradas

- **Modelo de pago**: manual. No hay desembolso automático. Super admin transfiere por fuera y registra `payout_reference`.
- **Tasa**: 20% fijo, snapshot por fila (`commission_rate` en cada commission para soportar cambios futuros sin retroactividad).
- **Niveles**: solo 1 (no MLM).
- **Elegibilidad**: solo si referido está `active` y `amount_paid > 0` al momento del renewal.
- **Backfill**: comercios existentes obtienen `referral_code` en la migración; sus `referred_by_commerce_id` quedan `NULL`.
- **Fiscal**: queda fuera del sistema. El `payout_reference` es el único registro contable.

---

## 6. Testing

### Backend (PHPUnit, TDD)
- `CommerceObserverTest`: genera código único en creación; reintenta en colisión.
- `CommerceStoreReferralTest`: acepta `referral_code` válido; ignora inválido; ignora autorreferencia; `referred_by_commerce_id` es inmutable.
- `CommerceRenewalObserverTest`:
  - Genera commission cuando: tiene referidor + `active` + `amount_paid > 0`.
  - No genera si: sin referidor; status pending/suspended; `amount_paid = 0`.
  - Idempotencia: una segunda inserción del mismo `commerce_renewal_id` falla por unique.
  - Al borrar renewal → commission pasa a `void`.
- `PayoutAccountTest`: encriptación; solo owner/super admin pueden leerla; validación all-or-nothing.
- `ReferralEndpointsTest`: stats correctos, lista de referidos, filtros de comisiones.
- `SuperAdminCommissionsTest`: aprobar, pagar (bloquea sin cuenta), anular, filtros.

### Frontend
- `npm run type-check` sin errores.
- Smoke manual: ver checklist en Task final del plan.

---

## 7. Despliegue

- Migraciones se aplican vía `php artisan migrate --force` del workflow existente.
- Backfill ocurre dentro de la migración (no hay paso manual).
- No requiere cambios al pipeline de CI.
