# Programa de Afiliados — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the referral program: 20% recurring commission per referee renewal, encrypted bank-account registration, super-admin payment workflow, and dashboard surfaces for both the referrer commerce and the super admin.

**Architecture:** Add `referral_code` + `referred_by_commerce_id` + payout-account columns to `commerces`; new `referral_commissions` table keyed by `commerce_renewal_id` (UNIQUE → idempotent). Observers on `Commerce::creating` (generate code) and `CommerceRenewal::created`/`deleting` (manage commissions). Controllers for admin (own data) and super admin (management). React tabs in `/dashboard` and `/super-admin`.

**Tech Stack:** Laravel 11, PostgreSQL, Eloquent Observers, Sanctum, React 18, TypeScript, Tailwind, TanStack Query, PHPUnit (TDD backend), `npm run type-check` (frontend).

**Spec:** [docs/superpowers/specs/2026-05-16-programa-de-afiliados.md](../specs/2026-05-16-programa-de-afiliados.md)

---

## Phase 1 — Database schema

### Task 1: Migrations (commerces columns + referral_commissions table + backfill)

**Files:**
- Create: `apps/api/database/migrations/2026_05_16_000001_add_referral_fields_to_commerces_table.php`
- Create: `apps/api/database/migrations/2026_05_16_000002_add_payout_account_fields_to_commerces_table.php`
- Create: `apps/api/database/migrations/2026_05_16_000003_create_referral_commissions_table.php`

- [ ] **Step 1: Migration 1 — referral fields on commerces with backfill**

```php
<?php
use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;

return new class extends Migration {
    public function up(): void
    {
        Schema::table('commerces', function (Blueprint $table) {
            $table->string('referral_code', 20)->nullable()->after('name');
            $table->foreignId('referred_by_commerce_id')->nullable()
                ->after('referral_code')
                ->constrained('commerces')->nullOnDelete();
            $table->index('referred_by_commerce_id');
        });

        // Backfill existing rows
        DB::table('commerces')->whereNull('referral_code')->orderBy('id')->each(function ($row) {
            $base = Str::slug(Str::limit($row->name, 8, ''), '-');
            if ($base === '') {
                $base = 'com';
            }
            $attempts = 0;
            do {
                $code = $base . '-' . Str::lower(Str::random(4));
                $exists = DB::table('commerces')->where('referral_code', $code)->exists();
                $attempts++;
            } while ($exists && $attempts < 5);
            if ($exists) {
                $code = $base . '-' . time() . Str::lower(Str::random(2));
            }
            DB::table('commerces')->where('id', $row->id)->update(['referral_code' => $code]);
        });

        Schema::table('commerces', function (Blueprint $table) {
            $table->string('referral_code', 20)->nullable(false)->unique()->change();
        });
    }

    public function down(): void
    {
        Schema::table('commerces', function (Blueprint $table) {
            $table->dropForeign(['referred_by_commerce_id']);
            $table->dropColumn(['referral_code', 'referred_by_commerce_id']);
        });
    }
};
```

- [ ] **Step 2: Migration 2 — payout account fields on commerces**

```php
<?php
use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration {
    public function up(): void
    {
        Schema::table('commerces', function (Blueprint $table) {
            $table->string('payout_bank', 80)->nullable()->after('referred_by_commerce_id');
            $table->string('payout_account_type', 20)->nullable()->after('payout_bank');
            $table->text('payout_account_number')->nullable()->after('payout_account_type');
            $table->string('payout_account_holder', 150)->nullable()->after('payout_account_number');
            $table->string('payout_account_holder_doc', 20)->nullable()->after('payout_account_holder');
        });
    }

    public function down(): void
    {
        Schema::table('commerces', function (Blueprint $table) {
            $table->dropColumn([
                'payout_bank',
                'payout_account_type',
                'payout_account_number',
                'payout_account_holder',
                'payout_account_holder_doc',
            ]);
        });
    }
};
```

- [ ] **Step 3: Migration 3 — referral_commissions table**

```php
<?php
use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration {
    public function up(): void
    {
        Schema::create('referral_commissions', function (Blueprint $table) {
            $table->id();
            $table->foreignId('referrer_commerce_id')->constrained('commerces')->cascadeOnDelete();
            $table->foreignId('referred_commerce_id')->constrained('commerces')->cascadeOnDelete();
            $table->foreignId('commerce_renewal_id')->unique()->constrained('commerce_renewals')->cascadeOnDelete();
            $table->decimal('base_amount', 10, 2);
            $table->decimal('commission_rate', 5, 4)->default(0.2000);
            $table->decimal('amount', 10, 2);
            $table->string('status', 16)->default('pending');
            $table->timestamp('paid_at')->nullable();
            $table->string('payout_reference', 100)->nullable();
            $table->text('voided_reason')->nullable();
            $table->foreignId('approved_by_user_id')->nullable()->constrained('users')->nullOnDelete();
            $table->foreignId('paid_by_user_id')->nullable()->constrained('users')->nullOnDelete();
            $table->timestamps();

            $table->index(['referrer_commerce_id', 'status']);
            $table->index(['status', 'created_at']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('referral_commissions');
    }
};
```

- [ ] **Step 4: Run migrations**

Run: `docker compose exec api php artisan migrate`
Expected: 3 migrations applied. Verify `referral_code` populated on existing commerces.

- [ ] **Step 5: Commit**

```bash
git add apps/api/database/migrations/2026_05_16_*
git commit -m "feat(api): add referral schema (codes, payout account, commissions table)"
```

---

## Phase 2 — Models

### Task 2: Commerce + ReferralCommission models (TDD)

**Files:**
- Modify: `apps/api/app/Models/Commerce.php`
- Create: `apps/api/app/Models/ReferralCommission.php`
- Test: `apps/api/tests/Feature/Referral/CommerceReferralModelTest.php`

- [ ] **Step 1: Write failing test**

```php
<?php
namespace Tests\Feature\Referral;

use App\Models\Commerce;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Crypt;
use Tests\TestCase;

class CommerceReferralModelTest extends TestCase
{
    use RefreshDatabase;

    public function test_payout_account_number_is_encrypted_in_db(): void
    {
        $owner = User::factory()->create();
        $commerce = Commerce::create([
            'name' => 'Test Commerce',
            'owner_user_id' => $owner->id,
            'status' => 'active',
            'payout_account_number' => '00112233445566778899',
        ]);

        $raw = \DB::table('commerces')->where('id', $commerce->id)->value('payout_account_number');
        $this->assertNotSame('00112233445566778899', $raw);
        $this->assertSame('00112233445566778899', Crypt::decryptString($raw));
        $this->assertSame('00112233445566778899', $commerce->fresh()->payout_account_number);
    }

    public function test_referrer_relationship_returns_commerce(): void
    {
        $owner1 = User::factory()->create();
        $owner2 = User::factory()->create();
        $referrer = Commerce::create(['name' => 'Karol', 'owner_user_id' => $owner1->id, 'status' => 'active']);
        $referred = Commerce::create(['name' => 'Pepe', 'owner_user_id' => $owner2->id, 'status' => 'active', 'referred_by_commerce_id' => $referrer->id]);

        $this->assertEquals($referrer->id, $referred->referrer->id);
        $this->assertTrue($referrer->referrals->contains('id', $referred->id));
    }
}
```

- [ ] **Step 2: Run test, expect FAIL**

Run: `docker compose exec api php artisan test --filter=CommerceReferralModelTest`

- [ ] **Step 3: Update Commerce model**

Add to `$fillable`: `referral_code`, `referred_by_commerce_id`, `payout_bank`, `payout_account_type`, `payout_account_number`, `payout_account_holder`, `payout_account_holder_doc`.

Add to `$hidden`: `payout_account_number` (so it never accidentally serializes).

Add accessor/mutator for encryption:

```php
protected function payoutAccountNumber(): \Illuminate\Database\Eloquent\Casts\Attribute
{
    return \Illuminate\Database\Eloquent\Casts\Attribute::make(
        get: fn ($value) => $value === null ? null : \Illuminate\Support\Facades\Crypt::decryptString($value),
        set: fn ($value) => $value === null ? null : \Illuminate\Support\Facades\Crypt::encryptString($value),
    );
}
```

Add relationships:

```php
public function referrer(): \Illuminate\Database\Eloquent\Relations\BelongsTo
{
    return $this->belongsTo(Commerce::class, 'referred_by_commerce_id');
}

public function referrals(): \Illuminate\Database\Eloquent\Relations\HasMany
{
    return $this->hasMany(Commerce::class, 'referred_by_commerce_id');
}

public function referralCommissionsEarned(): \Illuminate\Database\Eloquent\Relations\HasMany
{
    return $this->hasMany(ReferralCommission::class, 'referrer_commerce_id');
}

public function hasPayoutAccount(): bool
{
    return filled($this->payout_bank)
        && filled($this->payout_account_type)
        && filled($this->payout_account_number)
        && filled($this->payout_account_holder)
        && filled($this->payout_account_holder_doc);
}
```

- [ ] **Step 4: Create ReferralCommission model**

```php
<?php
namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class ReferralCommission extends Model
{
    use HasFactory;

    public const STATUS_PENDING = 'pending';
    public const STATUS_APPROVED = 'approved';
    public const STATUS_PAID = 'paid';
    public const STATUS_VOID = 'void';

    protected $fillable = [
        'referrer_commerce_id',
        'referred_commerce_id',
        'commerce_renewal_id',
        'base_amount',
        'commission_rate',
        'amount',
        'status',
        'paid_at',
        'payout_reference',
        'voided_reason',
        'approved_by_user_id',
        'paid_by_user_id',
    ];

    protected $casts = [
        'base_amount' => 'decimal:2',
        'commission_rate' => 'decimal:4',
        'amount' => 'decimal:2',
        'paid_at' => 'datetime',
    ];

    public function referrer(): BelongsTo { return $this->belongsTo(Commerce::class, 'referrer_commerce_id'); }
    public function referred(): BelongsTo { return $this->belongsTo(Commerce::class, 'referred_commerce_id'); }
    public function renewal(): BelongsTo { return $this->belongsTo(CommerceRenewal::class, 'commerce_renewal_id'); }
    public function approvedBy(): BelongsTo { return $this->belongsTo(User::class, 'approved_by_user_id'); }
    public function paidBy(): BelongsTo { return $this->belongsTo(User::class, 'paid_by_user_id'); }
}
```

- [ ] **Step 5: Run tests, expect PASS**

Run: `docker compose exec api php artisan test --filter=CommerceReferralModelTest`

- [ ] **Step 6: Commit**

```bash
git add apps/api/app/Models/ apps/api/tests/Feature/Referral/
git commit -m "feat(api): Commerce/ReferralCommission models with encrypted payout account"
```

---

## Phase 3 — Referral lifecycle

### Task 3: CommerceObserver generates referral_code (TDD)

**Files:**
- Create: `apps/api/app/Observers/CommerceObserver.php`
- Modify: `apps/api/app/Providers/AppServiceProvider.php` (register observer)
- Test: `apps/api/tests/Feature/Referral/ReferralCodeGenerationTest.php`

- [ ] **Step 1: Write failing test**

```php
<?php
namespace Tests\Feature\Referral;

use App\Models\Commerce;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class ReferralCodeGenerationTest extends TestCase
{
    use RefreshDatabase;

    public function test_creating_commerce_auto_generates_referral_code(): void
    {
        $owner = User::factory()->create();
        $commerce = Commerce::create([
            'name' => 'Karol Pharmacy',
            'owner_user_id' => $owner->id,
            'status' => 'pending',
        ]);

        $this->assertNotNull($commerce->referral_code);
        $this->assertMatchesRegularExpression('/^[a-z0-9-]+$/', $commerce->referral_code);
        $this->assertStringStartsWith('karol-ph', $commerce->referral_code);
    }

    public function test_referral_code_is_unique_across_commerces(): void
    {
        $owner = User::factory()->create();
        $a = Commerce::create(['name' => 'Test', 'owner_user_id' => $owner->id, 'status' => 'pending']);
        $b = Commerce::create(['name' => 'Test', 'owner_user_id' => $owner->id, 'status' => 'pending']);
        $this->assertNotSame($a->referral_code, $b->referral_code);
    }

    public function test_existing_referral_code_is_preserved(): void
    {
        $owner = User::factory()->create();
        $commerce = Commerce::create([
            'name' => 'Test',
            'owner_user_id' => $owner->id,
            'status' => 'pending',
            'referral_code' => 'custom-aaaa',
        ]);
        $this->assertSame('custom-aaaa', $commerce->referral_code);
    }
}
```

- [ ] **Step 2: Run, expect FAIL**

Run: `docker compose exec api php artisan test --filter=ReferralCodeGenerationTest`

- [ ] **Step 3: Implement CommerceObserver**

```php
<?php
namespace App\Observers;

use App\Models\Commerce;
use Illuminate\Support\Str;

class CommerceObserver
{
    public function creating(Commerce $commerce): void
    {
        if (! empty($commerce->referral_code)) {
            return;
        }
        $commerce->referral_code = $this->generateUniqueCode($commerce->name);
    }

    private function generateUniqueCode(string $name): string
    {
        $base = Str::slug(Str::limit($name, 8, ''), '-');
        if ($base === '') {
            $base = 'com';
        }
        for ($i = 0; $i < 5; $i++) {
            $code = $base . '-' . Str::lower(Str::random(4));
            if (! Commerce::where('referral_code', $code)->exists()) {
                return $code;
            }
        }
        return $base . '-' . time() . Str::lower(Str::random(2));
    }
}
```

- [ ] **Step 4: Register observer in AppServiceProvider::boot()**

```php
\App\Models\Commerce::observe(\App\Observers\CommerceObserver::class);
```

- [ ] **Step 5: Run tests, expect PASS**

- [ ] **Step 6: Commit**

```bash
git add apps/api/app/Observers/CommerceObserver.php apps/api/app/Providers/AppServiceProvider.php apps/api/tests/Feature/Referral/
git commit -m "feat(api): auto-generate referral_code on commerce creation"
```

---

### Task 4: CommerceController::store accepts referral_code (TDD)

**Files:**
- Modify: `apps/api/app/Http/Controllers/CommerceController.php`
- Modify: `apps/api/app/Services/CommerceService.php` (if it exists; else inline in controller)
- Test: `apps/api/tests/Feature/Referral/CommerceStoreReferralTest.php`

- [ ] **Step 1: Write failing test**

```php
<?php
namespace Tests\Feature\Referral;

use App\Models\Commerce;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class CommerceStoreReferralTest extends TestCase
{
    use RefreshDatabase;

    public function test_valid_referral_code_sets_referred_by(): void
    {
        $referrerOwner = User::factory()->create();
        $referrer = Commerce::create(['name' => 'Karol', 'owner_user_id' => $referrerOwner->id, 'status' => 'active']);

        $newOwner = User::factory()->create();
        Sanctum::actingAs($newOwner);
        $response = $this->postJson('/api/commerces', [
            'name' => 'Pepe Bodega',
            'referral_code' => $referrer->referral_code,
        ]);
        $response->assertCreated();

        $newCommerce = Commerce::where('name', 'Pepe Bodega')->first();
        $this->assertEquals($referrer->id, $newCommerce->referred_by_commerce_id);
    }

    public function test_invalid_referral_code_is_ignored_silently(): void
    {
        $newOwner = User::factory()->create();
        Sanctum::actingAs($newOwner);
        $response = $this->postJson('/api/commerces', [
            'name' => 'New Co',
            'referral_code' => 'nonexistent-zzz',
        ]);
        $response->assertCreated();
        $this->assertNull(Commerce::where('name', 'New Co')->first()->referred_by_commerce_id);
    }

    public function test_self_referral_is_ignored(): void
    {
        $owner = User::factory()->create();
        $existing = Commerce::create(['name' => 'Mine', 'owner_user_id' => $owner->id, 'status' => 'active']);
        // Simulate a user trying to self-refer by trying to reuse own code on a new commerce creation
        // Force the user to have no commerce_id so the endpoint allows store
        $owner->commerce_id = null;
        $owner->save();

        Sanctum::actingAs($owner);
        $response = $this->postJson('/api/commerces', [
            'name' => 'Another',
            'referral_code' => $existing->referral_code,
        ]);
        $response->assertCreated();
        $this->assertNull(Commerce::where('name', 'Another')->first()->referred_by_commerce_id);
    }
}
```

- [ ] **Step 2: Run, expect FAIL**

- [ ] **Step 3: Modify CommerceController::store**

In the validation rules add `'referral_code' => 'nullable|string|max:30'`.

After creating the commerce, if `referral_code` is present:

```php
if ($request->filled('referral_code')) {
    $referrer = \App\Models\Commerce::where('referral_code', $request->input('referral_code'))->first();
    if ($referrer && $referrer->owner_user_id !== $user->id) {
        $commerce->referred_by_commerce_id = $referrer->id;
        $commerce->save();
    } else {
        \Log::warning('Referral code rejected', [
            'reason' => $referrer ? 'self_referral' : 'not_found',
            'code' => $request->input('referral_code'),
            'user_id' => $user->id,
        ]);
    }
}
```

- [ ] **Step 4: Run tests, expect PASS**

- [ ] **Step 5: Commit**

```bash
git add apps/api/app/Http/Controllers/CommerceController.php apps/api/tests/Feature/Referral/CommerceStoreReferralTest.php
git commit -m "feat(api): accept and validate referral_code on commerce creation"
```

---

## Phase 4 — Commission generation

### Task 5: CommerceRenewalObserver (create + delete) (TDD)

**Files:**
- Create: `apps/api/app/Observers/CommerceRenewalObserver.php`
- Modify: `apps/api/app/Providers/AppServiceProvider.php`
- Test: `apps/api/tests/Feature/Referral/CommissionGenerationTest.php`

- [ ] **Step 1: Write failing tests**

```php
<?php
namespace Tests\Feature\Referral;

use App\Models\Commerce;
use App\Models\CommerceRenewal;
use App\Models\Plan;
use App\Models\ReferralCommission;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class CommissionGenerationTest extends TestCase
{
    use RefreshDatabase;

    private function makeReferredPair(string $status = 'active'): array
    {
        $u1 = User::factory()->create();
        $u2 = User::factory()->create();
        $referrer = Commerce::create(['name' => 'Karol', 'owner_user_id' => $u1->id, 'status' => 'active']);
        $referred = Commerce::create([
            'name' => 'Pepe',
            'owner_user_id' => $u2->id,
            'status' => $status,
            'referred_by_commerce_id' => $referrer->id,
        ]);
        return [$referrer, $referred];
    }

    public function test_renewal_generates_pending_commission_at_20pct(): void
    {
        [$referrer, $referred] = $this->makeReferredPair();
        $plan = Plan::factory()->create(['price' => 49.00]);
        $admin = User::factory()->create(['role' => 'super_admin']);

        $renewal = CommerceRenewal::create([
            'commerce_id' => $referred->id,
            'plan_id' => $plan->id,
            'renewed_by_user_id' => $admin->id,
            'new_expires_at' => now()->addMonth(),
            'amount_paid' => 49.00,
        ]);

        $commission = ReferralCommission::where('commerce_renewal_id', $renewal->id)->first();
        $this->assertNotNull($commission);
        $this->assertSame('pending', $commission->status);
        $this->assertEquals(49.00, (float) $commission->base_amount);
        $this->assertEquals(0.2000, (float) $commission->commission_rate);
        $this->assertEquals(9.80, (float) $commission->amount);
        $this->assertEquals($referrer->id, $commission->referrer_commerce_id);
    }

    public function test_no_commission_when_no_referrer(): void
    {
        $u = User::factory()->create();
        $commerce = Commerce::create(['name' => 'X', 'owner_user_id' => $u->id, 'status' => 'active']);
        $plan = Plan::factory()->create(['price' => 49.00]);
        $admin = User::factory()->create(['role' => 'super_admin']);

        CommerceRenewal::create([
            'commerce_id' => $commerce->id,
            'plan_id' => $plan->id,
            'renewed_by_user_id' => $admin->id,
            'new_expires_at' => now()->addMonth(),
            'amount_paid' => 49.00,
        ]);
        $this->assertSame(0, ReferralCommission::count());
    }

    public function test_no_commission_when_referred_not_active(): void
    {
        [$referrer, $referred] = $this->makeReferredPair('pending');
        $plan = Plan::factory()->create(['price' => 49.00]);
        $admin = User::factory()->create(['role' => 'super_admin']);
        CommerceRenewal::create([
            'commerce_id' => $referred->id,
            'plan_id' => $plan->id,
            'renewed_by_user_id' => $admin->id,
            'new_expires_at' => now()->addMonth(),
            'amount_paid' => 49.00,
        ]);
        $this->assertSame(0, ReferralCommission::count());
    }

    public function test_no_commission_when_amount_zero(): void
    {
        [$referrer, $referred] = $this->makeReferredPair();
        $plan = Plan::factory()->create(['price' => 49.00]);
        $admin = User::factory()->create(['role' => 'super_admin']);
        CommerceRenewal::create([
            'commerce_id' => $referred->id,
            'plan_id' => $plan->id,
            'renewed_by_user_id' => $admin->id,
            'new_expires_at' => now()->addMonth(),
            'amount_paid' => 0,
        ]);
        $this->assertSame(0, ReferralCommission::count());
    }

    public function test_deleting_renewal_voids_commission(): void
    {
        [$referrer, $referred] = $this->makeReferredPair();
        $plan = Plan::factory()->create(['price' => 49.00]);
        $admin = User::factory()->create(['role' => 'super_admin']);
        $renewal = CommerceRenewal::create([
            'commerce_id' => $referred->id,
            'plan_id' => $plan->id,
            'renewed_by_user_id' => $admin->id,
            'new_expires_at' => now()->addMonth(),
            'amount_paid' => 49.00,
        ]);
        $renewal->delete();
        $c = ReferralCommission::where('commerce_renewal_id', $renewal->id)->first();
        $this->assertSame('void', $c->status);
        $this->assertNotNull($c->voided_reason);
    }
}
```

Note: the renewal `delete()` cascades the FK, so the test for `void` must use **soft delete behavior** or change the FK to NO action and let the observer mark void before delete commits. **Adjust the migration** to NOT cascade `commerce_renewal_id` — set it to `restrictOnDelete` instead, and have the observer void the commission and detach the renewal_id (or null it) — OR change strategy to soft-delete renewals.

**Decision for this plan:** keep FK cascade off (`restrictOnDelete` won't work since we need delete to succeed). Better approach: change the migration so `commerce_renewal_id` is `nullable()` and `nullOnDelete()`. The observer `deleting` marks the commission void BEFORE delete completes (setting `voided_reason` and `status`), and the FK then nulls on the cascade. Update Task 1 Step 3 accordingly: `->foreignId('commerce_renewal_id')->unique()->nullable()->constrained('commerce_renewals')->nullOnDelete()`.

- [ ] **Step 2: Update Task 1 migration if not yet pushed: change `commerce_renewal_id` to `nullable()->constrained()->nullOnDelete()`**

If migration already ran, write a follow-up migration:

```php
Schema::table('referral_commissions', function (Blueprint $table) {
    $table->dropForeign(['commerce_renewal_id']);
    $table->foreignId('commerce_renewal_id')->nullable()->change();
    $table->foreign('commerce_renewal_id')->references('id')->on('commerce_renewals')->nullOnDelete();
});
```

- [ ] **Step 3: Run tests, expect FAIL**

- [ ] **Step 4: Implement CommerceRenewalObserver**

```php
<?php
namespace App\Observers;

use App\Models\CommerceRenewal;
use App\Models\ReferralCommission;

class CommerceRenewalObserver
{
    public const COMMISSION_RATE = 0.20;

    public function created(CommerceRenewal $renewal): void
    {
        $commerce = $renewal->commerce;
        if (! $commerce || ! $commerce->referred_by_commerce_id) {
            return;
        }
        if ($commerce->status !== 'active') {
            return;
        }
        if ((float) $renewal->amount_paid <= 0) {
            return;
        }

        $base = (float) $renewal->amount_paid;
        $rate = self::COMMISSION_RATE;
        $amount = round($base * $rate, 2);

        ReferralCommission::create([
            'referrer_commerce_id' => $commerce->referred_by_commerce_id,
            'referred_commerce_id' => $commerce->id,
            'commerce_renewal_id' => $renewal->id,
            'base_amount' => $base,
            'commission_rate' => $rate,
            'amount' => $amount,
            'status' => ReferralCommission::STATUS_PENDING,
        ]);
    }

    public function deleting(CommerceRenewal $renewal): void
    {
        ReferralCommission::where('commerce_renewal_id', $renewal->id)
            ->whereIn('status', [ReferralCommission::STATUS_PENDING, ReferralCommission::STATUS_APPROVED])
            ->update([
                'status' => ReferralCommission::STATUS_VOID,
                'voided_reason' => 'Renewal eliminado',
            ]);
    }
}
```

- [ ] **Step 5: Register in AppServiceProvider::boot()**

```php
\App\Models\CommerceRenewal::observe(\App\Observers\CommerceRenewalObserver::class);
```

- [ ] **Step 6: Run tests, expect PASS**

- [ ] **Step 7: Commit**

```bash
git add apps/api/app/Observers/CommerceRenewalObserver.php apps/api/app/Providers/AppServiceProvider.php apps/api/database/migrations/ apps/api/tests/Feature/Referral/CommissionGenerationTest.php
git commit -m "feat(api): generate referral commissions on renewal; void on renewal delete"
```

---

## Phase 5 — Endpoints

### Task 6: Payout account endpoints (TDD)

**Files:**
- Create: `apps/api/app/Http/Controllers/PayoutAccountController.php`
- Create: `apps/api/app/Http/Requests/UpdatePayoutAccountRequest.php`
- Modify: `apps/api/routes/api.php`
- Test: `apps/api/tests/Feature/Referral/PayoutAccountEndpointsTest.php`

- [ ] **Step 1: Write failing tests**

```php
<?php
namespace Tests\Feature\Referral;

use App\Models\Commerce;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class PayoutAccountEndpointsTest extends TestCase
{
    use RefreshDatabase;

    public function test_admin_can_update_own_payout_account(): void
    {
        $admin = User::factory()->create(['role' => 'admin']);
        $commerce = Commerce::create(['name' => 'C', 'owner_user_id' => $admin->id, 'status' => 'active']);
        $admin->commerce_id = $commerce->id;
        $admin->save();

        Sanctum::actingAs($admin);
        $response = $this->putJson('/api/commerces/me/payout-account', [
            'payout_bank' => 'BCP',
            'payout_account_type' => 'cci',
            'payout_account_number' => '00219012345678901234',
            'payout_account_holder' => 'Karol Mendoza',
            'payout_account_holder_doc' => '12345678',
        ]);

        $response->assertOk();
        $commerce->refresh();
        $this->assertSame('BCP', $commerce->payout_bank);
        $this->assertSame('00219012345678901234', $commerce->payout_account_number);
    }

    public function test_get_returns_decrypted_account(): void
    {
        $admin = User::factory()->create(['role' => 'admin']);
        $commerce = Commerce::create([
            'name' => 'C',
            'owner_user_id' => $admin->id,
            'status' => 'active',
            'payout_bank' => 'BBVA',
            'payout_account_type' => 'ahorros',
            'payout_account_number' => '11122233344455',
            'payout_account_holder' => 'X',
            'payout_account_holder_doc' => '11111111',
        ]);
        $admin->commerce_id = $commerce->id;
        $admin->save();

        Sanctum::actingAs($admin);
        $response = $this->getJson('/api/commerces/me/payout-account');
        $response->assertOk()->assertJsonPath('payout_account_number', '11122233344455');
    }

    public function test_partial_payload_rejected_when_any_field_present(): void
    {
        $admin = User::factory()->create(['role' => 'admin']);
        $commerce = Commerce::create(['name' => 'C', 'owner_user_id' => $admin->id, 'status' => 'active']);
        $admin->commerce_id = $commerce->id;
        $admin->save();

        Sanctum::actingAs($admin);
        $response = $this->putJson('/api/commerces/me/payout-account', [
            'payout_bank' => 'BCP',
        ]);
        $response->assertStatus(422);
    }

    public function test_captador_cannot_update_payout_account(): void
    {
        $captador = User::factory()->create(['role' => 'captador']);
        $commerce = Commerce::create(['name' => 'C', 'owner_user_id' => $captador->id, 'status' => 'active']);
        $captador->commerce_id = $commerce->id;
        $captador->save();

        Sanctum::actingAs($captador);
        $response = $this->putJson('/api/commerces/me/payout-account', [
            'payout_bank' => 'BCP',
            'payout_account_type' => 'cci',
            'payout_account_number' => '0021901',
            'payout_account_holder' => 'X',
            'payout_account_holder_doc' => '1',
        ]);
        $response->assertStatus(403);
    }
}
```

- [ ] **Step 2: Run, expect FAIL**

- [ ] **Step 3: Implement FormRequest**

```php
<?php
namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

class UpdatePayoutAccountRequest extends FormRequest
{
    public function authorize(): bool { return true; }

    public function rules(): array
    {
        return [
            'payout_bank' => 'required|string|max:80',
            'payout_account_type' => 'required|in:corriente,ahorros,cci',
            'payout_account_number' => 'required|string|max:40',
            'payout_account_holder' => 'required|string|max:150',
            'payout_account_holder_doc' => 'required|string|max:20',
        ];
    }
}
```

- [ ] **Step 4: Implement PayoutAccountController**

```php
<?php
namespace App\Http\Controllers;

use App\Http\Requests\UpdatePayoutAccountRequest;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class PayoutAccountController extends Controller
{
    public function show(Request $request): JsonResponse
    {
        $commerce = $request->user()->commerce;
        abort_unless($commerce, 404);
        return response()->json([
            'payout_bank' => $commerce->payout_bank,
            'payout_account_type' => $commerce->payout_account_type,
            'payout_account_number' => $commerce->payout_account_number,
            'payout_account_holder' => $commerce->payout_account_holder,
            'payout_account_holder_doc' => $commerce->payout_account_holder_doc,
            'is_complete' => $commerce->hasPayoutAccount(),
        ]);
    }

    public function update(UpdatePayoutAccountRequest $request): JsonResponse
    {
        $commerce = $request->user()->commerce;
        abort_unless($commerce, 404);
        $commerce->fill($request->validated())->save();
        return response()->json(['message' => 'Cuenta actualizada', 'is_complete' => true]);
    }
}
```

Assumes `User::commerce()` relationship exists (verify; if not, use `Commerce::where('owner_user_id', $user->id)`).

- [ ] **Step 5: Register routes under `require_admin` middleware**

In `apps/api/routes/api.php` inside the existing `Route::middleware('require_admin')` group (or a new one):

```php
Route::get('/commerces/me/payout-account', [PayoutAccountController::class, 'show']);
Route::put('/commerces/me/payout-account', [PayoutAccountController::class, 'update']);
```

- [ ] **Step 6: Run tests, expect PASS**

- [ ] **Step 7: Commit**

```bash
git add apps/api/app/Http/Controllers/PayoutAccountController.php apps/api/app/Http/Requests/UpdatePayoutAccountRequest.php apps/api/routes/api.php apps/api/tests/Feature/Referral/PayoutAccountEndpointsTest.php
git commit -m "feat(api): payout account endpoints with encrypted storage"
```

---

### Task 7: Referral query endpoints for commerce admin (TDD)

**Files:**
- Create: `apps/api/app/Http/Controllers/ReferralController.php`
- Modify: `apps/api/routes/api.php`
- Test: `apps/api/tests/Feature/Referral/ReferralEndpointsTest.php`

- [ ] **Step 1: Write failing test**

```php
<?php
namespace Tests\Feature\Referral;

use App\Models\Commerce;
use App\Models\ReferralCommission;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class ReferralEndpointsTest extends TestCase
{
    use RefreshDatabase;

    public function test_stats_returns_aggregate_for_current_admin(): void
    {
        $admin = User::factory()->create(['role' => 'admin']);
        $referrer = Commerce::create(['name' => 'Karol', 'owner_user_id' => $admin->id, 'status' => 'active']);
        $admin->commerce_id = $referrer->id;
        $admin->save();

        $u = User::factory()->create();
        $referred = Commerce::create(['name' => 'X', 'owner_user_id' => $u->id, 'status' => 'active', 'referred_by_commerce_id' => $referrer->id]);

        ReferralCommission::create([
            'referrer_commerce_id' => $referrer->id,
            'referred_commerce_id' => $referred->id,
            'commerce_renewal_id' => null, // simulate detached for test
            'base_amount' => 49.00, 'commission_rate' => 0.2000, 'amount' => 9.80,
            'status' => 'approved',
        ]);

        Sanctum::actingAs($admin);
        $response = $this->getJson('/api/referrals/stats');
        $response->assertOk()
            ->assertJsonStructure(['month_earnings', 'pending_balance', 'lifetime_paid', 'active_referrals_count']);
        $this->assertEqualsWithDelta(9.80, $response->json('pending_balance'), 0.01);
        $this->assertSame(1, $response->json('active_referrals_count'));
    }

    public function test_referrals_endpoint_lists_only_own_referrals(): void
    {
        $admin = User::factory()->create(['role' => 'admin']);
        $referrer = Commerce::create(['name' => 'K', 'owner_user_id' => $admin->id, 'status' => 'active']);
        $admin->commerce_id = $referrer->id;
        $admin->save();

        $u1 = User::factory()->create();
        $u2 = User::factory()->create();
        Commerce::create(['name' => 'Mine', 'owner_user_id' => $u1->id, 'status' => 'active', 'referred_by_commerce_id' => $referrer->id]);
        Commerce::create(['name' => 'NotMine', 'owner_user_id' => $u2->id, 'status' => 'active']);

        Sanctum::actingAs($admin);
        $response = $this->getJson('/api/referrals/referrals');
        $response->assertOk();
        $names = array_column($response->json('data'), 'name');
        $this->assertContains('Mine', $names);
        $this->assertNotContains('NotMine', $names);
    }
}
```

- [ ] **Step 2: Run, expect FAIL**

- [ ] **Step 3: Implement ReferralController**

```php
<?php
namespace App\Http\Controllers;

use App\Models\Commerce;
use App\Models\ReferralCommission;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class ReferralController extends Controller
{
    public function stats(Request $request): JsonResponse
    {
        $commerce = $request->user()->commerce;
        abort_unless($commerce, 404);

        $startOfMonth = now()->startOfMonth();
        $monthEarnings = ReferralCommission::where('referrer_commerce_id', $commerce->id)
            ->whereIn('status', ['pending', 'approved', 'paid'])
            ->where('created_at', '>=', $startOfMonth)
            ->sum('amount');

        $pendingBalance = ReferralCommission::where('referrer_commerce_id', $commerce->id)
            ->whereIn('status', ['pending', 'approved'])
            ->sum('amount');

        $lifetimePaid = ReferralCommission::where('referrer_commerce_id', $commerce->id)
            ->where('status', 'paid')
            ->sum('amount');

        $activeReferralsCount = Commerce::where('referred_by_commerce_id', $commerce->id)
            ->where('status', 'active')
            ->count();

        return response()->json([
            'month_earnings' => round((float) $monthEarnings, 2),
            'pending_balance' => round((float) $pendingBalance, 2),
            'lifetime_paid' => round((float) $lifetimePaid, 2),
            'active_referrals_count' => $activeReferralsCount,
            'referral_code' => $commerce->referral_code,
        ]);
    }

    public function referrals(Request $request): JsonResponse
    {
        $commerce = $request->user()->commerce;
        abort_unless($commerce, 404);
        $rows = Commerce::where('referred_by_commerce_id', $commerce->id)
            ->with('plan')
            ->withSum('renewals as total_paid', 'amount_paid')
            ->orderByDesc('created_at')
            ->get(['id', 'name', 'status', 'plan_id', 'plan_expires_at', 'created_at']);
        return response()->json(['data' => $rows]);
    }

    public function commissions(Request $request): JsonResponse
    {
        $commerce = $request->user()->commerce;
        abort_unless($commerce, 404);
        $q = ReferralCommission::where('referrer_commerce_id', $commerce->id)
            ->with(['referred:id,name', 'renewal:id,new_expires_at'])
            ->orderByDesc('created_at');
        if ($status = $request->query('status')) {
            $q->where('status', $status);
        }
        return response()->json($q->paginate(20));
    }
}
```

- [ ] **Step 4: Routes (inside `require_admin` group)**

```php
Route::get('/referrals/stats', [ReferralController::class, 'stats']);
Route::get('/referrals/referrals', [ReferralController::class, 'referrals']);
Route::get('/referrals/commissions', [ReferralController::class, 'commissions']);
```

- [ ] **Step 5: Run, expect PASS**

- [ ] **Step 6: Commit**

```bash
git add apps/api/app/Http/Controllers/ReferralController.php apps/api/routes/api.php apps/api/tests/Feature/Referral/ReferralEndpointsTest.php
git commit -m "feat(api): commerce admin referral query endpoints"
```

---

### Task 8: Super admin commission management endpoints (TDD)

**Files:**
- Create: `apps/api/app/Http/Controllers/SuperAdmin/CommissionsController.php`
- Modify: `apps/api/routes/api.php`
- Test: `apps/api/tests/Feature/SuperAdmin/CommissionsTest.php`

- [ ] **Step 1: Write failing test**

```php
<?php
namespace Tests\Feature\SuperAdmin;

use App\Models\Commerce;
use App\Models\ReferralCommission;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class CommissionsTest extends TestCase
{
    use RefreshDatabase;

    private function setup_commission(string $status = 'pending', bool $withAccount = true): ReferralCommission
    {
        $u1 = User::factory()->create();
        $u2 = User::factory()->create();
        $referrer = Commerce::create([
            'name' => 'K', 'owner_user_id' => $u1->id, 'status' => 'active',
            ...($withAccount ? [
                'payout_bank' => 'BCP', 'payout_account_type' => 'cci',
                'payout_account_number' => '0021', 'payout_account_holder' => 'X',
                'payout_account_holder_doc' => '11111111',
            ] : []),
        ]);
        $referred = Commerce::create(['name' => 'P', 'owner_user_id' => $u2->id, 'status' => 'active', 'referred_by_commerce_id' => $referrer->id]);
        return ReferralCommission::create([
            'referrer_commerce_id' => $referrer->id,
            'referred_commerce_id' => $referred->id,
            'commerce_renewal_id' => null,
            'base_amount' => 49, 'commission_rate' => 0.2, 'amount' => 9.80,
            'status' => $status,
        ]);
    }

    public function test_super_admin_can_approve_pending_commission(): void
    {
        $sa = User::factory()->create(['role' => 'super_admin']);
        $c = $this->setup_commission('pending');
        Sanctum::actingAs($sa);
        $response = $this->postJson("/api/super-admin/commissions/{$c->id}/approve");
        $response->assertOk();
        $this->assertSame('approved', $c->fresh()->status);
        $this->assertEquals($sa->id, $c->fresh()->approved_by_user_id);
    }

    public function test_super_admin_pays_with_reference_when_account_complete(): void
    {
        $sa = User::factory()->create(['role' => 'super_admin']);
        $c = $this->setup_commission('approved', withAccount: true);
        Sanctum::actingAs($sa);
        $response = $this->postJson("/api/super-admin/commissions/{$c->id}/pay", ['payout_reference' => 'OP-12345']);
        $response->assertOk();
        $this->assertSame('paid', $c->fresh()->status);
        $this->assertSame('OP-12345', $c->fresh()->payout_reference);
    }

    public function test_pay_blocked_when_no_account(): void
    {
        $sa = User::factory()->create(['role' => 'super_admin']);
        $c = $this->setup_commission('approved', withAccount: false);
        Sanctum::actingAs($sa);
        $response = $this->postJson("/api/super-admin/commissions/{$c->id}/pay", ['payout_reference' => 'OP-1']);
        $response->assertStatus(422);
        $this->assertSame('approved', $c->fresh()->status);
    }

    public function test_void_with_reason(): void
    {
        $sa = User::factory()->create(['role' => 'super_admin']);
        $c = $this->setup_commission('pending');
        Sanctum::actingAs($sa);
        $response = $this->postJson("/api/super-admin/commissions/{$c->id}/void", ['reason' => 'fraude']);
        $response->assertOk();
        $this->assertSame('void', $c->fresh()->status);
        $this->assertSame('fraude', $c->fresh()->voided_reason);
    }

    public function test_non_super_admin_forbidden(): void
    {
        $admin = User::factory()->create(['role' => 'admin']);
        $c = $this->setup_commission();
        Sanctum::actingAs($admin);
        $this->postJson("/api/super-admin/commissions/{$c->id}/approve")->assertStatus(403);
    }
}
```

- [ ] **Step 2: Run, expect FAIL**

- [ ] **Step 3: Implement CommissionsController**

```php
<?php
namespace App\Http\Controllers\SuperAdmin;

use App\Http\Controllers\Controller;
use App\Models\ReferralCommission;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class CommissionsController extends Controller
{
    public function index(Request $request): JsonResponse
    {
        $q = ReferralCommission::with(['referrer:id,name', 'referred:id,name', 'renewal:id,new_expires_at,amount_paid'])
            ->orderByDesc('created_at');
        if ($s = $request->query('status')) $q->where('status', $s);
        if ($m = $request->query('month')) {
            $q->whereYear('created_at', substr($m, 0, 4))
              ->whereMonth('created_at', substr($m, 5, 2));
        }
        if ($r = $request->query('referrer_id')) $q->where('referrer_commerce_id', $r);
        if ($r = $request->query('referred_id')) $q->where('referred_commerce_id', $r);
        return response()->json($q->paginate(20));
    }

    public function approve(Request $request, int $id): JsonResponse
    {
        $c = ReferralCommission::findOrFail($id);
        abort_if($c->status !== 'pending', 422, 'Solo se pueden aprobar comisiones pendientes');
        $c->update(['status' => 'approved', 'approved_by_user_id' => $request->user()->id]);
        return response()->json($c);
    }

    public function pay(Request $request, int $id): JsonResponse
    {
        $request->validate(['payout_reference' => 'required|string|max:100']);
        $c = ReferralCommission::with('referrer')->findOrFail($id);
        abort_if($c->status !== 'approved', 422, 'Solo se pueden pagar comisiones aprobadas');
        abort_unless($c->referrer && $c->referrer->hasPayoutAccount(), 422, 'El comercio referidor no tiene cuenta de pago configurada');

        $c->update([
            'status' => 'paid',
            'paid_at' => now(),
            'payout_reference' => $request->input('payout_reference'),
            'paid_by_user_id' => $request->user()->id,
        ]);
        return response()->json($c);
    }

    public function void(Request $request, int $id): JsonResponse
    {
        $request->validate(['reason' => 'required|string|max:500']);
        $c = ReferralCommission::findOrFail($id);
        abort_if($c->status === 'void', 422, 'Ya está anulada');
        $c->update(['status' => 'void', 'voided_reason' => $request->input('reason')]);
        return response()->json($c);
    }
}
```

- [ ] **Step 4: Add a `super_admin` middleware check route group**

If a middleware alias `super_admin` doesn't exist, reuse the existing pattern (look at how `SuperAdminController` routes guard themselves). Add routes inside that group:

```php
Route::prefix('super-admin')->group(function () {
    Route::get('/commissions', [SuperAdmin\CommissionsController::class, 'index']);
    Route::post('/commissions/{id}/approve', [SuperAdmin\CommissionsController::class, 'approve']);
    Route::post('/commissions/{id}/pay', [SuperAdmin\CommissionsController::class, 'pay']);
    Route::post('/commissions/{id}/void', [SuperAdmin\CommissionsController::class, 'void']);
});
```

- [ ] **Step 5: Run, expect PASS**

- [ ] **Step 6: Commit**

```bash
git add apps/api/app/Http/Controllers/SuperAdmin/CommissionsController.php apps/api/routes/api.php apps/api/tests/Feature/SuperAdmin/CommissionsTest.php
git commit -m "feat(api): super admin commission management endpoints"
```

---

## Phase 6 — Frontend

### Task 9: TypeScript types + API service methods

**Files:**
- Modify: `apps/web-dashboard/src/types/index.ts`
- Modify: `apps/web-dashboard/src/services/api.ts`

- [ ] **Step 1: Add types**

```typescript
export interface PayoutAccount {
  payout_bank: string | null;
  payout_account_type: 'corriente' | 'ahorros' | 'cci' | null;
  payout_account_number: string | null;
  payout_account_holder: string | null;
  payout_account_holder_doc: string | null;
  is_complete: boolean;
}

export interface ReferralStats {
  month_earnings: number;
  pending_balance: number;
  lifetime_paid: number;
  active_referrals_count: number;
  referral_code: string;
}

export interface ReferralCommission {
  id: number;
  referrer_commerce_id: number;
  referred_commerce_id: number;
  commerce_renewal_id: number | null;
  base_amount: string;
  commission_rate: string;
  amount: string;
  status: 'pending' | 'approved' | 'paid' | 'void';
  paid_at: string | null;
  payout_reference: string | null;
  voided_reason: string | null;
  created_at: string;
  referred?: { id: number; name: string };
  referrer?: { id: number; name: string };
}

export interface ReferralCommerceRow {
  id: number;
  name: string;
  status: 'pending' | 'active' | 'suspended';
  plan_id: number | null;
  plan_expires_at: string | null;
  total_paid: number | null;
  created_at: string;
  plan?: Plan;
}
```

Also extend the existing `Commerce` interface with `referral_code: string` and `referred_by_commerce_id: number | null`.

- [ ] **Step 2: Add API methods to `api.ts`**

```typescript
async getReferralStats(): Promise<ReferralStats> {
  const { data } = await api.get('/referrals/stats');
  return data;
}
async getReferrals(): Promise<{ data: ReferralCommerceRow[] }> {
  const { data } = await api.get('/referrals/referrals');
  return data;
}
async getReferralCommissions(params?: { status?: string; page?: number }): Promise<PaginatedResponse<ReferralCommission>> {
  const { data } = await api.get('/referrals/commissions', { params });
  return data;
}
async getPayoutAccount(): Promise<PayoutAccount> {
  const { data } = await api.get('/commerces/me/payout-account');
  return data;
}
async updatePayoutAccount(payload: Omit<PayoutAccount, 'is_complete'>): Promise<{ message: string; is_complete: boolean }> {
  const { data } = await api.put('/commerces/me/payout-account', payload);
  return data;
}
// Super admin
async listAllCommissions(params: { status?: string; month?: string; referrer_id?: number; referred_id?: number; page?: number }): Promise<PaginatedResponse<ReferralCommission>> {
  const { data } = await api.get('/super-admin/commissions', { params });
  return data;
}
async approveCommission(id: number): Promise<ReferralCommission> {
  const { data } = await api.post(`/super-admin/commissions/${id}/approve`);
  return data;
}
async payCommission(id: number, payout_reference: string): Promise<ReferralCommission> {
  const { data } = await api.post(`/super-admin/commissions/${id}/pay`, { payout_reference });
  return data;
}
async voidCommission(id: number, reason: string): Promise<ReferralCommission> {
  const { data } = await api.post(`/super-admin/commissions/${id}/void`, { reason });
  return data;
}
```

- [ ] **Step 3: Run type-check**

```bash
cd apps/web-dashboard && npm run type-check
```

Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add apps/web-dashboard/src/types/index.ts apps/web-dashboard/src/services/api.ts
git commit -m "feat(dashboard): types + API for referral program"
```

---

### Task 10: "Programa de Referidos" tab in dashboard (with payout account form)

**Files:**
- Create: `apps/web-dashboard/src/pages/tabs/ReferralsTab.tsx`
- Modify: `apps/web-dashboard/src/pages/DashboardPage.tsx` (add tab)
- Modify: `apps/web-dashboard/src/hooks/useDashboardTabs.ts` (add 'referrals' tab)

- [ ] **Step 1: Add 'referrals' to valid tabs**

In `useDashboardTabs.ts`, extend `isValidTab` to include `'referrals'`. Update any tab-list union.

- [ ] **Step 2: Build ReferralsTab.tsx**

Sections (single file, ~250 lines):
1. Header card with `referral_code` + copy-to-clipboard + WhatsApp share. URL = `${window.location.origin}/register?ref=${code}`.
2. KPI row (4 cards): comisiones del mes, saldo pendiente, total pagado, ahijados activos. Use `useQuery(['referral-stats'])`.
3. Payout account form: collapsible/expandable. On load, GET `payout-account`; PUT on save. Show "✓ Completa" or "⚠ Falta configurar" badge.
4. Referrals table: `useQuery(['my-referrals'])` — name, plan, status badge, last renewal date, total paid.
5. Commissions table: `useQuery(['my-commissions', { status }])`, paginated with status filter — date, referred name, base, rate, amount, status badge, payout reference.

Use existing Tailwind classes from sibling tabs (`EmployeesPage.tsx` is a good template). Toast on success/error via existing toast system.

- [ ] **Step 3: Add tab to DashboardPage**

Add a nav item "Programa de Referidos" (icon: `Users` or `Gift` from lucide-react) that switches to the new tab.

- [ ] **Step 4: Type-check**

```bash
cd apps/web-dashboard && npm run type-check
```

- [ ] **Step 5: Commit**

```bash
git add apps/web-dashboard/src/
git commit -m "feat(dashboard): referrals tab with code sharing, KPIs, payout account, tables"
```

---

### Task 11: Super admin commissions sub-tab

**Files:**
- Create: `apps/web-dashboard/src/pages/SuperAdminCommissionsTab.tsx`
- Modify: `apps/web-dashboard/src/App.tsx` (lazy import + route)
- Modify: `apps/web-dashboard/src/pages/SuperAdminPage.tsx` (nav link)

- [ ] **Step 1: Add lazy import + route**

In `App.tsx`:

```tsx
const SuperAdminCommissionsTab = lazy(() => import('./pages/SuperAdminCommissionsTab'));
// inside <Route element={<SuperAdminPage />}>:
<Route path="commissions" element={<SuperAdminCommissionsTab />} />
```

- [ ] **Step 2: Build the tab**

- Top KPI cards: pending / approved / paid totals for the selected month.
- Filters: status select, month input (YYYY-MM), referrer search, referred search.
- Table: id, fecha, referidor (con cuenta? badge), referido, monto base, %, comisión, status, acciones.
- Actions per row:
  - **Aprobar** button (only if status=`pending`).
  - **Marcar pagada** opens modal asking for `payout_reference`; shows the referrer's payout account info read-only inside the modal. Disabled if no account.
  - **Anular** opens modal asking for `reason`.
- All mutations invalidate `['admin-commissions']` query.

- [ ] **Step 3: Add to SuperAdminPage nav**

```tsx
<NavLink to="/super-admin/commissions">Comisiones</NavLink>
```

- [ ] **Step 4: Type-check**

- [ ] **Step 5: Commit**

```bash
git add apps/web-dashboard/src/
git commit -m "feat(dashboard): super admin commissions management tab"
```

---

### Task 12: Register / create-commerce accepts ?ref=

**Files:**
- Modify: `apps/web-dashboard/src/pages/RegisterPage.tsx`
- Modify: `apps/web-dashboard/src/pages/CreateCommercePage.tsx`
- Modify: `apps/web-dashboard/src/services/api.ts` (createCommerce signature)

- [ ] **Step 1: Read `?ref=` from URL in RegisterPage**

```tsx
const [searchParams] = useSearchParams();
const refCode = searchParams.get('ref');
// stash to sessionStorage if present, so it survives navigation to create-commerce
useEffect(() => {
  if (refCode) sessionStorage.setItem('referral_code', refCode);
}, [refCode]);
```

- [ ] **Step 2: In CreateCommercePage**

- Read `sessionStorage.getItem('referral_code')` and pre-fill an optional input field.
- Add a visible "Código de referido (opcional)" input that the user can edit/clear.
- On submit, pass `referral_code` to `apiService.createCommerce`.
- Clear `sessionStorage` after successful create.

- [ ] **Step 3: Update `apiService.createCommerce`**

```typescript
async createCommerce(name: string, referral_code?: string): Promise<Commerce> {
  const { data } = await api.post('/commerces', { name, referral_code });
  return data;
}
```

- [ ] **Step 4: Type-check**

- [ ] **Step 5: Commit**

```bash
git add apps/web-dashboard/src/
git commit -m "feat(dashboard): capture referral code from URL into commerce creation"
```

---

## Phase 7 — Smoke test

### Task 13: Manual smoke test checklist

**Files:**
- Create: `docs/superpowers/smoke-tests/2026-05-16-programa-de-afiliados.md`

- [ ] **Step 1: Write the checklist**

```markdown
# Smoke test — Programa de Afiliados

## Backend
- [ ] `php artisan migrate` aplica las 3 migraciones sin error.
- [ ] Todos los comercios existentes tienen `referral_code` no nulo.
- [ ] `php artisan test --filter='Referral|Commissions'` → 100% verde.

## Flujo registro con referido
- [ ] Comercio A inicia sesión, copia su link `/register?ref=<code>`.
- [ ] En navegador limpio, abrir el link y registrarse como nuevo usuario.
- [ ] Crear comercio nuevo → confirmar en BD que `referred_by_commerce_id` apunta a A.

## Cuenta bancaria
- [ ] Como admin de A, ir a "Programa de Referidos" → "Cuenta para pagos".
- [ ] Guardar datos completos → badge "Completa".
- [ ] En BD: `payout_account_number` está cifrado (no se lee en plano).
- [ ] Como super admin: ver la misma cuenta en el detalle de una comisión de A.

## Comisión
- [ ] Super admin activa el comercio nuevo (status = `active`).
- [ ] Super admin crea un renewal con `amount_paid = 49`.
- [ ] Comprobar que se creó `referral_commission` para A con `amount = 9.80`, status `pending`.

## Aprobación / pago / anulación
- [ ] Super admin aprueba → status `approved`.
- [ ] Sin cuenta de pago → al intentar pagar, error 422.
- [ ] Con cuenta de pago + `payout_reference` → status `paid`, `paid_at` registrado.
- [ ] Crear otra comisión y anularla con razón → status `void`.

## Anulación por delete de renewal
- [ ] Crear renewal nuevo (commission `pending`).
- [ ] Borrar el renewal → commission queda `void` con `voided_reason = 'Renewal eliminado'`.

## UI
- [ ] Tab "Programa de Referidos" muestra KPIs y tabla.
- [ ] KPIs se actualizan tras aprobar/pagar.
- [ ] Captador (rol restringido) no ve la tab (o ve mensaje "Solo admin").

## Casos negativos
- [ ] Registrar con `?ref=codigo-inexistente` → registro funciona, sin referidor.
- [ ] Mismo usuario intenta autorreferirse → registra sin referidor.
```

- [ ] **Step 2: Commit**

```bash
git add docs/superpowers/smoke-tests/
git commit -m "docs: smoke test checklist for referral program"
```

---

## Self-Review (after writing all tasks)

**Spec coverage check:**
- [x] DB schema: Tasks 1-2 ✓
- [x] Referral code generation: Task 3 ✓
- [x] Referral attribution on registration: Task 4 + Task 12 ✓
- [x] Commission generation: Task 5 ✓
- [x] Payout account: Task 6 ✓
- [x] Referrer dashboard endpoints: Task 7 ✓
- [x] Super admin management: Task 8 + Task 11 ✓
- [x] Frontend types/API: Task 9 ✓
- [x] Frontend dashboard tab: Task 10 ✓
- [x] Register flow ref capture: Task 12 ✓
- [x] Smoke test: Task 13 ✓

**Type consistency:**
- `ReferralCommission.status` union same across backend constants, TS type, controller responses.
- `payout_account_type` enum matches: `'corriente' | 'ahorros' | 'cci'` in DB + TS.
- `commission_rate` is decimal(5,4) → string in JSON, parse with `parseFloat` in UI.

**Known gotcha addressed:** `commerce_renewal_id` FK is `nullable + nullOnDelete` (Task 1 Step 3 + Task 5 follow-up note) so that delete-renewal first triggers observer voiding and then nulls the FK — without losing the commission row.
