<?php
namespace Tests\Feature\Referral;

use App\Models\Commerce;
use App\Models\CommerceRenewal;
use App\Models\Plan;
use App\Models\ReferralCommission;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class ReferralEndpointsTest extends TestCase
{
    use RefreshDatabase;

    private function makeAdminWithCommerce(): array
    {
        $admin = User::factory()->create(['role' => 'admin']);
        $commerce = Commerce::create([
            'name' => 'Karol',
            'owner_user_id' => $admin->id,
            'status' => 'active',
            'referral_code' => 'karol-eeee',
        ]);
        $admin->update(['commerce_id' => $commerce->id]);
        return [$admin->fresh(), $commerce];
    }

    public function test_stats_aggregates_correctly(): void
    {
        [$admin, $referrer] = $this->makeAdminWithCommerce();
        $u = User::factory()->create();
        $referred = Commerce::create([
            'name' => 'Pepe',
            'owner_user_id' => $u->id,
            'status' => 'active',
            'referral_code' => 'pepe-eeee',
            'referred_by_commerce_id' => $referrer->id,
        ]);

        // Two commissions: one pending, one paid
        ReferralCommission::create([
            'referrer_commerce_id' => $referrer->id,
            'referred_commerce_id' => $referred->id,
            'commerce_renewal_id' => null,
            'base_amount' => 49, 'commission_rate' => 0.2, 'amount' => 9.80,
            'status' => 'pending',
        ]);
        ReferralCommission::create([
            'referrer_commerce_id' => $referrer->id,
            'referred_commerce_id' => $referred->id,
            'commerce_renewal_id' => null,
            'base_amount' => 49, 'commission_rate' => 0.2, 'amount' => 9.80,
            'status' => 'paid',
        ]);

        Sanctum::actingAs($admin);
        $response = $this->getJson('/api/referrals/stats');
        $response->assertOk()
            ->assertJsonStructure(['month_earnings', 'pending_balance', 'lifetime_paid', 'active_referrals_count', 'referral_code'])
            ->assertJsonPath('referral_code', 'karol-eeee')
            ->assertJsonPath('active_referrals_count', 1);
        $this->assertEqualsWithDelta(19.60, (float) $response->json('month_earnings'), 0.01);
        $this->assertEqualsWithDelta(9.80, (float) $response->json('pending_balance'), 0.01);
        $this->assertEqualsWithDelta(9.80, (float) $response->json('lifetime_paid'), 0.01);
    }

    public function test_referrals_endpoint_lists_only_own_referrals(): void
    {
        [$admin, $referrer] = $this->makeAdminWithCommerce();
        $u1 = User::factory()->create();
        $u2 = User::factory()->create();
        Commerce::create(['name' => 'Mine', 'owner_user_id' => $u1->id, 'status' => 'active', 'referral_code' => 'mine-eeee', 'referred_by_commerce_id' => $referrer->id]);
        Commerce::create(['name' => 'NotMine', 'owner_user_id' => $u2->id, 'status' => 'active', 'referral_code' => 'notm-eeee']);

        Sanctum::actingAs($admin);
        $response = $this->getJson('/api/referrals/referrals');
        $response->assertOk();
        $names = array_column($response->json('data'), 'name');
        $this->assertContains('Mine', $names);
        $this->assertNotContains('NotMine', $names);
    }

    public function test_referrals_endpoint_includes_total_paid_aggregate(): void
    {
        [$admin, $referrer] = $this->makeAdminWithCommerce();
        $u = User::factory()->create();
        $referred = Commerce::create(['name' => 'X', 'owner_user_id' => $u->id, 'status' => 'active', 'referral_code' => 'x-eeee', 'referred_by_commerce_id' => $referrer->id]);

        $plan = Plan::factory()->create(['price' => 49]);
        $sa = User::factory()->create(['role' => 'super_admin']);
        CommerceRenewal::create([
            'commerce_id' => $referred->id, 'plan_id' => $plan->id,
            'renewed_by_user_id' => $sa->id, 'new_expires_at' => now()->addMonth(),
            'amount_paid' => 49.00,
        ]);
        CommerceRenewal::create([
            'commerce_id' => $referred->id, 'plan_id' => $plan->id,
            'renewed_by_user_id' => $sa->id, 'new_expires_at' => now()->addMonths(2),
            'amount_paid' => 49.00,
        ]);

        Sanctum::actingAs($admin);
        $response = $this->getJson('/api/referrals/referrals');
        $row = collect($response->json('data'))->firstWhere('name', 'X');
        $this->assertNotNull($row);
        $this->assertEqualsWithDelta(98.00, (float) $row['total_paid'], 0.01);
    }

    public function test_commissions_endpoint_paginated_and_filterable(): void
    {
        [$admin, $referrer] = $this->makeAdminWithCommerce();
        $u = User::factory()->create();
        $referred = Commerce::create(['name' => 'P', 'owner_user_id' => $u->id, 'status' => 'active', 'referral_code' => 'p-eeee', 'referred_by_commerce_id' => $referrer->id]);

        ReferralCommission::create([
            'referrer_commerce_id' => $referrer->id, 'referred_commerce_id' => $referred->id, 'commerce_renewal_id' => null,
            'base_amount' => 49, 'commission_rate' => 0.2, 'amount' => 9.80, 'status' => 'pending',
        ]);
        ReferralCommission::create([
            'referrer_commerce_id' => $referrer->id, 'referred_commerce_id' => $referred->id, 'commerce_renewal_id' => null,
            'base_amount' => 49, 'commission_rate' => 0.2, 'amount' => 9.80, 'status' => 'paid',
        ]);

        Sanctum::actingAs($admin);

        // unfiltered
        $all = $this->getJson('/api/referrals/commissions');
        $all->assertOk();
        $this->assertCount(2, $all->json('data'));

        // filtered by status=paid
        $paid = $this->getJson('/api/referrals/commissions?status=paid');
        $paid->assertOk();
        $this->assertCount(1, $paid->json('data'));
        $this->assertSame('paid', $paid->json('data.0.status'));
    }

    public function test_endpoints_require_admin_commerce(): void
    {
        // captador not authorized
        $admin = User::factory()->create(['role' => 'admin']);
        $commerce = Commerce::create(['name' => 'C', 'owner_user_id' => $admin->id, 'status' => 'active', 'referral_code' => 'c-eeff']);
        $captador = User::factory()->create(['role' => 'captador', 'commerce_id' => $commerce->id]);
        Sanctum::actingAs($captador);

        $this->getJson('/api/referrals/stats')->assertStatus(403);
        $this->getJson('/api/referrals/referrals')->assertStatus(403);
        $this->getJson('/api/referrals/commissions')->assertStatus(403);
    }
}
