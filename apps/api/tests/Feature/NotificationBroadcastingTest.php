<?php

namespace Tests\Feature;

use App\Events\NotificationCreated;
use App\Models\AppInstance;
use App\Models\Commerce;
use App\Models\Device;
use App\Models\Notification;
use App\Models\User;
use Illuminate\Broadcasting\PrivateChannel;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Event;
use Tests\TestCase;

class NotificationBroadcastingTest extends TestCase
{
    use RefreshDatabase;

    public function test_notification_created_event_is_broadcasted(): void
    {
        Event::fake();

        $commerce = Commerce::factory()->create();
        $user = User::factory()->create(['commerce_id' => $commerce->id]);
        $device = Device::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce->id,
        ]);

        $notification = Notification::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce->id,
            'device_id' => $device->id,
        ]);

        broadcast(new NotificationCreated($notification));

        Event::assertDispatched(NotificationCreated::class);
    }

    public function test_notification_is_broadcasted_to_correct_channel(): void
    {
        $commerce = Commerce::factory()->create();
        $user = User::factory()->create(['commerce_id' => $commerce->id]);
        $device = Device::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce->id,
        ]);

        $notification = Notification::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce->id,
            'device_id' => $device->id,
        ]);

        $event = new NotificationCreated($notification);
        $channels = $event->broadcastOn();

        $this->assertCount(1, $channels);
        $this->assertInstanceOf(PrivateChannel::class, $channels[0]);
        $this->assertEquals('commerce.'.$commerce->id, $channels[0]->name);
    }

    public function test_notification_broadcast_includes_required_data(): void
    {
        $commerce = Commerce::factory()->create();
        $user = User::factory()->create(['commerce_id' => $commerce->id]);
        $device = Device::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce->id,
            'alias' => 'Mi Dispositivo',
        ]);

        $appInstance = AppInstance::factory()->create([
            'commerce_id' => $commerce->id,
            'device_id' => $device->id,
            'instance_label' => 'Yape Principal',
        ]);

        $notification = Notification::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce->id,
            'device_id' => $device->id,
            'app_instance_id' => $appInstance->id,
            'amount' => 150.50,
            'currency' => 'PEN',
        ]);

        // Load relationships
        $notification->load(['appInstance', 'device']);

        $event = new NotificationCreated($notification);
        $data = $event->broadcastWith();

        $this->assertArrayHasKey('id', $data);
        $this->assertArrayHasKey('commerce_id', $data);
        $this->assertArrayHasKey('device_id', $data);
        $this->assertArrayHasKey('amount', $data);
        $this->assertArrayHasKey('currency', $data);
        $this->assertArrayHasKey('app_instance_label', $data);
        $this->assertArrayHasKey('device_alias', $data);
        $this->assertEquals('Yape Principal', $data['app_instance_label']);
        $this->assertEquals('Mi Dispositivo', $data['device_alias']);
        $this->assertEquals(150.50, $data['amount']);
        $this->assertEquals('PEN', $data['currency']);
    }

    public function test_notification_broadcast_event_name_is_correct(): void
    {
        $commerce = Commerce::factory()->create();
        $user = User::factory()->create(['commerce_id' => $commerce->id]);
        $device = Device::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce->id,
        ]);

        $notification = Notification::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce->id,
            'device_id' => $device->id,
        ]);

        $event = new NotificationCreated($notification);

        $this->assertEquals('notification.created', $event->broadcastAs());
    }

    public function test_notification_broadcast_handles_null_relationships(): void
    {
        $commerce = Commerce::factory()->create();
        $user = User::factory()->create(['commerce_id' => $commerce->id]);
        $device = Device::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce->id,
            'alias' => null,
        ]);

        $notification = Notification::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce->id,
            'device_id' => $device->id,
            'app_instance_id' => null,
        ]);

        // Load relationships (device will be loaded, appInstance will be null)
        $notification->load(['appInstance', 'device']);

        $event = new NotificationCreated($notification);
        $data = $event->broadcastWith();

        $this->assertNull($data['app_instance_label']);
        $this->assertNull($data['device_alias']);
    }
}



