<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;
use Illuminate\Support\Facades\DB;

return new class extends Migration
{
    public function up(): void
    {
        // El campo role ya existe como string, solo actualizamos el comentario
        // Los valores válidos ahora son: 'super_admin', 'admin', 'captador'
        // super_admin no pertenece a ningún commerce (commerce_id = null)
        DB::statement("COMMENT ON COLUMN users.role IS 'super_admin, admin, captador'");
    }

    public function down(): void
    {
        DB::statement("COMMENT ON COLUMN users.role IS 'admin, captador'");
    }
};
