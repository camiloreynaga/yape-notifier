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
            $table->foreignId('commerce_id')->nullable()->after('user_id')->constrained()->onDelete('cascade');
            $table->string('alias')->nullable()->after('name');
            $table->index('commerce_id');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('devices', function (Blueprint $table) {
            $table->dropForeign(['commerce_id']);
            $table->dropColumn(['commerce_id', 'alias']);
        });
    }
};



