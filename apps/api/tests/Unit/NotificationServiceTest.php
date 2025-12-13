<?php

namespace Tests\Unit;

use App\Models\Device;
use App\Models\Notification;
use App\Models\User;
use App\Services\NotificationService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class NotificationServiceTest extends TestCase
{
    use RefreshDatabase;

    private NotificationService $service;

    protected function setUp(): void
    {
        parent::setUp();
        $this->service = new NotificationService;
    }

    public function test_it_creates_notification(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create(['user_id' => $user->id]);

        $data = [
            'source_app' => 'yape',
            'body' => 'Test notification',
            'amount' => 100.00,
            'currency' => 'PEN',
        ];

        $notification = $this->service->createNotification($data, $device);

        $this->assertInstanceOf(Notification::class, $notification);
        $this->assertEquals($user->id, $notification->user_id);
        $this->assertEquals($device->id, $notification->device_id);
        $this->assertEquals('yape', $notification->source_app);
    }

    public function test_it_detects_duplicate_notifications(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create(['user_id' => $user->id]);

        $data = [
            'source_app' => 'yape',
            'body' => 'Duplicate test',
            'received_at' => now()->toIso8601String(),
        ];

        // Create first notification
        $this->service->createNotification($data, $device);

        // Create duplicate
        $notification = $this->service->createNotification($data, $device);

        $this->assertTrue($notification->is_duplicate);
    }
}







