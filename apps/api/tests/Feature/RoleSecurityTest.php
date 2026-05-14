<?php

namespace Tests\Feature;

use App\Models\Commerce;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class RoleSecurityTest extends TestCase
{
    use RefreshDatabase;

    /**
     * Create an active commerce with an owner of the given role.
     */
    private function commerceWithOwner(string $role): array
    {
        $owner = User::factory()->create(['role' => $role]);
        $commerce = Commerce::factory()->create([
            'owner_user_id' => $owner->id,
            'status' => 'active',
        ]);
        $owner->update(['commerce_id' => $commerce->id]);
        return [$owner, $commerce];
    }

    public function test_captador_cannot_list_employees(): void
    {
        [$admin, $commerce] = $this->commerceWithOwner('admin');
        $captador = User::factory()->create([
            'role' => 'captador',
            'commerce_id' => $commerce->id,
        ]);

        $this->actingAs($captador, 'sanctum');
        $this->getJson('/api/users')->assertStatus(403);
    }

    public function test_captador_cannot_create_employees(): void
    {
        [$admin, $commerce] = $this->commerceWithOwner('admin');
        $captador = User::factory()->create([
            'role' => 'captador',
            'commerce_id' => $commerce->id,
        ]);

        $this->actingAs($captador, 'sanctum');
        $this->postJson('/api/users', [
            'name' => 'Hack Admin',
            'email' => 'hack@test.com',
            'role' => 'admin',
        ])->assertStatus(403);
    }

    public function test_admin_can_list_employees(): void
    {
        [$admin] = $this->commerceWithOwner('admin');
        $this->actingAs($admin, 'sanctum');
        $this->getJson('/api/users')->assertOk();
    }
}
