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

    public function test_system_role_is_rejected_on_create(): void
    {
        [$admin] = $this->commerceWithOwner('admin');
        $this->actingAs($admin, 'sanctum');

        $this->postJson('/api/users', [
            'name' => 'Sys User',
            'email' => 'sys@test.com',
            'role' => 'system',
        ])->assertStatus(422)
          ->assertJsonValidationErrors('role');
    }

    public function test_admin_and_captador_roles_are_accepted_on_create(): void
    {
        [$admin] = $this->commerceWithOwner('admin');
        $this->actingAs($admin, 'sanctum');

        $this->postJson('/api/users', [
            'name' => 'Captador Uno',
            'email' => 'cap1@test.com',
            'role' => 'captador',
        ])->assertStatus(201);

        $this->postJson('/api/users', [
            'name' => 'Admin Dos',
            'email' => 'adm2@test.com',
            'role' => 'admin',
        ])->assertStatus(201);
    }

    public function test_admin_cannot_login_with_pin(): void
    {
        [$admin, $commerce] = $this->commerceWithOwner('admin');
        // give the admin a PIN directly (legacy data scenario)
        $admin->update(['pin' => '4321', 'is_active' => true]);

        $this->postJson('/api/auth/login-pin', ['pin' => '4321'])
            ->assertStatus(403);
    }

    public function test_captador_can_login_with_pin(): void
    {
        [$admin, $commerce] = $this->commerceWithOwner('admin');
        $captador = User::factory()->create([
            'role' => 'captador',
            'commerce_id' => $commerce->id,
            'pin' => '8765',
            'is_active' => true,
        ]);

        $this->postJson('/api/auth/login-pin', ['pin' => '8765'])
            ->assertOk()
            ->assertJsonPath('user.role', 'captador');
    }
}
