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
}
