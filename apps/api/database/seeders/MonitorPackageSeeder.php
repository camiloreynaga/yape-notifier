<?php

namespace Database\Seeders;

use App\Models\MonitorPackage;
use Illuminate\Database\Seeder;

class MonitorPackageSeeder extends Seeder
{
    /**
     * Get default packages configuration.
     * This method is used to seed packages for new commerces.
     * 
     * @return array
     */
    public static function getDefaultPackages(): array
    {
        return [
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
    }

    /**
     * Run the database seeds.
     * 
     * NOTE: This seeder is now deprecated for production use.
     * Packages are automatically created when a commerce is created.
     * This method is kept for backward compatibility and migration purposes.
     */
    public function run(): void
    {
        $packages = self::getDefaultPackages();

        foreach ($packages as $package) {
            // Only create if package doesn't exist (for migration purposes)
            MonitorPackage::firstOrCreate(
                ['package_name' => $package['package_name'], 'commerce_id' => null],
                $package
            );
        }
    }

    /**
     * Seed default packages for a specific commerce.
     * 
     * @param int $commerceId
     * @return int Number of packages created
     */
    public static function seedForCommerce(int $commerceId): int
    {
        $packages = self::getDefaultPackages();
        $created = 0;

        foreach ($packages as $package) {
            // Check if package already exists for this commerce
            $exists = MonitorPackage::where('commerce_id', $commerceId)
                ->where('package_name', $package['package_name'])
                ->exists();

            if (!$exists) {
                MonitorPackage::create([
                    ...$package,
                    'commerce_id' => $commerceId,
                ]);
                $created++;
            }
        }

        return $created;
    }
}

