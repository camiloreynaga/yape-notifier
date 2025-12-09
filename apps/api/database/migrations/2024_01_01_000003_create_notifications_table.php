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
        Schema::create('notifications', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained()->onDelete('cascade');
            $table->foreignId('device_id')->constrained()->onDelete('cascade');
            $table->string('source_app'); // yape, plin, bcp, interbank, bbva, scotiabank
            $table->string('title')->nullable();
            $table->text('body');
            $table->decimal('amount', 10, 2)->nullable();
            $table->string('currency', 3)->nullable()->default('PEN');
            $table->string('payer_name')->nullable();
            $table->timestamp('received_at');
            $table->json('raw_json')->nullable();
            $table->string('status')->default('pending'); // pending, validated, inconsistent
            $table->boolean('is_duplicate')->default(false);
            $table->timestamps();

            $table->index(['user_id', 'received_at']);
            $table->index(['device_id', 'received_at']);
            $table->index(['source_app', 'received_at']);
            $table->index(['status', 'received_at']);

            // Index for duplicate detection
            $table->index(['device_id', 'source_app', 'received_at']);
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('notifications');
    }
};
