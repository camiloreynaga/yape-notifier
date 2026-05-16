<?php
use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration {
    public function up(): void
    {
        Schema::create('referral_commissions', function (Blueprint $table) {
            $table->id();
            $table->foreignId('referrer_commerce_id')->constrained('commerces')->cascadeOnDelete();
            $table->foreignId('referred_commerce_id')->constrained('commerces')->cascadeOnDelete();
            $table->foreignId('commerce_renewal_id')->nullable()->unique()
                ->constrained('commerce_renewals')->nullOnDelete();
            $table->decimal('base_amount', 10, 2);
            $table->decimal('commission_rate', 5, 4)->default(0.2000);
            $table->decimal('amount', 10, 2);
            $table->string('status', 16)->default('pending');
            $table->timestamp('paid_at')->nullable();
            $table->string('payout_reference', 100)->nullable();
            $table->text('voided_reason')->nullable();
            $table->foreignId('approved_by_user_id')->nullable()->constrained('users')->nullOnDelete();
            $table->foreignId('paid_by_user_id')->nullable()->constrained('users')->nullOnDelete();
            $table->timestamps();

            $table->index(['referrer_commerce_id', 'status']);
            $table->index(['status', 'created_at']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('referral_commissions');
    }
};
