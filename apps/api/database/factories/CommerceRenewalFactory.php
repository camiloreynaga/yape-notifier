<?php

namespace Database\Factories;

use App\Models\Commerce;
use App\Models\CommerceRenewal;
use App\Models\Plan;
use App\Models\User;
use Illuminate\Database\Eloquent\Factories\Factory;

class CommerceRenewalFactory extends Factory
{
    protected $model = CommerceRenewal::class;

    public function definition(): array
    {
        $previous = $this->faker->dateTimeBetween('-60 days', '-30 days');
        $new = (clone $previous)->modify('+30 days');

        return [
            'commerce_id' => Commerce::factory(),
            'plan_id' => Plan::factory(),
            'renewed_by_user_id' => User::factory(),
            'previous_expires_at' => $previous,
            'new_expires_at' => $new,
            'amount_paid' => $this->faker->randomFloat(2, 0, 299),
            'notes' => $this->faker->optional()->sentence(),
        ];
    }
}
