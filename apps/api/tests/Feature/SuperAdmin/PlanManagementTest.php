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
