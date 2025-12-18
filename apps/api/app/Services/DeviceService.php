<?php

namespace App\Services;

use App\Models\Device;
use App\Models\User;
use Illuminate\Support\Str;

class DeviceService
{
    /**
     * Create a new device for a user.
     */
    public function createDevice(User $user, array $data): Device
    {
        return Device::create([
            'user_id' => $user->id,
            'commerce_id' => $user->commerce_id,
            'uuid' => $data['uuid'] ?? (string) Str::uuid(),
            'name' => $data['name'],
            'alias' => $data['alias'] ?? null,
            'platform' => $data['platform'] ?? 'android',
            'is_active' => $data['is_active'] ?? true,
        ]);
    }

    /**
     * Update device information.
     */
    public function updateDevice(Device $device, array $data): Device
    {
        $device->update($data);

        return $device->fresh();
    }

    /**
     * Get devices for a user.
     */
    public function getUserDevices(User $user, bool $activeOnly = false)
    {
        $query = Device::where('user_id', $user->id);

        if ($activeOnly) {
            $query->where('is_active', true);
        }

        return $query->orderBy('created_at', 'desc')->get();
    }

    /**
     * Find device by UUID and user.
     */
    public function findDeviceByUuid(User $user, string $uuid): ?Device
    {
        return Device::where('user_id', $user->id)
            ->where('uuid', $uuid)
            ->first();
    }

    /**
     * Activate or deactivate a device.
     */
    public function toggleDeviceStatus(Device $device, bool $isActive): Device
    {
        $device->update(['is_active' => $isActive]);

        return $device->fresh();
    }
}
