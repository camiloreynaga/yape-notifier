<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('plans', function (Blueprint $table) {
            $table->id();
            $table->string('name');                          // "Starter", "Basic", "Pro", "Enterprise"
            $table->string('slug')->unique();                // "starter", "basic", "pro", "enterprise"
            $table->unsignedInteger('max_devices')->nullable();           // NULL = ilimitado
            $table->unsignedInteger('max_notifications_per_day')->nullable(); // NULL = ilimitado
            $table->decimal('price', 8, 2)->default(0.00);  // precio mensual
            $table->boolean('is_active')->default(true);
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('plans');
    }
};
