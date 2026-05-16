<?php
namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class ReferralCommission extends Model
{
    use HasFactory;

    public const STATUS_PENDING = 'pending';
    public const STATUS_APPROVED = 'approved';
    public const STATUS_PAID = 'paid';
    public const STATUS_VOID = 'void';

    protected $fillable = [
        'referrer_commerce_id',
        'referred_commerce_id',
        'commerce_renewal_id',
        'base_amount',
        'commission_rate',
        'amount',
        'status',
        'paid_at',
        'payout_reference',
        'voided_reason',
        'approved_by_user_id',
        'paid_by_user_id',
    ];

    protected function casts(): array
    {
        return [
            'base_amount' => 'decimal:2',
            'commission_rate' => 'decimal:4',
            'amount' => 'decimal:2',
            'paid_at' => 'datetime',
        ];
    }

    public function referrer(): BelongsTo
    {
        return $this->belongsTo(Commerce::class, 'referrer_commerce_id');
    }

    public function referred(): BelongsTo
    {
        return $this->belongsTo(Commerce::class, 'referred_commerce_id');
    }

    public function renewal(): BelongsTo
    {
        return $this->belongsTo(CommerceRenewal::class, 'commerce_renewal_id');
    }

    public function approvedBy(): BelongsTo
    {
        return $this->belongsTo(User::class, 'approved_by_user_id');
    }

    public function paidBy(): BelongsTo
    {
        return $this->belongsTo(User::class, 'paid_by_user_id');
    }
}
