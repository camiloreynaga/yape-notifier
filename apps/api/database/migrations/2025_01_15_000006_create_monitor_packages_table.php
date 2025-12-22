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
        Schema::create('monitor_packages', function (Blueprint $table) {
            $table->id();
            $table->string('package_name')->unique();
            $table->string('app_name')->nullable(); // Nombre descriptivo de la app
            $table->text('description')->nullable(); // Descripción opcional
            $table->boolean('is_active')->default(true);
            $table->integer('priority')->default(0); // Para ordenar si es necesario
            $table->timestamps();

            $table->index('is_active');
            $table->index('package_name');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('monitor_packages');
    }
};

