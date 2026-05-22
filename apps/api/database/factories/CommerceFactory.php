<?php

namespace Database\Factories;

use App\Models\Commerce;
use App\Models\Plan;
use App\Models\User;
use Illuminate\Database\Eloquent\Factories\Factory;

class CommerceFactory extends Factory
{
    protected $model = Commerce::class;

    public function definition(): array
    {
        return [
            'name' => $this->faker->company(),
            'owner_user_id' => User::factory(),
            'status' => 'active',
            'plan_id' => Plan::factory(),
            'plan_expires_at' => now()->addDays(20),
        ];
    }

    public function pending(): self
    {
        return $this->state(fn () => [
            'status' => 'pending',
            'plan_id' => null,
            'plan_expires_at' => null,
        ]);
    }

    public function expiringSoon(): self
    {
        return $this->state(fn () => [
            'status' => 'active',
            'plan_expires_at' => now()->addDays(3),
        ]);
    }

    public function inGrace(): self
    {
        return $this->state(fn () => [
            'status' => 'active',
            'plan_expires_at' => now()->subDays(1),
        ]);
    }

    public function expiredPostGrace(): self
    {
        return $this->state(fn () => [
            'status' => 'active',
            'plan_expires_at' => now()->subDays(5),
        ]);
    }

    public function suspended(): self
    {
        return $this->state(fn () => [
            'status' => 'suspended',
            'plan_expires_at' => now()->subDays(10),
        ]);
    }
}
