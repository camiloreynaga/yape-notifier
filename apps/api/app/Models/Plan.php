<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Plan extends Model
{
    use HasFactory;

    protected $fillable = [
        'name',
        'slug',
        'max_devices',
        'max_notifications_per_day',
        'price',
        'is_active',
    ];

    protected function casts(): array
    {
        return [
            'max_devices' => 'integer',
            'max_notifications_per_day' => 'integer',
            'price' => 'decimal:2',
            'is_active' => 'boolean',
        ];
    }

    public function commerces(): HasMany
    {
        return $this->hasMany(Commerce::class);
    }

    /**
     * Check if the plan has a device limit.
     */
    public function hasDeviceLimit(): bool
    {
        return !is_null($this->max_devices);
    }

    /**
     * Check if the plan has a daily notification limit.
     */
    public function hasNotificationLimit(): bool
    {
        return !is_null($this->max_notifications_per_day);
    }
}
