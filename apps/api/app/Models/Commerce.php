<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Casts\Attribute;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Support\Facades\Crypt;

class Commerce extends Model
{
    use HasFactory;

    public const GRACE_DAYS = 3;
    public const EXPIRING_SOON_DAYS = 7;

    protected $fillable = [
        'name',
        'owner_user_id',
        'status',
        'plan_id',
        'plan_expires_at',
        'approved_at',
        'approved_by',
        'referral_code',
        'referred_by_commerce_id',
        'payout_bank',
        'payout_account_type',
        'payout_account_number',
        'payout_account_holder',
        'payout_account_holder_doc',
    ];

    protected $hidden = ['payout_account_number'];

    protected function casts(): array
    {
        return [
            'approved_at' => 'datetime',
            'plan_expires_at' => 'datetime',
        ];
    }

    protected function payoutAccountNumber(): Attribute
    {
        return Attribute::make(
            get: fn ($value) => $value === null ? null : Crypt::decryptString($value),
            set: fn ($value) => $value === null ? null : Crypt::encryptString($value),
        );
    }

    public function isActive(): bool
    {
        return $this->status === 'active';
    }

    public function isPending(): bool
    {
        return $this->status === 'pending';
    }

    public function isSuspended(): bool
    {
        return $this->status === 'suspended';
    }

    /**
     * Count active (linked) devices for this commerce.
     */
    public function activeDevicesCount(): int
    {
        return $this->devices()->where('is_active', true)->count();
    }

    /**
     * Check if the commerce can add more devices based on its plan.
     */
    public function canAddDevice(): bool
    {
        if (!$this->plan || !$this->plan->hasDeviceLimit()) {
            return true;
        }

        return $this->activeDevicesCount() < $this->plan->max_devices;
    }

    /**
     * Count notifications sent today for this commerce.
     */
    public function todayNotificationsCount(): int
    {
        return $this->notifications()
            ->whereDate('created_at', today())
            ->count();
    }

    /**
     * Check if the commerce can send more notifications today based on its plan.
     */
    public function canSendNotificationToday(): bool
    {
        if (!$this->plan || !$this->plan->hasNotificationLimit()) {
            return true;
        }

        return $this->todayNotificationsCount() < $this->plan->max_notifications_per_day;
    }

    public function expiryStatus(): string
    {
        if ($this->status === 'pending') {
            return 'pending';
        }
        if ($this->status === 'suspended') {
            return 'suspended';
        }
        if (! $this->plan_expires_at) {
            return 'active';
        }

        $now = now();
        $expires = $this->plan_expires_at;
        $graceEnd = $expires->copy()->addDays(self::GRACE_DAYS);

        if ($now->greaterThan($graceEnd)) {
            return 'expired';
        }
        if ($now->greaterThan($expires)) {
            return 'in_grace';
        }
        if ($now->diffInDays($expires) < self::EXPIRING_SOON_DAYS) {
            return 'expiring_soon';
        }
        return 'active';
    }

    public function daysUntilExpiry(): ?int
    {
        if (! $this->plan_expires_at) {
            return null;
        }
        return (int) round(now()->diffInDays($this->plan_expires_at, false));
    }

    public function hasPayoutAccount(): bool
    {
        return filled($this->payout_bank)
            && filled($this->payout_account_type)
            && filled($this->payout_account_number)
            && filled($this->payout_account_holder)
            && filled($this->payout_account_holder_doc);
    }

    public function owner(): BelongsTo
    {
        return $this->belongsTo(User::class, 'owner_user_id');
    }

    public function referrer(): BelongsTo
    {
        return $this->belongsTo(Commerce::class, 'referred_by_commerce_id');
    }

    public function referrals(): HasMany
    {
        return $this->hasMany(Commerce::class, 'referred_by_commerce_id');
    }

    public function referralCommissionsEarned(): HasMany
    {
        return $this->hasMany(ReferralCommission::class, 'referrer_commerce_id');
    }

    public function plan(): BelongsTo
    {
        return $this->belongsTo(Plan::class);
    }

    public function approvedBy(): BelongsTo
    {
        return $this->belongsTo(User::class, 'approved_by');
    }

    /**
     * Get the users for this commerce.
     */
    public function users(): HasMany
    {
        return $this->hasMany(User::class);
    }

    /**
     * Get the devices for this commerce.
     */
    public function devices(): HasMany
    {
        return $this->hasMany(Device::class);
    }

    /**
     * Get the notifications for this commerce.
     */
    public function notifications(): HasMany
    {
        return $this->hasMany(Notification::class);
    }

    /**
     * Get the app instances for this commerce.
     */
    public function appInstances(): HasMany
    {
        return $this->hasMany(AppInstance::class);
    }

    /**
     * Get the monitor packages for this commerce.
     */
    public function monitorPackages(): HasMany
    {
        return $this->hasMany(MonitorPackage::class);
    }

    /**
     * Get the device link codes for this commerce.
     */
    public function deviceLinkCodes(): HasMany
    {
        return $this->hasMany(DeviceLinkCode::class);
    }

    /**
     * Get the renewals for this commerce.
     */
    public function renewals(): HasMany
    {
        return $this->hasMany(CommerceRenewal::class)->orderBy('created_at', 'desc');
    }
}



