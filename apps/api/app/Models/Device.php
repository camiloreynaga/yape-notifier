<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Support\Str;

class Device extends Model
{
    use HasFactory;

    /**
     * The attributes that are mass assignable.
     *
     * @var array<int, string>
     */
    protected $fillable = [
        'user_id',
        'commerce_id',
        'uuid',
        'name',
        'alias',
        'platform',
        'is_active',
        'last_seen_at',
        'battery_level',
        'battery_optimization_disabled',
        'notification_permission_enabled',
        'last_heartbeat',
    ];

    /**
     * Get the attributes that should be cast.
     *
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return [
            'is_active' => 'boolean',
            'last_seen_at' => 'datetime',
            'battery_optimization_disabled' => 'boolean',
            'notification_permission_enabled' => 'boolean',
            'last_heartbeat' => 'datetime',
        ];
    }

    /**
     * Boot the model.
     */
    protected static function boot()
    {
        parent::boot();

        static::creating(function ($device) {
            if (empty($device->uuid)) {
                $device->uuid = (string) Str::uuid();
            }
        });
    }

    /**
     * Get the user that owns the device.
     */
    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    /**
     * Get the notifications for the device.
     */
    public function notifications(): HasMany
    {
        return $this->hasMany(Notification::class);
    }

    /**
     * Get the commerce for this device.
     */
    public function commerce(): BelongsTo
    {
        return $this->belongsTo(Commerce::class);
    }

    /**
     * Get the app instances for this device.
     */
    public function appInstances(): HasMany
    {
        return $this->hasMany(AppInstance::class);
    }

    /**
     * Get the monitored apps for this device.
     */
    public function monitoredApps(): HasMany
    {
        return $this->hasMany(DeviceMonitoredApp::class);
    }

    /**
     * Get the device link code that was used to link this device.
     */
    public function linkCode(): BelongsTo
    {
        return $this->belongsTo(DeviceLinkCode::class);
    }

    /**
     * Check if device is online (last heartbeat within 5 minutes).
     *
     * @return bool
     */
    public function isOnline(): bool
    {
        if (!$this->last_heartbeat) {
            return false;
        }

        return $this->last_heartbeat->diffInMinutes(now()) < 5;
    }

    /**
     * Get health status summary.
     *
     * @return array
     */
    public function getHealthStatus(): array
    {
        return [
            'is_online' => $this->isOnline(),
            'battery_level' => $this->battery_level,
            'battery_optimization_disabled' => $this->battery_optimization_disabled,
            'notification_permission_enabled' => $this->notification_permission_enabled,
            'last_heartbeat' => $this->last_heartbeat?->toIso8601String(),
            'last_seen_at' => $this->last_seen_at?->toIso8601String(),
        ];
    }
}
