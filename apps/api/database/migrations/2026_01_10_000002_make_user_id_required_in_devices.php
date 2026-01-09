<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;
use App\Models\User;
use App\Models\Device;
use Illuminate\Support\Str;

return new class extends Migration
{
    /**
     * Run the migrations.
     * 
     * Professional Architecture Approach:
     * - Every device MUST have an owner (user_id)
     * - Orphan devices are assigned to a "System" user for data integrity
     * - This ensures complete traceability and accountability
     */
    public function up(): void
    {
        // Paso 1: Crear usuario "Sistema" para dispositivos huérfanos
        $systemUser = User::firstOrCreate(
            ['email' => 'system@yapenotifier.internal'],
            [
                'name' => 'Sistema',
                'password' => bcrypt(Str::random(32)), // Password aleatorio imposible de adivinar
                'role' => 'system',
                'is_active' => false, // Usuario inactivo, solo para datos huérfanos
            ]
        );

        // Si hay commerces, asignar al primero
        if (\DB::table('commerces')->exists()) {
            $firstCommerce = \DB::table('commerces')->first();
            $systemUser->update(['commerce_id' => $firstCommerce->id]);
        }

        // Paso 2: Asignar user_id a dispositivos sin usuario
        Device::whereNull('user_id')->update([
            'user_id' => $systemUser->id,
        ]);

        // Paso 3: Drop foreign key constraint
        Schema::table('devices', function (Blueprint $table) {
            $table->dropForeign(['user_id']);
        });

        // Paso 4: Hacer user_id NOT NULL
        Schema::table('devices', function (Blueprint $table) {
            $table->unsignedBigInteger('user_id')->nullable(false)->change();
        });

        // Paso 5: Re-add foreign key constraint
        Schema::table('devices', function (Blueprint $table) {
            $table->foreign('user_id')
                ->references('id')
                ->on('users')
                ->onDelete('cascade');
        });
    }

    /**
     * Reverse the migrations.
     * 
     * WARNING: This will make user_id nullable again.
     * Consider the implications for data integrity.
     */
    public function down(): void
    {
        Schema::table('devices', function (Blueprint $table) {
            $table->dropForeign(['user_id']);
        });

        Schema::table('devices', function (Blueprint $table) {
            $table->unsignedBigInteger('user_id')->nullable()->change();
        });

        Schema::table('devices', function (Blueprint $table) {
            $table->foreign('user_id')
                ->references('id')
                ->on('users')
                ->onDelete('cascade');
        });

        // Optionally delete system user
        User::where('email', 'system@yapenotifier.internal')->delete();
    }
};

