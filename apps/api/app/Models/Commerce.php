<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Commerce extends Model
{
    use HasFactory;

    /**
     * The attributes that are mass assignable.
     *
     * @var array<int, string>
     */
    protected $fillable = [
        'name',
        'owner_user_id',
    ];

    /**
     * Get the owner user.
     */
    public function owner(): BelongsTo
    {
        return $this->belongsTo(User::class, 'owner_user_id');
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
}


