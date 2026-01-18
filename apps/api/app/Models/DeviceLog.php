<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class DeviceLog extends Model
{
    use HasFactory;

    /**
     * The attributes that are mass assignable.
     *
     * @var array<int, string>
     */
    protected $fillable = [
        'device_id',
        'commerce_id',
        'category',
        'level',
        'message',
        'details',
        'device_timestamp',
    ];

    /**
     * Get the attributes that should be cast.
     *
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return [
            'device_timestamp' => 'datetime',
        ];
    }

    /**
     * Valid categories for logs.
     */
    public const CATEGORIES = [
        'SERVICE',
        'DISCONNECT',
        'RECONNECT',
        'ERROR',
        'HEALTH',
        'SYSTEM',
    ];

    /**
     * Valid log levels.
     */
    public const LEVELS = [
        'debug',
        'info',
        'warning',
        'error',
        'critical',
    ];

    /**
     * Get the device that owns the log.
     */
    public function device(): BelongsTo
    {
        return $this->belongsTo(Device::class);
    }

    /**
     * Get the commerce that owns the log.
     */
    public function commerce(): BelongsTo
    {
        return $this->belongsTo(Commerce::class);
    }

    /**
     * Scope for filtering by category.
     */
    public function scopeCategory($query, string $category)
    {
        return $query->where('category', $category);
    }

    /**
     * Scope for filtering by level.
     */
    public function scopeLevel($query, string $level)
    {
        return $query->where('level', $level);
    }

    /**
     * Scope for critical logs (disconnections, errors).
     */
    public function scopeCritical($query)
    {
        return $query->whereIn('category', ['DISCONNECT', 'ERROR'])
            ->orWhere('level', 'critical');
    }

    /**
     * Scope for recent logs.
     */
    public function scopeRecent($query, int $hours = 24)
    {
        return $query->where('created_at', '>=', now()->subHours($hours));
    }
}
