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
        Schema::table('monitor_packages', function (Blueprint $table) {
            $table->foreignId('commerce_id')->nullable()->after('id')->constrained()->onDelete('cascade');
            $table->boolean('enabled_default')->default(true)->after('is_active');
            $table->index('commerce_id');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('monitor_packages', function (Blueprint $table) {
            $table->dropForeign(['commerce_id']);
            $table->dropColumn(['commerce_id', 'enabled_default']);
        });
    }
};



