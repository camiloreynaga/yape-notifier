<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     * 
     * Professional Architecture Approach:
     * - Notifications can exist without a user (capturer mode)
     * - Device linked via QR (commerce_id) is the authorization
     * - user_id is optional (for traceability when user exists)
     */
    public function up(): void
    {
        Schema::table('notifications', function (Blueprint $table) {
            // Make user_id nullable to support capturer mode
            $table->foreignId('user_id')->nullable()->change();
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('notifications', function (Blueprint $table) {
            // Revert to NOT NULL (will fail if there are null values)
            $table->foreignId('user_id')->nullable(false)->change();
        });
    }
};

