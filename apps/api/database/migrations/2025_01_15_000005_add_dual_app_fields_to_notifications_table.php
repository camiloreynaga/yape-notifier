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
        Schema::table('notifications', function (Blueprint $table) {
            $table->foreignId('commerce_id')->nullable()->after('user_id')->constrained()->onDelete('cascade');
            $table->string('package_name')->nullable()->after('source_app'); // ej. "com.bcp.innovacxion.yapeapp"
            $table->integer('android_user_id')->nullable()->after('package_name'); // UserHandle identifier
            $table->integer('android_uid')->nullable()->after('android_user_id'); // opcional
            $table->foreignId('app_instance_id')->nullable()->after('android_uid')->constrained('app_instances')->onDelete('set null');
            $table->timestamp('posted_at')->nullable()->after('received_at'); // hora original de la notificación
            
            // Actualizar índices
            $table->index(['commerce_id', 'received_at']);
            $table->index(['device_id', 'package_name', 'android_user_id', 'received_at'], 'idx_notification_dedupe');
            $table->index(['app_instance_id', 'received_at']);
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('notifications', function (Blueprint $table) {
            $table->dropForeign(['commerce_id']);
            $table->dropForeign(['app_instance_id']);
            $table->dropIndex(['commerce_id', 'received_at']);
            $table->dropIndex('idx_notification_dedupe');
            $table->dropIndex(['app_instance_id', 'received_at']);
            $table->dropColumn([
                'commerce_id',
                'package_name',
                'android_user_id',
                'android_uid',
                'app_instance_id',
                'posted_at',
            ]);
        });
    }
};



