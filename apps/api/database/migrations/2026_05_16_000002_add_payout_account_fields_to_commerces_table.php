<?php
use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration {
    public function up(): void
    {
        Schema::table('commerces', function (Blueprint $table) {
            $table->string('payout_bank', 80)->nullable()->after('referred_by_commerce_id');
            $table->string('payout_account_type', 20)->nullable()->after('payout_bank');
            $table->text('payout_account_number')->nullable()->after('payout_account_type');
            $table->string('payout_account_holder', 150)->nullable()->after('payout_account_number');
            $table->string('payout_account_holder_doc', 20)->nullable()->after('payout_account_holder');
        });
    }

    public function down(): void
    {
        Schema::table('commerces', function (Blueprint $table) {
            $table->dropColumn([
                'payout_bank', 'payout_account_type', 'payout_account_number',
                'payout_account_holder', 'payout_account_holder_doc',
            ]);
        });
    }
};
