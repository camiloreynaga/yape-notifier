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

        // Use the same exact timestamp for both notifications to ensure they're within the 5-second window
        $timestamp = now();
        $timestampString = $timestamp->toIso8601String();

        $data = [
            'source_app' => 'yape',
            'body' => 'Duplicate test',
            'received_at' => $timestampString,
        ];

        // Create first notification
        $firstNotification = $this->service->createNotification($data, $device);

        // Wait a tiny bit to ensure different received_at but still within 5-second window
        // Actually, use the exact same timestamp string to ensure they match
        $data2 = [
            'source_app' => 'yape',
            'body' => 'Duplicate test',
            'received_at' => $timestampString, // Same exact timestamp
        ];

        // Create duplicate with the same timestamp (within 5-second window)
        $notification = $this->service->createNotification($data2, $device);

        $this->assertTrue($notification->is_duplicate, 'Second notification should be marked as duplicate');
    }

    public function test_it_filters_notifications_by_package_name(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create(['user_id' => $user->id]);

        // Create notifications with different package names
        $this->service->createNotification([
            'source_app' => 'yape',
            'body' => 'Notification 1',
            'package_name' => 'com.bcp.innovacxion.yapeapp',
        ], $device);

        $this->service->createNotification([
            'source_app' => 'yape',
            'body' => 'Notification 2',
            'package_name' => 'com.bcp.innovacxion.plinapp',
        ], $device);

        $this->service->createNotification([
            'source_app' => 'yape',
            'body' => 'Notification 3',
            'package_name' => 'com.bcp.innovacxion.yapeapp',
        ], $device);

        // Filter by package_name
        $query = $this->service->getUserNotifications($user, [
            'package_name' => 'com.bcp.innovacxion.yapeapp',
        ]);

        $notifications = $query->get();

        $this->assertCount(2, $notifications);
        foreach ($notifications as $notification) {
            $this->assertEquals('com.bcp.innovacxion.yapeapp', $notification->package_name);
        }
    }

    public function test_it_filters_notifications_by_package_name_returns_empty_when_no_match(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create(['user_id' => $user->id]);

        // Create notification with package_name
        $this->service->createNotification([
            'source_app' => 'yape',
            'body' => 'Notification 1',
            'package_name' => 'com.bcp.innovacxion.yapeapp',
        ], $device);

        // Filter by different package_name
        $query = $this->service->getUserNotifications($user, [
            'package_name' => 'com.nonexistent.package',
        ]);

        $notifications = $query->get();

        $this->assertCount(0, $notifications);
    }

    public function test_it_filters_notifications_by_package_name_with_null_values(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create(['user_id' => $user->id]);

        // Create notification without package_name
        $this->service->createNotification([
            'source_app' => 'yape',
            'body' => 'Notification without package',
        ], $device);

        // Create notification with package_name
        $this->service->createNotification([
            'source_app' => 'yape',
            'body' => 'Notification with package',
            'package_name' => 'com.bcp.innovacxion.yapeapp',
        ], $device);

        // Filter by package_name should only return the one with package_name
        $query = $this->service->getUserNotifications($user, [
            'package_name' => 'com.bcp.innovacxion.yapeapp',
        ]);

        $notifications = $query->get();

        $this->assertCount(1, $notifications);
        $this->assertEquals('com.bcp.innovacxion.yapeapp', $notifications->first()->package_name);
    }
}









