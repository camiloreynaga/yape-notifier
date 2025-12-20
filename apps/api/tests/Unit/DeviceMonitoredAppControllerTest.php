<?php

namespace Tests\Unit;

use App\Models\Commerce;
use App\Models\Device;
use App\Models\DeviceMonitoredApp;
use App\Models\MonitorPackage;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class DeviceMonitoredAppControllerTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
    }

    public function test_index_returns_monitored_apps_for_device(): void
    {
        $user = User::factory()->create();
        $commerce = Commerce::factory()->create(['owner_user_id' => $user->id]);
        $user->update(['commerce_id' => $commerce->id]);
        
        $device = Device::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce->id,
        ]);

        $monitorPackage = MonitorPackage::factory()->create([
            'commerce_id' => $commerce->id,
            'package_name' => 'com.bcp.innovacxion.yapeapp',
        ]);

        $monitoredApp = DeviceMonitoredApp::factory()->create([
            'device_id' => $device->id,
            'package_name' => 'com.bcp.innovacxion.yapeapp',
        ]);

        Sanctum::actingAs($user);

        $response = $this->getJson("/api/devices/{$device->id}/monitored-apps");

        $response->assertStatus(200)
            ->assertJsonStructure([
                'monitored_apps' => [
                    '*' => [
                        'id',
                        'device_id',
                        'package_name',
                        'enabled',
                        'monitor_package',
                    ],
                ],
            ]);

        $this->assertCount(1, $response->json('monitored_apps'));
        $this->assertEquals('com.bcp.innovacxion.yapeapp', $response->json('monitored_apps.0.package_name'));
    }

    public function test_index_returns_404_for_nonexistent_device(): void
    {
        $user = User::factory()->create();
        Sanctum::actingAs($user);

        $response = $this->getJson('/api/devices/999/monitored-apps');

        $response->assertStatus(404);
    }

    public function test_index_returns_403_for_device_from_different_user(): void
    {
        $user1 = User::factory()->create();
        $user2 = User::factory()->create();
        
        $device = Device::factory()->create(['user_id' => $user2->id]);

        Sanctum::actingAs($user1);

        $response = $this->getJson("/api/devices/{$device->id}/monitored-apps");

        $response->assertStatus(404);
    }

    public function test_store_syncs_monitored_apps(): void
    {
        $user = User::factory()->create();
        $commerce = Commerce::factory()->create(['owner_user_id' => $user->id]);
        $user->update(['commerce_id' => $commerce->id]);
        
        $device = Device::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce->id,
        ]);

        $package1 = MonitorPackage::factory()->create([
            'commerce_id' => $commerce->id,
            'package_name' => 'com.bcp.innovacxion.yapeapp',
        ]);

        $package2 = MonitorPackage::factory()->create([
            'commerce_id' => $commerce->id,
            'package_name' => 'com.bcp.innovacxion.plinapp',
        ]);

        Sanctum::actingAs($user);

        $response = $this->postJson("/api/devices/{$device->id}/monitored-apps", [
            'package_names' => [
                'com.bcp.innovacxion.yapeapp',
                'com.bcp.innovacxion.plinapp',
            ],
        ]);

        $response->assertStatus(200)
            ->assertJsonStructure([
                'message',
                'monitored_apps',
            ]);

        $this->assertCount(2, DeviceMonitoredApp::where('device_id', $device->id)->get());
    }

    public function test_store_removes_apps_not_in_list(): void
    {
        $user = User::factory()->create();
        $commerce = Commerce::factory()->create(['owner_user_id' => $user->id]);
        $user->update(['commerce_id' => $commerce->id]);
        
        $device = Device::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce->id,
        ]);

        $package1 = MonitorPackage::factory()->create([
            'commerce_id' => $commerce->id,
            'package_name' => 'com.bcp.innovacxion.yapeapp',
        ]);

        $package2 = MonitorPackage::factory()->create([
            'commerce_id' => $commerce->id,
            'package_name' => 'com.bcp.innovacxion.plinapp',
        ]);

        // Create existing monitored app
        DeviceMonitoredApp::factory()->create([
            'device_id' => $device->id,
            'package_name' => 'com.bcp.innovacxion.yapeapp',
        ]);

        DeviceMonitoredApp::factory()->create([
            'device_id' => $device->id,
            'package_name' => 'com.bcp.innovacxion.plinapp',
        ]);

        Sanctum::actingAs($user);

        // Sync with only one package
        $response = $this->postJson("/api/devices/{$device->id}/monitored-apps", [
            'package_names' => [
                'com.bcp.innovacxion.yapeapp',
            ],
        ]);

        $response->assertStatus(200);

        // Should only have one monitored app now
        $this->assertCount(1, DeviceMonitoredApp::where('device_id', $device->id)->get());
        $this->assertEquals(
            'com.bcp.innovacxion.yapeapp',
            DeviceMonitoredApp::where('device_id', $device->id)->first()->package_name
        );
    }

    public function test_store_returns_error_for_invalid_package(): void
    {
        $user = User::factory()->create();
        $commerce = Commerce::factory()->create(['owner_user_id' => $user->id]);
        $user->update(['commerce_id' => $commerce->id]);
        
        $device = Device::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce->id,
        ]);

        Sanctum::actingAs($user);

        $response = $this->postJson("/api/devices/{$device->id}/monitored-apps", [
            'package_names' => [
                'com.invalid.package',
            ],
        ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['package_names.0']);
    }

    public function test_store_returns_error_for_package_from_different_commerce(): void
    {
        $user = User::factory()->create();
        $commerce1 = Commerce::factory()->create(['owner_user_id' => $user->id]);
        $commerce2 = Commerce::factory()->create();
        $user->update(['commerce_id' => $commerce1->id]);
        
        $device = Device::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce1->id,
        ]);

        // Create package in different commerce
        MonitorPackage::factory()->create([
            'commerce_id' => $commerce2->id,
            'package_name' => 'com.bcp.innovacxion.yapeapp',
        ]);

        Sanctum::actingAs($user);

        $response = $this->postJson("/api/devices/{$device->id}/monitored-apps", [
            'package_names' => [
                'com.bcp.innovacxion.yapeapp',
            ],
        ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['package_names.0']);
    }

    public function test_store_returns_error_for_inactive_package(): void
    {
        $user = User::factory()->create();
        $commerce = Commerce::factory()->create(['owner_user_id' => $user->id]);
        $user->update(['commerce_id' => $commerce->id]);
        
        $device = Device::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce->id,
        ]);

        // Create inactive package
        MonitorPackage::factory()->inactive()->create([
            'commerce_id' => $commerce->id,
            'package_name' => 'com.bcp.innovacxion.yapeapp',
        ]);

        Sanctum::actingAs($user);

        $response = $this->postJson("/api/devices/{$device->id}/monitored-apps", [
            'package_names' => [
                'com.bcp.innovacxion.yapeapp',
            ],
        ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['package_names.0']);
    }

    public function test_store_returns_error_when_device_has_no_commerce(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => null,
        ]);

        Sanctum::actingAs($user);

        $response = $this->postJson("/api/devices/{$device->id}/monitored-apps", [
            'package_names' => [
                'com.bcp.innovacxion.yapeapp',
            ],
        ]);

        $response->assertStatus(400)
            ->assertJson([
                'message' => 'El dispositivo debe estar vinculado a un negocio',
            ]);
    }

    public function test_store_validates_required_fields(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create(['user_id' => $user->id]);

        Sanctum::actingAs($user);

        $response = $this->postJson("/api/devices/{$device->id}/monitored-apps", []);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['package_names']);
    }

    public function test_store_validates_minimum_packages(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create(['user_id' => $user->id]);

        Sanctum::actingAs($user);

        $response = $this->postJson("/api/devices/{$device->id}/monitored-apps", [
            'package_names' => [],
        ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['package_names']);
    }
}

