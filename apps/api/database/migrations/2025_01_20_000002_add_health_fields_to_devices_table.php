<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        Schema::table('devices', function (Blueprint $table) {
            $table->integer('battery_level')->nullable()->after('last_seen_at');
            $table->boolean('battery_optimization_disabled')->nullable()->after('battery_level');
            $table->boolean('notification_permission_enabled')->nullable()->after('battery_optimization_disabled');
            $table->timestamp('last_heartbeat')->nullable()->after('notification_permission_enabled');

            $table->index('last_heartbeat');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('devices', function (Blueprint $table) {
            $table->dropIndex(['last_heartbeat']);
            $table->dropColumn([
                'battery_level',
                'battery_optimization_disabled',
                'notification_permission_enabled',
                'last_heartbeat',
            ]);
        });
    }
};

