<?php

namespace Database\Seeders;

use App\Models\Plan;
use Illuminate\Database\Seeder;

class PlanSeeder extends Seeder
{
    public function run(): void
    {
        $plans = [
            [
                'name'                      => 'Starter',
                'slug'                      => 'starter',
                'max_devices'               => 1,
                'max_notifications_per_day' => 10,
                'price'                     => 0.00,
                'is_active'                 => true,
            ],
            [
                'name'                      => 'Basic',
                'slug'                      => 'basic',
                'max_devices'               => 3,
                'max_notifications_per_day' => null, // ilimitado
                'price'                     => 49.00,
                'is_active'                 => true,
            ],
            [
                'name'                      => 'Pro',
                'slug'                      => 'pro',
                'max_devices'               => 10,
                'max_notifications_per_day' => null,
                'price'                     => 129.00,
                'is_active'                 => true,
            ],
            [
                'name'                      => 'Enterprise',
                'slug'                      => 'enterprise',
                'max_devices'               => null, // ilimitado
                'max_notifications_per_day' => null,
                'price'                     => 299.00,
                'is_active'                 => true,
            ],
        ];

        foreach ($plans as $plan) {
            Plan::updateOrCreate(['slug' => $plan['slug']], $plan);
        }
    }
}
