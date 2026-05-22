<?php

namespace Database\Factories;

use App\Models\Device;
use App\Models\DeviceMonitoredApp;
use Illuminate\Database\Eloquent\Factories\Factory;

/**
 * @extends \Illuminate\Database\Eloquent\Factories\Factory<\App\Models\DeviceMonitoredApp>
 */
class DeviceMonitoredAppFactory extends Factory
{
    protected $model = DeviceMonitoredApp::class;

    /**
     * Define the model's default state.
     *
     * @return array<string, mixed>
     */
    public function definition(): array
    {
        return [
            'device_id' => Device::factory(),
            'package_name' => 'com.' . fake()->word() . '.' . fake()->word(),
            'enabled' => true,
        ];
    }

    /**
     * Indicate that the app is disabled.
     */
    public function disabled(): static
    {
        return $this->state(fn (array $attributes) => [
            'enabled' => false,
        ]);
    }
}

