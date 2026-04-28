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
}
