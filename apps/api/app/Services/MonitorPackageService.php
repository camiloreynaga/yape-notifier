<?php

namespace App\Services;

use App\Models\MonitorPackage;
use Illuminate\Support\Collection;

class MonitorPackageService
{
    /**
     * Get all active monitor packages as a simple array.
     * This is used for the public API endpoint that clients consume.
     * Requires commerceId to return packages for that specific commerce.
     */
    public function getActivePackagesArray(?int $commerceId = null): array
    {
        if (!$commerceId) {
            // If no commerce_id provided, return empty array
            // Public endpoint should provide commerce_id via device linking
            return [];
        }

        return MonitorPackage::where('commerce_id', $commerceId)
            ->active()
            ->ordered()
            ->pluck('package_name')
            ->toArray();
    }

    /**
     * Get all monitor packages (for admin/management).
     * Returns packages for the specified commerce.
     * 
     * @param int|null $commerceId Commerce ID (required)
     * @return \Illuminate\Database\Eloquent\Collection
     */
    public function getAllPackages(?int $commerceId = null)
    {
        if (!$commerceId) {
            return collect();
        }

        return MonitorPackage::where('commerce_id', $commerceId)
            ->ordered()
            ->get();
    }

    /**
     * Get a specific monitor package by ID.
     */
    public function getPackageById(int $id): ?MonitorPackage
    {
        return MonitorPackage::find($id);
    }

    /**
     * Create a new monitor package.
     * Automatically assigns commerce_id if not provided.
     */
    public function createPackage(array $data, ?int $defaultCommerceId = null): MonitorPackage
    {
        // If commerce_id not provided, use default
        if (!isset($data['commerce_id']) && $defaultCommerceId) {
            $data['commerce_id'] = $defaultCommerceId;
        }

        return MonitorPackage::create($data);
    }

    /**
     * Update a monitor package.
     */
    public function updatePackage(MonitorPackage $package, array $data): MonitorPackage
    {
        $package->update($data);

        return $package->fresh();
    }

    /**
     * Delete a monitor package.
     */
    public function deletePackage(MonitorPackage $package): bool
    {
        return $package->delete();
    }

    /**
     * Toggle package active status.
     */
    public function togglePackageStatus(MonitorPackage $package, bool $isActive): MonitorPackage
    {
        $package->update(['is_active' => $isActive]);

        return $package->fresh();
    }

    /**
     * Bulk create packages from an array of package names.
     * Checks for duplicates within the commerce.
     * 
     * @param array $packageNames Array of package names
     * @param int $commerceId Commerce ID (required)
     * @return Collection Created packages
     */
    public function bulkCreatePackages(array $packageNames, int $commerceId): Collection
    {
        $packages = collect();

        foreach ($packageNames as $packageName) {
            // Check if package already exists for this commerce
            $exists = MonitorPackage::where('commerce_id', $commerceId)
                ->where('package_name', $packageName)
                ->exists();

            if ($exists) {
                continue;
            }

            $packages->push(
                MonitorPackage::create([
                    'package_name' => $packageName,
                    'commerce_id' => $commerceId,
                    'is_active' => true,
                ])
            );
        }

        return $packages;
    }

    /**
     * Get detected package names from notifications for a commerce.
     * Returns unique package names that have sent notifications.
     */
    public function getDetectedPackagesFromNotifications(?int $commerceId = null): array
    {
        $query = \App\Models\Notification::query()
            ->whereNotNull('package_name')
            ->where('package_name', '!=', '');

        if ($commerceId) {
            $query->where('commerce_id', $commerceId);
        }

        return $query
            ->distinct()
            ->pluck('package_name')
            ->toArray();
    }

    /**
     * Get detected package names from app instances for a commerce.
     * Returns unique package names from app instances.
     */
    public function getDetectedPackagesFromAppInstances(?int $commerceId = null): array
    {
        $query = \App\Models\AppInstance::query()
            ->whereNotNull('package_name')
            ->where('package_name', '!=', '');

        if ($commerceId) {
            $query->where('commerce_id', $commerceId);
        }

        return $query
            ->distinct()
            ->pluck('package_name')
            ->toArray();
    }

    /**
     * Get all detected packages (from notifications and app instances) for a commerce.
     * Returns packages that are not yet in MonitorPackage list.
     */
    public function getUndetectedPackages(?int $commerceId = null): array
    {
        // Get packages from notifications
        $fromNotifications = $this->getDetectedPackagesFromNotifications($commerceId);
        
        // Get packages from app instances
        $fromAppInstances = $this->getDetectedPackagesFromAppInstances($commerceId);
        
        // Merge and get unique
        $allDetected = array_unique(array_merge($fromNotifications, $fromAppInstances));
        
        // Get existing monitor packages for this commerce
        $existing = MonitorPackage::where('commerce_id', $commerceId)
            ->pluck('package_name')
            ->toArray();
        
        // Return only packages that are not yet configured
        return array_values(array_diff($allDetected, $existing));
    }
}

