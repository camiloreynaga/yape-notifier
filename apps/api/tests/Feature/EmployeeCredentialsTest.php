<?php

namespace Tests\Feature;

use App\Models\Commerce;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Crypt;
use Illuminate\Support\Facades\Hash;
use Tests\TestCase;

class EmployeeCredentialsTest extends TestCase
{
    use RefreshDatabase;

    private function actingAdmin(): array
    {
        $owner = User::factory()->create(['role' => 'admin']);
        $commerce = Commerce::factory()->create([
            'owner_user_id' => $owner->id,
            'status' => 'active',
        ]);
        $owner->update(['commerce_id' => $commerce->id]);
        $this->actingAs($owner, 'sanctum');
        return [$owner, $commerce];
    }

    public function test_creating_captador_generates_pin_and_no_visible_password(): void
    {
        $this->actingAdmin();

        $response = $this->postJson('/api/users', [
            'name' => 'Cap Uno',
            'email' => 'cap.uno@test.com',
            'role' => 'captador',
        ]);

        $response->assertStatus(201)
            ->assertJsonPath('user.role', 'captador');
        $this->assertNotEmpty($response->json('user.pin'));

        $user = User::where('email', 'cap.uno@test.com')->first();
        $this->assertNotNull($user->pin);
        $this->assertNull($user->password_visible);
    }

    public function test_creating_admin_generates_visible_password_and_no_pin(): void
    {
        $this->actingAdmin();

        $response = $this->postJson('/api/users', [
            'name' => 'Admin Uno',
            'email' => 'admin.uno@test.com',
            'role' => 'admin',
        ]);

        $response->assertStatus(201)
            ->assertJsonPath('user.role', 'admin');

        $plain = $response->json('user.password');
        $this->assertNotEmpty($plain);

        $user = User::where('email', 'admin.uno@test.com')->first();
        $this->assertNull($user->pin);
        $this->assertNotNull($user->password_visible);
        $this->assertEquals($plain, Crypt::decryptString($user->password_visible));
        $this->assertTrue(Hash::check($plain, $user->password));
    }

    public function test_regenerate_password_for_admin_returns_new_usable_password(): void
    {
        [$owner, $commerce] = $this->actingAdmin();
        $employee = User::factory()->create([
            'role' => 'admin',
            'commerce_id' => $commerce->id,
            'pin' => null,
        ]);

        $response = $this->postJson("/api/users/{$employee->id}/regenerate-password");

        $response->assertOk();
        $plain = $response->json('password');
        $this->assertNotEmpty($plain);

        $employee->refresh();
        $this->assertTrue(Hash::check($plain, $employee->password));
        $this->assertEquals($plain, Crypt::decryptString($employee->password_visible));
    }

    public function test_regenerate_password_for_captador_is_rejected(): void
    {
        [$owner, $commerce] = $this->actingAdmin();
        $captador = User::factory()->create([
            'role' => 'captador',
            'commerce_id' => $commerce->id,
            'pin' => '1111',
        ]);

        $this->postJson("/api/users/{$captador->id}/regenerate-password")
            ->assertStatus(400);
    }

    public function test_regenerate_pin_for_admin_is_rejected(): void
    {
        [$owner, $commerce] = $this->actingAdmin();
        $employee = User::factory()->create([
            'role' => 'admin',
            'commerce_id' => $commerce->id,
            'pin' => null,
        ]);

        $this->postJson("/api/users/{$employee->id}/regenerate-pin")
            ->assertStatus(400);
    }

    public function test_index_returns_decrypted_password_for_admin_employees(): void
    {
        [$owner, $commerce] = $this->actingAdmin();

        $create = $this->postJson('/api/users', [
            'name' => 'Admin Visible',
            'email' => 'admin.visible@test.com',
            'role' => 'admin',
        ]);
        $plain = $create->json('user.password');

        $response = $this->getJson('/api/users');
        $response->assertOk();

        $row = collect($response->json('users'))
            ->firstWhere('email', 'admin.visible@test.com');

        $this->assertNotNull($row);
        $this->assertEquals($plain, $row['password_visible']);
    }

    public function test_index_returns_null_password_for_captadores(): void
    {
        [$owner, $commerce] = $this->actingAdmin();

        $this->postJson('/api/users', [
            'name' => 'Cap Visible',
            'email' => 'cap.visible@test.com',
            'role' => 'captador',
        ]);

        $response = $this->getJson('/api/users');
        $row = collect($response->json('users'))
            ->firstWhere('email', 'cap.visible@test.com');

        $this->assertNull($row['password_visible']);
    }
}
