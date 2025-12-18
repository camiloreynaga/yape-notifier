<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class AppInstance extends Model
{
    use HasFactory;

    /**
     * The attributes that are mass assignable.
     *
     * @var array<int, string>
     */
    protected $fillable = [
        'commerce_id',
        'device_id',
        'package_name',
        'android_user_id',
        'instance_label',
    ];

    /**
     * Get the commerce that owns this app instance.
     */
    public function commerce(): BelongsTo
    {
        return $this->belongsTo(Commerce::class);
    }

    /**
     * Get the device that owns this app instance.
     */
    public function device(): BelongsTo
    {
        return $this->belongsTo(Device::class);
    }

    /**
     * Get the notifications for this app instance.
     */
    public function notifications(): HasMany
    {
        return $this->hasMany(Notification::class);
    }

    /**
     * Get display name for the instance.
     */
    public function getDisplayNameAttribute(): string
    {
        return $this->instance_label ?? sprintf(
            '%s (User %d)',
            $this->package_name,
            $this->android_user_id
        );
    }

    /**
     * Find or create an app instance.
     */
    public static function findOrCreate(
        int $commerceId,
        int $deviceId,
        string $packageName,
        int $androidUserId,
        ?string $instanceLabel = null
    ): self {
        return self::firstOrCreate(
            [
                'device_id' => $deviceId,
                'package_name' => $packageName,
                'android_user_id' => $androidUserId,
            ],
            [
                'commerce_id' => $commerceId,
                'instance_label' => $instanceLabel,
            ]
        );
    }
}


