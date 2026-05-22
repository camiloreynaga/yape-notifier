# Super Admin Panel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the super admin panel with commerce lifecycle management (auto-suspension after 3-day grace period, monthly renewal with manual payment confirmation) and plan editing.

**Architecture:** Phased implementation: backend foundation (migrations + models + tests) → backend services/endpoints (TDD) → auto-suspension job → frontend types/API/hooks → frontend Comercios tab → frontend Drawer/Modals → frontend Planes tab → wiring/integration. Each phase produces working software with green tests before moving on.

**Tech Stack:** Laravel 11 (PHP 8.2), PostgreSQL 15, PHPUnit. React 18 + TypeScript + Vite + Tailwind CSS, React Query (TanStack Query v5), Vitest + React Testing Library, lucide-react, date-fns.

**Source spec:** `docs/superpowers/specs/2026-04-28-super-admin-panel-design.md`

---

## File Structure

### Backend (Laravel)
- Create: `apps/api/database/migrations/2026_04_28_000001_add_phone_to_users_table.php`
- Create: `apps/api/database/migrations/2026_04_28_000002_create_commerce_renewals_table.php`
- Create: `apps/api/app/Models/CommerceRenewal.php`
- Create: `apps/api/database/factories/CommerceRenewalFactory.php`
- Modify: `apps/api/database/factories/CommerceFactory.php` (add status, plan_id, plan_expires_at)
- Modify: `apps/api/app/Models/Commerce.php` (add `renewals()` relationship + `expiryStatus()` accessor)
- Modify: `apps/api/app/Services/CommerceService.php` (modify `createCommerce` to set plan_expires_at trial, add `approve()`, `renew()`)
- Create: `apps/api/app/Http/Controllers/SuperAdmin/DashboardController.php` (KPIs endpoint)
- Modify: `apps/api/app/Http/Controllers/SuperAdmin/CommerceManagementController.php` (add `renew`, expand `index`/`show` payload)
- Modify: `apps/api/app/Http/Controllers/SuperAdmin/PlanController.php` (restrict `update` payload)
- Modify: `apps/api/routes/api.php` (add new routes)
- Create: `apps/api/app/Jobs/SuspendExpiredCommercesJob.php`
- Modify: `apps/api/routes/console.php` (schedule the job daily)
- Create: `apps/api/tests/Feature/SuperAdmin/CommerceManagementTest.php`
- Create: `apps/api/tests/Feature/SuperAdmin/PlanManagementTest.php`
- Create: `apps/api/tests/Feature/SuperAdmin/DashboardKpisTest.php`
- Create: `apps/api/tests/Unit/SuspendExpiredCommercesJobTest.php`

### Frontend (React)
- Modify: `apps/web-dashboard/src/types/index.ts` (add CommerceRenewal, Plan, ExpiryStatus, ExtendedCommerce)
- Modify: `apps/web-dashboard/src/config/api.ts` (add `admin` endpoints group)
- Create: `apps/web-dashboard/src/services/superAdminApi.ts`
- Create: `apps/web-dashboard/src/hooks/useSuperAdminCommerces.ts`
- Create: `apps/web-dashboard/src/hooks/useSuperAdminCommerce.ts`
- Create: `apps/web-dashboard/src/hooks/useSuperAdminKpis.ts`
- Create: `apps/web-dashboard/src/hooks/useSuperAdminPlans.ts`
- Create: `apps/web-dashboard/src/hooks/useRenewCommerce.ts`
- Create: `apps/web-dashboard/src/hooks/useApproveCommerce.ts`
- Create: `apps/web-dashboard/src/hooks/useChangePlan.ts`
- Create: `apps/web-dashboard/src/hooks/useSuspendCommerce.ts`
- Create: `apps/web-dashboard/src/hooks/useReactivateCommerce.ts`
- Create: `apps/web-dashboard/src/hooks/useUpdatePlan.ts`
- Create: `apps/web-dashboard/src/components/SuperAdmin/KpiCards.tsx`
- Create: `apps/web-dashboard/src/components/SuperAdmin/CommerceStatusBadge.tsx`
- Create: `apps/web-dashboard/src/components/SuperAdmin/CommercesTable.tsx`
- Create: `apps/web-dashboard/src/components/SuperAdmin/CommerceDetailDrawer.tsx`
- Create: `apps/web-dashboard/src/components/SuperAdmin/RenewCommerceModal.tsx`
- Create: `apps/web-dashboard/src/components/SuperAdmin/ApproveCommerceModal.tsx`
- Create: `apps/web-dashboard/src/components/SuperAdmin/ChangePlanModal.tsx`
- Create: `apps/web-dashboard/src/components/SuperAdmin/SuspendConfirmDialog.tsx`
- Create: `apps/web-dashboard/src/components/SuperAdmin/PlansTable.tsx`
- Create: `apps/web-dashboard/src/components/SuperAdmin/EditPlanModal.tsx`
- Create: `apps/web-dashboard/src/pages/SuperAdminCommercesTab.tsx`
- Create: `apps/web-dashboard/src/pages/SuperAdminPlansTab.tsx`
- Modify: `apps/web-dashboard/src/pages/SuperAdminPage.tsx` (replace placeholder with tabs)
- Modify: `apps/web-dashboard/src/App.tsx` (add nested routes for tabs)
- Modify: `apps/web-dashboard/src/components/Layout.tsx` (show "SUPER ADMIN" badge in header for super_admin)

---

## Phase 1 — Backend foundation: migrations + models

### Task 1: Migration — add `phone` to `users`

**Files:**
- Create: `apps/api/database/migrations/2026_04_28_000001_add_phone_to_users_table.php`

- [ ] **Step 1: Write the migration**

```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->string('phone', 20)->nullable()->after('email');
        });
    }

    public function down(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->dropColumn('phone');
        });
    }
};
```

- [ ] **Step 2: Run migration in dev**

Run: `cd infra/docker/environments/development && docker compose --env-file .env exec -T php-fpm php artisan migrate`

Expected: `Migrated: 2026_04_28_000001_add_phone_to_users_table` line.

- [ ] **Step 3: Add `phone` to User fillable**

Modify `apps/api/app/Models/User.php` `$fillable` array:

```php
protected $fillable = [
    'name',
    'email',
    'phone',
    'password',
    'pin',
    'commerce_id',
    'role',
    'is_active',
];
```

- [ ] **Step 4: Commit**

```bash
git add apps/api/database/migrations/2026_04_28_000001_add_phone_to_users_table.php apps/api/app/Models/User.php
git commit -m "feat(api): add phone column to users for owner contact"
```

---

### Task 2: Migration — create `commerce_renewals` table

**Files:**
- Create: `apps/api/database/migrations/2026_04_28_000002_create_commerce_renewals_table.php`

- [ ] **Step 1: Write the migration**

```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('commerce_renewals', function (Blueprint $table) {
            $table->id();
            $table->foreignId('commerce_id')->constrained('commerces')->cascadeOnDelete();
            $table->foreignId('plan_id')->constrained('plans');
            $table->foreignId('renewed_by_user_id')->constrained('users');
            $table->timestamp('previous_expires_at')->nullable();
            $table->timestamp('new_expires_at');
            $table->decimal('amount_paid', 10, 2)->nullable();
            $table->text('notes')->nullable();
            $table->timestamps();

            $table->index(['commerce_id', 'created_at']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('commerce_renewals');
    }
};
```

- [ ] **Step 2: Run migration**

Run: `docker compose --env-file .env exec -T php-fpm php artisan migrate`
Expected: success line for the new migration.

- [ ] **Step 3: Commit**

```bash
git add apps/api/database/migrations/2026_04_28_000002_create_commerce_renewals_table.php
git commit -m "feat(api): create commerce_renewals table for renewal history"
```

---

### Task 3: Model — `CommerceRenewal`

**Files:**
- Create: `apps/api/app/Models/CommerceRenewal.php`
- Modify: `apps/api/app/Models/Commerce.php` (add relationship)

- [ ] **Step 1: Create the model**

```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class CommerceRenewal extends Model
{
    use HasFactory;

    protected $fillable = [
        'commerce_id',
        'plan_id',
        'renewed_by_user_id',
        'previous_expires_at',
        'new_expires_at',
        'amount_paid',
        'notes',
    ];

    protected function casts(): array
    {
        return [
            'previous_expires_at' => 'datetime',
            'new_expires_at' => 'datetime',
            'amount_paid' => 'decimal:2',
        ];
    }

    public function commerce(): BelongsTo
    {
        return $this->belongsTo(Commerce::class);
    }

    public function plan(): BelongsTo
    {
        return $this->belongsTo(Plan::class);
    }

    public function renewedBy(): BelongsTo
    {
        return $this->belongsTo(User::class, 'renewed_by_user_id');
    }
}
```

- [ ] **Step 2: Add `renewals()` relationship to Commerce model**

In `apps/api/app/Models/Commerce.php`, after the existing relationships:

```php
public function renewals(): \Illuminate\Database\Eloquent\Relations\HasMany
{
    return $this->hasMany(CommerceRenewal::class)->orderBy('created_at', 'desc');
}
```

- [ ] **Step 3: Commit**

```bash
git add apps/api/app/Models/CommerceRenewal.php apps/api/app/Models/Commerce.php
git commit -m "feat(api): add CommerceRenewal model and relationship"
```

---

### Task 4: Factory — `CommerceRenewalFactory`

**Files:**
- Create: `apps/api/database/factories/CommerceRenewalFactory.php`

- [ ] **Step 1: Create the factory**

```php
<?php

namespace Database\Factories;

use App\Models\Commerce;
use App\Models\CommerceRenewal;
use App\Models\Plan;
use App\Models\User;
use Illuminate\Database\Eloquent\Factories\Factory;

class CommerceRenewalFactory extends Factory
{
    protected $model = CommerceRenewal::class;

    public function definition(): array
    {
        $previous = $this->faker->dateTimeBetween('-60 days', '-30 days');
        $new = (clone $previous)->modify('+30 days');

        return [
            'commerce_id' => Commerce::factory(),
            'plan_id' => Plan::factory(),
            'renewed_by_user_id' => User::factory(),
            'previous_expires_at' => $previous,
            'new_expires_at' => $new,
            'amount_paid' => $this->faker->randomFloat(2, 0, 299),
            'notes' => $this->faker->optional()->sentence(),
        ];
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/api/database/factories/CommerceRenewalFactory.php
git commit -m "test(api): add CommerceRenewal factory"
```

---

### Task 5: Update `CommerceFactory` for richer test data

**Files:**
- Modify: `apps/api/database/factories/CommerceFactory.php`

- [ ] **Step 1: Replace factory with state-aware version**

```php
<?php

namespace Database\Factories;

use App\Models\Commerce;
use App\Models\Plan;
use App\Models\User;
use Illuminate\Database\Eloquent\Factories\Factory;

class CommerceFactory extends Factory
{
    protected $model = Commerce::class;

    public function definition(): array
    {
        return [
            'name' => $this->faker->company(),
            'owner_user_id' => User::factory(),
            'status' => 'active',
            'plan_id' => Plan::factory(),
            'plan_expires_at' => now()->addDays(20),
        ];
    }

    public function pending(): self
    {
        return $this->state(fn () => [
            'status' => 'pending',
            'plan_id' => null,
            'plan_expires_at' => null,
        ]);
    }

    public function expiringSoon(): self
    {
        return $this->state(fn () => [
            'status' => 'active',
            'plan_expires_at' => now()->addDays(3),
        ]);
    }

    public function inGrace(): self
    {
        return $this->state(fn () => [
            'status' => 'active',
            'plan_expires_at' => now()->subDays(1),
        ]);
    }

    public function expiredPostGrace(): self
    {
        return $this->state(fn () => [
            'status' => 'active',
            'plan_expires_at' => now()->subDays(5),
        ]);
    }

    public function suspended(): self
    {
        return $this->state(fn () => [
            'status' => 'suspended',
            'plan_expires_at' => now()->subDays(10),
        ]);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/api/database/factories/CommerceFactory.php
git commit -m "test(api): add commerce factory states for lifecycle scenarios"
```

---

### Task 6: Add `expiryStatus()` accessor to Commerce

**Files:**
- Modify: `apps/api/app/Models/Commerce.php`

- [ ] **Step 1: Write the failing test**

Create `apps/api/tests/Unit/CommerceExpiryStatusTest.php`:

```php
<?php

namespace Tests\Unit;

use App\Models\Commerce;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class CommerceExpiryStatusTest extends TestCase
{
    use RefreshDatabase;

    public function test_pending_commerce_returns_pending_status(): void
    {
        $commerce = Commerce::factory()->pending()->create();
        $this->assertEquals('pending', $commerce->expiryStatus());
    }

    public function test_suspended_commerce_returns_suspended_status(): void
    {
        $commerce = Commerce::factory()->suspended()->create();
        $this->assertEquals('suspended', $commerce->expiryStatus());
    }

    public function test_active_with_more_than_7_days_returns_active(): void
    {
        $commerce = Commerce::factory()->create([
            'plan_expires_at' => now()->addDays(15),
        ]);
        $this->assertEquals('active', $commerce->expiryStatus());
    }

    public function test_active_with_less_than_7_days_returns_expiring_soon(): void
    {
        $commerce = Commerce::factory()->expiringSoon()->create();
        $this->assertEquals('expiring_soon', $commerce->expiryStatus());
    }

    public function test_active_past_expiry_within_grace_returns_in_grace(): void
    {
        $commerce = Commerce::factory()->inGrace()->create();
        $this->assertEquals('in_grace', $commerce->expiryStatus());
    }

    public function test_active_past_grace_returns_expired(): void
    {
        $commerce = Commerce::factory()->expiredPostGrace()->create();
        $this->assertEquals('expired', $commerce->expiryStatus());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=CommerceExpiryStatusTest`
Expected: FAIL with "Method expiryStatus does not exist".

- [ ] **Step 3: Implement `expiryStatus()`**

In `apps/api/app/Models/Commerce.php` add:

```php
public const GRACE_DAYS = 3;
public const EXPIRING_SOON_DAYS = 7;

public function expiryStatus(): string
{
    if ($this->status === 'pending') {
        return 'pending';
    }
    if ($this->status === 'suspended') {
        return 'suspended';
    }
    if (! $this->plan_expires_at) {
        return 'active';
    }

    $now = now();
    $expires = $this->plan_expires_at;
    $graceEnd = $expires->copy()->addDays(self::GRACE_DAYS);

    if ($now->greaterThan($graceEnd)) {
        return 'expired';
    }
    if ($now->greaterThan($expires)) {
        return 'in_grace';
    }
    if ($expires->diffInDays($now) < self::EXPIRING_SOON_DAYS) {
        return 'expiring_soon';
    }
    return 'active';
}

public function daysUntilExpiry(): ?int
{
    if (! $this->plan_expires_at) {
        return null;
    }
    return (int) round(now()->diffInDays($this->plan_expires_at, false));
}
```

- [ ] **Step 4: Run tests, expect green**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=CommerceExpiryStatusTest`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add apps/api/app/Models/Commerce.php apps/api/tests/Unit/CommerceExpiryStatusTest.php
git commit -m "feat(api): add expiryStatus and daysUntilExpiry to Commerce"
```

---

## Phase 2 — Backend services

### Task 7: `CommerceService::approve()` sets `plan_expires_at`

**Files:**
- Modify: `apps/api/app/Services/CommerceService.php`
- Modify: `apps/api/app/Http/Controllers/SuperAdmin/CommerceManagementController.php` (use the service)
- Create: `apps/api/tests/Feature/SuperAdmin/CommerceManagementTest.php`

- [ ] **Step 1: Write the failing test**

```php
<?php

namespace Tests\Feature\SuperAdmin;

use App\Models\Commerce;
use App\Models\Plan;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class CommerceManagementTest extends TestCase
{
    use RefreshDatabase;

    private function actingAsSuperAdmin(): User
    {
        $admin = User::factory()->create(['role' => 'super_admin', 'commerce_id' => null]);
        $this->actingAs($admin, 'sanctum');
        return $admin;
    }

    public function test_approve_sets_status_active_and_plan_expires_at_30_days_from_now(): void
    {
        $this->actingAsSuperAdmin();
        $commerce = Commerce::factory()->pending()->create();
        $plan = Plan::factory()->create(['slug' => 'basic']);

        $response = $this->patchJson("/api/admin/commerces/{$commerce->id}/approve", [
            'plan_slug' => 'basic',
        ]);

        $response->assertOk();
        $commerce->refresh();
        $this->assertEquals('active', $commerce->status);
        $this->assertEquals($plan->id, $commerce->plan_id);
        $this->assertNotNull($commerce->plan_expires_at);
        $this->assertEqualsWithDelta(
            now()->addDays(30)->timestamp,
            $commerce->plan_expires_at->timestamp,
            60
        );
    }
}
```

- [ ] **Step 2: Run, expect fail**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=test_approve_sets_status_active_and_plan_expires_at_30_days_from_now`
Expected: FAIL — `plan_expires_at` is null.

- [ ] **Step 3: Add `approve()` to CommerceService**

In `apps/api/app/Services/CommerceService.php`:

```php
public function approve(Commerce $commerce, Plan $plan, User $approvedBy): Commerce
{
    $commerce->update([
        'status'          => 'active',
        'plan_id'         => $plan->id,
        'plan_expires_at' => now()->addDays(30),
        'approved_at'     => now(),
        'approved_by'     => $approvedBy->id,
    ]);
    return $commerce->fresh();
}
```

- [ ] **Step 4: Refactor CommerceManagementController::approve to use service**

Replace the body of `approve()` in `CommerceManagementController.php`:

```php
public function approve(Request $request, int $id): JsonResponse
{
    $request->validate([
        'plan_slug' => 'required|string|exists:plans,slug',
    ]);

    $commerce = Commerce::findOrFail($id);

    if ($commerce->isActive()) {
        return response()->json([
            'message' => 'Commerce is already active.',
        ], 400);
    }

    $plan = Plan::where('slug', $request->plan_slug)->where('is_active', true)->firstOrFail();
    $commerce = app(\App\Services\CommerceService::class)->approve($commerce, $plan, $request->user());

    return response()->json([
        'message'  => 'Commerce approved successfully.',
        'commerce' => $commerce->load(['owner', 'plan', 'approvedBy']),
    ]);
}
```

- [ ] **Step 5: Run, expect pass**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=CommerceManagementTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add apps/api/app/Services/CommerceService.php apps/api/app/Http/Controllers/SuperAdmin/CommerceManagementController.php apps/api/tests/Feature/SuperAdmin/CommerceManagementTest.php
git commit -m "feat(api): approve sets plan_expires_at to now+30 days"
```

---

### Task 8: `CommerceService::renew()` + endpoint `PATCH /admin/commerces/{id}/renew`

**Files:**
- Modify: `apps/api/app/Services/CommerceService.php`
- Modify: `apps/api/app/Http/Controllers/SuperAdmin/CommerceManagementController.php`
- Modify: `apps/api/routes/api.php`
- Modify: `apps/api/tests/Feature/SuperAdmin/CommerceManagementTest.php`

- [ ] **Step 1: Write failing tests for the renew flow**

Append to `CommerceManagementTest.php`:

```php
public function test_renew_active_commerce_extends_30_days_from_current_expiry(): void
{
    $this->actingAsSuperAdmin();
    $expires = now()->addDays(10);
    $commerce = Commerce::factory()->create(['plan_expires_at' => $expires]);
    $plan = Plan::factory()->create(['slug' => 'basic']);

    $response = $this->patchJson("/api/admin/commerces/{$commerce->id}/renew", [
        'plan_slug' => 'basic',
        'amount_paid' => 49.00,
    ]);

    $response->assertOk();
    $commerce->refresh();
    $this->assertEqualsWithDelta(
        $expires->copy()->addDays(30)->timestamp,
        $commerce->plan_expires_at->timestamp,
        60
    );
    $this->assertEquals('active', $commerce->status);
    $this->assertEquals(1, $commerce->renewals()->count());
}

public function test_renew_suspended_commerce_extends_30_days_from_now_and_reactivates(): void
{
    $this->actingAsSuperAdmin();
    $commerce = Commerce::factory()->suspended()->create();
    $plan = Plan::factory()->create(['slug' => 'basic']);

    $response = $this->patchJson("/api/admin/commerces/{$commerce->id}/renew", [
        'plan_slug' => 'basic',
    ]);

    $response->assertOk();
    $commerce->refresh();
    $this->assertEquals('active', $commerce->status);
    $this->assertEqualsWithDelta(
        now()->addDays(30)->timestamp,
        $commerce->plan_expires_at->timestamp,
        60
    );
}

public function test_renew_records_history_with_renewer_and_amount(): void
{
    $admin = $this->actingAsSuperAdmin();
    $commerce = Commerce::factory()->create();
    Plan::factory()->create(['slug' => 'basic']);

    $this->patchJson("/api/admin/commerces/{$commerce->id}/renew", [
        'plan_slug' => 'basic',
        'amount_paid' => 49.00,
        'notes' => 'Yape Juan',
    ])->assertOk();

    $renewal = $commerce->renewals()->first();
    $this->assertEquals($admin->id, $renewal->renewed_by_user_id);
    $this->assertEquals('49.00', $renewal->amount_paid);
    $this->assertEquals('Yape Juan', $renewal->notes);
}

public function test_renew_pending_commerce_returns_400(): void
{
    $this->actingAsSuperAdmin();
    $commerce = Commerce::factory()->pending()->create();
    Plan::factory()->create(['slug' => 'basic']);

    $this->patchJson("/api/admin/commerces/{$commerce->id}/renew", [
        'plan_slug' => 'basic',
    ])->assertStatus(400);
}
```

- [ ] **Step 2: Run, expect fail**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=CommerceManagementTest`
Expected: 4 new tests fail (404 for the route).

- [ ] **Step 3: Add `renew()` method to CommerceService**

In `apps/api/app/Services/CommerceService.php`:

```php
public function renew(
    Commerce $commerce,
    Plan $plan,
    User $renewedBy,
    ?float $amountPaid = null,
    ?string $notes = null
): Commerce {
    $previousExpiresAt = $commerce->plan_expires_at;

    if ($commerce->status === 'suspended' || ! $previousExpiresAt || $previousExpiresAt->isPast()) {
        $newExpiresAt = now()->addDays(30);
    } else {
        $newExpiresAt = $previousExpiresAt->copy()->addDays(30);
    }

    \DB::transaction(function () use ($commerce, $plan, $renewedBy, $previousExpiresAt, $newExpiresAt, $amountPaid, $notes) {
        $commerce->update([
            'status'          => 'active',
            'plan_id'         => $plan->id,
            'plan_expires_at' => $newExpiresAt,
        ]);

        \App\Models\CommerceRenewal::create([
            'commerce_id'        => $commerce->id,
            'plan_id'            => $plan->id,
            'renewed_by_user_id' => $renewedBy->id,
            'previous_expires_at'=> $previousExpiresAt,
            'new_expires_at'     => $newExpiresAt,
            'amount_paid'        => $amountPaid,
            'notes'              => $notes,
        ]);
    });

    return $commerce->fresh();
}
```

- [ ] **Step 4: Add `renew()` controller method**

In `apps/api/app/Http/Controllers/SuperAdmin/CommerceManagementController.php`:

```php
public function renew(Request $request, int $id): JsonResponse
{
    $request->validate([
        'plan_slug'   => 'required|string|exists:plans,slug',
        'amount_paid' => 'nullable|numeric|min:0',
        'notes'       => 'nullable|string|max:1000',
    ]);

    $commerce = Commerce::findOrFail($id);

    if ($commerce->status === 'pending') {
        return response()->json([
            'message' => 'Cannot renew a pending commerce. Approve it first.',
        ], 400);
    }

    $plan = Plan::where('slug', $request->plan_slug)->where('is_active', true)->firstOrFail();

    $commerce = app(\App\Services\CommerceService::class)->renew(
        $commerce,
        $plan,
        $request->user(),
        $request->input('amount_paid'),
        $request->input('notes')
    );

    return response()->json([
        'message'  => 'Commerce renewed successfully.',
        'commerce' => $commerce->load(['owner', 'plan', 'renewals.plan', 'renewals.renewedBy']),
    ]);
}
```

- [ ] **Step 5: Register route**

In `apps/api/routes/api.php`, inside the `Route::middleware(['auth:sanctum', 'super_admin'])->prefix('admin')->group(...)`, add after the existing commerce routes:

```php
Route::patch('/commerces/{id}/renew', [CommerceManagementController::class, 'renew']);
```

- [ ] **Step 6: Run, expect pass**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=CommerceManagementTest`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add apps/api/app/Services/CommerceService.php apps/api/app/Http/Controllers/SuperAdmin/CommerceManagementController.php apps/api/routes/api.php apps/api/tests/Feature/SuperAdmin/CommerceManagementTest.php
git commit -m "feat(api): add renew endpoint with renewal history"
```

---

### Task 9: Expand `index` payload with computed fields

**Files:**
- Modify: `apps/api/app/Http/Controllers/SuperAdmin/CommerceManagementController.php`
- Modify: `apps/api/tests/Feature/SuperAdmin/CommerceManagementTest.php`

- [ ] **Step 1: Write failing test**

Append:

```php
public function test_index_includes_expiry_status_and_days_until_expiry(): void
{
    $this->actingAsSuperAdmin();
    Commerce::factory()->expiringSoon()->create(['name' => 'Café Luna']);

    $response = $this->getJson('/api/admin/commerces');

    $response->assertOk()
        ->assertJsonPath('data.0.name', 'Café Luna')
        ->assertJsonPath('data.0.expiry_status', 'expiring_soon')
        ->assertJsonStructure(['data' => [['days_until_expiry', 'captadores_count']]]);
}

public function test_index_filters_by_status(): void
{
    $this->actingAsSuperAdmin();
    Commerce::factory()->pending()->create(['name' => 'Pendiente Co']);
    Commerce::factory()->create(['name' => 'Activo Co']);

    $response = $this->getJson('/api/admin/commerces?status=pending');

    $response->assertOk()
        ->assertJsonCount(1, 'data')
        ->assertJsonPath('data.0.name', 'Pendiente Co');
}
```

- [ ] **Step 2: Run, expect fail**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=test_index_includes_expiry_status_and_days_until_expiry`
Expected: FAIL — `expiry_status` not in JSON.

- [ ] **Step 3: Modify `index()` to compute fields**

In `CommerceManagementController.php`, replace `index()`:

```php
public function index(Request $request): JsonResponse
{
    $query = Commerce::with(['owner', 'plan', 'approvedBy'])
        ->withCount([
            'devices',
            'users',
            'notifications',
            'users as captadores_count' => function ($q) {
                $q->where('role', 'captador');
            },
        ]);

    if ($request->filled('status')) {
        $query->where('status', $request->status);
    }
    if ($request->filled('q')) {
        $term = '%' . $request->q . '%';
        $query->where(function ($q) use ($term) {
            $q->where('name', 'ILIKE', $term)
              ->orWhereHas('owner', fn ($qq) => $qq->where('email', 'ILIKE', $term));
        });
    }

    $commerces = $query->orderBy('created_at', 'desc')->paginate(20);

    $commerces->getCollection()->transform(function ($commerce) {
        $commerce->expiry_status = $commerce->expiryStatus();
        $commerce->days_until_expiry = $commerce->daysUntilExpiry();
        return $commerce;
    });

    return response()->json($commerces);
}
```

- [ ] **Step 4: Run, expect pass**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=CommerceManagementTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add apps/api/app/Http/Controllers/SuperAdmin/CommerceManagementController.php apps/api/tests/Feature/SuperAdmin/CommerceManagementTest.php
git commit -m "feat(api): include expiry_status and captadores_count in commerce list"
```

---

### Task 10: Expand `show` payload with renewals + captadores

**Files:**
- Modify: `apps/api/app/Http/Controllers/SuperAdmin/CommerceManagementController.php`
- Modify: `apps/api/tests/Feature/SuperAdmin/CommerceManagementTest.php`

- [ ] **Step 1: Write failing test**

Append:

```php
public function test_show_includes_renewals_and_captadores(): void
{
    $this->actingAsSuperAdmin();
    $commerce = Commerce::factory()->create();
    User::factory()->create(['commerce_id' => $commerce->id, 'role' => 'captador', 'name' => 'María']);
    User::factory()->create(['commerce_id' => $commerce->id, 'role' => 'captador', 'name' => 'Pedro']);
    \App\Models\CommerceRenewal::factory()->create(['commerce_id' => $commerce->id]);

    $response = $this->getJson("/api/admin/commerces/{$commerce->id}");

    $response->assertOk()
        ->assertJsonPath('commerce.captadores.0.name', 'María')
        ->assertJsonCount(2, 'commerce.captadores')
        ->assertJsonCount(1, 'commerce.renewals')
        ->assertJsonPath('commerce.expiry_status', $commerce->expiryStatus());
}
```

- [ ] **Step 2: Run, expect fail**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=test_show_includes_renewals_and_captadores`
Expected: FAIL.

- [ ] **Step 3: Modify `show()`**

In `CommerceManagementController.php`, replace `show()`:

```php
public function show(int $id): JsonResponse
{
    $commerce = Commerce::with([
            'owner',
            'plan',
            'approvedBy',
            'devices',
            'renewals.plan',
            'renewals.renewedBy',
        ])
        ->withCount(['notifications'])
        ->findOrFail($id);

    $captadores = $commerce->users()
        ->where('role', 'captador')
        ->orderBy('name')
        ->get(['id', 'name', 'pin']);

    $commerce->captadores = $captadores;
    $commerce->expiry_status = $commerce->expiryStatus();
    $commerce->days_until_expiry = $commerce->daysUntilExpiry();

    return response()->json(['commerce' => $commerce]);
}
```

- [ ] **Step 4: Run, expect pass**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=CommerceManagementTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add apps/api/app/Http/Controllers/SuperAdmin/CommerceManagementController.php apps/api/tests/Feature/SuperAdmin/CommerceManagementTest.php
git commit -m "feat(api): expand commerce detail with renewals and captadores"
```

---

### Task 11: KPI dashboard endpoint

**Files:**
- Create: `apps/api/app/Http/Controllers/SuperAdmin/DashboardController.php`
- Modify: `apps/api/routes/api.php`
- Create: `apps/api/tests/Feature/SuperAdmin/DashboardKpisTest.php`

- [ ] **Step 1: Write failing test**

```php
<?php

namespace Tests\Feature\SuperAdmin;

use App\Models\Commerce;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class DashboardKpisTest extends TestCase
{
    use RefreshDatabase;

    public function test_returns_kpi_counts_by_lifecycle_state(): void
    {
        $admin = User::factory()->create(['role' => 'super_admin', 'commerce_id' => null]);
        $this->actingAs($admin, 'sanctum');

        Commerce::factory()->pending()->count(2)->create();
        Commerce::factory()->expiringSoon()->count(3)->create();
        Commerce::factory()->inGrace()->count(1)->create();
        Commerce::factory()->expiredPostGrace()->count(1)->create();
        Commerce::factory()->suspended()->count(2)->create();
        Commerce::factory()->count(4)->create(); // active default

        $response = $this->getJson('/api/admin/dashboard/kpis');

        $response->assertOk()
            ->assertJson([
                'total'          => 13,
                'pending'        => 2,
                'active'         => 4,
                'expiring_soon'  => 3,
                'in_grace'       => 1,
                'expired'        => 1,
                'suspended'      => 2,
            ]);
    }
}
```

- [ ] **Step 2: Run, expect fail**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=DashboardKpisTest`
Expected: FAIL — 404.

- [ ] **Step 3: Create controller**

```php
<?php

namespace App\Http\Controllers\SuperAdmin;

use App\Http\Controllers\Controller;
use App\Models\Commerce;
use Illuminate\Http\JsonResponse;

class DashboardController extends Controller
{
    public function kpis(): JsonResponse
    {
        $all = Commerce::all();
        $buckets = [
            'pending' => 0, 'active' => 0, 'expiring_soon' => 0,
            'in_grace' => 0, 'expired' => 0, 'suspended' => 0,
        ];
        foreach ($all as $commerce) {
            $status = $commerce->expiryStatus();
            if (isset($buckets[$status])) {
                $buckets[$status]++;
            }
        }
        return response()->json(array_merge(['total' => $all->count()], $buckets));
    }
}
```

- [ ] **Step 4: Register route**

In `apps/api/routes/api.php`, inside the super_admin prefix group, add:

```php
use App\Http\Controllers\SuperAdmin\DashboardController;

Route::get('/dashboard/kpis', [DashboardController::class, 'kpis']);
```

(Add the `use` statement at the top of the file if not already present.)

- [ ] **Step 5: Run, expect pass**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=DashboardKpisTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add apps/api/app/Http/Controllers/SuperAdmin/DashboardController.php apps/api/routes/api.php apps/api/tests/Feature/SuperAdmin/DashboardKpisTest.php
git commit -m "feat(api): add super admin KPIs endpoint"
```

---

### Task 12: Restrict `PlanController::update` payload

**Files:**
- Modify: `apps/api/app/Http/Controllers/SuperAdmin/PlanController.php`
- Create: `apps/api/tests/Feature/SuperAdmin/PlanManagementTest.php`

- [ ] **Step 1: Write failing test**

```php
<?php

namespace Tests\Feature\SuperAdmin;

use App\Models\Plan;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class PlanManagementTest extends TestCase
{
    use RefreshDatabase;

    private function actingAsSuperAdmin(): void
    {
        $admin = User::factory()->create(['role' => 'super_admin', 'commerce_id' => null]);
        $this->actingAs($admin, 'sanctum');
    }

    public function test_update_can_change_price_and_limits_and_active_flag(): void
    {
        $this->actingAsSuperAdmin();
        $plan = Plan::factory()->create([
            'slug' => 'basic',
            'price' => 49,
            'max_devices' => 3,
        ]);

        $response = $this->patchJson("/api/admin/plans/{$plan->id}", [
            'price' => 59,
            'max_devices' => 5,
            'is_active' => false,
        ]);

        $response->assertOk();
        $plan->refresh();
        $this->assertEquals(59, $plan->price);
        $this->assertEquals(5, $plan->max_devices);
        $this->assertFalse($plan->is_active);
    }

    public function test_update_ignores_slug_changes(): void
    {
        $this->actingAsSuperAdmin();
        $plan = Plan::factory()->create(['slug' => 'basic']);

        $this->patchJson("/api/admin/plans/{$plan->id}", [
            'slug' => 'something-else',
        ]);

        $plan->refresh();
        $this->assertEquals('basic', $plan->slug);
    }
}
```

- [ ] **Step 2: Run, expect fail (test 2)**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=PlanManagementTest`
Expected: `test_update_ignores_slug_changes` fails (slug got changed).

- [ ] **Step 3: Modify `update()` in PlanController**

Replace `update()`:

```php
public function update(Request $request, int $id): JsonResponse
{
    $plan = Plan::findOrFail($id);

    $validated = $request->validate([
        'name'                      => 'sometimes|string|max:255',
        'max_devices'               => 'nullable|integer|min:1',
        'max_notifications_per_day' => 'nullable|integer|min:1',
        'price'                     => 'sometimes|numeric|min:0',
        'is_active'                 => 'sometimes|boolean',
    ]);

    $plan->update($validated);

    return response()->json(['plan' => $plan->fresh()]);
}
```

- [ ] **Step 4: Run, expect pass**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=PlanManagementTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add apps/api/app/Http/Controllers/SuperAdmin/PlanController.php apps/api/tests/Feature/SuperAdmin/PlanManagementTest.php
git commit -m "feat(api): restrict plan update payload (no slug change)"
```

---

## Phase 3 — Auto-suspension job

### Task 13: `SuspendExpiredCommercesJob`

**Files:**
- Create: `apps/api/app/Jobs/SuspendExpiredCommercesJob.php`
- Create: `apps/api/tests/Unit/SuspendExpiredCommercesJobTest.php`

- [ ] **Step 1: Write failing test**

```php
<?php

namespace Tests\Unit;

use App\Jobs\SuspendExpiredCommercesJob;
use App\Models\Commerce;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class SuspendExpiredCommercesJobTest extends TestCase
{
    use RefreshDatabase;

    public function test_suspends_active_commerces_past_grace_period(): void
    {
        $expired = Commerce::factory()->expiredPostGrace()->create();
        $inGrace = Commerce::factory()->inGrace()->create();
        $active = Commerce::factory()->create();

        (new SuspendExpiredCommercesJob())->handle();

        $this->assertEquals('suspended', $expired->fresh()->status);
        $this->assertEquals('active', $inGrace->fresh()->status);
        $this->assertEquals('active', $active->fresh()->status);
    }
}
```

- [ ] **Step 2: Run, expect fail**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=SuspendExpiredCommercesJobTest`
Expected: FAIL — class missing.

- [ ] **Step 3: Create the job**

```php
<?php

namespace App\Jobs;

use App\Models\Commerce;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Queue\SerializesModels;
use Illuminate\Support\Facades\Log;

class SuspendExpiredCommercesJob implements ShouldQueue
{
    use Dispatchable, InteractsWithQueue, Queueable, SerializesModels;

    public function handle(): void
    {
        $cutoff = now()->subDays(Commerce::GRACE_DAYS);

        Commerce::where('status', 'active')
            ->whereNotNull('plan_expires_at')
            ->where('plan_expires_at', '<', $cutoff)
            ->each(function (Commerce $commerce) {
                $commerce->update(['status' => 'suspended']);
                Log::info('Auto-suspended commerce after grace period', [
                    'commerce_id' => $commerce->id,
                    'name'        => $commerce->name,
                    'expired_at'  => $commerce->plan_expires_at?->toDateTimeString(),
                ]);
            });
    }
}
```

- [ ] **Step 4: Run, expect pass**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=SuspendExpiredCommercesJobTest`
Expected: PASS.

- [ ] **Step 5: Schedule the job**

In `apps/api/routes/console.php` add at the end:

```php
use App\Jobs\SuspendExpiredCommercesJob;
use Illuminate\Support\Facades\Schedule;

Schedule::job(new SuspendExpiredCommercesJob())->dailyAt('02:00')->name('suspend-expired-commerces');
```

(If `routes/console.php` is empty, add the `<?php` opening tag and these lines.)

- [ ] **Step 6: Commit**

```bash
git add apps/api/app/Jobs/SuspendExpiredCommercesJob.php apps/api/tests/Unit/SuspendExpiredCommercesJobTest.php apps/api/routes/console.php
git commit -m "feat(api): auto-suspend commerces past 3-day grace period"
```

---

## Phase 4 — Frontend foundation: types, API client, hooks

### Task 14: Type definitions

**Files:**
- Modify: `apps/web-dashboard/src/types/index.ts`

- [ ] **Step 1: Add types**

Append to `types/index.ts`:

```typescript
export type ExpiryStatus =
  | 'pending'
  | 'active'
  | 'expiring_soon'
  | 'in_grace'
  | 'expired'
  | 'suspended';

export interface Plan {
  id: number;
  name: string;
  slug: string;
  max_devices: number | null;
  max_notifications_per_day: number | null;
  price: number;
  is_active: boolean;
  commerces_count?: number;
}

export interface CommerceRenewal {
  id: number;
  commerce_id: number;
  plan_id: number;
  renewed_by_user_id: number;
  previous_expires_at: string | null;
  new_expires_at: string;
  amount_paid: number | null;
  notes: string | null;
  created_at: string;
  plan?: Plan;
  renewedBy?: { id: number; name: string };
}

export interface CommerceListItem {
  id: number;
  name: string;
  status: 'pending' | 'active' | 'suspended';
  plan_id: number | null;
  plan_expires_at: string | null;
  expiry_status: ExpiryStatus;
  days_until_expiry: number | null;
  captadores_count: number;
  devices_count: number;
  notifications_count: number;
  owner?: { id: number; name: string; email: string; phone: string | null };
  plan?: Plan;
  approved_at: string | null;
  created_at: string;
}

export interface CommerceDetail extends CommerceListItem {
  captadores: Array<{ id: number; name: string; pin: string | null }>;
  renewals: CommerceRenewal[];
}

export interface SuperAdminKpis {
  total: number;
  pending: number;
  active: number;
  expiring_soon: number;
  in_grace: number;
  expired: number;
  suspended: number;
}
```

- [ ] **Step 2: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add apps/web-dashboard/src/types/index.ts
git commit -m "feat(dashboard): add super admin types"
```

---

### Task 15: API endpoints config

**Files:**
- Modify: `apps/web-dashboard/src/config/api.ts`

- [ ] **Step 1: Add admin endpoints**

In `apps/web-dashboard/src/config/api.ts`, before the closing `} as const;`:

```typescript
  admin: {
    commerces: '/api/admin/commerces',
    commerce: (id: number) => `/api/admin/commerces/${id}`,
    approve: (id: number) => `/api/admin/commerces/${id}/approve`,
    suspend: (id: number) => `/api/admin/commerces/${id}/suspend`,
    reactivate: (id: number) => `/api/admin/commerces/${id}/reactivate`,
    changePlan: (id: number) => `/api/admin/commerces/${id}/plan`,
    renew: (id: number) => `/api/admin/commerces/${id}/renew`,
    plans: '/api/admin/plans',
    plan: (id: number) => `/api/admin/plans/${id}`,
    dashboardKpis: '/api/admin/dashboard/kpis',
  },
```

- [ ] **Step 2: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add apps/web-dashboard/src/config/api.ts
git commit -m "feat(dashboard): add super admin API endpoints config"
```

---

### Task 16: Super admin API client service

**Files:**
- Create: `apps/web-dashboard/src/services/superAdminApi.ts`

- [ ] **Step 1: Create the service**

```typescript
import axios, { AxiosInstance } from 'axios';
import { API_BASE_URL, API_ENDPOINTS } from '@/config/api';
import type {
  CommerceListItem,
  CommerceDetail,
  Plan,
  SuperAdminKpis,
} from '@/types';

interface PaginatedResponse<T> {
  data: T[];
  current_page: number;
  last_page: number;
  per_page: number;
  total: number;
}

class SuperAdminApiService {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
      timeout: 30000,
    });
    this.client.interceptors.request.use((config) => {
      const token = localStorage.getItem('auth_token');
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    });
  }

  async listCommerces(params: { status?: string; q?: string; page?: number } = {}): Promise<PaginatedResponse<CommerceListItem>> {
    const { data } = await this.client.get<PaginatedResponse<CommerceListItem>>(
      API_ENDPOINTS.admin.commerces, { params }
    );
    return data;
  }

  async getCommerce(id: number): Promise<CommerceDetail> {
    const { data } = await this.client.get<{ commerce: CommerceDetail }>(API_ENDPOINTS.admin.commerce(id));
    return data.commerce;
  }

  async approveCommerce(id: number, planSlug: string): Promise<CommerceDetail> {
    const { data } = await this.client.patch<{ commerce: CommerceDetail }>(
      API_ENDPOINTS.admin.approve(id), { plan_slug: planSlug }
    );
    return data.commerce;
  }

  async renewCommerce(
    id: number,
    payload: { plan_slug: string; amount_paid?: number; notes?: string }
  ): Promise<CommerceDetail> {
    const { data } = await this.client.patch<{ commerce: CommerceDetail }>(
      API_ENDPOINTS.admin.renew(id), payload
    );
    return data.commerce;
  }

  async changePlan(id: number, planSlug: string): Promise<CommerceDetail> {
    const { data } = await this.client.patch<{ commerce: CommerceDetail }>(
      API_ENDPOINTS.admin.changePlan(id), { plan_slug: planSlug }
    );
    return data.commerce;
  }

  async suspendCommerce(id: number): Promise<CommerceDetail> {
    const { data } = await this.client.patch<{ commerce: CommerceDetail }>(API_ENDPOINTS.admin.suspend(id));
    return data.commerce;
  }

  async reactivateCommerce(id: number): Promise<CommerceDetail> {
    const { data } = await this.client.patch<{ commerce: CommerceDetail }>(API_ENDPOINTS.admin.reactivate(id));
    return data.commerce;
  }

  async getKpis(): Promise<SuperAdminKpis> {
    const { data } = await this.client.get<SuperAdminKpis>(API_ENDPOINTS.admin.dashboardKpis);
    return data;
  }

  async listPlans(): Promise<Plan[]> {
    const { data } = await this.client.get<{ plans: Plan[] }>(API_ENDPOINTS.admin.plans);
    return data.plans;
  }

  async updatePlan(
    id: number,
    payload: Partial<Pick<Plan, 'name' | 'max_devices' | 'max_notifications_per_day' | 'price' | 'is_active'>>
  ): Promise<Plan> {
    const { data } = await this.client.patch<{ plan: Plan }>(API_ENDPOINTS.admin.plan(id), payload);
    return data.plan;
  }
}

export const superAdminApi = new SuperAdminApiService();
```

- [ ] **Step 2: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add apps/web-dashboard/src/services/superAdminApi.ts
git commit -m "feat(dashboard): add superAdminApi client"
```

---

### Task 17: React Query hooks (queries)

**Files:**
- Create: `apps/web-dashboard/src/hooks/useSuperAdminCommerces.ts`
- Create: `apps/web-dashboard/src/hooks/useSuperAdminCommerce.ts`
- Create: `apps/web-dashboard/src/hooks/useSuperAdminKpis.ts`
- Create: `apps/web-dashboard/src/hooks/useSuperAdminPlans.ts`

- [ ] **Step 1: Create `useSuperAdminCommerces`**

```typescript
import { useQuery } from '@tanstack/react-query';
import { superAdminApi } from '@/services/superAdminApi';

export interface CommercesQueryParams {
  status?: string;
  q?: string;
  page?: number;
}

export function useSuperAdminCommerces(params: CommercesQueryParams) {
  return useQuery({
    queryKey: ['superAdmin', 'commerces', params],
    queryFn: () => superAdminApi.listCommerces(params),
    staleTime: 30_000,
  });
}
```

- [ ] **Step 2: Create `useSuperAdminCommerce`**

```typescript
import { useQuery } from '@tanstack/react-query';
import { superAdminApi } from '@/services/superAdminApi';

export function useSuperAdminCommerce(id: number | null) {
  return useQuery({
    queryKey: ['superAdmin', 'commerce', id],
    queryFn: () => superAdminApi.getCommerce(id as number),
    enabled: id !== null,
    staleTime: 30_000,
  });
}
```

- [ ] **Step 3: Create `useSuperAdminKpis`**

```typescript
import { useQuery } from '@tanstack/react-query';
import { superAdminApi } from '@/services/superAdminApi';

export function useSuperAdminKpis() {
  return useQuery({
    queryKey: ['superAdmin', 'kpis'],
    queryFn: () => superAdminApi.getKpis(),
    staleTime: 30_000,
  });
}
```

- [ ] **Step 4: Create `useSuperAdminPlans`**

```typescript
import { useQuery } from '@tanstack/react-query';
import { superAdminApi } from '@/services/superAdminApi';

export function useSuperAdminPlans() {
  return useQuery({
    queryKey: ['superAdmin', 'plans'],
    queryFn: () => superAdminApi.listPlans(),
    staleTime: 60_000,
  });
}
```

- [ ] **Step 5: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no errors.

- [ ] **Step 6: Commit**

```bash
git add apps/web-dashboard/src/hooks/useSuperAdmin*.ts
git commit -m "feat(dashboard): add super admin query hooks"
```

---

### Task 18: React Query mutation hooks

**Files:**
- Create: `apps/web-dashboard/src/hooks/useRenewCommerce.ts`
- Create: `apps/web-dashboard/src/hooks/useApproveCommerce.ts`
- Create: `apps/web-dashboard/src/hooks/useChangePlan.ts`
- Create: `apps/web-dashboard/src/hooks/useSuspendCommerce.ts`
- Create: `apps/web-dashboard/src/hooks/useReactivateCommerce.ts`
- Create: `apps/web-dashboard/src/hooks/useUpdatePlan.ts`

- [ ] **Step 1: Create `useRenewCommerce`**

```typescript
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { superAdminApi } from '@/services/superAdminApi';

export function useRenewCommerce() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, plan_slug, amount_paid, notes }: {
      id: number; plan_slug: string; amount_paid?: number; notes?: string;
    }) => superAdminApi.renewCommerce(id, { plan_slug, amount_paid, notes }),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['superAdmin', 'commerces'] });
      qc.invalidateQueries({ queryKey: ['superAdmin', 'commerce', vars.id] });
      qc.invalidateQueries({ queryKey: ['superAdmin', 'kpis'] });
    },
  });
}
```

- [ ] **Step 2: Create `useApproveCommerce`**

```typescript
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { superAdminApi } from '@/services/superAdminApi';

export function useApproveCommerce() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, plan_slug }: { id: number; plan_slug: string }) =>
      superAdminApi.approveCommerce(id, plan_slug),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['superAdmin', 'commerces'] });
      qc.invalidateQueries({ queryKey: ['superAdmin', 'commerce', vars.id] });
      qc.invalidateQueries({ queryKey: ['superAdmin', 'kpis'] });
    },
  });
}
```

- [ ] **Step 3: Create `useChangePlan`**

```typescript
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { superAdminApi } from '@/services/superAdminApi';

export function useChangePlan() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, plan_slug }: { id: number; plan_slug: string }) =>
      superAdminApi.changePlan(id, plan_slug),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['superAdmin', 'commerces'] });
      qc.invalidateQueries({ queryKey: ['superAdmin', 'commerce', vars.id] });
    },
  });
}
```

- [ ] **Step 4: Create `useSuspendCommerce`**

```typescript
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { superAdminApi } from '@/services/superAdminApi';

export function useSuspendCommerce() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => superAdminApi.suspendCommerce(id),
    onSuccess: (_, id) => {
      qc.invalidateQueries({ queryKey: ['superAdmin', 'commerces'] });
      qc.invalidateQueries({ queryKey: ['superAdmin', 'commerce', id] });
      qc.invalidateQueries({ queryKey: ['superAdmin', 'kpis'] });
    },
  });
}
```

- [ ] **Step 5: Create `useReactivateCommerce`**

```typescript
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { superAdminApi } from '@/services/superAdminApi';

export function useReactivateCommerce() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => superAdminApi.reactivateCommerce(id),
    onSuccess: (_, id) => {
      qc.invalidateQueries({ queryKey: ['superAdmin', 'commerces'] });
      qc.invalidateQueries({ queryKey: ['superAdmin', 'commerce', id] });
      qc.invalidateQueries({ queryKey: ['superAdmin', 'kpis'] });
    },
  });
}
```

- [ ] **Step 6: Create `useUpdatePlan`**

```typescript
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { superAdminApi } from '@/services/superAdminApi';
import type { Plan } from '@/types';

export function useUpdatePlan() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, ...payload }: {
      id: number;
    } & Partial<Pick<Plan, 'name' | 'max_devices' | 'max_notifications_per_day' | 'price' | 'is_active'>>) =>
      superAdminApi.updatePlan(id, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['superAdmin', 'plans'] });
    },
  });
}
```

- [ ] **Step 7: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no errors.

- [ ] **Step 8: Commit**

```bash
git add apps/web-dashboard/src/hooks/use*.ts
git commit -m "feat(dashboard): add super admin mutation hooks"
```

---

## Phase 5 — Frontend: Comercios tab (read views)

### Task 19: `CommerceStatusBadge` component

**Files:**
- Create: `apps/web-dashboard/src/components/SuperAdmin/CommerceStatusBadge.tsx`

- [ ] **Step 1: Create the component**

```tsx
import type { ExpiryStatus } from '@/types';

const STATUS_CONFIG: Record<ExpiryStatus, { label: string; classes: string; dot: string }> = {
  pending:       { label: 'Pendiente',  classes: 'bg-yellow-50 text-yellow-800 ring-yellow-600/20',   dot: 'bg-yellow-500' },
  active:        { label: 'Activo',     classes: 'bg-green-50 text-green-700 ring-green-600/20',      dot: 'bg-green-500'  },
  expiring_soon: { label: 'Por vencer', classes: 'bg-orange-50 text-orange-800 ring-orange-600/20',   dot: 'bg-orange-500' },
  in_grace:      { label: 'En gracia',  classes: 'bg-rose-50 text-rose-800 ring-rose-600/20',         dot: 'bg-rose-500'   },
  expired:       { label: 'Vencido',    classes: 'bg-red-50 text-red-800 ring-red-600/20',            dot: 'bg-red-600'    },
  suspended:     { label: 'Suspendido', classes: 'bg-gray-100 text-gray-700 ring-gray-500/20',        dot: 'bg-gray-500'   },
};

export default function CommerceStatusBadge({ status }: { status: ExpiryStatus }) {
  const cfg = STATUS_CONFIG[status];
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ring-1 ring-inset ${cfg.classes}`}>
      <span className={`h-1.5 w-1.5 rounded-full ${cfg.dot}`} />
      {cfg.label}
    </span>
  );
}
```

- [ ] **Step 2: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add apps/web-dashboard/src/components/SuperAdmin/CommerceStatusBadge.tsx
git commit -m "feat(dashboard): add CommerceStatusBadge"
```

---

### Task 20: `KpiCards` component

**Files:**
- Create: `apps/web-dashboard/src/components/SuperAdmin/KpiCards.tsx`

- [ ] **Step 1: Create the component**

```tsx
import { Building2, Clock, AlertTriangle, Ban } from 'lucide-react';
import { useSuperAdminKpis } from '@/hooks/useSuperAdminKpis';

interface KpiCardsProps {
  activeFilter: string | null;
  onFilterChange: (filter: string | null) => void;
}

interface CardConfig {
  key: string;
  label: string;
  count: number;
  icon: React.ComponentType<{ className?: string }>;
  bg: string;
  fg: string;
  ring: string;
}

export default function KpiCards({ activeFilter, onFilterChange }: KpiCardsProps) {
  const { data, isLoading } = useSuperAdminKpis();

  if (isLoading || !data) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className="h-24 rounded-xl bg-gray-100 animate-pulse" />
        ))}
      </div>
    );
  }

  const cards: CardConfig[] = [
    { key: 'all',     label: 'Total',       count: data.total,                          icon: Building2,     bg: 'bg-gray-50',    fg: 'text-gray-700',    ring: 'ring-gray-200' },
    { key: 'pending', label: 'Pendientes',  count: data.pending,                        icon: Clock,         bg: 'bg-yellow-50',  fg: 'text-yellow-800',  ring: 'ring-yellow-200' },
    { key: 'expiring',label: 'Por vencer',  count: data.expiring_soon + data.in_grace,  icon: AlertTriangle, bg: 'bg-orange-50',  fg: 'text-orange-800',  ring: 'ring-orange-200' },
    { key: 'suspended',label: 'Suspendidos',count: data.suspended,                      icon: Ban,           bg: 'bg-red-50',     fg: 'text-red-800',     ring: 'ring-red-200' },
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map((card) => {
        const isActive = activeFilter === card.key || (activeFilter === null && card.key === 'all');
        const Icon = card.icon;
        return (
          <button
            key={card.key}
            onClick={() => onFilterChange(card.key === 'all' ? null : card.key)}
            className={`text-left rounded-xl ${card.bg} p-5 ring-1 ring-inset ${card.ring} transition-all hover:scale-[1.02] hover:shadow-md ${isActive ? 'ring-2 ring-offset-2 ring-primary-500' : ''}`}
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
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add apps/web-dashboard/src/components/SuperAdmin/KpiCards.tsx
git commit -m "feat(dashboard): add KpiCards for super admin"
```

---

### Task 21: `CommercesTable` component

**Files:**
- Create: `apps/web-dashboard/src/components/SuperAdmin/CommercesTable.tsx`

- [ ] **Step 1: Create the component**

```tsx
import { useSuperAdminCommerces, type CommercesQueryParams } from '@/hooks/useSuperAdminCommerces';
import CommerceStatusBadge from './CommerceStatusBadge';
import type { CommerceListItem, ExpiryStatus } from '@/types';

interface Props {
  filters: CommercesQueryParams;
  onRowClick: (id: number) => void;
  onAction: (action: 'approve' | 'renew' | 'reactivate' | 'change_plan' | 'suspend', commerce: CommerceListItem) => void;
}

function formatExpiry(item: CommerceListItem): { primary: string; secondary: string } {
  if (item.status === 'pending') return { primary: '—', secondary: 'No aprobado aun' };
  if (!item.plan_expires_at) return { primary: '—', secondary: '' };
  const days = item.days_until_expiry ?? 0;
  const date = new Date(item.plan_expires_at).toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' });
  if (days < 0) return { primary: `Vencio hace ${Math.abs(days)} dias`, secondary: date };
  if (days === 0) return { primary: 'Vence hoy', secondary: date };
  return { primary: `En ${days} dias`, secondary: date };
}

function ActionButton({ commerce, onAction }: { commerce: CommerceListItem; onAction: Props['onAction'] }) {
  const status = commerce.expiry_status as ExpiryStatus;
  const stop = (e: React.MouseEvent) => e.stopPropagation();

  if (status === 'pending') {
    return (
      <button onClick={(e) => { stop(e); onAction('approve', commerce); }}
        className="rounded-md bg-primary-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-primary-700">
        Aprobar
      </button>
    );
  }
  if (status === 'suspended') {
    return (
      <button onClick={(e) => { stop(e); onAction('reactivate', commerce); }}
        className="rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-700">
        Reactivar
      </button>
    );
  }
  if (status === 'in_grace' || status === 'expired') {
    return (
      <button onClick={(e) => { stop(e); onAction('renew', commerce); }}
        className="rounded-md bg-red-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-red-700">
        Renovar
      </button>
    );
  }
  if (status === 'expiring_soon') {
    return (
      <button onClick={(e) => { stop(e); onAction('renew', commerce); }}
        className="rounded-md bg-orange-500 px-3 py-1.5 text-sm font-medium text-white hover:bg-orange-600">
        Renovar
      </button>
    );
  }
  // active
  return (
    <button onClick={(e) => { stop(e); onAction('change_plan', commerce); }}
      className="rounded-md bg-gray-100 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-200">
      Cambiar plan
    </button>
  );
}

export default function CommercesTable({ filters, onRowClick, onAction }: Props) {
  const { data, isLoading } = useSuperAdminCommerces(filters);

  if (isLoading) {
    return <div className="rounded-xl bg-white p-8 text-center text-gray-500">Cargando comercios...</div>;
  }
  if (!data || data.data.length === 0) {
    return <div className="rounded-xl bg-white p-8 text-center text-gray-500">No hay comercios con los filtros actuales</div>;
  }

  return (
    <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">Comercio · Dueño</th>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">Plan</th>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">Estado</th>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">Vencimiento</th>
            <th className="px-4 py-3 text-right text-xs font-semibold text-gray-600">Accion</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-200">
          {data.data.map((commerce) => {
            const expiry = formatExpiry(commerce);
            return (
              <tr key={commerce.id} onClick={() => onRowClick(commerce.id)} className="cursor-pointer hover:bg-gray-50">
                <td className="px-4 py-3">
                  <div className="font-medium text-gray-900">{commerce.name}</div>
                  <div className="text-sm text-gray-500">{commerce.owner?.name ?? '—'}</div>
                </td>
                <td className="px-4 py-3 text-sm text-gray-700">
                  {commerce.plan?.name ?? '—'}
                </td>
                <td className="px-4 py-3">
                  <CommerceStatusBadge status={commerce.expiry_status} />
                </td>
                <td className="px-4 py-3">
                  <div className="text-sm text-gray-900">{expiry.primary}</div>
                  <div className="text-xs text-gray-500">{expiry.secondary}</div>
                </td>
                <td className="px-4 py-3 text-right">
                  <ActionButton commerce={commerce} onAction={onAction} />
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

- [ ] **Step 2: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add apps/web-dashboard/src/components/SuperAdmin/CommercesTable.tsx
git commit -m "feat(dashboard): add CommercesTable with contextual action buttons"
```

---

### Task 22: `SuperAdminCommercesTab` page (composes KPIs + filters + table)

**Files:**
- Create: `apps/web-dashboard/src/pages/SuperAdminCommercesTab.tsx`

- [ ] **Step 1: Create the page**

```tsx
import { useState } from 'react';
import { Search } from 'lucide-react';
import KpiCards from '@/components/SuperAdmin/KpiCards';
import CommercesTable from '@/components/SuperAdmin/CommercesTable';
import type { CommerceListItem } from '@/types';

const FILTER_TO_STATUS: Record<string, string | undefined> = {
  pending: 'pending',
  expiring: undefined, // backend treats as 'active' but we filter client-side
  suspended: 'suspended',
};

export default function SuperAdminCommercesTab() {
  const [filter, setFilter] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [drawerCommerceId, setDrawerCommerceId] = useState<number | null>(null);
  const [pendingAction, setPendingAction] = useState<{
    action: 'approve' | 'renew' | 'reactivate' | 'change_plan' | 'suspend';
    commerce: CommerceListItem;
  } | null>(null);

  const queryParams = {
    status: filter ? FILTER_TO_STATUS[filter] : undefined,
    q: search || undefined,
  };

  return (
    <div className="space-y-6">
      <KpiCards activeFilter={filter} onFilterChange={setFilter} />

      <div className="flex flex-col sm:flex-row gap-3">
        <div className="flex gap-2 flex-wrap">
          {[
            { key: null, label: 'Todos' },
            { key: 'pending', label: 'Pendientes' },
            { key: 'expiring', label: 'Por vencer' },
            { key: 'suspended', label: 'Suspendidos' },
          ].map((f) => (
            <button
              key={f.key ?? 'all'}
              onClick={() => setFilter(f.key)}
              className={`rounded-full px-3 py-1.5 text-sm font-medium ${filter === f.key ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'}`}
            >
              {f.label}
            </button>
          ))}
        </div>
        <div className="relative flex-1 sm:max-w-xs sm:ml-auto">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Buscar comercio o email..."
            className="w-full rounded-lg border border-gray-300 bg-white pl-9 pr-3 py-2 text-sm focus:border-primary-500 focus:ring-primary-500"
          />
        </div>
      </div>

      <CommercesTable
        filters={queryParams}
        onRowClick={(id) => setDrawerCommerceId(id)}
        onAction={(action, commerce) => setPendingAction({ action, commerce })}
      />

      {/* Drawer + modals are wired in Task 28 — for now, both are placeholders */}
      {drawerCommerceId !== null && (
        <div className="hidden">{drawerCommerceId}</div>
      )}
      {pendingAction !== null && (
        <div className="hidden">{pendingAction.action}</div>
      )}
    </div>
  );
}
```

- [ ] **Step 2: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add apps/web-dashboard/src/pages/SuperAdminCommercesTab.tsx
git commit -m "feat(dashboard): wire SuperAdminCommercesTab with KPIs and table"
```

---

## Phase 6 — Frontend: Drawer + Modals

### Task 23: `RenewCommerceModal` component

**Files:**
- Create: `apps/web-dashboard/src/components/SuperAdmin/RenewCommerceModal.tsx`

- [ ] **Step 1: Create the modal**

```tsx
import { useState, useMemo } from 'react';
import { X } from 'lucide-react';
import { useSuperAdminPlans } from '@/hooks/useSuperAdminPlans';
import { useRenewCommerce } from '@/hooks/useRenewCommerce';
import type { CommerceListItem, CommerceDetail } from '@/types';

interface Props {
  commerce: CommerceListItem | CommerceDetail;
  onClose: () => void;
  onSuccess?: () => void;
}

export default function RenewCommerceModal({ commerce, onClose, onSuccess }: Props) {
  const { data: plans } = useSuperAdminPlans();
  const renew = useRenewCommerce();
  const [planSlug, setPlanSlug] = useState(commerce.plan?.slug ?? '');
  const [amount, setAmount] = useState<string>(commerce.plan?.price?.toString() ?? '');
  const [notes, setNotes] = useState('');
  const [confirmed, setConfirmed] = useState(false);

  const newExpiresAt = useMemo(() => {
    const base = commerce.plan_expires_at && new Date(commerce.plan_expires_at) > new Date()
      ? new Date(commerce.plan_expires_at)
      : new Date();
    base.setDate(base.getDate() + 30);
    return base.toLocaleDateString('es-PE', { day: '2-digit', month: 'long', year: 'numeric' });
  }, [commerce.plan_expires_at]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await renew.mutateAsync({
      id: commerce.id,
      plan_slug: planSlug,
      amount_paid: amount ? parseFloat(amount) : undefined,
      notes: notes || undefined,
    });
    onSuccess?.();
    onClose();
  };

  const activePlans = plans?.filter((p) => p.is_active) ?? [];

  return (
    <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="bg-white rounded-2xl shadow-xl max-w-md w-full" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between p-6 border-b">
          <h2 className="text-lg font-semibold">Renovar comercio</h2>
          <button onClick={onClose}><X className="h-5 w-5 text-gray-500 hover:text-gray-700" /></button>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <p className="text-sm text-gray-500">Comercio</p>
            <p className="font-medium">{commerce.name}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Plan</label>
            <select
              value={planSlug}
              onChange={(e) => {
                setPlanSlug(e.target.value);
                const p = activePlans.find((pp) => pp.slug === e.target.value);
                if (p) setAmount(p.price.toString());
              }}
              className="w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500"
              required
            >
              <option value="">Selecciona un plan</option>
              {activePlans.map((p) => (
                <option key={p.id} value={p.slug}>{p.name} — S/{p.price}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Monto recibido (opcional)</label>
            <input type="number" step="0.01" min="0" value={amount} onChange={(e) => setAmount(e.target.value)}
              className="w-full rounded-md border-gray-300 shadow-sm" placeholder="0.00" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Nota (opcional)</label>
            <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={2}
              className="w-full rounded-md border-gray-300 shadow-sm" placeholder="Ej: Yape Juan Perez" />
          </div>
          <label className="flex items-start gap-2 text-sm">
            <input type="checkbox" checked={confirmed} onChange={(e) => setConfirmed(e.target.checked)}
              className="mt-0.5 rounded border-gray-300 text-primary-600 focus:ring-primary-500" />
            <span>Confirmo que el cliente ya pago</span>
          </label>
          <div className="rounded-lg bg-blue-50 p-3 text-sm text-blue-900">
            Nuevo vencimiento: <strong>{newExpiresAt}</strong>
          </div>
          <div className="flex gap-2 justify-end pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 rounded-md">
              Cancelar
            </button>
            <button type="submit" disabled={!confirmed || !planSlug || renew.isPending}
              className="px-4 py-2 text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 rounded-md disabled:opacity-50 disabled:cursor-not-allowed">
              {renew.isPending ? 'Renovando...' : 'Confirmar y renovar'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add apps/web-dashboard/src/components/SuperAdmin/RenewCommerceModal.tsx
git commit -m "feat(dashboard): add RenewCommerceModal"
```

---

### Task 24: `ApproveCommerceModal` component

**Files:**
- Create: `apps/web-dashboard/src/components/SuperAdmin/ApproveCommerceModal.tsx`

- [ ] **Step 1: Create the modal**

```tsx
import { useState } from 'react';
import { X } from 'lucide-react';
import { useSuperAdminPlans } from '@/hooks/useSuperAdminPlans';
import { useApproveCommerce } from '@/hooks/useApproveCommerce';
import type { CommerceListItem, CommerceDetail } from '@/types';

interface Props {
  commerce: CommerceListItem | CommerceDetail;
  onClose: () => void;
  onSuccess?: () => void;
}

export default function ApproveCommerceModal({ commerce, onClose, onSuccess }: Props) {
  const { data: plans } = useSuperAdminPlans();
  const approve = useApproveCommerce();
  const [planSlug, setPlanSlug] = useState('starter');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await approve.mutateAsync({ id: commerce.id, plan_slug: planSlug });
    onSuccess?.();
    onClose();
  };

  const activePlans = plans?.filter((p) => p.is_active) ?? [];

  return (
    <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="bg-white rounded-2xl shadow-xl max-w-md w-full" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between p-6 border-b">
          <h2 className="text-lg font-semibold">Aprobar comercio</h2>
          <button onClick={onClose}><X className="h-5 w-5 text-gray-500 hover:text-gray-700" /></button>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <p className="text-sm text-gray-500">Comercio</p>
            <p className="font-medium">{commerce.name}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Plan inicial</label>
            <select value={planSlug} onChange={(e) => setPlanSlug(e.target.value)}
              className="w-full rounded-md border-gray-300" required>
              {activePlans.map((p) => (
                <option key={p.id} value={p.slug}>{p.name} — S/{p.price}</option>
              ))}
            </select>
          </div>
          <p className="text-sm text-gray-600">
            Vencimiento inicial: <strong>30 dias desde hoy</strong>
          </p>
          <div className="flex gap-2 justify-end pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 rounded-md">
              Cancelar
            </button>
            <button type="submit" disabled={!planSlug || approve.isPending}
              className="px-4 py-2 text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 rounded-md disabled:opacity-50">
              {approve.isPending ? 'Aprobando...' : 'Aprobar'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/web-dashboard/src/components/SuperAdmin/ApproveCommerceModal.tsx
git commit -m "feat(dashboard): add ApproveCommerceModal"
```

---

### Task 25: `ChangePlanModal` component

**Files:**
- Create: `apps/web-dashboard/src/components/SuperAdmin/ChangePlanModal.tsx`

- [ ] **Step 1: Create the modal**

```tsx
import { useState } from 'react';
import { X } from 'lucide-react';
import { useSuperAdminPlans } from '@/hooks/useSuperAdminPlans';
import { useChangePlan } from '@/hooks/useChangePlan';
import type { CommerceListItem, CommerceDetail } from '@/types';

interface Props {
  commerce: CommerceListItem | CommerceDetail;
  onClose: () => void;
  onSuccess?: () => void;
}

export default function ChangePlanModal({ commerce, onClose, onSuccess }: Props) {
  const { data: plans } = useSuperAdminPlans();
  const change = useChangePlan();
  const [planSlug, setPlanSlug] = useState(commerce.plan?.slug ?? '');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await change.mutateAsync({ id: commerce.id, plan_slug: planSlug });
    onSuccess?.();
    onClose();
  };

  const activePlans = plans?.filter((p) => p.is_active) ?? [];

  return (
    <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="bg-white rounded-2xl shadow-xl max-w-md w-full" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between p-6 border-b">
          <h2 className="text-lg font-semibold">Cambiar plan</h2>
          <button onClick={onClose}><X className="h-5 w-5 text-gray-500 hover:text-gray-700" /></button>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <p className="text-sm text-gray-500">Plan actual</p>
            <p className="font-medium">{commerce.plan?.name ?? '—'} {commerce.plan ? `· S/${commerce.plan.price}` : ''}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Nuevo plan</label>
            <select value={planSlug} onChange={(e) => setPlanSlug(e.target.value)}
              className="w-full rounded-md border-gray-300" required>
              {activePlans.map((p) => (
                <option key={p.id} value={p.slug}>{p.name} — S/{p.price}</option>
              ))}
            </select>
          </div>
          <p className="text-sm text-gray-600">
            El cambio de plan no afecta la fecha de vencimiento.
          </p>
          <div className="flex gap-2 justify-end pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 rounded-md">
              Cancelar
            </button>
            <button type="submit" disabled={!planSlug || change.isPending || planSlug === commerce.plan?.slug}
              className="px-4 py-2 text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 rounded-md disabled:opacity-50">
              {change.isPending ? 'Cambiando...' : 'Cambiar plan'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/web-dashboard/src/components/SuperAdmin/ChangePlanModal.tsx
git commit -m "feat(dashboard): add ChangePlanModal"
```

---

### Task 26: `SuspendConfirmDialog` component

**Files:**
- Create: `apps/web-dashboard/src/components/SuperAdmin/SuspendConfirmDialog.tsx`

- [ ] **Step 1: Create the component**

```tsx
import { AlertTriangle } from 'lucide-react';
import { useSuspendCommerce } from '@/hooks/useSuspendCommerce';
import type { CommerceListItem, CommerceDetail } from '@/types';

interface Props {
  commerce: CommerceListItem | CommerceDetail;
  onClose: () => void;
  onSuccess?: () => void;
}

export default function SuspendConfirmDialog({ commerce, onClose, onSuccess }: Props) {
  const suspend = useSuspendCommerce();

  const handleConfirm = async () => {
    await suspend.mutateAsync(commerce.id);
    onSuccess?.();
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-start gap-3 mb-4">
          <div className="flex-shrink-0 rounded-full bg-red-100 p-2">
            <AlertTriangle className="h-5 w-5 text-red-600" />
          </div>
          <div>
            <h2 className="text-lg font-semibold">Suspender comercio</h2>
            <p className="text-sm text-gray-600 mt-1">
              ¿Seguro que quieres suspender <strong>{commerce.name}</strong>? Sus dispositivos dejaran de aceptar notificaciones inmediatamente.
            </p>
          </div>
        </div>
        <div className="flex gap-2 justify-end mt-4">
          <button onClick={onClose} className="px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 rounded-md">
            Cancelar
          </button>
          <button onClick={handleConfirm} disabled={suspend.isPending}
            className="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-md disabled:opacity-50">
            {suspend.isPending ? 'Suspendiendo...' : 'Si, suspender'}
          </button>
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/web-dashboard/src/components/SuperAdmin/SuspendConfirmDialog.tsx
git commit -m "feat(dashboard): add SuspendConfirmDialog"
```

---

### Task 27: `CommerceDetailDrawer` component

**Files:**
- Create: `apps/web-dashboard/src/components/SuperAdmin/CommerceDetailDrawer.tsx`

- [ ] **Step 1: Create the drawer**

```tsx
import { X, Mail, Phone, Copy, Users, Smartphone, BarChart, History } from 'lucide-react';
import { useSuperAdminCommerce } from '@/hooks/useSuperAdminCommerce';
import CommerceStatusBadge from './CommerceStatusBadge';
import type { CommerceDetail } from '@/types';

interface Props {
  commerceId: number | null;
  onClose: () => void;
  onAction: (action: 'renew' | 'change_plan' | 'suspend' | 'reactivate' | 'approve', commerce: CommerceDetail) => void;
}

function copyToClipboard(text: string) {
  navigator.clipboard.writeText(text).catch(() => {});
}

export default function CommerceDetailDrawer({ commerceId, onClose, onAction }: Props) {
  const { data: commerce, isLoading } = useSuperAdminCommerce(commerceId);

  if (commerceId === null) return null;

  return (
    <div className="fixed inset-0 z-40 bg-black/30" onClick={onClose}>
      <div
        className="fixed top-0 right-0 h-full w-full sm:w-[480px] bg-white shadow-xl overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="sticky top-0 bg-white border-b p-4 flex justify-between items-center">
          <h2 className="text-lg font-semibold">Detalle del comercio</h2>
          <button onClick={onClose}><X className="h-5 w-5 text-gray-500 hover:text-gray-700" /></button>
        </div>

        {isLoading || !commerce ? (
          <div className="p-6 text-center text-gray-500">Cargando...</div>
        ) : (
          <div className="p-4 space-y-5">
            <section>
              <h3 className="text-xl font-bold">{commerce.name}</h3>
              <div className="flex items-center gap-2 mt-1">
                <CommerceStatusBadge status={commerce.expiry_status} />
                {commerce.plan && <span className="text-sm text-gray-600">· {commerce.plan.name}</span>}
              </div>
            </section>

            <section className="rounded-lg border p-3">
              <h4 className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">Dueño</h4>
              <p className="font-medium">{commerce.owner?.name ?? '—'}</p>
              <div className="flex items-center gap-2 mt-1 text-sm">
                <Mail className="h-4 w-4 text-gray-400" />
                <span className="flex-1 truncate">{commerce.owner?.email ?? '—'}</span>
                {commerce.owner?.email && (
                  <button onClick={() => copyToClipboard(commerce.owner!.email)} className="text-gray-400 hover:text-gray-700" title="Copiar email">
                    <Copy className="h-4 w-4" />
                  </button>
                )}
              </div>
              <div className="flex items-center gap-2 mt-1 text-sm">
                <Phone className="h-4 w-4 text-gray-400" />
                <span className="flex-1">{commerce.owner?.phone ?? 'Sin telefono'}</span>
                {commerce.owner?.phone && (
                  <button onClick={() => copyToClipboard(commerce.owner!.phone!)} className="text-gray-400 hover:text-gray-700" title="Copiar telefono">
                    <Copy className="h-4 w-4" />
                  </button>
                )}
              </div>
            </section>

            {commerce.plan && (
              <section className="rounded-lg border p-3">
                <h4 className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">Plan actual</h4>
                <div className="flex justify-between mb-2">
                  <span className="font-medium">{commerce.plan.name}</span>
                  <span className="text-sm">S/{commerce.plan.price}/mes</span>
                </div>
                <p className="text-sm">Dispositivos: {commerce.plan.max_devices ?? '∞'}</p>
                <p className="text-sm">Notificaciones/dia: {commerce.plan.max_notifications_per_day ?? '∞'}</p>
                {commerce.plan_expires_at && (
                  <p className="text-sm mt-2">
                    Vence: {new Date(commerce.plan_expires_at).toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' })}
                    {commerce.days_until_expiry !== null && (
                      <span className="text-gray-500"> ({commerce.days_until_expiry >= 0 ? `en ${commerce.days_until_expiry} dias` : `vencio hace ${Math.abs(commerce.days_until_expiry)} dias`})</span>
                    )}
                  </p>
                )}
              </section>
            )}

            <section className="rounded-lg border p-3">
              <h4 className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2 flex items-center gap-2">
                <Users className="h-4 w-4" /> Captadores ({commerce.captadores.length})
              </h4>
              {commerce.captadores.length === 0 ? (
                <p className="text-sm text-gray-500">Sin captadores</p>
              ) : (
                <ul className="text-sm space-y-1">
                  {commerce.captadores.map((c) => (
                    <li key={c.id} className="flex justify-between">
                      <span>{c.name}</span>
                      <span className="text-gray-500 font-mono">PIN {c.pin ?? '—'}</span>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section className="rounded-lg border p-3">
              <h4 className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2 flex items-center gap-2">
                <BarChart className="h-4 w-4" /> Uso
              </h4>
              <p className="text-sm flex items-center gap-2"><Smartphone className="h-3.5 w-3.5 text-gray-400" /> {commerce.devices_count} dispositivos</p>
            </section>

            {commerce.renewals.length > 0 && (
              <section className="rounded-lg border p-3">
                <h4 className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2 flex items-center gap-2">
                  <History className="h-4 w-4" /> Renovaciones recientes
                </h4>
                <ul className="text-sm space-y-2">
                  {commerce.renewals.slice(0, 5).map((r) => (
                    <li key={r.id} className="border-l-2 border-gray-200 pl-2">
                      <div className="flex justify-between text-xs text-gray-500">
                        <span>{new Date(r.created_at).toLocaleDateString('es-PE')}</span>
                        <span>{r.plan?.name}</span>
                      </div>
                      <div>{r.amount_paid !== null ? `S/${r.amount_paid}` : 'Sin monto'} · {r.renewedBy?.name}</div>
                      {r.notes && <div className="text-xs text-gray-500">{r.notes}</div>}
                    </li>
                  ))}
                </ul>
              </section>
            )}

            <section className="space-y-2">
              {commerce.expiry_status === 'pending' && (
                <button onClick={() => onAction('approve', commerce)} className="w-full rounded-md bg-primary-600 text-white py-2.5 font-medium hover:bg-primary-700">
                  Aprobar comercio
                </button>
              )}
              {commerce.expiry_status !== 'pending' && commerce.status !== 'suspended' && (
                <>
                  <button onClick={() => onAction('renew', commerce)} className="w-full rounded-md bg-primary-600 text-white py-2.5 font-medium hover:bg-primary-700">
                    Renovar 30 dias
                  </button>
                  <button onClick={() => onAction('change_plan', commerce)} className="w-full rounded-md bg-gray-100 text-gray-700 py-2.5 font-medium hover:bg-gray-200">
                    Cambiar plan
                  </button>
                  <button onClick={() => onAction('suspend', commerce)} className="w-full rounded-md text-red-700 py-2.5 font-medium hover:bg-red-50">
                    Suspender comercio
                  </button>
                </>
              )}
              {commerce.status === 'suspended' && (
                <button onClick={() => onAction('reactivate', commerce)} className="w-full rounded-md bg-blue-600 text-white py-2.5 font-medium hover:bg-blue-700">
                  Reactivar (renovar)
                </button>
              )}
            </section>
          </div>
        )}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add apps/web-dashboard/src/components/SuperAdmin/CommerceDetailDrawer.tsx
git commit -m "feat(dashboard): add CommerceDetailDrawer"
```

---

### Task 28: Wire drawer + modals into `SuperAdminCommercesTab`

**Files:**
- Modify: `apps/web-dashboard/src/pages/SuperAdminCommercesTab.tsx`

- [ ] **Step 1: Replace placeholder content**

Replace the placeholder section at the bottom with:

```tsx
import RenewCommerceModal from '@/components/SuperAdmin/RenewCommerceModal';
import ApproveCommerceModal from '@/components/SuperAdmin/ApproveCommerceModal';
import ChangePlanModal from '@/components/SuperAdmin/ChangePlanModal';
import SuspendConfirmDialog from '@/components/SuperAdmin/SuspendConfirmDialog';
import CommerceDetailDrawer from '@/components/SuperAdmin/CommerceDetailDrawer';
import { useReactivateCommerce } from '@/hooks/useReactivateCommerce';
```

Then near the end, replace the `{drawerCommerceId !== null && ...}` block with:

```tsx
      <CommerceDetailDrawer
        commerceId={drawerCommerceId}
        onClose={() => setDrawerCommerceId(null)}
        onAction={(action, commerce) => setPendingAction({ action, commerce })}
      />

      {pendingAction?.action === 'approve' && (
        <ApproveCommerceModal commerce={pendingAction.commerce} onClose={() => setPendingAction(null)} />
      )}
      {pendingAction?.action === 'renew' && (
        <RenewCommerceModal commerce={pendingAction.commerce} onClose={() => setPendingAction(null)} />
      )}
      {pendingAction?.action === 'reactivate' && (
        <RenewCommerceModal commerce={pendingAction.commerce} onClose={() => setPendingAction(null)} />
      )}
      {pendingAction?.action === 'change_plan' && (
        <ChangePlanModal commerce={pendingAction.commerce} onClose={() => setPendingAction(null)} />
      )}
      {pendingAction?.action === 'suspend' && (
        <SuspendConfirmDialog commerce={pendingAction.commerce} onClose={() => setPendingAction(null)} />
      )}
```

(Remove the unused `useReactivateCommerce` import — reactivate is handled by `RenewCommerceModal` since reactivation requires renewal.)

- [ ] **Step 2: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add apps/web-dashboard/src/pages/SuperAdminCommercesTab.tsx
git commit -m "feat(dashboard): wire drawer and action modals"
```

---

## Phase 7 — Frontend: Planes tab

### Task 29: `EditPlanModal` component

**Files:**
- Create: `apps/web-dashboard/src/components/SuperAdmin/EditPlanModal.tsx`

- [ ] **Step 1: Create the component**

```tsx
import { useState } from 'react';
import { X } from 'lucide-react';
import { useUpdatePlan } from '@/hooks/useUpdatePlan';
import type { Plan } from '@/types';

interface Props {
  plan: Plan;
  onClose: () => void;
}

export default function EditPlanModal({ plan, onClose }: Props) {
  const update = useUpdatePlan();
  const [price, setPrice] = useState(plan.price.toString());
  const [maxDevices, setMaxDevices] = useState(plan.max_devices?.toString() ?? '');
  const [maxNotifs, setMaxNotifs] = useState(plan.max_notifications_per_day?.toString() ?? '');
  const [isActive, setIsActive] = useState(plan.is_active);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await update.mutateAsync({
      id: plan.id,
      price: parseFloat(price),
      max_devices: maxDevices ? parseInt(maxDevices, 10) : null,
      max_notifications_per_day: maxNotifs ? parseInt(maxNotifs, 10) : null,
      is_active: isActive,
    });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="bg-white rounded-2xl shadow-xl max-w-md w-full" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between p-6 border-b">
          <h2 className="text-lg font-semibold">Editar plan: {plan.name}</h2>
          <button onClick={onClose}><X className="h-5 w-5 text-gray-500 hover:text-gray-700" /></button>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Precio (S/)</label>
            <input type="number" step="0.01" min="0" value={price} onChange={(e) => setPrice(e.target.value)}
              className="w-full rounded-md border-gray-300" required />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Maximo dispositivos (vacio = ilimitado)</label>
            <input type="number" min="1" value={maxDevices} onChange={(e) => setMaxDevices(e.target.value)}
              className="w-full rounded-md border-gray-300" placeholder="∞" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Notificaciones por dia (vacio = ilimitado)</label>
            <input type="number" min="1" value={maxNotifs} onChange={(e) => setMaxNotifs(e.target.value)}
              className="w-full rounded-md border-gray-300" placeholder="∞" />
          </div>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={isActive} onChange={(e) => setIsActive(e.target.checked)}
              className="rounded border-gray-300 text-primary-600" />
            <span>Plan activo (visible al renovar)</span>
          </label>
          <p className="text-xs text-gray-500">Nombre y slug no se pueden editar para evitar romper relaciones existentes.</p>
          <div className="flex gap-2 justify-end pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 rounded-md">
              Cancelar
            </button>
            <button type="submit" disabled={update.isPending}
              className="px-4 py-2 text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 rounded-md disabled:opacity-50">
              {update.isPending ? 'Guardando...' : 'Guardar cambios'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/web-dashboard/src/components/SuperAdmin/EditPlanModal.tsx
git commit -m "feat(dashboard): add EditPlanModal"
```

---

### Task 30: `PlansTable` component

**Files:**
- Create: `apps/web-dashboard/src/components/SuperAdmin/PlansTable.tsx`

- [ ] **Step 1: Create the component**

```tsx
import { Pencil } from 'lucide-react';
import { useSuperAdminPlans } from '@/hooks/useSuperAdminPlans';
import type { Plan } from '@/types';

interface Props {
  onEdit: (plan: Plan) => void;
}

export default function PlansTable({ onEdit }: Props) {
  const { data, isLoading } = useSuperAdminPlans();

  if (isLoading) return <div className="rounded-xl bg-white p-8 text-center text-gray-500">Cargando planes...</div>;
  if (!data || data.length === 0) return <div className="rounded-xl bg-white p-8 text-center text-gray-500">No hay planes</div>;

  return (
    <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">Plan</th>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">Precio</th>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">Dispositivos</th>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">Notif/dia</th>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">Estado</th>
            <th className="px-4 py-3 text-right text-xs font-semibold text-gray-600">Accion</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-200">
          {data.map((plan) => (
            <tr key={plan.id}>
              <td className="px-4 py-3 font-medium text-gray-900">{plan.name}</td>
              <td className="px-4 py-3 text-sm text-gray-700">S/{plan.price}</td>
              <td className="px-4 py-3 text-sm text-gray-700">{plan.max_devices ?? 'Ilimitado'}</td>
              <td className="px-4 py-3 text-sm text-gray-700">{plan.max_notifications_per_day ?? 'Ilimitado'}</td>
              <td className="px-4 py-3">
                <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${plan.is_active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-700'}`}>
                  {plan.is_active ? 'Activo' : 'Inactivo'}
                </span>
              </td>
              <td className="px-4 py-3 text-right">
                <button onClick={() => onEdit(plan)} className="text-primary-600 hover:text-primary-700">
                  <Pencil className="h-4 w-4 inline" /> <span className="text-sm">Editar</span>
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/web-dashboard/src/components/SuperAdmin/PlansTable.tsx
git commit -m "feat(dashboard): add PlansTable"
```

---

### Task 31: `SuperAdminPlansTab` page

**Files:**
- Create: `apps/web-dashboard/src/pages/SuperAdminPlansTab.tsx`

- [ ] **Step 1: Create the page**

```tsx
import { useState } from 'react';
import PlansTable from '@/components/SuperAdmin/PlansTable';
import EditPlanModal from '@/components/SuperAdmin/EditPlanModal';
import type { Plan } from '@/types';

export default function SuperAdminPlansTab() {
  const [editing, setEditing] = useState<Plan | null>(null);

  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-xl font-bold text-gray-900">Planes de suscripcion</h2>
        <p className="text-sm text-gray-600">Edita precio y limites de los planes existentes.</p>
      </div>
      <PlansTable onEdit={setEditing} />
      {editing && <EditPlanModal plan={editing} onClose={() => setEditing(null)} />}
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/web-dashboard/src/pages/SuperAdminPlansTab.tsx
git commit -m "feat(dashboard): add SuperAdminPlansTab page"
```

---

## Phase 8 — Wiring + integration

### Task 32: Refactor `SuperAdminPage` with tabs

**Files:**
- Modify: `apps/web-dashboard/src/pages/SuperAdminPage.tsx`

- [ ] **Step 1: Replace placeholder with tabs layout**

```tsx
import { Outlet, NavLink } from 'react-router-dom';
import { Building2, CreditCard, Shield } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';

export default function SuperAdminPage() {
  const { user } = useAuth();

  return (
    <div className="space-y-4" role="main" aria-label="Panel de Super Admin">
      <div className="rounded-2xl bg-gradient-to-br from-indigo-600 via-purple-600 to-pink-600 p-6 shadow-xl text-white">
        <div className="flex items-center gap-3">
          <Shield className="h-7 w-7" />
          <div>
            <h1 className="text-2xl font-bold">Panel de Super Admin</h1>
            <p className="text-sm text-indigo-100">Bienvenido, {user?.name}</p>
          </div>
        </div>
      </div>

      <div className="border-b border-gray-200">
        <nav className="-mb-px flex gap-6" aria-label="Tabs">
          <NavLink
            to="/super-admin/commerces"
            className={({ isActive }) =>
              `flex items-center gap-2 border-b-2 py-3 px-1 text-sm font-medium ${isActive ? 'border-primary-600 text-primary-700' : 'border-transparent text-gray-500 hover:text-gray-700'}`
            }
          >
            <Building2 className="h-4 w-4" /> Comercios
          </NavLink>
          <NavLink
            to="/super-admin/plans"
            className={({ isActive }) =>
              `flex items-center gap-2 border-b-2 py-3 px-1 text-sm font-medium ${isActive ? 'border-primary-600 text-primary-700' : 'border-transparent text-gray-500 hover:text-gray-700'}`
            }
          >
            <CreditCard className="h-4 w-4" /> Planes
          </NavLink>
        </nav>
      </div>

      <Outlet />
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/web-dashboard/src/pages/SuperAdminPage.tsx
git commit -m "feat(dashboard): convert SuperAdminPage to tabs layout"
```

---

### Task 33: Update routing in `App.tsx`

**Files:**
- Modify: `apps/web-dashboard/src/App.tsx`

- [ ] **Step 1: Update lazy imports**

Add at the top with other lazy imports:

```tsx
const SuperAdminCommercesTab = lazy(() => import('./pages/SuperAdminCommercesTab'));
const SuperAdminPlansTab = lazy(() => import('./pages/SuperAdminPlansTab'));
```

- [ ] **Step 2: Update the `/super-admin` route block**

Replace the existing `/super-admin` route:

```tsx
        <Route path="/super-admin" element={
          <PrivateRoute requireCommerce={false}>
            <Layout />
          </PrivateRoute>
        }>
          <Route element={<SuperAdminPage />}>
            <Route index element={<Navigate to="/super-admin/commerces" replace />} />
            <Route path="commerces" element={<SuperAdminCommercesTab />} />
            <Route path="plans" element={<SuperAdminPlansTab />} />
          </Route>
        </Route>
```

- [ ] **Step 3: Update PrivateRoute super_admin redirect**

In the existing `PrivateRoute`, the line:

```tsx
if (isSuperAdmin && !location.pathname.startsWith('/super-admin')) {
  return <Navigate to="/super-admin" replace />;
}
```

This already works because `/super-admin` redirects to `/super-admin/commerces` via the index route. No change needed.

- [ ] **Step 4: Type-check + lint**

Run: `cd apps/web-dashboard && npm run type-check && npm run lint`
Expected: no errors.

- [ ] **Step 5: Commit**

```bash
git add apps/web-dashboard/src/App.tsx
git commit -m "feat(dashboard): wire super admin routes to tabs"
```

---

### Task 34: Show "SUPER ADMIN" badge in header

**Files:**
- Modify: `apps/web-dashboard/src/components/Layout.tsx`

- [ ] **Step 1: Add the badge conditional**

In `Layout.tsx`, after the existing `<h1>` tag, add the badge:

```tsx
              <h1 className="text-2xl font-bold text-primary-600">Yape Notifier</h1>
              {user?.role === 'super_admin' && (
                <span className="inline-flex items-center rounded-full bg-indigo-100 px-2.5 py-0.5 text-xs font-bold text-indigo-700 ring-1 ring-indigo-200">
                  SUPER ADMIN
                </span>
              )}
```

- [ ] **Step 2: Commit**

```bash
git add apps/web-dashboard/src/components/Layout.tsx
git commit -m "feat(dashboard): show SUPER ADMIN badge in header"
```

---

### Task 35: Manual end-to-end smoke test

**Files:** none (manual testing)

- [ ] **Step 1: Start the local dev environment if not running**

Run: `cd infra/docker/environments/development && docker compose --env-file .env up -d`

- [ ] **Step 2: Confirm migrations and seeders ran**

Run: `docker compose --env-file .env exec -T php-fpm php artisan migrate:status | tail -5`
Expected: latest migration `2026_04_28_000002_create_commerce_renewals_table` shows as Ran.

- [ ] **Step 3: Re-seed for clean state**

Run: `docker compose --env-file .env exec -T php-fpm php artisan db:seed --force`
Expected: SuperAdminSeeder confirms super admin is ready.

- [ ] **Step 4: Run all backend tests**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test`
Expected: green.

- [ ] **Step 5: Type-check + lint frontend**

Run: `cd apps/web-dashboard && npm run type-check && npm run lint`
Expected: no errors.

- [ ] **Step 6: Manually verify each user flow**

In the browser:

1. Login as `admin@local.test` / `admin1234`
2. Lands on `/super-admin/commerces` with KPI cards (all zeros initially)
3. Tabs visible: Comercios + Planes
4. SUPER ADMIN badge visible in header
5. Use tinker to seed test data:
   ```bash
   docker compose --env-file .env exec -T php-fpm php artisan tinker --execute="
   \$plan = App\Models\Plan::where('slug','basic')->first();
   for (\$i = 1; \$i <= 5; \$i++) {
       \$user = App\Models\User::factory()->create(['email' => \"admin{\$i}@test.com\", 'role' => 'admin']);
       \$states = ['', 'pending', 'expiringSoon', 'inGrace', 'suspended'];
       \$state = \$states[\$i - 1];
       \$factory = App\Models\Commerce::factory();
       if (\$state) \$factory = \$factory->\$state();
       \$commerce = \$factory->create(['name' => \"Test Commerce \$i\", 'owner_user_id' => \$user->id, 'plan_id' => \$plan->id]);
       \$user->update(['commerce_id' => \$commerce->id]);
   }
   "
   ```
6. Refresh `/super-admin/commerces` — KPI cards now show counts
7. Click each filter chip — table updates
8. Click on a row — drawer opens with details
9. Click "Renovar" on an expiring/in-grace commerce — modal opens, fill form, confirm — toast and refresh
10. Click "Aprobar" on a pending commerce — modal opens with plan dropdown — approve — refresh
11. Click "Suspender" on an active commerce → confirm dialog → suspend
12. Click "Reactivar" on a suspended commerce → renew modal opens
13. Go to Planes tab → click Editar on Basic → modify price → save → reflects in table

- [ ] **Step 7: Commit any incidental fixes**

If you needed to tweak anything during smoke testing, commit those fixes:

```bash
git add -A
git commit -m "fix(superadmin): minor adjustments from smoke test"
```

---

## Verification

After all tasks complete, run the full suite:

- [ ] Backend tests: `docker compose --env-file .env exec -T php-fpm php artisan test` — green
- [ ] Frontend type-check: `cd apps/web-dashboard && npm run type-check` — no errors
- [ ] Frontend lint: `cd apps/web-dashboard && npm run lint` — no new warnings
- [ ] Manual smoke test from Task 35 — all flows work
