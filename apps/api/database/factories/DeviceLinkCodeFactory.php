<?php

namespace Database\Factories;

use App\Models\Commerce;
use App\Models\Device;
use App\Models\DeviceLinkCode;
use Illuminate\Database\Eloquent\Factories\Factory;

/**
 * @extends \Illuminate\Database\Eloquent\Factories\Factory<\App\Models\DeviceLinkCode>
 */
class DeviceLinkCodeFactory extends Factory
{
    protected $model = DeviceLinkCode::class;

    /**
     * Define the model's default state.
     *
     * @return array<string, mixed>
     */
    public function definition(): array
    {
        return [
            'commerce_id' => Commerce::factory(),
            'code' => DeviceLinkCode::generateUniqueCode(),
            'device_id' => null,
            'expires_at' => now()->addHours(24),
            'used_at' => null,
        ];
    }

    /**
     * Indicate that the code is used.
     */
    public function used(?Device $device = null): static
    {
        return $this->state(fn (array $attributes) => [
            'used_at' => now(),
            'device_id' => $device?->id ?? Device::factory(),
        ]);
    }

    /**
     * Indicate that the code is expired.
     */
    public function expired(): static
    {
        return $this->state(fn (array $attributes) => [
            'expires_at' => now()->subHour(),
        ]);
    }
}

