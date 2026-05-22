<?php
use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;

return new class extends Migration {
    public function up(): void
    {
        Schema::table('commerces', function (Blueprint $table) {
            $table->string('referral_code', 20)->nullable()->after('name');
            $table->foreignId('referred_by_commerce_id')->nullable()
                ->after('referral_code')
                ->constrained('commerces')->nullOnDelete();
            $table->index('referred_by_commerce_id');
        });

        // Backfill: each existing commerce gets a unique referral_code
        DB::table('commerces')->whereNull('referral_code')->orderBy('id')->each(function ($row) {
            $base = Str::slug(Str::limit($row->name, 8, ''), '-');
            if ($base === '') {
                $base = 'com';
            }
            $attempts = 0;
            do {
                $code = $base . '-' . Str::lower(Str::random(4));
                $exists = DB::table('commerces')->where('referral_code', $code)->exists();
                $attempts++;
            } while ($exists && $attempts < 5);
            if ($exists) {
                $code = $base . '-' . time() . Str::lower(Str::random(2));
            }
            DB::table('commerces')->where('id', $row->id)->update(['referral_code' => $code]);
        });

        // Now lock it down using raw SQL (doctrine/dbal not in composer.json)
        DB::statement('ALTER TABLE commerces ALTER COLUMN referral_code SET NOT NULL');
        DB::statement('ALTER TABLE commerces ADD CONSTRAINT commerces_referral_code_unique UNIQUE (referral_code)');
    }

    public function down(): void
    {
        Schema::table('commerces', function (Blueprint $table) {
            $table->dropForeign(['referred_by_commerce_id']);
            $table->dropColumn(['referral_code', 'referred_by_commerce_id']);
        });
    }
};
