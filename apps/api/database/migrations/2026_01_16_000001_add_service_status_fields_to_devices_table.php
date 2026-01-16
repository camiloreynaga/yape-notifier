<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     *
     * These fields provide real visibility into whether the notification
     * listener service is actually working, not just if the permission is enabled.
     */
    public function up(): void
    {
        Schema::table('devices', function (Blueprint $table) {
            // Real state of the NotificationListenerService (connected/disconnected)
            // This is different from notification_permission_enabled which only checks permission
            $table->boolean('notification_service_connected')->nullable()->after('notification_permission_enabled');

            // Count of notifications pending in local DB (not yet sent to API)
            // If this is > 0 with heartbeat active, something is wrong with sending
            $table->integer('pending_notifications_count')->nullable()->after('notification_service_connected');

            // Timestamp of last captured notification
            // Allows detecting if device is capturing but not sending
            $table->timestamp('last_notification_captured_at')->nullable()->after('pending_notifications_count');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('devices', function (Blueprint $table) {
            $table->dropColumn([
                'notification_service_connected',
                'pending_notifications_count',
                'last_notification_captured_at',
            ]);
        });
    }
};
