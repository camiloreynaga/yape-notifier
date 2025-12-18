<?php

namespace Tests\Unit;

use App\Models\AppInstance;
use App\Models\Commerce;
use App\Models\Device;
use App\Models\User;
use App\Services\AppInstanceService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class AppInstanceServiceTest extends TestCase
{
    use RefreshDatabase;

    private AppInstanceService $service;

    protected function setUp(): void
    {
        parent::setUp();
        $this->service = new AppInstanceService();
    }

    public function test_find_or_create_app_instance(): void
    {
        $user = User::factory()->create();
        $commerce = Commerce::factory()->create(['owner_user_id' => $user->id]);
        $user->update(['commerce_id' => $commerce->id]);
        $device = Device::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce->id,
        ]);

        $instance = $this->service->findOrCreate(
            $device,
            'com.bcp.innovacxion.yapeapp',
            10,
            'Yape 1 (Rocío)'
        );

        $this->assertInstanceOf(AppInstance::class, $instance);
        $this->assertEquals($commerce->id, $instance->commerce_id);
        $this->assertEquals($device->id, $instance->device_id);
        $this->assertEquals('com.bcp.innovacxion.yapeapp', $instance->package_name);
        $this->assertEquals(10, $instance->android_user_id);
        $this->assertEquals('Yape 1 (Rocío)', $instance->instance_label);
    }

    public function test_find_or_create_returns_existing_instance(): void
    {
        $user = User::factory()->create();
        $commerce = Commerce::factory()->create(['owner_user_id' => $user->id]);
        $user->update(['commerce_id' => $commerce->id]);
        $device = Device::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce->id,
        ]);

        $instance1 = $this->service->findOrCreate(
            $device,
            'com.bcp.innovacxion.yapeapp',
            10,
            'Yape 1 (Rocío)'
        );

        $instance2 = $this->service->findOrCreate(
            $device,
            'com.bcp.innovacxion.yapeapp',
            10,
            'Yape 2 (Pamela)'
        );

        $this->assertEquals($instance1->id, $instance2->id);
        $this->assertEquals('Yape 1 (Rocío)', $instance1->instance_label);
    }

    public function test_update_label(): void
    {
        $user = User::factory()->create();
        $commerce = Commerce::factory()->create(['owner_user_id' => $user->id]);
        $user->update(['commerce_id' => $commerce->id]);
        $device = Device::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce->id,
        ]);

        $instance = AppInstance::factory()->create([
            'commerce_id' => $commerce->id,
            'device_id' => $device->id,
            'instance_label' => 'Old Label',
        ]);

        $updated = $this->service->updateLabel($instance, 'New Label');

        $this->assertEquals('New Label', $updated->instance_label);
    }

    public function test_get_device_instances(): void
    {
        $user = User::factory()->create();
        $commerce = Commerce::factory()->create(['owner_user_id' => $user->id]);
        $user->update(['commerce_id' => $commerce->id]);
        $device = Device::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce->id,
        ]);

        AppInstance::factory()->count(3)->create([
            'commerce_id' => $commerce->id,
            'device_id' => $device->id,
        ]);

        $instances = $this->service->getDeviceInstances($device);

        $this->assertCount(3, $instances);
    }
}


