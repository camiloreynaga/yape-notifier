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
        Schema::table('users', function (Blueprint $table) {
            $table->foreignId('commerce_id')->nullable()->after('id')->constrained()->onDelete('set null');
            $table->string('role')->default('admin')->after('commerce_id'); // admin, captador
            $table->index('commerce_id');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->dropForeign(['commerce_id']);
            $table->dropColumn(['commerce_id', 'role']);
        });
    }
};



