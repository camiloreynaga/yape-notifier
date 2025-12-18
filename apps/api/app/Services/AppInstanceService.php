<?php

namespace App\Services;

use App\Models\AppInstance;
use App\Models\Device;

class AppInstanceService
{
    /**
     * Find or create an app instance.
     */
    public function findOrCreate(
        Device $device,
        string $packageName,
        ?int $androidUserId = null,
        ?string $instanceLabel = null
    ): ?AppInstance {
        // If no androidUserId provided, we can't create an instance
        if ($androidUserId === null) {
            return null;
        }

        $commerceId = $device->commerce_id ?? $device->user->commerce_id;

        if (!$commerceId) {
            return null;
        }

        return AppInstance::findOrCreate(
            $commerceId,
            $device->id,
            $packageName,
            $androidUserId,
            $instanceLabel
        );
    }

    /**
     * Update instance label.
     */
    public function updateLabel(AppInstance $instance, string $label): AppInstance
    {
        $instance->update(['instance_label' => $label]);

        return $instance->fresh();
    }

    /**
     * Get app instances for a device.
     */
    public function getDeviceInstances(Device $device)
    {
        return AppInstance::where('device_id', $device->id)
            ->orderBy('package_name')
            ->orderBy('android_user_id')
            ->get();
    }

    /**
     * Get app instances for a commerce.
     */
    public function getCommerceInstances(int $commerceId, ?int $deviceId = null)
    {
        $query = AppInstance::where('commerce_id', $commerceId)
            ->with('device')
            ->orderBy('package_name')
            ->orderBy('android_user_id');

        if ($deviceId) {
            $query->where('device_id', $deviceId);
        }

        return $query->get();
    }
}


