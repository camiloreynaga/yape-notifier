<?php

namespace Database\Factories;

use App\Models\Device;
use App\Models\User;
use Illuminate\Database\Eloquent\Factories\Factory;

/**
 * @extends \Illuminate\Database\Eloquent\Factories\Factory<\App\Models\Notification>
 */
class NotificationFactory extends Factory
{
    /**
     * Define the model's default state.
     *
     * @return array<string, mixed>
     */
    public function definition(): array
    {
        $sourceApps = ['yape', 'plin', 'bcp', 'interbank', 'bbva', 'scotiabank'];

        return [
            'user_id' => User::factory(),
            'device_id' => Device::factory(),
            'source_app' => fake()->randomElement($sourceApps),
            'title' => fake()->sentence(),
            'body' => fake()->text(),
            'amount' => fake()->randomFloat(2, 10, 1000),
            'currency' => 'PEN',
            'payer_name' => fake()->name(),
            'received_at' => now(),
            'raw_json' => [],
            'status' => 'pending',
            'is_duplicate' => false,
        ];
    }

    public function duplicate(): static
    {
        return $this->state(fn (array $attributes) => [
            'is_duplicate' => true,
        ]);
    }

    public function validated(): static
    {
        return $this->state(fn (array $attributes) => [
            'status' => 'validated',
        ]);
    }
}









