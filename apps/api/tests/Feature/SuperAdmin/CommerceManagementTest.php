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
