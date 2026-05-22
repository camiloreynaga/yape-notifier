# Frontend Redesign Fase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the new design system (Pharmly-inspired green/lime palette + sidebar layout) and rebuild the Notifications module with inline Validate, drawer-based detail, KPIs, and instance breakdown.

**Architecture:** Phased: backend filters first → frontend design tokens → primitives (Button, Badge) → layout (Sidebar + TopBar + AppLayout) → Notifications module pieces (KPIs, Toolbar, Table, Drawer, InstancesBreakdown) → wiring + cleanup.

**Tech Stack:** Laravel 11 (backend), PHPUnit. React 18 + TypeScript + Vite + Tailwind CSS, React Query (TanStack Query v5), Vitest, lucide-react, date-fns.

**Source spec:** `docs/superpowers/specs/2026-04-29-frontend-redesign-fase-1-notificaciones.md`

---

## File Structure

### Backend (Laravel)
- Modify: `apps/api/app/Http/Controllers/NotificationController.php` — add `instance_id[]`/`device_id[]` array filters, add `byInstance()` method
- Modify: `apps/api/app/Services/NotificationService.php` — extend `getUserNotifications` filter contract to accept arrays; add `getByInstanceStats`
- Modify: `apps/api/routes/api.php` — register `GET /api/notifications/by-instance`
- Modify: `apps/api/tests/Feature/NotificationTest.php` (or new) — coverage for the new behavior

### Frontend (React)
- Modify: `apps/web-dashboard/tailwind.config.js` — new palette
- Create: `apps/web-dashboard/src/components/UI/Button.tsx`
- Create: `apps/web-dashboard/src/components/UI/Badge.tsx`
- Create: `apps/web-dashboard/src/components/UI/StatusBadge.tsx`
- Create: `apps/web-dashboard/src/components/Layout/Sidebar.tsx`
- Create: `apps/web-dashboard/src/components/Layout/TopBar.tsx`
- Create: `apps/web-dashboard/src/components/Layout/AppLayout.tsx`
- Create: `apps/web-dashboard/src/components/Notifications/NotificationsKpis.tsx`
- Create: `apps/web-dashboard/src/components/Notifications/NotificationsToolbar.tsx`
- Create: `apps/web-dashboard/src/components/Notifications/NotificationsTable.tsx`
- Create: `apps/web-dashboard/src/components/Notifications/NotificationDrawer.tsx`
- Create: `apps/web-dashboard/src/components/Notifications/InstancesBreakdown.tsx`
- Create: `apps/web-dashboard/src/components/Notifications/PossibleDuplicateBadge.tsx`
- Create: `apps/web-dashboard/src/hooks/useValidateNotification.ts`
- Create: `apps/web-dashboard/src/hooks/useNotificationsByInstance.ts`
- Modify: `apps/web-dashboard/src/types/index.ts` — extend `NotificationFilters` with array fields; add `NotificationsByInstanceRow` type
- Modify: `apps/web-dashboard/src/services/api.ts` — `getNotifications` filter serialization for arrays; add `getNotificationsByInstance`
- Modify: `apps/web-dashboard/src/config/api.ts` — add `byInstance` endpoint
- Modify: `apps/web-dashboard/src/pages/NotificationsPage.tsx` — full refactor using new components
- Modify: `apps/web-dashboard/src/App.tsx` — use `AppLayout` instead of `Layout`, remove `/notifications/:id` route
- Delete (last task): `apps/web-dashboard/src/pages/NotificationDetailPage.tsx`, plus old `Layout.tsx`, `NotificationCard`, `NotificationList` if unused

---

## Phase 1 — Backend filters and by-instance endpoint

### Task 1: Accept `instance_id[]` and `device_id[]` arrays in notifications list

**Files:**
- Modify: `apps/api/app/Http/Controllers/NotificationController.php`
- Modify: `apps/api/app/Services/NotificationService.php`
- Modify: `apps/api/tests/Feature/NotificationTest.php`

- [ ] **Step 1: Write failing test**

Append to `apps/api/tests/Feature/NotificationTest.php` (inside the existing class, before the closing brace):

```php
public function test_list_filters_by_multiple_instance_ids(): void
{
    $user = \App\Models\User::factory()->create(['role' => 'admin']);
    $commerce = \App\Models\Commerce::factory()->create(['owner_user_id' => $user->id, 'status' => 'active']);
    $user->update(['commerce_id' => $commerce->id]);
    $token = $user->createToken('test')->plainTextToken;

    $device = \App\Models\Device::factory()->create([
        'user_id' => $user->id,
        'commerce_id' => $commerce->id,
    ]);

    $instanceA = \App\Models\AppInstance::factory()->create(['device_id' => $device->id]);
    $instanceB = \App\Models\AppInstance::factory()->create(['device_id' => $device->id]);
    $instanceC = \App\Models\AppInstance::factory()->create(['device_id' => $device->id]);

    \App\Models\Notification::factory()->count(2)->create([
        'user_id' => $user->id,
        'commerce_id' => $commerce->id,
        'device_id' => $device->id,
        'app_instance_id' => $instanceA->id,
    ]);
    \App\Models\Notification::factory()->count(3)->create([
        'user_id' => $user->id,
        'commerce_id' => $commerce->id,
        'device_id' => $device->id,
        'app_instance_id' => $instanceB->id,
    ]);
    \App\Models\Notification::factory()->count(1)->create([
        'user_id' => $user->id,
        'commerce_id' => $commerce->id,
        'device_id' => $device->id,
        'app_instance_id' => $instanceC->id,
    ]);

    $response = $this->withHeader('Authorization', "Bearer {$token}")
        ->getJson("/api/notifications?instance_id[]={$instanceA->id}&instance_id[]={$instanceB->id}");

    $response->assertOk();
    $this->assertEquals(5, $response->json('total'));
}
```

- [ ] **Step 2: Run, expect fail**

Run: `cd infra/docker/environments/development && docker compose --env-file .env exec -T php-fpm php artisan test --filter=test_list_filters_by_multiple_instance_ids`
Expected: FAIL — current code accepts only scalar `app_instance_id`/`device_id`.

- [ ] **Step 3: Update service to handle arrays**

In `apps/api/app/Services/NotificationService.php`, find `getUserNotifications` (the method used by the controller's index). Locate the where clauses for `device_id` and `app_instance_id` and replace them so they accept either scalar or array:

```php
// inside getUserNotifications, after applying scoping by user/commerce
if (! empty($filters['device_id'])) {
    $deviceIds = (array) $filters['device_id'];
    $query->whereIn('device_id', $deviceIds);
}

if (! empty($filters['app_instance_id'])) {
    $instanceIds = (array) $filters['app_instance_id'];
    $query->whereIn('app_instance_id', $instanceIds);
}
```

Remove any prior scalar `where('device_id', ...)` / `where('app_instance_id', ...)` so they are not applied twice.

- [ ] **Step 4: Update controller to forward arrays**

In `apps/api/app/Http/Controllers/NotificationController.php`, replace the `$filters = $request->only([...])` block in `index()` with this version that accepts both scalar and array:

```php
$filters = [
    'device_id'          => $request->input('device_id', $request->input('device_id')),
    'app_instance_id'    => $request->input('app_instance_id'),
    'instance_id'        => $request->input('instance_id'), // accept alias
    'source_app'         => $request->input('source_app'),
    'package_name'       => $request->input('package_name'),
    'start_date'         => $request->input('start_date'),
    'end_date'           => $request->input('end_date'),
    'status'             => $request->input('status'),
    'exclude_duplicates' => $request->boolean('exclude_duplicates'),
];

// Allow instance_id as alias of app_instance_id (frontend uses instance_id[])
if (! empty($filters['instance_id']) && empty($filters['app_instance_id'])) {
    $filters['app_instance_id'] = $filters['instance_id'];
}
unset($filters['instance_id']);

// Drop nulls so the service doesn't apply empty filters
$filters = array_filter($filters, fn ($v) => $v !== null && $v !== '');
```

- [ ] **Step 5: Run test, expect pass**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=test_list_filters_by_multiple_instance_ids`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add apps/api/app/Http/Controllers/NotificationController.php apps/api/app/Services/NotificationService.php apps/api/tests/Feature/NotificationTest.php
git commit -m "feat(api): accept instance_id[] and device_id[] arrays in notifications list"
```

---

### Task 2: `GET /api/notifications/by-instance` endpoint

**Files:**
- Modify: `apps/api/app/Http/Controllers/NotificationController.php`
- Modify: `apps/api/routes/api.php`
- Modify: `apps/api/tests/Feature/NotificationTest.php`

- [ ] **Step 1: Write failing test**

Append to `NotificationTest.php`:

```php
public function test_by_instance_returns_aggregated_stats_per_instance(): void
{
    $user = \App\Models\User::factory()->create(['role' => 'admin']);
    $commerce = \App\Models\Commerce::factory()->create(['owner_user_id' => $user->id, 'status' => 'active']);
    $user->update(['commerce_id' => $commerce->id]);
    $token = $user->createToken('test')->plainTextToken;

    $device = \App\Models\Device::factory()->create([
        'user_id' => $user->id,
        'commerce_id' => $commerce->id,
    ]);
    $instanceA = \App\Models\AppInstance::factory()->create([
        'device_id' => $device->id,
        'label' => 'Katty - Yape',
    ]);
    $instanceB = \App\Models\AppInstance::factory()->create([
        'device_id' => $device->id,
        'label' => 'Erika - Yape',
    ]);

    \App\Models\Notification::factory()->create([
        'user_id' => $user->id,
        'commerce_id' => $commerce->id,
        'device_id' => $device->id,
        'app_instance_id' => $instanceA->id,
        'amount' => 70,
        'status' => 'pending',
    ]);
    \App\Models\Notification::factory()->create([
        'user_id' => $user->id,
        'commerce_id' => $commerce->id,
        'device_id' => $device->id,
        'app_instance_id' => $instanceA->id,
        'amount' => 30,
        'status' => 'validated',
    ]);
    \App\Models\Notification::factory()->create([
        'user_id' => $user->id,
        'commerce_id' => $commerce->id,
        'device_id' => $device->id,
        'app_instance_id' => $instanceB->id,
        'amount' => 50,
        'status' => 'pending',
    ]);

    $response = $this->withHeader('Authorization', "Bearer {$token}")
        ->getJson('/api/notifications/by-instance');

    $response->assertOk()
        ->assertJsonStructure([
            'data' => [
                ['instance_id', 'instance_label', 'total', 'validated', 'pending', 'inconsistent', 'amount_total'],
            ],
        ])
        ->assertJsonCount(2, 'data');

    $rows = collect($response->json('data'))->keyBy('instance_id');
    $this->assertEquals(2, $rows[$instanceA->id]['total']);
    $this->assertEquals(1, $rows[$instanceA->id]['validated']);
    $this->assertEquals(1, $rows[$instanceA->id]['pending']);
    $this->assertEquals('100.00', $rows[$instanceA->id]['amount_total']);
    $this->assertEquals(1, $rows[$instanceB->id]['total']);
}
```

- [ ] **Step 2: Run, expect fail (404)**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=test_by_instance_returns_aggregated_stats_per_instance`
Expected: FAIL — route does not exist.

- [ ] **Step 3: Add controller method**

Append to `NotificationController.php` (as a new public method, do not touch existing methods):

```php
public function byInstance(Request $request): JsonResponse
{
    $user = $request->user();
    $commerceId = $user->commerce_id;

    $startDate = $request->input('start_date');
    $endDate = $request->input('end_date');

    $query = \App\Models\Notification::query()
        ->select([
            'app_instance_id',
            \DB::raw('COUNT(*) as total'),
            \DB::raw("COUNT(CASE WHEN status = 'validated' THEN 1 END) as validated"),
            \DB::raw("COUNT(CASE WHEN status = 'pending' THEN 1 END) as pending"),
            \DB::raw("COUNT(CASE WHEN status = 'inconsistent' THEN 1 END) as inconsistent"),
            \DB::raw('COALESCE(SUM(amount), 0) as amount_total'),
        ])
        ->whereNotNull('app_instance_id')
        ->where('commerce_id', $commerceId)
        ->groupBy('app_instance_id');

    if ($startDate) {
        $query->where('created_at', '>=', $startDate);
    }
    if ($endDate) {
        $query->where('created_at', '<=', $endDate);
    }

    $rows = $query->get();
    $instanceIds = $rows->pluck('app_instance_id')->all();
    $labels = \App\Models\AppInstance::whereIn('id', $instanceIds)
        ->pluck('label', 'id');

    $data = $rows->map(fn ($r) => [
        'instance_id'    => (int) $r->app_instance_id,
        'instance_label' => $labels[$r->app_instance_id] ?? 'Sin etiqueta',
        'total'          => (int) $r->total,
        'validated'      => (int) $r->validated,
        'pending'        => (int) $r->pending,
        'inconsistent'   => (int) $r->inconsistent,
        'amount_total'   => number_format((float) $r->amount_total, 2, '.', ''),
    ])->values();

    return response()->json(['data' => $data]);
}
```

- [ ] **Step 4: Register route**

In `apps/api/routes/api.php`, inside the `Route::middleware(['auth:sanctum', 'commerce.active'])->group(...)` block, ABOVE the existing `Route::get('/notifications', ...)`, add:

```php
Route::get('/notifications/by-instance', [NotificationController::class, 'byInstance']);
```

(Order matters: this route must appear before `/notifications/{id}` if any wildcard collides, but `by-instance` does not match `{id}` integer so order is safe; placing it above keeps the file readable.)

- [ ] **Step 5: Run test, expect pass**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=test_by_instance_returns_aggregated_stats_per_instance`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add apps/api/app/Http/Controllers/NotificationController.php apps/api/routes/api.php apps/api/tests/Feature/NotificationTest.php
git commit -m "feat(api): add notifications/by-instance aggregated stats endpoint"
```

---

## Phase 2 — Frontend design tokens (Tailwind palette)

### Task 3: Update `tailwind.config.js` with new palette

**Files:**
- Modify: `apps/web-dashboard/tailwind.config.js`

- [ ] **Step 1: Replace the file with the new palette**

```js
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50:  '#F0F4F1',
          100: '#DCE6DE',
          200: '#B8CCBC',
          300: '#8FAD96',
          400: '#648E6F',
          500: '#406E4F',
          600: '#2A5238',
          700: '#1F3D2A',
          800: '#1A2E2A',
          900: '#14211F',
        },
        accent: {
          50:  '#F4FBE3',
          100: '#E8F5C4',
          200: '#DAEDA0',
          300: '#C5E865',
          400: '#B0D850',
          500: '#94BC34',
        },
        surface: {
          DEFAULT: '#FFFFFF',
          dark: '#1A2E2A',
          warm: '#F7F7F2',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
```

- [ ] **Step 2: Type-check + smoke test**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no errors.

Visually browse to `http://localhost:3000` to confirm the dashboard still renders (will look "off" because old components used the old blue `primary-*`; that is expected and gets fixed in later tasks).

- [ ] **Step 3: Commit**

```bash
git add apps/web-dashboard/tailwind.config.js
git commit -m "feat(dashboard): switch palette to Pharmly-inspired green/lime"
```

---

## Phase 3 — UI primitives (Button, Badge, StatusBadge)

### Task 4: `Button` component

**Files:**
- Create: `apps/web-dashboard/src/components/UI/Button.tsx`

- [ ] **Step 1: Create the component**

```tsx
import { forwardRef, ButtonHTMLAttributes } from 'react';
import { Loader2 } from 'lucide-react';

type Variant = 'primary' | 'dark' | 'outline' | 'ghost' | 'danger' | 'success';
type Size = 'sm' | 'md' | 'lg';

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
  icon?: React.ReactNode;
}

const VARIANTS: Record<Variant, string> = {
  primary: 'bg-accent-300 text-primary-900 hover:bg-accent-400 disabled:bg-accent-100 disabled:text-primary-400',
  dark:    'bg-primary-800 text-white hover:bg-primary-700 disabled:bg-primary-300',
  outline: 'border border-gray-300 bg-white text-gray-800 hover:bg-gray-50 disabled:opacity-50',
  ghost:   'bg-transparent text-gray-700 hover:bg-gray-100 disabled:opacity-50',
  danger:  'bg-red-600 text-white hover:bg-red-700 disabled:bg-red-300',
  success: 'bg-green-600 text-white hover:bg-green-700 disabled:bg-green-300',
};

const SIZES: Record<Size, string> = {
  sm: 'px-2.5 py-1.5 text-xs gap-1.5',
  md: 'px-3.5 py-2 text-sm gap-2',
  lg: 'px-5 py-2.5 text-base gap-2',
};

const Button = forwardRef<HTMLButtonElement, Props>(
  ({ variant = 'primary', size = 'md', loading = false, icon, children, className = '', disabled, ...rest }, ref) => {
    return (
      <button
        ref={ref}
        disabled={disabled || loading}
        className={`inline-flex items-center justify-center rounded-lg font-medium transition-colors disabled:cursor-not-allowed ${VARIANTS[variant]} ${SIZES[size]} ${className}`}
        {...rest}
      >
        {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : icon}
        {children}
      </button>
    );
  }
);
Button.displayName = 'Button';
export default Button;
```

- [ ] **Step 2: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no new errors.

- [ ] **Step 3: Commit**

```bash
git add apps/web-dashboard/src/components/UI/Button.tsx
git commit -m "feat(dashboard): add Button primitive with variants"
```

---

### Task 5: `Badge` and `StatusBadge` components

**Files:**
- Create: `apps/web-dashboard/src/components/UI/Badge.tsx`
- Create: `apps/web-dashboard/src/components/UI/StatusBadge.tsx`

- [ ] **Step 1: Create `Badge.tsx`**

```tsx
import { ReactNode } from 'react';

type Tone = 'gray' | 'green' | 'yellow' | 'red' | 'blue' | 'purple' | 'lime';

interface Props {
  tone?: Tone;
  children: ReactNode;
  dot?: boolean;
}

const TONES: Record<Tone, { bg: string; text: string; ring: string; dot: string }> = {
  gray:   { bg: 'bg-gray-100',    text: 'text-gray-700',    ring: 'ring-gray-300',    dot: 'bg-gray-500'   },
  green:  { bg: 'bg-green-50',    text: 'text-green-700',   ring: 'ring-green-300',   dot: 'bg-green-500'  },
  yellow: { bg: 'bg-yellow-50',   text: 'text-yellow-800',  ring: 'ring-yellow-300',  dot: 'bg-yellow-500' },
  red:    { bg: 'bg-red-50',      text: 'text-red-700',     ring: 'ring-red-300',     dot: 'bg-red-500'    },
  blue:   { bg: 'bg-blue-50',     text: 'text-blue-700',    ring: 'ring-blue-300',    dot: 'bg-blue-500'   },
  purple: { bg: 'bg-purple-50',   text: 'text-purple-700',  ring: 'ring-purple-300',  dot: 'bg-purple-500' },
  lime:   { bg: 'bg-accent-100',  text: 'text-primary-800', ring: 'ring-accent-300',  dot: 'bg-accent-300' },
};

export default function Badge({ tone = 'gray', children, dot = false }: Props) {
  const t = TONES[tone];
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset ${t.bg} ${t.text} ${t.ring}`}>
      {dot && <span className={`h-1.5 w-1.5 rounded-full ${t.dot}`} />}
      {children}
    </span>
  );
}
```

- [ ] **Step 2: Create `StatusBadge.tsx`**

```tsx
import Badge from './Badge';

type Status = 'pending' | 'validated' | 'inconsistent';

const CONFIG: Record<Status, { tone: 'yellow' | 'green' | 'red'; label: string }> = {
  pending:      { tone: 'yellow', label: 'Pendiente' },
  validated:    { tone: 'green',  label: 'Validada' },
  inconsistent: { tone: 'red',    label: 'Inconsistente' },
};

export default function StatusBadge({ status }: { status: Status }) {
  const cfg = CONFIG[status];
  return <Badge tone={cfg.tone} dot>{cfg.label}</Badge>;
}
```

- [ ] **Step 3: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no new errors.

- [ ] **Step 4: Commit**

```bash
git add apps/web-dashboard/src/components/UI/Badge.tsx apps/web-dashboard/src/components/UI/StatusBadge.tsx
git commit -m "feat(dashboard): add Badge and StatusBadge primitives"
```

---

## Phase 4 — Layout: Sidebar + TopBar + AppLayout

### Task 6: `Sidebar` component

**Files:**
- Create: `apps/web-dashboard/src/components/Layout/Sidebar.tsx`

- [ ] **Step 1: Create the component**

```tsx
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Bell,
  Smartphone,
  Users,
  ScrollText,
  Settings,
  LogOut,
  Shield,
} from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';

interface NavItem {
  to: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
}

const NAV: NavItem[] = [
  { to: '/dashboard',                       label: 'Resumen',        icon: LayoutDashboard },
  { to: '/dashboard?tab=notifications',     label: 'Notificaciones', icon: Bell },
  { to: '/dashboard?tab=devices',           label: 'Dispositivos',   icon: Smartphone },
  { to: '/dashboard?tab=employees',         label: 'Empleados',      icon: Users },
  { to: '/dashboard?tab=logs',              label: 'Logs',           icon: ScrollText },
  { to: '/dashboard?tab=settings',          label: 'Configuracion',  icon: Settings },
];

export default function Sidebar({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { user, logout } = useAuth();
  const isSuperAdmin = user?.role === 'super_admin';

  return (
    <>
      {open && (
        <button
          aria-label="Cerrar menu"
          onClick={onClose}
          className="fixed inset-0 z-30 bg-black/40 md:hidden"
        />
      )}
      <aside
        className={`fixed md:static z-40 h-full w-56 shrink-0 bg-primary-800 text-white transition-transform ${open ? 'translate-x-0' : '-translate-x-full md:translate-x-0'}`}
      >
        <div className="flex h-16 items-center gap-2 px-5 border-b border-primary-700">
          <span className="text-xl font-bold">Yape Notifier</span>
        </div>

        {isSuperAdmin && (
          <div className="px-4 py-3 border-b border-primary-700">
            <NavLink
              to="/super-admin"
              onClick={onClose}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium ${isActive ? 'bg-accent-300 text-primary-900' : 'text-white/80 hover:bg-primary-700 hover:text-white'}`
              }
            >
              <Shield className="h-4 w-4" /> Panel Super Admin
            </NavLink>
          </div>
        )}

        <nav className="px-4 py-4 space-y-1">
          {NAV.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/dashboard'}
              onClick={onClose}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium ${isActive ? 'bg-accent-300 text-primary-900' : 'text-white/80 hover:bg-primary-700 hover:text-white'}`
              }
            >
              <Icon className="h-4 w-4" /> {label}
            </NavLink>
          ))}
        </nav>

        <div className="absolute bottom-0 left-0 right-0 border-t border-primary-700 p-4">
          <div className="text-xs text-white/60 mb-2 truncate">{user?.email ?? ''}</div>
          <button
            onClick={logout}
            className="flex items-center gap-2 text-sm text-white/80 hover:text-white"
          >
            <LogOut className="h-4 w-4" /> Cerrar sesion
          </button>
        </div>
      </aside>
    </>
  );
}
```

- [ ] **Step 2: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no new errors.

- [ ] **Step 3: Commit**

```bash
git add apps/web-dashboard/src/components/Layout/Sidebar.tsx
git commit -m "feat(dashboard): add dark Sidebar component"
```

---

### Task 7: `TopBar` component

**Files:**
- Create: `apps/web-dashboard/src/components/Layout/TopBar.tsx`

- [ ] **Step 1: Create the component**

```tsx
import { Menu } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';

interface Props {
  onMenuClick: () => void;
}

export default function TopBar({ onMenuClick }: Props) {
  const { user } = useAuth();
  const isSuperAdmin = user?.role === 'super_admin';

  return (
    <header className="sticky top-0 z-20 flex h-16 items-center justify-between border-b border-gray-200 bg-white px-4 md:px-6">
      <div className="flex items-center gap-3">
        <button
          aria-label="Abrir menu"
          onClick={onMenuClick}
          className="md:hidden rounded-md p-2 hover:bg-gray-100"
        >
          <Menu className="h-5 w-5" />
        </button>
        {isSuperAdmin && (
          <span className="inline-flex items-center rounded-full bg-accent-100 px-2.5 py-0.5 text-xs font-bold text-primary-800 ring-1 ring-accent-300">
            SUPER ADMIN
          </span>
        )}
      </div>
      <div className="flex items-center gap-3">
        <div className="text-right">
          <p className="text-sm font-medium text-gray-900">{user?.name ?? '—'}</p>
          <p className="text-xs text-gray-500">{user?.email ?? ''}</p>
        </div>
      </div>
    </header>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/web-dashboard/src/components/Layout/TopBar.tsx
git commit -m "feat(dashboard): add TopBar component"
```

---

### Task 8: `AppLayout` component

**Files:**
- Create: `apps/web-dashboard/src/components/Layout/AppLayout.tsx`

- [ ] **Step 1: Create the layout**

```tsx
import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import TopBar from './TopBar';

export default function AppLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  return (
    <div className="flex min-h-screen bg-surface-warm">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      <div className="flex flex-1 flex-col min-w-0">
        <TopBar onMenuClick={() => setSidebarOpen(true)} />
        <main className="flex-1 p-4 md:p-6">
          <div className="max-w-[1600px] mx-auto">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no new errors.

- [ ] **Step 3: Commit**

```bash
git add apps/web-dashboard/src/components/Layout/AppLayout.tsx
git commit -m "feat(dashboard): add AppLayout combining Sidebar and TopBar"
```

---

### Task 9: Wire `AppLayout` in App.tsx (replace old `Layout`)

**Files:**
- Modify: `apps/web-dashboard/src/App.tsx`

- [ ] **Step 1: Replace `Layout` with `AppLayout` in routing**

In `App.tsx`, find the import of `Layout`:

```tsx
import Layout from './components/Layout';
```

Replace with:

```tsx
import AppLayout from './components/Layout/AppLayout';
```

Then in the `<Routes>` block, every `<Layout />` element is replaced with `<AppLayout />`. There are typically two places:
- The `/super-admin` route element
- The `/` (root) route element

After change, the route blocks should look like:

```tsx
        <Route path="/super-admin" element={
          <PrivateRoute requireCommerce={false}>
            <AppLayout />
          </PrivateRoute>
        }>
          <Route element={<SuperAdminPage />}>
            <Route index element={<Navigate to="/super-admin/commerces" replace />} />
            <Route path="commerces" element={<SuperAdminCommercesTab />} />
            <Route path="plans" element={<SuperAdminPlansTab />} />
          </Route>
        </Route>
        <Route path="/" element={
          <PrivateRoute requireCommerce={true}>
            <AppLayout />
          </PrivateRoute>
        }>
          {/* existing children unchanged */}
        </Route>
```

- [ ] **Step 2: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no new errors.

- [ ] **Step 3: Smoke test in browser**

Reload `http://localhost:3000`. The dashboard should now render with the dark sidebar on the left and topbar on top. The old header with "Yape Notifier" title is gone. Some interior visuals will look "off" until later cleanup tasks.

- [ ] **Step 4: Commit**

```bash
git add apps/web-dashboard/src/App.tsx
git commit -m "feat(dashboard): switch routes to AppLayout (sidebar+topbar)"
```

---

### Task 10: Remove `/notifications/:id` route from App.tsx

**Files:**
- Modify: `apps/web-dashboard/src/App.tsx`

- [ ] **Step 1: Delete the route + lazy import**

In `App.tsx`, find and DELETE:

```tsx
const NotificationDetailPage = lazy(() => import('./pages/NotificationDetailPage'));
```

And the route line:

```tsx
<Route path="notifications/:id" element={<NotificationDetailPage />} />
```

Inside the `/` route block.

- [ ] **Step 2: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: TS error if `NotificationDetailPage` is referenced elsewhere. If not, clean.

- [ ] **Step 3: Commit**

```bash
git add apps/web-dashboard/src/App.tsx
git commit -m "feat(dashboard): remove broken /notifications/:id route"
```

---

## Phase 5 — Notifications: types and API client

### Task 11: Extend `NotificationFilters` type and add `NotificationsByInstanceRow`

**Files:**
- Modify: `apps/web-dashboard/src/types/index.ts`

- [ ] **Step 1: Replace the `NotificationFilters` interface**

Find the existing `NotificationFilters` interface and replace with:

```typescript
export interface NotificationFilters {
  device_id?: number | number[];
  source_app?: string;
  package_name?: string;
  app_instance_id?: number;
  instance_id?: number | number[]; // alias accepted by backend
  start_date?: string;
  end_date?: string;
  status?: 'pending' | 'validated' | 'inconsistent';
  exclude_duplicates?: boolean;
  per_page?: number;
  page?: number;
  q?: string;
}
```

- [ ] **Step 2: Append the new type at the bottom of the file**

```typescript
export interface NotificationsByInstanceRow {
  instance_id: number;
  instance_label: string;
  total: number;
  validated: number;
  pending: number;
  inconsistent: number;
  amount_total: string; // backend returns formatted decimal as string
}
```

- [ ] **Step 3: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no new errors.

- [ ] **Step 4: Commit**

```bash
git add apps/web-dashboard/src/types/index.ts
git commit -m "feat(dashboard): extend NotificationFilters and add by-instance row type"
```

---

### Task 12: Add `byInstance` endpoint and `getNotificationsByInstance` method

**Files:**
- Modify: `apps/web-dashboard/src/config/api.ts`
- Modify: `apps/web-dashboard/src/services/api.ts`

- [ ] **Step 1: Add the endpoint**

In `apps/web-dashboard/src/config/api.ts`, inside the `notifications` group:

```typescript
  notifications: {
    list: "/api/notifications",
    create: "/api/notifications",
    show: (id: number) => `/api/notifications/${id}`,
    statistics: "/api/notifications/statistics",
    updateStatus: (id: number) => `/api/notifications/${id}/status`,
    byInstance: "/api/notifications/by-instance",
  },
```

- [ ] **Step 2: Add the API method and adjust array serialization**

In `apps/web-dashboard/src/services/api.ts`, find `getNotifications`. Replace it with:

```typescript
async getNotifications(filters?: NotificationFilters): Promise<PaginatedResponse<Notification>> {
  // axios serializes arrays with the [] suffix when paramsSerializer is left default;
  // we explicitly set arrayFormat to brackets via custom serializer for safety.
  const response = await this.client.get<PaginatedResponse<Notification>>(
    API_ENDPOINTS.notifications.list,
    {
      params: filters,
      paramsSerializer: {
        indexes: null, // produces ?instance_id[]=1&instance_id[]=2
      },
    }
  );
  return response.data;
}
```

Add a new method right below it:

```typescript
async getNotificationsByInstance(filters?: { start_date?: string; end_date?: string }): Promise<{ data: import('@/types').NotificationsByInstanceRow[] }> {
  const response = await this.client.get<{ data: import('@/types').NotificationsByInstanceRow[] }>(
    API_ENDPOINTS.notifications.byInstance,
    { params: filters }
  );
  return response.data;
}
```

- [ ] **Step 3: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no new errors.

- [ ] **Step 4: Commit**

```bash
git add apps/web-dashboard/src/config/api.ts apps/web-dashboard/src/services/api.ts
git commit -m "feat(dashboard): add notifications/by-instance API method and array params"
```

---

## Phase 6 — Notifications hooks (mutations + new query)

### Task 13: `useValidateNotification` mutation hook

**Files:**
- Create: `apps/web-dashboard/src/hooks/useValidateNotification.ts`

- [ ] **Step 1: Create the hook**

```typescript
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { apiService } from '@/services/api';

export function useValidateNotification() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: 'validated' | 'pending' | 'inconsistent' }) =>
      apiService.updateNotificationStatus(id, status),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['notifications'] });
      qc.invalidateQueries({ queryKey: ['notifications', 'byInstance'] });
      qc.invalidateQueries({ queryKey: ['notifications', 'statistics'] });
    },
  });
}
```

NOTE: this assumes `apiService.updateNotificationStatus(id, status)` exists. Verify it does. If the existing method has a different signature, adjust the `mutationFn` accordingly.

- [ ] **Step 2: Verify dependency exists**

Run: `grep -n "updateNotificationStatus" apps/web-dashboard/src/services/api.ts`
If it does not exist, add the following method to `api.ts` right after `getNotification`:

```typescript
async updateNotificationStatus(id: number, status: 'validated' | 'pending' | 'inconsistent'): Promise<void> {
  await this.client.patch(API_ENDPOINTS.notifications.updateStatus(id), { status });
}
```

- [ ] **Step 3: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no new errors.

- [ ] **Step 4: Commit**

```bash
git add apps/web-dashboard/src/hooks/useValidateNotification.ts apps/web-dashboard/src/services/api.ts
git commit -m "feat(dashboard): add useValidateNotification mutation hook"
```

---

### Task 14: `useNotificationsByInstance` query hook

**Files:**
- Create: `apps/web-dashboard/src/hooks/useNotificationsByInstance.ts`

- [ ] **Step 1: Create the hook**

```typescript
import { useQuery } from '@tanstack/react-query';
import { apiService } from '@/services/api';

export function useNotificationsByInstance(params: { start_date?: string; end_date?: string } = {}) {
  return useQuery({
    queryKey: ['notifications', 'byInstance', params],
    queryFn: () => apiService.getNotificationsByInstance(params),
    staleTime: 30_000,
  });
}
```

- [ ] **Step 2: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no new errors.

- [ ] **Step 3: Commit**

```bash
git add apps/web-dashboard/src/hooks/useNotificationsByInstance.ts
git commit -m "feat(dashboard): add useNotificationsByInstance query hook"
```

---

## Phase 7 — Notifications UI components

### Task 15: `NotificationsKpis` component

**Files:**
- Create: `apps/web-dashboard/src/components/Notifications/NotificationsKpis.tsx`

- [ ] **Step 1: Create the component**

```tsx
import { Bell, Clock, CheckCircle, AlertTriangle } from 'lucide-react';
import type { NotificationStatistics } from '@/types';

interface Props {
  stats?: NotificationStatistics;
  loading?: boolean;
  activeFilter: 'all' | 'pending' | 'validated' | 'inconsistent';
  onFilterChange: (filter: 'all' | 'pending' | 'validated' | 'inconsistent') => void;
}

interface CardConfig {
  key: 'all' | 'pending' | 'validated' | 'inconsistent';
  label: string;
  count: number;
  icon: React.ComponentType<{ className?: string }>;
  bg: string;
  fg: string;
  ring: string;
}

export default function NotificationsKpis({ stats, loading, activeFilter, onFilterChange }: Props) {
  if (loading || !stats) {
    return (
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className="h-24 rounded-xl bg-gray-100 animate-pulse" />
        ))}
      </div>
    );
  }

  const total = stats.total ?? 0;
  const byStatus = stats.by_status ?? [];
  const get = (s: string) => byStatus.find((x: { status: string; count: number }) => x.status === s)?.count ?? 0;

  const cards: CardConfig[] = [
    { key: 'all',          label: 'Total del periodo',     count: total,                  icon: Bell,           bg: 'bg-white',       fg: 'text-gray-800',   ring: 'ring-gray-200' },
    { key: 'pending',      label: 'Pendientes',            count: get('pending'),         icon: Clock,          bg: 'bg-yellow-50',   fg: 'text-yellow-800', ring: 'ring-yellow-200' },
    { key: 'validated',    label: 'Validadas',             count: get('validated'),       icon: CheckCircle,    bg: 'bg-green-50',    fg: 'text-green-800',  ring: 'ring-green-200' },
    { key: 'inconsistent', label: 'Inconsistentes',        count: get('inconsistent'),    icon: AlertTriangle,  bg: 'bg-red-50',      fg: 'text-red-800',    ring: 'ring-red-200' },
  ];

  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map((card) => {
        const isActive = activeFilter === card.key;
        const Icon = card.icon;
        return (
          <button
            key={card.key}
            onClick={() => onFilterChange(card.key)}
            className={`text-left rounded-xl ${card.bg} p-5 ring-1 ring-inset ${card.ring} transition-all hover:scale-[1.01] hover:shadow-sm ${isActive ? 'ring-2 ring-offset-2 ring-accent-300' : ''}`}
          >
            <div className="flex items-center justify-between mb-2">
              <Icon className={`h-5 w-5 ${card.fg}`} />
            </div>
            <div className={`text-3xl font-bold ${card.fg}`}>{card.count}</div>
            <div className={`text-sm ${card.fg} opacity-80`}>{card.label}</div>
          </button>
        );
      })}
    </div>
  );
}
```

- [ ] **Step 2: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no new errors.

- [ ] **Step 3: Commit**

```bash
git add apps/web-dashboard/src/components/Notifications/NotificationsKpis.tsx
git commit -m "feat(dashboard): add NotificationsKpis cards"
```

---

### Task 16: `NotificationsToolbar` component

**Files:**
- Create: `apps/web-dashboard/src/components/Notifications/NotificationsToolbar.tsx`

- [ ] **Step 1: Create the component**

```tsx
import { Search, Calendar, RefreshCw, Download } from 'lucide-react';
import Button from '@/components/UI/Button';
import { useDevices } from '@/hooks/useDevices';
import { useAppInstances } from '@/hooks/useAppInstances';

export type Period = 'today' | 'yesterday' | 'last7' | 'last30' | 'thisMonth' | 'lastMonth' | 'custom';

export interface ToolbarFilters {
  q: string;
  instance_ids: number[];
  device_ids: number[];
  period: Period;
}

interface Props {
  filters: ToolbarFilters;
  onChange: (next: ToolbarFilters) => void;
  onRefresh: () => void;
  onExport: () => void;
  exporting?: boolean;
}

const PERIODS: Array<{ key: Period; label: string }> = [
  { key: 'today',      label: 'Hoy' },
  { key: 'yesterday',  label: 'Ayer' },
  { key: 'last7',      label: 'Ultimos 7 dias' },
  { key: 'last30',     label: 'Ultimos 30 dias' },
  { key: 'thisMonth',  label: 'Este mes' },
  { key: 'lastMonth',  label: 'Mes pasado' },
];

export default function NotificationsToolbar({ filters, onChange, onRefresh, onExport, exporting }: Props) {
  const { devices = [] } = useDevices();
  const { data: instances = [] } = useAppInstances();

  const toggleInArray = (arr: number[], id: number) =>
    arr.includes(id) ? arr.filter((x) => x !== id) : [...arr, id];

  return (
    <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
      <div className="flex flex-wrap gap-2">
        <select
          multiple={false}
          value={filters.instance_ids[0] ?? ''}
          onChange={(e) => {
            const v = e.target.value ? Number(e.target.value) : null;
            onChange({ ...filters, instance_ids: v ? [v] : [] });
          }}
          className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm"
        >
          <option value="">Todas las instancias</option>
          {instances.map((i: { id: number; label: string | null }) => (
            <option key={i.id} value={i.id}>{i.label ?? `Instancia #${i.id}`}</option>
          ))}
        </select>

        <select
          value={filters.device_ids[0] ?? ''}
          onChange={(e) => {
            const v = e.target.value ? Number(e.target.value) : null;
            onChange({ ...filters, device_ids: v ? [v] : [] });
          }}
          className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm"
        >
          <option value="">Todos los dispositivos</option>
          {devices.map((d: { id: number; alias: string | null; name: string }) => (
            <option key={d.id} value={d.id}>{d.alias ?? d.name}</option>
          ))}
        </select>

        <select
          value={filters.period}
          onChange={(e) => onChange({ ...filters, period: e.target.value as Period })}
          className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm"
        >
          {PERIODS.map((p) => <option key={p.key} value={p.key}>{p.label}</option>)}
        </select>
      </div>

      <div className="relative flex-1 max-w-md">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
        <input
          type="text"
          value={filters.q}
          onChange={(e) => onChange({ ...filters, q: e.target.value })}
          placeholder="Buscar codigo, monto o pagador..."
          className="w-full rounded-lg border border-gray-300 bg-white pl-9 pr-3 py-2 text-sm focus:border-primary-500 focus:ring-primary-500"
        />
      </div>

      <div className="flex gap-2 lg:ml-auto">
        <Button variant="outline" size="md" icon={<RefreshCw className="h-4 w-4" />} onClick={onRefresh}>
          Actualizar
        </Button>
        <Button variant="dark" size="md" icon={<Download className="h-4 w-4" />} onClick={onExport} loading={exporting}>
          Exportar
        </Button>
      </div>
    </div>
  );
}

// Helper for parent to convert Period → start_date/end_date strings (YYYY-MM-DD)
export function periodToRange(period: Period): { start_date?: string; end_date?: string } {
  const today = new Date();
  const fmt = (d: Date) => d.toISOString().slice(0, 10);
  const startOfDay = (d: Date) => { d.setHours(0, 0, 0, 0); return d; };
  const endOfDay = (d: Date) => { d.setHours(23, 59, 59, 999); return d; };
  const t = startOfDay(new Date(today));

  switch (period) {
    case 'today':
      return { start_date: fmt(t), end_date: fmt(endOfDay(new Date(today))) };
    case 'yesterday': {
      const y = new Date(t); y.setDate(y.getDate() - 1);
      return { start_date: fmt(y), end_date: fmt(y) };
    }
    case 'last7': {
      const s = new Date(t); s.setDate(s.getDate() - 6);
      return { start_date: fmt(s), end_date: fmt(t) };
    }
    case 'last30': {
      const s = new Date(t); s.setDate(s.getDate() - 29);
      return { start_date: fmt(s), end_date: fmt(t) };
    }
    case 'thisMonth': {
      const s = new Date(today.getFullYear(), today.getMonth(), 1);
      return { start_date: fmt(s), end_date: fmt(t) };
    }
    case 'lastMonth': {
      const s = new Date(today.getFullYear(), today.getMonth() - 1, 1);
      const e = new Date(today.getFullYear(), today.getMonth(), 0);
      return { start_date: fmt(s), end_date: fmt(e) };
    }
    default:
      return {};
  }
}
```

NOTE: `useDevices` and `useAppInstances` already exist in the codebase; the destructured property names may differ slightly (e.g. `devices` vs `data`). If type-check fails on the destructure, adjust to match the existing hook return shape.

- [ ] **Step 2: Type-check, fix hook destructures if needed**

Run: `cd apps/web-dashboard && npm run type-check`
If errors mention `useDevices` or `useAppInstances`, open those hook files and adjust the destructure in `NotificationsToolbar` to match.

- [ ] **Step 3: Commit**

```bash
git add apps/web-dashboard/src/components/Notifications/NotificationsToolbar.tsx
git commit -m "feat(dashboard): add NotificationsToolbar with filters and search"
```

---

### Task 17: `PossibleDuplicateBadge` component

**Files:**
- Create: `apps/web-dashboard/src/components/Notifications/PossibleDuplicateBadge.tsx`

- [ ] **Step 1: Create the component**

```tsx
import Badge from '@/components/UI/Badge';
import type { Notification } from '@/types';

export function isPossibleDuplicate(target: Notification, all: Notification[]): boolean {
  return all.some(
    (n) =>
      n.id !== target.id &&
      n.amount === target.amount &&
      n.security_code &&
      n.security_code === target.security_code &&
      Math.abs(new Date(n.created_at).getTime() - new Date(target.created_at).getTime()) < 60_000
  );
}

export default function PossibleDuplicateBadge() {
  return <Badge tone="yellow">POSIBLE DUPLICADO</Badge>;
}
```

NOTE: Adjust property name `security_code` if the actual `Notification` type uses a different name (it might be `securityCode`). Check `apps/web-dashboard/src/types/index.ts` and adapt.

- [ ] **Step 2: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no new errors. If `security_code` is wrong, fix to the actual property name.

- [ ] **Step 3: Commit**

```bash
git add apps/web-dashboard/src/components/Notifications/PossibleDuplicateBadge.tsx
git commit -m "feat(dashboard): add PossibleDuplicateBadge with duplicate detection"
```

---

### Task 18: `NotificationsTable` component

**Files:**
- Create: `apps/web-dashboard/src/components/Notifications/NotificationsTable.tsx`

- [ ] **Step 1: Create the component**

```tsx
import { Eye, Check, MoreVertical, RotateCcw } from 'lucide-react';
import { format } from 'date-fns';
import Button from '@/components/UI/Button';
import StatusBadge from '@/components/UI/StatusBadge';
import Badge from '@/components/UI/Badge';
import PossibleDuplicateBadge, { isPossibleDuplicate } from './PossibleDuplicateBadge';
import type { Notification } from '@/types';

interface Props {
  notifications: Notification[];
  loading?: boolean;
  validatingId?: number | null;
  onValidate: (n: Notification) => void;
  onMarkInconsistent: (n: Notification) => void;
  onRevert: (n: Notification) => void;
  onView: (n: Notification) => void;
}

const APP_TONE: Record<string, 'purple' | 'blue' | 'red' | 'gray'> = {
  yape: 'purple',
  plin: 'blue',
  bcp: 'red',
};

function appBadge(sourceApp?: string | null) {
  const key = (sourceApp ?? '').toLowerCase();
  const tone = APP_TONE[key] ?? 'gray';
  return <Badge tone={tone}>{(sourceApp ?? 'OTRA').toUpperCase()}</Badge>;
}

export default function NotificationsTable({
  notifications, loading, validatingId, onValidate, onMarkInconsistent, onRevert, onView,
}: Props) {
  if (loading) {
    return <div className="rounded-xl bg-white p-8 text-center text-gray-500">Cargando notificaciones...</div>;
  }
  if (notifications.length === 0) {
    return (
      <div className="rounded-xl bg-white p-12 text-center">
        <p className="text-gray-700 font-medium">No hay notificaciones</p>
        <p className="text-sm text-gray-500 mt-1">Ajusta filtros o periodo para ver mas resultados.</p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white shadow-sm">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-3 py-3 text-left text-xs font-semibold text-gray-600">Fecha</th>
            <th className="px-3 py-3 text-left text-xs font-semibold text-gray-600">App</th>
            <th className="px-3 py-3 text-left text-xs font-semibold text-gray-600">Instancia</th>
            <th className="px-3 py-3 text-left text-xs font-semibold text-gray-600">Dispositivo</th>
            <th className="px-3 py-3 text-left text-xs font-semibold text-gray-600">Pagador</th>
            <th className="px-3 py-3 text-right text-xs font-semibold text-gray-600">Monto</th>
            <th className="px-3 py-3 text-left text-xs font-semibold text-gray-600">Codigo</th>
            <th className="px-3 py-3 text-left text-xs font-semibold text-gray-600">Estado</th>
            <th className="px-3 py-3 text-right text-xs font-semibold text-gray-600">Acciones</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {notifications.map((n) => {
            const dup = isPossibleDuplicate(n, notifications);
            return (
              <tr key={n.id} className="hover:bg-gray-50">
                <td className="px-3 py-3 text-sm text-gray-700 whitespace-nowrap">
                  {format(new Date(n.created_at), 'dd/MM HH:mm')}
                </td>
                <td className="px-3 py-3">{appBadge(n.source_app)}</td>
                <td className="px-3 py-3 text-sm text-gray-700">{n.app_instance?.label ?? '—'}</td>
                <td className="px-3 py-3 text-sm text-gray-700">{n.device?.alias ?? n.device?.name ?? '—'}</td>
                <td className="px-3 py-3 text-sm text-gray-700 max-w-[180px] truncate" title={n.payer_name ?? ''}>
                  {n.payer_name ?? '—'}
                </td>
                <td className="px-3 py-3 text-right text-sm font-semibold text-gray-900 whitespace-nowrap">
                  S/ {Number(n.amount).toFixed(2)}
                </td>
                <td className="px-3 py-3">
                  <span className="font-mono text-xs rounded-md bg-accent-100 text-primary-800 px-2 py-1">
                    {n.security_code ?? '—'}
                  </span>
                  {dup && <div className="mt-1"><PossibleDuplicateBadge /></div>}
                </td>
                <td className="px-3 py-3"><StatusBadge status={n.status} /></td>
                <td className="px-3 py-3 text-right">
                  <div className="flex justify-end gap-1.5">
                    {n.status === 'pending' && (
                      <Button
                        size="sm"
                        variant="success"
                        icon={<Check className="h-3.5 w-3.5" />}
                        onClick={() => onValidate(n)}
                        loading={validatingId === n.id}
                      >
                        Validar
                      </Button>
                    )}
                    {n.status === 'inconsistent' && (
                      <Button
                        size="sm"
                        variant="outline"
                        icon={<RotateCcw className="h-3.5 w-3.5" />}
                        onClick={() => onRevert(n)}
                      >
                        Revertir
                      </Button>
                    )}
                    <Button size="sm" variant="ghost" icon={<Eye className="h-3.5 w-3.5" />} onClick={() => onView(n)}>
                      Ver
                    </Button>
                    {n.status === 'pending' && (
                      <button
                        onClick={() => onMarkInconsistent(n)}
                        className="rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
                        title="Marcar inconsistente"
                      >
                        <MoreVertical className="h-3.5 w-3.5" />
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
```

NOTE: Property names `payer_name`, `security_code`, `app_instance`, `device.alias` may differ from current `Notification` type. Open `apps/web-dashboard/src/types/index.ts` and adjust if names are different.

- [ ] **Step 2: Type-check, adjust property names if needed**

Run: `cd apps/web-dashboard && npm run type-check`
Fix any property mismatches against the `Notification` type.

- [ ] **Step 3: Commit**

```bash
git add apps/web-dashboard/src/components/Notifications/NotificationsTable.tsx
git commit -m "feat(dashboard): add NotificationsTable with inline Validar action"
```

---

### Task 19: `NotificationDrawer` component

**Files:**
- Create: `apps/web-dashboard/src/components/Notifications/NotificationDrawer.tsx`

- [ ] **Step 1: Create the component**

```tsx
import { X, Check, AlertTriangle, RotateCcw } from 'lucide-react';
import { format } from 'date-fns';
import Button from '@/components/UI/Button';
import StatusBadge from '@/components/UI/StatusBadge';
import type { Notification } from '@/types';

interface Props {
  notification: Notification | null;
  onClose: () => void;
  onValidate: (n: Notification) => void;
  onMarkInconsistent: (n: Notification) => void;
  onRevert: (n: Notification) => void;
  busy?: boolean;
}

export default function NotificationDrawer({ notification, onClose, onValidate, onMarkInconsistent, onRevert, busy }: Props) {
  if (!notification) return null;

  return (
    <div className="fixed inset-0 z-40 bg-black/30" onClick={onClose}>
      <div
        className="fixed top-0 right-0 h-full w-full sm:w-[480px] bg-white shadow-xl overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="sticky top-0 bg-white border-b p-4 flex justify-between items-center">
          <h2 className="text-lg font-semibold">Detalle de notificacion</h2>
          <button onClick={onClose}><X className="h-5 w-5 text-gray-500 hover:text-gray-700" /></button>
        </div>

        <div className="p-5 space-y-5">
          <section>
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">Monto</p>
            <p className="text-3xl font-bold text-gray-900">S/ {Number(notification.amount).toFixed(2)}</p>
            <div className="mt-2"><StatusBadge status={notification.status} /></div>
          </section>

          {notification.security_code && (
            <section>
              <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">Codigo de seguridad</p>
              <p className="font-mono text-2xl text-primary-800 bg-accent-100 inline-block px-3 py-1 rounded-md mt-1">
                {notification.security_code}
              </p>
            </section>
          )}

          <section className="grid grid-cols-2 gap-3 text-sm">
            <div>
              <p className="text-xs text-gray-500">Pagador</p>
              <p className="font-medium">{notification.payer_name ?? '—'}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Fecha</p>
              <p className="font-medium">{format(new Date(notification.created_at), 'dd MMM yyyy HH:mm')}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Dispositivo</p>
              <p className="font-medium">{notification.device?.alias ?? notification.device?.name ?? '—'}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Instancia</p>
              <p className="font-medium">{notification.app_instance?.label ?? '—'}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500">App origen</p>
              <p className="font-medium">{notification.source_app ?? '—'}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Paquete</p>
              <p className="font-medium font-mono text-xs">{notification.package_name ?? '—'}</p>
            </div>
          </section>

          {notification.body && (
            <section>
              <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1">Mensaje original</p>
              <pre className="text-xs whitespace-pre-wrap bg-gray-50 rounded-md p-3 border border-gray-200">{notification.body}</pre>
            </section>
          )}

          <section className="space-y-2 pt-2">
            {notification.status === 'pending' && (
              <>
                <Button variant="success" className="w-full" icon={<Check className="h-4 w-4" />}
                  onClick={() => onValidate(notification)} loading={busy}>
                  Validar notificacion
                </Button>
                <Button variant="outline" className="w-full" icon={<AlertTriangle className="h-4 w-4" />}
                  onClick={() => onMarkInconsistent(notification)}>
                  Marcar inconsistente
                </Button>
              </>
            )}
            {notification.status === 'inconsistent' && (
              <Button variant="outline" className="w-full" icon={<RotateCcw className="h-4 w-4" />}
                onClick={() => onRevert(notification)}>
                Revertir a pendiente
              </Button>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Type-check, fix property mismatches**

Run: `cd apps/web-dashboard && npm run type-check`
Same drill as before — adjust property names if the actual `Notification` type differs.

- [ ] **Step 3: Commit**

```bash
git add apps/web-dashboard/src/components/Notifications/NotificationDrawer.tsx
git commit -m "feat(dashboard): add NotificationDrawer detail panel"
```

---

### Task 20: `InstancesBreakdown` component

**Files:**
- Create: `apps/web-dashboard/src/components/Notifications/InstancesBreakdown.tsx`

- [ ] **Step 1: Create the component**

```tsx
import { useState } from 'react';
import { ChevronDown, ChevronRight } from 'lucide-react';
import { useNotificationsByInstance } from '@/hooks/useNotificationsByInstance';

interface Props {
  start_date?: string;
  end_date?: string;
}

export default function InstancesBreakdown({ start_date, end_date }: Props) {
  const [open, setOpen] = useState(false);
  const { data, isLoading } = useNotificationsByInstance({ start_date, end_date });
  const rows = data?.data ?? [];

  return (
    <div className="rounded-xl border border-gray-200 bg-white">
      <button
        onClick={() => setOpen((v) => !v)}
        className="flex w-full items-center justify-between px-4 py-3 text-sm font-medium text-gray-800 hover:bg-gray-50"
      >
        <span className="flex items-center gap-2">
          {open ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
          Operaciones por instancia
        </span>
        <span className="text-xs text-gray-500">{rows.length} instancias</span>
      </button>
      {open && (
        <div className="border-t border-gray-200 overflow-x-auto">
          {isLoading ? (
            <div className="p-6 text-center text-sm text-gray-500">Cargando...</div>
          ) : rows.length === 0 ? (
            <div className="p-6 text-center text-sm text-gray-500">Sin datos en el periodo</div>
          ) : (
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-3 py-2 text-left font-semibold text-gray-600">Instancia</th>
                  <th className="px-3 py-2 text-right font-semibold text-gray-600">Ops</th>
                  <th className="px-3 py-2 text-right font-semibold text-gray-600">Validadas</th>
                  <th className="px-3 py-2 text-right font-semibold text-gray-600">Pendientes</th>
                  <th className="px-3 py-2 text-right font-semibold text-gray-600">Inconsistentes</th>
                  <th className="px-3 py-2 text-right font-semibold text-gray-600">Monto S/</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {rows.map((r) => (
                  <tr key={r.instance_id}>
                    <td className="px-3 py-2 font-medium text-gray-900">{r.instance_label}</td>
                    <td className="px-3 py-2 text-right">{r.total}</td>
                    <td className="px-3 py-2 text-right text-green-700">{r.validated}</td>
                    <td className="px-3 py-2 text-right text-yellow-700">{r.pending}</td>
                    <td className="px-3 py-2 text-right text-red-700">{r.inconsistent}</td>
                    <td className="px-3 py-2 text-right font-semibold">S/ {r.amount_total}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 2: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no new errors.

- [ ] **Step 3: Commit**

```bash
git add apps/web-dashboard/src/components/Notifications/InstancesBreakdown.tsx
git commit -m "feat(dashboard): add InstancesBreakdown collapsible table"
```

---

## Phase 8 — Notifications page wiring

### Task 21: Refactor `NotificationsPage` to use new components

**Files:**
- Modify: `apps/web-dashboard/src/pages/NotificationsPage.tsx`

This task replaces almost the entire current page. The old behavior (export, filtering, etc.) is preserved through the new components.

- [ ] **Step 1: Replace the page**

Replace the full content of `apps/web-dashboard/src/pages/NotificationsPage.tsx` with:

```tsx
import { useState, useMemo, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { apiService } from '@/services/api';
import { useNotifications } from '@/hooks/useNotifications';
import { useToast } from '@/hooks/useToast';
import { useDebouncedValue } from '@/hooks/useDebounce';
import { useValidateNotification } from '@/hooks/useValidateNotification';
import { logger } from '@/services/logger';
import type { Notification, NotificationFilters, NotificationStatistics } from '@/types';
import NotificationsKpis from '@/components/Notifications/NotificationsKpis';
import NotificationsToolbar, {
  periodToRange,
  type ToolbarFilters,
  type Period,
} from '@/components/Notifications/NotificationsToolbar';
import NotificationsTable from '@/components/Notifications/NotificationsTable';
import NotificationDrawer from '@/components/Notifications/NotificationDrawer';
import InstancesBreakdown from '@/components/Notifications/InstancesBreakdown';
import { useQuery } from '@tanstack/react-query';

type StatusFilter = 'all' | 'pending' | 'validated' | 'inconsistent';

export default function NotificationsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const toast = useToast();

  // Toolbar state synced to URL
  const [toolbar, setToolbar] = useState<ToolbarFilters>({
    q: searchParams.get('q') ?? '',
    instance_ids: searchParams.get('instance') ? [Number(searchParams.get('instance'))] : [],
    device_ids: searchParams.get('device') ? [Number(searchParams.get('device'))] : [],
    period: (searchParams.get('period') as Period) || 'last7',
  });
  const [statusFilter, setStatusFilter] = useState<StatusFilter>(
    (searchParams.get('status') as StatusFilter) || 'all'
  );

  const { debouncedValue: debouncedQ } = useDebouncedValue(toolbar.q, 300);

  // Build API filters
  const range = useMemo(() => periodToRange(toolbar.period), [toolbar.period]);
  const apiFilters: NotificationFilters = useMemo(() => ({
    per_page: 50,
    page: 1,
    status: statusFilter === 'all' ? undefined : statusFilter,
    instance_id: toolbar.instance_ids.length > 0 ? toolbar.instance_ids : undefined,
    device_id: toolbar.device_ids.length > 0 ? toolbar.device_ids : undefined,
    start_date: range.start_date,
    end_date: range.end_date,
  }), [statusFilter, toolbar.instance_ids, toolbar.device_ids, range]);

  // Sync URL with state
  const updateUrl = useCallback(() => {
    const next = new URLSearchParams();
    if (toolbar.q) next.set('q', toolbar.q);
    if (toolbar.instance_ids[0]) next.set('instance', String(toolbar.instance_ids[0]));
    if (toolbar.device_ids[0]) next.set('device', String(toolbar.device_ids[0]));
    if (toolbar.period !== 'last7') next.set('period', toolbar.period);
    if (statusFilter !== 'all') next.set('status', statusFilter);
    setSearchParams(next, { replace: true });
  }, [toolbar, statusFilter, setSearchParams]);

  // Notifications data via existing hook
  const { notifications, loading, refetch } = useNotifications({
    filters: apiFilters,
    enabled: true,
    onNewNotification: useCallback((n: Notification) => {
      logger.debug('Nueva notificacion', { id: n.id });
    }, []),
  });

  // Statistics for KPI cards
  const { data: stats } = useQuery<NotificationStatistics>({
    queryKey: ['notifications', 'statistics', range],
    queryFn: () => apiService.getStatistics({ start_date: range.start_date, end_date: range.end_date }),
    staleTime: 30_000,
  });

  // Client-side search by q across visible columns
  const filtered = useMemo(() => {
    if (!debouncedQ) return notifications;
    const q = debouncedQ.toLowerCase();
    return notifications.filter((n) =>
      [n.payer_name, n.security_code, String(n.amount), n.device?.alias, n.device?.name]
        .filter(Boolean)
        .some((v) => String(v).toLowerCase().includes(q))
    );
  }, [notifications, debouncedQ]);

  // Validate / mark inconsistent / revert mutations
  const validate = useValidateNotification();
  const [drawerNotif, setDrawerNotif] = useState<Notification | null>(null);
  const [exporting, setExporting] = useState(false);

  const onValidate = (n: Notification) => {
    validate.mutate(
      { id: n.id, status: 'validated' },
      {
        onSuccess: () => toast.success('Notificacion validada'),
        onError: (e) => toast.error('Error al validar: ' + (e as Error).message),
      }
    );
  };
  const onMarkInconsistent = (n: Notification) => {
    validate.mutate(
      { id: n.id, status: 'inconsistent' },
      {
        onSuccess: () => toast.success('Marcada como inconsistente'),
      }
    );
  };
  const onRevert = (n: Notification) => {
    validate.mutate(
      { id: n.id, status: 'pending' },
      {
        onSuccess: () => toast.success('Revertida a pendiente'),
      }
    );
  };

  const onExport = async () => {
    setExporting(true);
    try {
      // Reuse existing API export if any; fallback to JSON download
      const data = filtered;
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `notificaciones-${new Date().toISOString().slice(0, 10)}.json`;
      a.click();
      URL.revokeObjectURL(url);
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="space-y-6" onBlur={updateUrl}>
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Notificaciones</h1>
        <p className="text-sm text-gray-600">Visualiza y valida las notificaciones recibidas.</p>
      </div>

      <NotificationsKpis
        stats={stats}
        activeFilter={statusFilter}
        onFilterChange={setStatusFilter}
      />

      <NotificationsToolbar
        filters={toolbar}
        onChange={(next) => { setToolbar(next); updateUrl(); }}
        onRefresh={() => refetch()}
        onExport={onExport}
        exporting={exporting}
      />

      <InstancesBreakdown start_date={range.start_date} end_date={range.end_date} />

      <NotificationsTable
        notifications={filtered}
        loading={loading}
        validatingId={validate.isPending ? validate.variables?.id ?? null : null}
        onValidate={onValidate}
        onMarkInconsistent={onMarkInconsistent}
        onRevert={onRevert}
        onView={(n) => setDrawerNotif(n)}
      />

      <NotificationDrawer
        notification={drawerNotif}
        onClose={() => setDrawerNotif(null)}
        onValidate={(n) => { onValidate(n); setDrawerNotif(null); }}
        onMarkInconsistent={(n) => { onMarkInconsistent(n); setDrawerNotif(null); }}
        onRevert={(n) => { onRevert(n); setDrawerNotif(null); }}
        busy={validate.isPending}
      />
    </div>
  );
}
```

NOTE: `useToast` and `useDebouncedValue` hooks already exist; this code matches their existing API. If they have different signatures, adjust the destructure (e.g. `toast.success` vs `toast({type:'success'})`).

- [ ] **Step 2: Type-check, adjust hook calls if needed**

Run: `cd apps/web-dashboard && npm run type-check`
Fix any mismatches in hook return shapes against the actual files.

- [ ] **Step 3: Smoke test in browser**

Reload the dashboard. Notifications tab should now show:
- KPI cards
- Toolbar with instance/device/period filters and search
- "Operaciones por instancia" collapsible
- The new densely-styled table with `Validar` button on pending rows
- Click `Ver` opens the drawer; click `Validar` updates status without reload

- [ ] **Step 4: Commit**

```bash
git add apps/web-dashboard/src/pages/NotificationsPage.tsx
git commit -m "feat(dashboard): rewire NotificationsPage with new redesign"
```

---

## Phase 9 — Cleanup

### Task 22: Delete obsolete files

**Files:**
- Delete: `apps/web-dashboard/src/pages/NotificationDetailPage.tsx`
- Possibly delete: `apps/web-dashboard/src/components/Layout.tsx` (only if unused after task 9)
- Possibly delete: `apps/web-dashboard/src/components/NotificationCard/`, `apps/web-dashboard/src/components/NotificationList/` (if unused)

- [ ] **Step 1: Confirm no references remain**

Run: `cd apps/web-dashboard && grep -rn "NotificationDetailPage" src/`
Expected: only the file definition, no imports.

Run: `grep -rn "from '@/components/Layout'" src/ | grep -v "Layout/"`
If any results: those still use the old `Layout`. They must be migrated to `AppLayout` or have their imports cleaned.

Run: `grep -rn "NotificationCard\|NotificationList" src/`
If only the component definitions return, they are safe to delete.

- [ ] **Step 2: Delete confirmed-orphan files**

```bash
rm apps/web-dashboard/src/pages/NotificationDetailPage.tsx
# Only if grep above showed they are unused:
rm -rf apps/web-dashboard/src/components/NotificationCard apps/web-dashboard/src/components/NotificationList
# Only if Layout.tsx is unreferenced:
rm apps/web-dashboard/src/components/Layout.tsx
```

- [ ] **Step 3: Type-check + lint**

Run: `cd apps/web-dashboard && npm run type-check && npm run lint`
Expected: no new errors.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore(dashboard): remove obsolete components after redesign"
```

---

### Task 23: Manual smoke test

**Files:** none (manual)

- [ ] **Step 1: Run full backend test suite (super admin scope)**

Run: `cd infra/docker/environments/development && docker compose --env-file .env exec -T php-fpm php artisan test --filter='SuperAdmin|CommerceExpiry|SuspendExpired|NotificationTest'`
Expected: all green.

- [ ] **Step 2: Frontend type-check + lint**

Run: `cd apps/web-dashboard && npm run type-check && npm run lint`
Expected: no new errors.

- [ ] **Step 3: Manually verify in browser**

Login as a regular admin user (not super admin) at `http://localhost:3000`.

Confirm each:
- Sidebar dark with green active accent — visible on left
- Topbar shows user info on the right
- Notifications tab loads
- KPI cards show counts; clicking one filters the table
- Filters change the list and URL updates with query params
- Search input filters client-side
- "Operaciones por instancia" expands to show breakdown table
- A pending row shows `Validar` button — clicking it validates instantly with a toast and removes the button
- Clicking `Ver` opens the right drawer with full detail
- Drawer's `Validar` button works and closes the drawer
- Logout from sidebar works

- [ ] **Step 4: Commit any incidental fixes**

If anything failed during smoke test and you fixed it, commit those tweaks:

```bash
git add -A
git commit -m "fix(dashboard): minor tweaks from Fase 1 smoke test"
```

---

## Verification

After all tasks complete:

- [ ] Backend tests: `php artisan test --filter='SuperAdmin|CommerceExpiry|SuspendExpired|NotificationTest'` — green
- [ ] Frontend type-check: no new errors compared to baseline (StatCard.test.tsx and TabBadge.test.tsx pre-existing errors are OK)
- [ ] Frontend lint: no new errors
- [ ] Manual smoke test from Task 23 — all flows work
- [ ] `/notifications/:id` URLs no longer route — that bug is gone
- [ ] All commits atomic and follow the messages from each task
