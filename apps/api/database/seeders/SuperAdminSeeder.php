<?php

namespace Database\Seeders;

use App\Models\User;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;

class SuperAdminSeeder extends Seeder
{
    /**
     * Crea (o actualiza) el usuario super admin a partir de variables de entorno.
     *
     * Variables requeridas en .env:
     *   SUPER_ADMIN_EMAIL    — email del super admin
     *   SUPER_ADMIN_PASSWORD — password en texto plano (se hashea automaticamente)
     *
     * Variables opcionales:
     *   SUPER_ADMIN_NAME     — nombre, default "Super Admin"
     *
     * Idempotente: si el usuario ya existe se actualiza; si no, se crea.
     * Si las variables no estan configuradas, el seeder se salta sin error
     * (para no romper tests u otros entornos donde el super admin no aplica).
     */
    public function run(): void
    {
        $email = env('SUPER_ADMIN_EMAIL');
        $password = env('SUPER_ADMIN_PASSWORD');

        if (empty($email) || empty($password)) {
            $this->command->warn('SuperAdminSeeder: SUPER_ADMIN_EMAIL o SUPER_ADMIN_PASSWORD no configurados, saltando.');
            return;
        }

        $name = env('SUPER_ADMIN_NAME', 'Super Admin');

        $user = User::updateOrCreate(
            ['email' => $email],
            [
                'name' => $name,
                'password' => Hash::make($password),
                'role' => 'super_admin',
                'commerce_id' => null,
                'is_active' => true,
            ]
        );

        $this->command->info("SuperAdminSeeder: super admin '{$user->email}' listo (id={$user->id}).");
    }
}
