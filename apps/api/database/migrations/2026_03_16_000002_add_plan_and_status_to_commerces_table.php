<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('commerces', function (Blueprint $table) {
            $table->string('status')->default('pending')->after('name'); // pending, active, suspended
            $table->foreignId('plan_id')->nullable()->after('status')->constrained('plans')->onDelete('set null');
            $table->timestamp('plan_expires_at')->nullable()->after('plan_id');
            $table->timestamp('approved_at')->nullable()->after('plan_expires_at');
            $table->foreignId('approved_by')->nullable()->after('approved_at')->constrained('users')->onDelete('set null');

            $table->index('status');
            $table->index('plan_id');
        });
    }

    public function down(): void
    {
        Schema::table('commerces', function (Blueprint $table) {
            $table->dropForeign(['plan_id']);
            $table->dropForeign(['approved_by']);
            $table->dropColumn(['status', 'plan_id', 'plan_expires_at', 'approved_at', 'approved_by']);
        });
    }
};
