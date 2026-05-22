<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class DeviceMonitoredApp extends Model
{
    use HasFactory;

    /**
     * The attributes that are mass assignable.
     *
     * @var array<int, string>
     */
    protected $fillable = [
        'device_id',
        'package_name',
        'enabled',
    ];

    /**
     * Get the attributes that should be cast.
     *
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return [
            'enabled' => 'boolean',
        ];
    }

    /**
     * Get the device that owns this monitored app.
     */
    public function device(): BelongsTo
    {
        return $this->belongsTo(Device::class);
    }

    /**
     * Get monitor package using a more reliable method.
     * This method loads the device first to get commerce_id.
     * Note: This is not a direct Eloquent relationship because the relationship
     * depends on both package_name and commerce_id from the device.
     */
    public function getMonitorPackageAttribute(): ?MonitorPackage
    {
        if (!$this->device || !$this->device->commerce_id) {
            return null;
        }

        return MonitorPackage::where('package_name', $this->package_name)
            ->where('commerce_id', $this->device->commerce_id)
            ->first();
    }
}



