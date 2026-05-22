<?php

namespace Database\Factories;

use App\Models\Plan;
use Illuminate\Database\Eloquent\Factories\Factory;

class PlanFactory extends Factory
{
    protected $model = Plan::class;

    public function definition(): array
    {
        $name = $this->faker->unique()->word();

        return [
            'name'                      => ucfirst($name),
            'slug'                      => strtolower($name) . '-' . $this->faker->unique()->numberBetween(1000, 9999),
            'max_devices'               => $this->faker->numberBetween(1, 10),
            'max_notifications_per_day' => $this->faker->optional()->numberBetween(10, 1000),
            'price'                     => $this->faker->randomFloat(2, 0, 299),
            'is_active'                 => true,
        ];
    }
}
