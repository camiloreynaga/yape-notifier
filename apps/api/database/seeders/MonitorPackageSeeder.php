<?php

namespace Database\Seeders;

use App\Models\MonitorPackage;
use Illuminate\Database\Seeder;

class MonitorPackageSeeder extends Seeder
{
    /**
     * Run the database seeds.
     */
    public function run(): void
    {
        $packages = [
            [
                'package_name' => 'com.yapenotifier.android',
                'app_name' => 'Yape Notifier',
                'description' => 'Aplicación principal de notificaciones',
                'is_active' => true,
                'priority' => 100,
            ],
            [
                'package_name' => 'pe.com.interbank.mobilebanking',
                'app_name' => 'Interbank Mobile',
                'description' => 'Aplicación móvil de Interbank',
                'is_active' => true,
                'priority' => 90,
            ],
            [
                'package_name' => 'com.bcp.bancadigital',
                'app_name' => 'BCP Digital',
                'description' => 'Aplicación móvil del Banco de Crédito del Perú',
                'is_active' => true,
                'priority' => 90,
            ],
            [
                'package_name' => 'com.bbva.bbvacontinental',
                'app_name' => 'BBVA Continental',
                'description' => 'Aplicación móvil de BBVA Continental',
                'is_active' => true,
                'priority' => 90,
            ],
            [
                'package_name' => 'com.scotiabank.mobile',
                'app_name' => 'Scotiabank Mobile',
                'description' => 'Aplicación móvil de Scotiabank',
                'is_active' => true,
                'priority' => 90,
            ],
            [
                'package_name' => 'com.yape.android',
                'app_name' => 'Yape',
                'description' => 'Aplicación oficial de Yape',
                'is_active' => true,
                'priority' => 95,
            ],
            [
                'package_name' => 'com.bcp.innovacxion.yapeapp',
                'app_name' => 'Yape BCP',
                'description' => 'Aplicación Yape de BCP',
                'is_active' => true,
                'priority' => 95,
            ],
            [
                'package_name' => 'com.plin.android',
                'app_name' => 'Plin',
                'description' => 'Aplicación oficial de Plin',
                'is_active' => true,
                'priority' => 95,
            ],
        ];

        foreach ($packages as $package) {
            MonitorPackage::updateOrCreate(
                ['package_name' => $package['package_name']],
                $package
            );
        }
    }
}

