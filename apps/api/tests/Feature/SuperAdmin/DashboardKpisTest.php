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
