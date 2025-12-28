<?php

namespace App\Services;

use App\Models\Device;
use App\Models\DeviceMonitoredApp;
use App\Models\User;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Str;

class DeviceService
{
    /**
     * Create a new device for a user, or return existing device if UUID already exists.
     * Automatically assigns commerce_id from user if available.
     * 
     * This implements "find or create" pattern based on UUID:
     * - If device with UUID exists for this user, return existing device
     * - If UUID doesn't exist, create new device
     * - This ensures UUID uniqueness and prevents duplicate device creation
     */
    public function createDevice(User $user, array $data): Device
    {
        // Ensure user has commerce_id (refresh to get latest)
        $user->refresh();
        $commerceId = $user->commerce_id ?? $data['commerce_id'] ?? null;

        // If UUID is provided, check if device already exists
        if (isset($data['uuid']) && !empty($data['uuid'])) {
            $existingDevice = Device::where('uuid', $data['uuid'])
                ->where('user_id', $user->id)
                ->first();

            if ($existingDevice) {
                // Device already exists with this UUID for this user
                // Update commerce_id if user has one and device doesn't
                if (!$existingDevice->commerce_id && $commerceId) {
                    $existingDevice->update(['commerce_id' => $commerceId]);
                    Log::info('Device commerce_id updated during find-or-create', [
                        'device_id' => $existingDevice->id,
                        'commerce_id' => $commerceId,
                    ]);
                }

                // Update last_seen_at to indicate device is active
                $existingDevice->update(['last_seen_at' => now()]);

                Log::info('Device found by UUID (find-or-create)', [
                    'device_id' => $existingDevice->id,
                    'uuid' => $data['uuid'],
                    'user_id' => $user->id,
                ]);

                return $existingDevice->fresh();
            }
        }

        // Device doesn't exist, create new one
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

        Log::info('Device created (new)', [
            'device_id' => $device->id,
            'uuid' => $device->uuid,
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

    /**
     * Update device health information.
     *
     * @param Device $device
     * @param array $data
     * @return Device
     */
    public function updateHealth(Device $device, array $data): Device
    {
        $updateData = [];

        if (isset($data['battery_level'])) {
            $updateData['battery_level'] = $data['battery_level'];
        }

        if (isset($data['battery_optimization_disabled'])) {
            $updateData['battery_optimization_disabled'] = $data['battery_optimization_disabled'];
        }

        if (isset($data['notification_permission_enabled'])) {
            $updateData['notification_permission_enabled'] = $data['notification_permission_enabled'];
        }

        // Always update last_heartbeat when health is reported
        $updateData['last_heartbeat'] = now();

        $device->update($updateData);

        Log::info('Device health updated', [
            'device_id' => $device->id,
            'battery_level' => $updateData['battery_level'] ?? null,
            'battery_optimization_disabled' => $updateData['battery_optimization_disabled'] ?? null,
            'notification_permission_enabled' => $updateData['notification_permission_enabled'] ?? null,
        ]);

        return $device->fresh();
    }

    /**
     * Sync monitored apps for a device.
     * Removes apps not in the list and creates new ones.
     *
     * @param Device $device
     * @param array $packageNames
     * @return void
     */
    public function syncMonitoredApps(Device $device, array $packageNames): void
    {
        // Get current monitored apps
        $currentApps = $device->monitoredApps()->pluck('package_name')->toArray();

        // Apps to remove (in current but not in new list)
        $appsToRemove = array_diff($currentApps, $packageNames);

        // Apps to add (in new list but not in current)
        $appsToAdd = array_diff($packageNames, $currentApps);

        // Remove apps that are no longer in the list
        if (!empty($appsToRemove)) {
            DeviceMonitoredApp::where('device_id', $device->id)
                ->whereIn('package_name', $appsToRemove)
                ->delete();

            Log::info('Removed monitored apps from device', [
                'device_id' => $device->id,
                'removed_packages' => $appsToRemove,
            ]);
        }

        // Add new apps
        foreach ($appsToAdd as $packageName) {
            DeviceMonitoredApp::create([
                'device_id' => $device->id,
                'package_name' => $packageName,
                'enabled' => true,
            ]);
        }

        if (!empty($appsToAdd)) {
            Log::info('Added monitored apps to device', [
                'device_id' => $device->id,
                'added_packages' => $appsToAdd,
            ]);
        }

        // If no changes, log it
        if (empty($appsToRemove) && empty($appsToAdd)) {
            Log::debug('No changes to monitored apps', [
                'device_id' => $device->id,
                'package_names' => $packageNames,
            ]);
        }
    }
}
