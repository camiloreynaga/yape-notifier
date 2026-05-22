<?php

namespace Database\Factories;

use App\Models\AppInstance;
use App\Models\Commerce;
use App\Models\Device;
use Illuminate\Database\Eloquent\Factories\Factory;

/**
 * @extends \Illuminate\Database\Eloquent\Factories\Factory<\App\Models\AppInstance>
 */
class AppInstanceFactory extends Factory
{
    protected $model = AppInstance::class;

    /**
     * Define the model's default state.
     *
     * @return array<string, mixed>
     */
    public function definition(): array
    {
        return [
            'commerce_id' => Commerce::factory(),
            'device_id' => Device::factory(),
            'package_name' => $this->faker->randomElement([
                'com.bcp.innovacxion.yapeapp',
                'com.yape.android',
                'com.plin.android',
            ]),
            'android_user_id' => $this->faker->numberBetween(0, 10),
            'instance_label' => $this->faker->optional()->words(2, true),
        ];
    }
}



