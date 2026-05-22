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
        Schema::create('app_instances', function (Blueprint $table) {
            $table->id();
            $table->foreignId('commerce_id')->constrained()->onDelete('cascade');
            $table->foreignId('device_id')->constrained()->onDelete('cascade');
            $table->string('package_name');
            $table->integer('android_user_id'); // UserHandle identifier
            $table->string('instance_label')->nullable(); // ej. "Yape 1 (Rocío)"
            $table->timestamps();

            // Unique constraint: one instance per device+package+user combination
            $table->unique(['device_id', 'package_name', 'android_user_id'], 'unique_app_instance');
            $table->index(['commerce_id', 'device_id']);
            $table->index(['device_id', 'package_name']);
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('app_instances');
    }
};



