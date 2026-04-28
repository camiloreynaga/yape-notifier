<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class CommerceRenewal extends Model
{
    use HasFactory;

    protected $fillable = [
        'commerce_id',
        'plan_id',
        'renewed_by_user_id',
        'previous_expires_at',
        'new_expires_at',
        'amount_paid',
        'notes',
    ];

    protected function casts(): array
    {
        return [
            'previous_expires_at' => 'datetime',
            'new_expires_at' => 'datetime',
            'amount_paid' => 'decimal:2',
        ];
    }

    public function commerce(): BelongsTo
    {
        return $this->belongsTo(Commerce::class);
    }

    public function plan(): BelongsTo
    {
        return $this->belongsTo(Plan::class);
    }

    public function renewedBy(): BelongsTo
    {
        return $this->belongsTo(User::class, 'renewed_by_user_id');
    }
}
