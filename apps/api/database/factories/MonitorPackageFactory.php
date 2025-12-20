<?php

namespace Database\Factories;

use App\Models\Commerce;
use App\Models\MonitorPackage;
use Illuminate\Database\Eloquent\Factories\Factory;

/**
 * @extends \Illuminate\Database\Eloquent\Factories\Factory<\App\Models\MonitorPackage>
 */
class MonitorPackageFactory extends Factory
{
    protected $model = MonitorPackage::class;

    /**
     * Define the model's default state.
     *
     * @return array<string, mixed>
     */
    public function definition(): array
    {
        return [
            'commerce_id' => Commerce::factory(),
            'package_name' => 'com.' . fake()->word() . '.' . fake()->word(),
            'app_name' => fake()->words(2, true),
            'description' => fake()->sentence(),
            'is_active' => true,
            'enabled_default' => true,
            'priority' => fake()->numberBetween(1, 10),
        ];
    }

    /**
     * Indicate that the package is inactive.
     */
    public function inactive(): static
    {
        return $this->state(fn (array $attributes) => [
            'is_active' => false,
        ]);
    }
}

