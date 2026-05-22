<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     * 
     * Professional Architecture: Make user_id nullable to support notification creation
     * without authentication (flexible UX for capturer mode).
     * 
     * This migration:
     * 1. Drops the existing foreign key constraint
     * 2. Modifies the column to be nullable
     * 3. Re-adds the foreign key constraint (nullable foreign keys are supported)
     * 
     * Context: Devices can be linked via QR without user authentication.
     * When these devices send notifications, user_id will be NULL.
     * The commerce_id (from QR linking) is the authorization mechanism.
     */
    public function up(): void
    {
        Schema::table('notifications', function (Blueprint $table) {
            // Step 1: Drop the existing foreign key constraint
            // Use the naming convention: {table}_{column}_foreign
            $table->dropForeign(['user_id']);
        });
        
        // Step 2: Modify the column to be nullable
        // Note: Must be done in a separate Schema::table call for some DB drivers
        Schema::table('notifications', function (Blueprint $table) {
            $table->unsignedBigInteger('user_id')->nullable()->change();
        });
        
        // Step 3: Re-add the foreign key constraint with nullable support
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
     * WARNING: This will fail if there are notifications with null user_id.
     * Consider cleaning up null user_id notifications before rolling back.
     */
    public function down(): void
    {
        Schema::table('notifications', function (Blueprint $table) {
            // Step 1: Drop the foreign key constraint
            $table->dropForeign(['user_id']);
        });
        
        // Step 2: Make user_id NOT nullable again
        // WARNING: This will fail if there are notifications with null user_id
        Schema::table('notifications', function (Blueprint $table) {
            $table->unsignedBigInteger('user_id')->nullable(false)->change();
        });
        
        // Step 3: Re-add the foreign key constraint
        Schema::table('notifications', function (Blueprint $table) {
            $table->foreign('user_id')
                ->references('id')
                ->on('users')
                ->onDelete('cascade');
        });
    }
};

