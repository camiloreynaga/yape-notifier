<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('users', function (Blueprint $table) {
            // Password encrypted with Laravel Crypt (AES-256, reversible with
            // APP_KEY). Separate from `password` (bcrypt, used for auth) so the
            // commerce owner can view/copy an admin employee's credential.
            $table->text('password_visible')->nullable()->after('password');
        });
    }

    public function down(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->dropColumn('password_visible');
        });
    }
};
