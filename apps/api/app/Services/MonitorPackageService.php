<?php

namespace App\Services;

use App\Models\MonitorPackage;
use Illuminate\Support\Collection;

class MonitorPackageService
{
    /**
     * Get all active monitor packages as a simple array.
     * This is used for the public API endpoint that clients consume.
     * Filters by commerce_id if provided.
     */
    public function getActivePackagesArray(?int $commerceId = null): array
    {
        $query = MonitorPackage::active()->ordered();

        if ($commerceId) {
            $query->where('commerce_id', $commerceId);
        }

        return $query->pluck('package_name')->toArray();
    }

    /**
     * Get all monitor packages (for admin/management).
     * Filters by commerce_id if provided.
     */
    public function getAllPackages(?int $commerceId = null)
    {
        $query = MonitorPackage::ordered();

        if ($commerceId) {
            $query->where('commerce_id', $commerceId);
        }

        return $query->get();
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
     */
    public function bulkCreatePackages(array $packageNames): Collection
    {
        $packages = collect();

        foreach ($packageNames as $packageName) {
            // Skip if already exists
            if (MonitorPackage::where('package_name', $packageName)->exists()) {
                continue;
            }

            $packages->push(
                MonitorPackage::create([
                    'package_name' => $packageName,
                    'is_active' => true,
                ])
            );
        }

        return $packages;
    }
}

