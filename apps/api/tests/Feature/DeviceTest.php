<?php

namespace Tests\Feature;

use App\Models\Device;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class DeviceTest extends TestCase
{
    use RefreshDatabase;

    private function getAuthToken(User $user): string
    {
        return $user->createToken('test-token')->plainTextToken;
    }

    public function test_user_can_create_device(): void
    {
        $user = User::factory()->create();
        $token = $this->getAuthToken($user);

        $response = $this->withHeader('Authorization', "Bearer $token")
            ->postJson('/api/devices', [
                'name' => 'Mi Dispositivo',
                'platform' => 'android',
            ]);

        $response->assertStatus(201)
            ->assertJsonStructure([
                'message',
                'device' => ['id', 'uuid', 'name', 'platform', 'is_active'],
            ]);

        $this->assertDatabaseHas('devices', [
            'user_id' => $user->id,
            'name' => 'Mi Dispositivo',
        ]);
    }

    public function test_user_can_list_devices(): void
    {
        $user = User::factory()->create();
        Device::factory()->count(3)->create(['user_id' => $user->id]);
        $token = $this->getAuthToken($user);

        $response = $this->withHeader('Authorization', "Bearer $token")
            ->getJson('/api/devices');

        $response->assertStatus(200)
            ->assertJsonStructure([
                'devices' => [
                    '*' => ['id', 'uuid', 'name', 'platform', 'is_active'],
                ],
            ]);

        $this->assertCount(3, $response->json('devices'));
    }

    public function test_user_can_update_device(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create(['user_id' => $user->id]);
        $token = $this->getAuthToken($user);

        $response = $this->withHeader('Authorization', "Bearer $token")
            ->putJson("/api/devices/{$device->id}", [
                'name' => 'Dispositivo Actualizado',
            ]);

        $response->assertStatus(200);
        $this->assertDatabaseHas('devices', [
            'id' => $device->id,
            'name' => 'Dispositivo Actualizado',
        ]);
    }

    public function test_user_can_delete_device(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create(['user_id' => $user->id]);
        $token = $this->getAuthToken($user);

        $response = $this->withHeader('Authorization', "Bearer $token")
            ->deleteJson("/api/devices/{$device->id}");

        $response->assertStatus(200);
        $this->assertDatabaseMissing('devices', ['id' => $device->id]);
    }
}

