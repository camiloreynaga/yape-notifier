<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;
use Illuminate\Support\Facades\DB;

return new class extends Migration
{
    /**
     * Run the migrations.
     * 
     * Professional Architecture Approach:
     * - Every notification MUST have a capturer (user_id)
     * - Orphan notifications inherit user_id from their device
     * - This ensures complete traceability: who captured what, when
     */
    public function up(): void
    {
        // Paso 1: Asignar user_id del dispositivo a notificaciones huérfanas
        // Esto garantiza que todas las notificaciones tengan un responsable
        DB::statement('
            UPDATE notifications n
            SET user_id = (
                SELECT d.user_id 
                FROM devices d 
                WHERE d.id = n.device_id
            )
            WHERE n.user_id IS NULL
        ');

        // Paso 2: Drop foreign key constraint
        Schema::table('notifications', function (Blueprint $table) {
            $table->dropForeign(['user_id']);
        });

        // Paso 3: Hacer user_id NOT NULL
        Schema::table('notifications', function (Blueprint $table) {
            $table->unsignedBigInteger('user_id')->nullable(false)->change();
        });

        // Paso 4: Re-add foreign key constraint
        Schema::table('notifications', function (Blueprint $table) {
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
     * This reduces data integrity and traceability.
     */
    public function down(): void
    {
        Schema::table('notifications', function (Blueprint $table) {
            $table->dropForeign(['user_id']);
        });

        Schema::table('notifications', function (Blueprint $table) {
            $table->unsignedBigInteger('user_id')->nullable()->change();
        });

        Schema::table('notifications', function (Blueprint $table) {
            $table->foreign('user_id')
                ->references('id')
                ->on('users')
                ->onDelete('cascade');
        });
    }
};

