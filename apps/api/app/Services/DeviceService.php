<?php

namespace App\Services;

use App\Models\Device;
use App\Models\User;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Str;

class DeviceService
{
    /**
     * Create a new device for a user.
     * Automatically assigns commerce_id from user if available.
     */
    public function createDevice(User $user, array $data): Device
    {
        // Ensure user has commerce_id (refresh to get latest)
        $user->refresh();
        $commerceId = $user->commerce_id ?? $data['commerce_id'] ?? null;

        if (!$commerceId) {
            Log::warning('Device created without commerce_id', [
                'user_id' => $user->id,
                'device_name' => $data['name'] ?? null,
            ]);
        }

        $device = Device::create([
            'user_id' => $user->id,
            'commerce_id' => $commerceId,
            'uuid' => $data['uuid'] ?? (string) Str::uuid(),
            'name' => $data['name'],
            'alias' => $data['alias'] ?? null,
            'platform' => $data['platform'] ?? 'android',
            'is_active' => $data['is_active'] ?? true,
        ]);

        Log::info('Device created', [
            'device_id' => $device->id,
            'user_id' => $user->id,
            'commerce_id' => $commerceId,
        ]);

        return $device;
    }

    /**
     * Update device information.
     * Automatically syncs commerce_id from user if device doesn't have one.
     */
    public function updateDevice(Device $device, array $data): Device
    {
        // If device doesn't have commerce_id but user does, sync it
        if (!$device->commerce_id && $device->user->commerce_id) {
            $data['commerce_id'] = $device->user->commerce_id;
            
            Log::info('Device commerce_id synced from user during update', [
                'device_id' => $device->id,
                'commerce_id' => $device->user->commerce_id,
            ]);
        }

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
     * Automatically syncs commerce_id if device doesn't have one but user does.
     */
    public function findDeviceByUuid(User $user, string $uuid): ?Device
    {
        $device = Device::where('user_id', $user->id)
            ->where('uuid', $uuid)
            ->first();

        // Auto-sync commerce_id if device doesn't have one but user does
        if ($device && !$device->commerce_id && $user->commerce_id) {
            $device->update(['commerce_id' => $user->commerce_id]);
            
            Log::info('Device commerce_id auto-synced from user', [
                'device_id' => $device->id,
                'commerce_id' => $user->commerce_id,
            ]);
        }

        return $device;
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
