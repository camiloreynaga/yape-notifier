<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     *
     * Device logs table for storing critical events from Android devices.
     * Used for debugging service disconnections and other issues.
     */
    public function up(): void
    {
        Schema::create('device_logs', function (Blueprint $table) {
            $table->id();
            $table->foreignId('device_id')->constrained()->onDelete('cascade');
            $table->foreignId('commerce_id')->nullable()->constrained()->onDelete('cascade');

            // Log categorization
            $table->enum('category', [
                'SERVICE',      // Service lifecycle events
                'DISCONNECT',   // Disconnection events (critical)
                'RECONNECT',    // Reconnection attempts
                'ERROR',        // Errors and exceptions
                'HEALTH',       // Health check events
                'SYSTEM'        // System events (app start, etc)
            ])->index();

            // Log level for filtering
            $table->enum('level', ['debug', 'info', 'warning', 'error', 'critical'])->default('info')->index();

            // Log content
            $table->string('message', 500);
            $table->text('details')->nullable(); // Stack traces, additional info

            // Timestamp from device (when event actually occurred)
            $table->timestamp('device_timestamp')->nullable();

            // Server timestamp (when we received it)
            $table->timestamps();

            // Indexes for efficient querying
            $table->index(['device_id', 'created_at']);
            $table->index(['commerce_id', 'created_at']);
            $table->index(['category', 'level']);
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('device_logs');
    }
};
