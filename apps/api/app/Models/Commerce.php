<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Commerce extends Model
{
    use HasFactory;

    protected $fillable = [
        'name',
        'owner_user_id',
        'status',
        'plan_id',
        'plan_expires_at',
        'approved_at',
        'approved_by',
    ];

    protected function casts(): array
    {
        return [
            'approved_at' => 'datetime',
            'plan_expires_at' => 'datetime',
        ];
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

    public function owner(): BelongsTo
    {
        return $this->belongsTo(User::class, 'owner_user_id');
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



