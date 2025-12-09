<?php

namespace Tests\Feature;

use App\Models\Device;
use App\Models\Notification;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class NotificationTest extends TestCase
{
    use RefreshDatabase;

    private function getAuthToken(User $user): string
    {
        return $user->createToken('test-token')->plainTextToken;
    }

    public function test_user_can_create_notification(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create(['user_id' => $user->id]);
        $token = $this->getAuthToken($user);

        $response = $this->withHeader('Authorization', "Bearer $token")
            ->postJson('/api/notifications', [
                'device_id' => $device->uuid,
                'source_app' => 'yape',
                'title' => 'Pago recibido',
                'body' => 'Recibiste S/ 150.00 de Juan Pérez',
                'amount' => 150.00,
                'currency' => 'PEN',
                'payer_name' => 'Juan Pérez',
            ]);

        $response->assertStatus(201)
            ->assertJsonStructure([
                'message',
                'notification' => [
                    'id',
                    'source_app',
                    'title',
                    'body',
                    'amount',
                    'currency',
                ],
            ]);

        $this->assertDatabaseHas('notifications', [
            'user_id' => $user->id,
            'device_id' => $device->id,
            'source_app' => 'yape',
        ]);
    }

    public function test_user_can_list_notifications(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create(['user_id' => $user->id]);
        Notification::factory()->count(5)->create([
            'user_id' => $user->id,
            'device_id' => $device->id,
        ]);
        $token = $this->getAuthToken($user);

        $response = $this->withHeader('Authorization', "Bearer $token")
            ->getJson('/api/notifications');

        $response->assertStatus(200)
            ->assertJsonStructure([
                'data' => [
                    '*' => [
                        'id',
                        'source_app',
                        'title',
                        'body',
                        'amount',
                    ],
                ],
            ]);
    }

    public function test_duplicate_notification_is_detected(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create(['user_id' => $user->id]);
        $token = $this->getAuthToken($user);

        $notificationData = [
            'device_id' => $device->uuid,
            'source_app' => 'yape',
            'body' => 'Recibiste S/ 150.00',
            'received_at' => now()->toIso8601String(),
        ];

        // Create first notification
        $this->withHeader('Authorization', "Bearer $token")
            ->postJson('/api/notifications', $notificationData);

        // Try to create duplicate
        $response = $this->withHeader('Authorization', "Bearer $token")
            ->postJson('/api/notifications', $notificationData);

        $response->assertStatus(201);
        $this->assertTrue($response->json('notification.is_duplicate'));
    }

    public function test_user_can_get_notification_statistics(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create(['user_id' => $user->id]);
        Notification::factory()->count(10)->create([
            'user_id' => $user->id,
            'device_id' => $device->id,
            'amount' => 100.00,
            'is_duplicate' => false,
        ]);
        $token = $this->getAuthToken($user);

        $response = $this->withHeader('Authorization', "Bearer $token")
            ->getJson('/api/notifications/statistics');

        $response->assertStatus(200)
            ->assertJsonStructure([
                'total',
                'total_amount',
                'by_source_app',
                'by_device',
                'by_date',
                'by_status',
                'duplicates',
            ]);
    }
}
