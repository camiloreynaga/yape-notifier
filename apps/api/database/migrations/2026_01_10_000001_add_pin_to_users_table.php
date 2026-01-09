<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     * 
     * Professional Architecture Approach:
     * - PIN is unique identifier for employee authentication
     * - 4-6 digits for easy memorization
     * - Nullable for existing users (will be required for new users)
     * - Indexed for fast lookups during authentication
     */
    public function up(): void
    {
        Schema::table('users', function (Blueprint $table) {
            // PIN de 4-6 dígitos, único, nullable (para usuarios existentes)
            $table->string('pin', 6)->unique()->nullable()->after('password');
            
            // Índice para búsquedas rápidas por PIN durante autenticación
            $table->index('pin');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->dropIndex(['pin']);
            $table->dropColumn('pin');
        });
    }
};

