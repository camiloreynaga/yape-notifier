<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     *
     * Adds security_code field to notifications table.
     * Yape -> Yape transfers include a security code (e.g., "693", "161")
     * Other bank/wallet -> Yape transfers do not include a security code
     */
    public function up(): void
    {
        Schema::table('notifications', function (Blueprint $table) {
            $table->string('security_code', 10)->nullable()->after('payer_name');

            // Add index for faster searches by security code
            $table->index('security_code');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('notifications', function (Blueprint $table) {
            $table->dropIndex(['security_code']);
            $table->dropColumn('security_code');
        });
    }
};
