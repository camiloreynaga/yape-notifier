<?php

namespace Tests\Unit;

use App\Models\Commerce;
use App\Models\Device;
use App\Models\Notification;
use App\Models\User;
use App\Services\AppInstanceService;
use App\Services\NotificationService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class NotificationServiceDualAppsTest extends TestCase
{
    use RefreshDatabase;

    private NotificationService $service;

    protected function setUp(): void
    {
        parent::setUp();
        $this->service = new NotificationService(new AppInstanceService());
    }

    public function test_create_notification_with_dual_app_identifiers(): void
    {
        $user = User::factory()->create();
        $commerce = Commerce::factory()->create(['owner_user_id' => $user->id]);
        $user->update(['commerce_id' => $commerce->id]);
        $device = Device::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce->id,
        ]);

        $data = [
            'device_id' => $device->uuid,
            'source_app' => 'yape',
            'package_name' => 'com.bcp.innovacxion.yapeapp',
            'android_user_id' => 10,
            'android_uid' => 10100,
            'title' => 'Pago recibido',
            'body' => 'Juan te yapeó S/ 50.00',
            'amount' => 50.00,
            'currency' => 'PEN',
            'payer_name' => 'Juan',
            'posted_at' => now()->toIso8601String(),
            'received_at' => now()->toIso8601String(),
        ];

        $notification = $this->service->createNotification($data, $device);

        $this->assertInstanceOf(Notification::class, $notification);
        $this->assertEquals('com.bcp.innovacxion.yapeapp', $notification->package_name);
        $this->assertEquals(10, $notification->android_user_id);
        $this->assertEquals(10100, $notification->android_uid);
        $this->assertNotNull($notification->app_instance_id);
        $this->assertEquals($commerce->id, $notification->commerce_id);
    }

    public function test_duplicate_detection_uses_dual_app_identifiers(): void
    {
        $user = User::factory()->create();
        $commerce = Commerce::factory()->create(['owner_user_id' => $user->id]);
        $user->update(['commerce_id' => $commerce->id]);
        $device = Device::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce->id,
        ]);

        $data = [
            'device_id' => $device->uuid,
            'source_app' => 'yape',
            'package_name' => 'com.bcp.innovacxion.yapeapp',
            'android_user_id' => 10,
            'title' => 'Pago recibido',
            'body' => 'Juan te yapeó S/ 50.00',
            'posted_at' => now()->toIso8601String(),
            'received_at' => now()->toIso8601String(),
        ];

        $notification1 = $this->service->createNotification($data, $device);
        $this->assertFalse($notification1->is_duplicate);

        // Same notification within 5 seconds should be duplicate
        $notification2 = $this->service->createNotification($data, $device);
        $this->assertTrue($notification2->is_duplicate);
    }

    public function test_different_android_user_ids_create_different_instances(): void
    {
        $user = User::factory()->create();
        $commerce = Commerce::factory()->create(['owner_user_id' => $user->id]);
        $user->update(['commerce_id' => $commerce->id]);
        $device = Device::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce->id,
        ]);

        $data1 = [
            'device_id' => $device->uuid,
            'source_app' => 'yape',
            'package_name' => 'com.bcp.innovacxion.yapeapp',
            'android_user_id' => 10,
            'body' => 'Juan te yapeó S/ 50.00',
            'posted_at' => now()->toIso8601String(),
            'received_at' => now()->toIso8601String(),
        ];

        $data2 = [
            'device_id' => $device->uuid,
            'source_app' => 'yape',
            'package_name' => 'com.bcp.innovacxion.yapeapp',
            'android_user_id' => 11,
            'body' => 'Pamela te yapeó S/ 30.00',
            'posted_at' => now()->toIso8601String(),
            'received_at' => now()->toIso8601String(),
        ];

        $notification1 = $this->service->createNotification($data1, $device);
        $notification2 = $this->service->createNotification($data2, $device);

        $this->assertNotEquals($notification1->app_instance_id, $notification2->app_instance_id);
    }
}



