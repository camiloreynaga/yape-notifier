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
     * Professional Architecture Approach - Handles QR Linking Flow:
     * 1. Search by UUID first (primary identifier) - regardless of user_id
     * 2. If device exists:
     *    a. If device has no user_id → associate with current user (QR link scenario)
     *    b. If device has different user_id → reject (security violation)
     *    c. If device belongs to current user → update and return
     * 3. If device doesn't exist → create new device
     * 
     * This prevents duplicate UUID errors when:
     * - Device is linked via QR without authentication (user_id = null)
     * - User logs in and tries to register the same device
     */
    public function createDevice(User $user, array $data): Device
    {
        // Ensure user has commerce_id (refresh to get latest)
        $user->refresh();
        $commerceId = $user->commerce_id ?? $data['commerce_id'] ?? null;

        // If UUID is provided, check if device already exists (search by UUID only)
        if (isset($data['uuid']) && !empty($data['uuid'])) {
            $existingDevice = Device::where('uuid', $data['uuid'])->first();

            if ($existingDevice) {
                // Device exists - handle different scenarios
                
                // Scenario 1: Device has no user_id (linked via QR without auth)
                // → Associate with current user
                if (!$existingDevice->user_id) {
                    // Multi-tenant security: verify commerce_id compatibility
                    if ($existingDevice->commerce_id && $commerceId && $existingDevice->commerce_id !== $commerceId) {
                        Log::warning('Device commerce_id mismatch during user association', [
                            'device_id' => $existingDevice->id,
                            'device_commerce_id' => $existingDevice->commerce_id,
                            'user_id' => $user->id,
                            'user_commerce_id' => $commerceId,
                        ]);
                        
                        throw new \RuntimeException(
                            'Este dispositivo pertenece a otro negocio. No se puede vincular a tu cuenta.'
                        );
                    }
                    
                    // Associate device with user
                    $updateData = ['user_id' => $user->id, 'last_seen_at' => now()];
                    
                    // Sync commerce_id if device doesn't have one but user does
                    if (!$existingDevice->commerce_id && $commerceId) {
                        $updateData['commerce_id'] = $commerceId;
                    }
                    
                    $existingDevice->update($updateData);
                    
                    Log::info('Device associated with user after QR link', [
                        'device_id' => $existingDevice->id,
                        'uuid' => $data['uuid'],
                        'user_id' => $user->id,
                        'commerce_id' => $existingDevice->commerce_id,
                    ]);
                    
                    return $existingDevice->fresh();
                }
                
                // Scenario 2: Device belongs to a different user
                // → Security violation - reject
                if ($existingDevice->user_id !== $user->id) {
                    Log::warning('Attempted to register device belonging to another user', [
                        'device_id' => $existingDevice->id,
                        'device_user_id' => $existingDevice->user_id,
                        'requesting_user_id' => $user->id,
                        'uuid' => $data['uuid'],
                    ]);
                    
                    throw new \RuntimeException(
                        'Este dispositivo ya está registrado con otra cuenta.'
                    );
                }
                
                // Scenario 3: Device already belongs to current user
                // → Update and return
                
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
     *
     * If user has commerce_id, returns all devices for that commerce (admin view).
     * Otherwise, returns only devices belonging to the user.
     */
    public function getUserDevices(User $user, bool $activeOnly = false)
    {
        // If user has commerce_id, show all devices for that commerce
        if ($user->commerce_id) {
            $query = Device::where('commerce_id', $user->commerce_id);
        } else {
            // Fallback: show only user's own devices
            $query = Device::where('user_id', $user->id);
        }

        if ($activeOnly) {
            $query->where('is_active', true);
        }

        return $query->orderBy('created_at', 'desc')->get();
    }

    /**
     * Find device by UUID and user.
     * 
     * Professional Architecture Approach:
     * - Devices can be linked without user_id (flexible UX for capturer mode)
     * - UUID is the primary identifier - if device exists, use it
     * - Multi-tenant security: device commerce_id must match user commerce_id (if both exist)
     * - Auto-sync user_id if device doesn't have one but user is authenticated
     * - Auto-sync commerce_id ONLY if device doesn't have one (device's commerce_id takes priority)
     * - This ensures devices linked via QR code work correctly while maintaining security
     * 
     * Security Rules:
     * 1. If device has commerce_id AND user has commerce_id → must match (multi-tenant security)
     * 2. If device has commerce_id but user doesn't → allow (device was linked via QR)
     * 3. If device doesn't have commerce_id but user does → sync (safe operation)
     * 4. If neither has commerce_id → allow (edge case, will be handled later)
     */
    public function findDeviceByUuid(User $user, string $uuid): ?Device
    {
        // Find device by UUID (UUID is the primary identifier)
        $device = Device::where('uuid', $uuid)->first();

        if (!$device) {
            return null;
        }

        // Multi-tenant security validation
        // If both device and user have commerce_id, they must match
        if ($device->commerce_id && $user->commerce_id) {
            if ($device->commerce_id !== $user->commerce_id) {
                // Security violation: device belongs to different commerce
                Log::warning('Device commerce_id mismatch - multi-tenant security violation', [
                    'device_id' => $device->id,
                    'device_uuid' => $uuid,
                    'device_commerce_id' => $device->commerce_id,
                    'user_id' => $user->id,
                    'user_commerce_id' => $user->commerce_id,
                ]);
                return null; // Reject for security
            }
        }

        // Auto-sync user_id if device doesn't have one but user is authenticated
        // This allows devices linked without auth to be associated with authenticated users
        if (!$device->user_id && $user->id) {
            $device->update(['user_id' => $user->id]);
            Log::info('Device user_id auto-synced from authenticated user', [
                'device_id' => $device->id,
                'user_id' => $user->id,
            ]);
        }

        // Auto-sync commerce_id ONLY if device doesn't have one
        // If device has commerce_id (from QR link), it takes priority (already validated above)
        // This ensures devices linked via QR code work correctly
        if (!$device->commerce_id && $user->commerce_id) {
            $device->update(['commerce_id' => $user->commerce_id]);
            
            Log::info('Device commerce_id auto-synced from user', [
                'device_id' => $device->id,
                'commerce_id' => $user->commerce_id,
            ]);
        }

        return $device->fresh();
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
     * Unlink a device from its commerce.
     * 
     * Professional Architecture Approach (QR Authorization):
     * - Clears commerce_id to allow re-linking to a different commerce
     * - Also clears user_id to allow complete re-association
     * - Preserves device UUID (physical device identifier)
     * - Does NOT delete the device (maintains historical data)
     * - Logs the operation for audit trail
     * 
     * Use cases:
     * - Device needs to be transferred to another commerce
     * - Device was linked to wrong commerce by mistake
     * - Commerce wants to remove a device from their fleet
     * - Device needs to be reset to "unlinked" state
     * 
     * Security:
     * - Only the commerce admin can unlink devices from their commerce
     * - Verified in controller layer (user must belong to device's commerce)
     * 
     * After unlinking:
     * - Device can be re-linked via QR code to any commerce
     * - Device cannot send notifications (no commerce_id)
     * - Device UUID is preserved for re-linking
     * 
     * @param Device $device Device to unlink
     * @return Device Updated device
     */
    public function unlinkDevice(Device $device): Device
    {
        $oldCommerceId = $device->commerce_id;
        $oldUserId = $device->user_id;
        
        // Clear both commerce and user linkage to allow complete re-association
        // This is the "reset to unlinked state" operation
        $device->update([
            'commerce_id' => null,
            'user_id' => null, // Also clear user_id for complete reset
            'is_active' => false, // Deactivate device for security
            'last_seen_at' => now(),
        ]);

        Log::info('Device unlinked from commerce and user', [
            'device_id' => $device->id,
            'device_uuid' => $device->uuid,
            'old_commerce_id' => $oldCommerceId,
            'old_user_id' => $oldUserId,
        ]);

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

        // NEW: Real service connection state (not just permission)
        if (isset($data['notification_service_connected'])) {
            $updateData['notification_service_connected'] = $data['notification_service_connected'];
        }

        // NEW: Count of pending notifications in local DB
        if (isset($data['pending_notifications_count'])) {
            $updateData['pending_notifications_count'] = $data['pending_notifications_count'];
        }

        // NEW: Timestamp of last captured notification
        if (isset($data['last_notification_captured_at'])) {
            // Convert from milliseconds timestamp to Carbon datetime
            $updateData['last_notification_captured_at'] = \Carbon\Carbon::createFromTimestampMs($data['last_notification_captured_at']);
        }

        // Always update last_heartbeat when health is reported
        $updateData['last_heartbeat'] = now();

        $device->update($updateData);

        Log::info('Device health updated', [
            'device_id' => $device->id,
            'battery_level' => $updateData['battery_level'] ?? null,
            'battery_optimization_disabled' => $updateData['battery_optimization_disabled'] ?? null,
            'notification_permission_enabled' => $updateData['notification_permission_enabled'] ?? null,
            'notification_service_connected' => $updateData['notification_service_connected'] ?? null,
            'pending_notifications_count' => $updateData['pending_notifications_count'] ?? null,
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
