<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Laravel\Sanctum\HasApiTokens;

class User extends Authenticatable
{
    use HasApiTokens, HasFactory, Notifiable;

    /**
     * The attributes that are mass assignable.
     *
     * @var array<int, string>
     */
    protected $fillable = [
        'name',
        'email',
        'phone',
        'password',
        'pin',
        'commerce_id',
        'role',
        'is_active',
    ];

    /**
     * The attributes that should be hidden for serialization.
     *
     * @var array<int, string>
     */
    protected $hidden = [
        'password',
        'pin',
        'remember_token',
    ];

    /**
     * Get the attributes that should be cast.
     *
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return [
            'email_verified_at' => 'datetime',
            'password' => 'hashed',
        ];
    }

    /**
     * Get the devices for the user.
     */
    public function devices()
    {
        return $this->hasMany(Device::class);
    }

    /**
     * Get the notifications for the user.
     */
    public function notifications()
    {
        return $this->hasMany(Notification::class);
    }

    /**
     * Get the commerce for this user.
     */
    public function commerce()
    {
        return $this->belongsTo(Commerce::class);
    }

    public function isAdmin(): bool
    {
        return $this->role === 'admin';
    }

    public function isCaptador(): bool
    {
        return $this->role === 'captador';
    }

    public function isSuperAdmin(): bool
    {
        return $this->role === 'super_admin';
    }

    /**
     * Validate PIN.
     * 
     * @param string $pin
     * @return bool
     */
    public function validatePin(string $pin): bool
    {
        return $this->pin === $pin;
    }

    /**
     * Generate unique PIN.
     * 
     * @param int $length
     * @return string
     */
    public static function generateUniquePin(int $length = 4): string
    {
        $max = (int) str_repeat('9', $length);
        $min = (int) str_pad('1', $length, '0', STR_PAD_LEFT);
        
        do {
            $pin = str_pad((string) random_int($min, $max), $length, '0', STR_PAD_LEFT);
        } while (self::where('pin', $pin)->exists());
        
        return $pin;
    }
}
