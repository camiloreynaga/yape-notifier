<?php

namespace App\Http\Controllers;

use App\Http\Requests\MonitorPackage\CreateMonitorPackageRequest;
use App\Http\Requests\MonitorPackage\UpdateMonitorPackageRequest;
use App\Services\MonitorPackageService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class MonitorPackageController extends Controller
{
    public function __construct(
        protected MonitorPackageService $monitorPackageService
    ) {}

    /**
     * Get active packages for clients (public endpoint).
     * This endpoint is used by Android clients to get the list of packages to monitor.
     */
    public function getActivePackages(): JsonResponse
    {
        $packages = $this->monitorPackageService->getActivePackagesArray();

        return response()->json([
            'packages' => $packages,
        ]);
    }

    /**
     * List all monitor packages (admin/management).
     */
    public function index(Request $request): JsonResponse
    {
        $activeOnly = $request->boolean('active_only', false);
        
        if ($activeOnly) {
            $packages = $this->monitorPackageService->getAllPackages()
                ->where('is_active', true)
                ->values();
        } else {
            $packages = $this->monitorPackageService->getAllPackages();
        }

        return response()->json([
            'packages' => $packages,
        ]);
    }

    /**
     * Create a new monitor package.
     */
    public function store(CreateMonitorPackageRequest $request): JsonResponse
    {
        $package = $this->monitorPackageService->createPackage($request->validated());

        return response()->json([
            'message' => 'Monitor package created successfully',
            'package' => $package,
        ], 201);
    }

    /**
     * Get a specific monitor package.
     */
    public function show(int $id): JsonResponse
    {
        $package = $this->monitorPackageService->getPackageById($id);

        if (! $package) {
            return response()->json([
                'message' => 'Monitor package not found',
            ], 404);
        }

        return response()->json([
            'package' => $package,
        ]);
    }

    /**
     * Update a monitor package.
     */
    public function update(UpdateMonitorPackageRequest $request, int $id): JsonResponse
    {
        $package = $this->monitorPackageService->getPackageById($id);

        if (! $package) {
            return response()->json([
                'message' => 'Monitor package not found',
            ], 404);
        }

        $package = $this->monitorPackageService->updatePackage($package, $request->validated());

        return response()->json([
            'message' => 'Monitor package updated successfully',
            'package' => $package,
        ]);
    }

    /**
     * Delete a monitor package.
     */
    public function destroy(int $id): JsonResponse
    {
        $package = $this->monitorPackageService->getPackageById($id);

        if (! $package) {
            return response()->json([
                'message' => 'Monitor package not found',
            ], 404);
        }

        $this->monitorPackageService->deletePackage($package);

        return response()->json([
            'message' => 'Monitor package deleted successfully',
        ]);
    }

    /**
     * Toggle package active status.
     */
    public function toggleStatus(Request $request, int $id): JsonResponse
    {
        $request->validate([
            'is_active' => 'sometimes|boolean',
        ]);

        $package = $this->monitorPackageService->getPackageById($id);

        if (! $package) {
            return response()->json([
                'message' => 'Monitor package not found',
            ], 404);
        }

        $isActive = $request->boolean('is_active', ! $package->is_active);
        $package = $this->monitorPackageService->togglePackageStatus($package, $isActive);

        return response()->json([
            'message' => 'Monitor package status updated',
            'package' => $package,
        ]);
    }

    /**
     * Bulk create packages from an array.
     */
    public function bulkCreate(Request $request): JsonResponse
    {
        $request->validate([
            'packages' => 'required|array',
            'packages.*' => 'required|string|regex:/^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$/',
        ]);

        $packages = $this->monitorPackageService->bulkCreatePackages($request->input('packages'));

        return response()->json([
            'message' => 'Packages created successfully',
            'created_count' => $packages->count(),
            'packages' => $packages,
        ], 201);
    }
}

