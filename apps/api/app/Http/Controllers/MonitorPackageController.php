<?php

namespace App\Http\Controllers;

use App\Http\Requests\MonitorPackage\CreateMonitorPackageRequest;
use App\Http\Requests\MonitorPackage\UpdateMonitorPackageRequest;
use App\Models\MonitorPackage;
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
     * Filters by user's commerce_id automatically.
     */
    public function index(Request $request): JsonResponse
    {
        $user = $request->user();
        $activeOnly = $request->boolean('active_only', false);
        
        // Get packages filtered by user's commerce
        $packages = $this->monitorPackageService->getAllPackages($user->commerce_id);
        
        if ($activeOnly) {
            $packages = $packages->where('is_active', true)->values();
        }

        return response()->json([
            'packages' => $packages,
        ]);
    }

    /**
     * Create a new monitor package.
     * Automatically assigns to user's commerce.
     */
    public function store(CreateMonitorPackageRequest $request): JsonResponse
    {
        $user = $request->user();

        // Ensure user has commerce
        if (!$user->commerce_id) {
            return response()->json([
                'message' => 'Usuario debe pertenecer a un negocio para crear paquetes monitoreados',
            ], 400);
        }

        $data = $request->validated();
        
        // Ensure commerce_id is set to user's commerce
        $data['commerce_id'] = $user->commerce_id;

        $package = $this->monitorPackageService->createPackage($data);

        return response()->json([
            'message' => 'Monitor package created successfully',
            'package' => $package,
        ], 201);
    }

    /**
     * Get a specific monitor package.
     * Verifies it belongs to user's commerce.
     */
    public function show(Request $request, int $id): JsonResponse
    {
        $user = $request->user();
        $package = $this->monitorPackageService->getPackageById($id);

        if (! $package) {
            return response()->json([
                'message' => 'Monitor package not found',
            ], 404);
        }

        // Verify package belongs to user's commerce
        if ($user->commerce_id && $package->commerce_id !== $user->commerce_id) {
            return response()->json([
                'message' => 'Monitor package no pertenece a tu negocio',
            ], 403);
        }

        return response()->json([
            'package' => $package,
        ]);
    }

    /**
     * Update a monitor package.
     * Verifies it belongs to user's commerce.
     */
    public function update(UpdateMonitorPackageRequest $request, int $id): JsonResponse
    {
        $user = $request->user();
        $package = $this->monitorPackageService->getPackageById($id);

        if (! $package) {
            return response()->json([
                'message' => 'Monitor package not found',
            ], 404);
        }

        // Verify package belongs to user's commerce
        if ($user->commerce_id && $package->commerce_id !== $user->commerce_id) {
            return response()->json([
                'message' => 'Monitor package no pertenece a tu negocio',
            ], 403);
        }

        $package = $this->monitorPackageService->updatePackage($package, $request->validated());

        return response()->json([
            'message' => 'Monitor package updated successfully',
            'package' => $package,
        ]);
    }

    /**
     * Delete a monitor package.
     * Verifies it belongs to user's commerce.
     */
    public function destroy(Request $request, int $id): JsonResponse
    {
        $user = $request->user();
        $package = $this->monitorPackageService->getPackageById($id);

        if (! $package) {
            return response()->json([
                'message' => 'Monitor package not found',
            ], 404);
        }

        // Verify package belongs to user's commerce
        if ($user->commerce_id && $package->commerce_id !== $user->commerce_id) {
            return response()->json([
                'message' => 'Monitor package no pertenece a tu negocio',
            ], 403);
        }

        $this->monitorPackageService->deletePackage($package);

        return response()->json([
            'message' => 'Monitor package deleted successfully',
        ]);
    }

    /**
     * Toggle package active status.
     * Verifies it belongs to user's commerce.
     */
    public function toggleStatus(Request $request, int $id): JsonResponse
    {
        $user = $request->user();
        $request->validate([
            'is_active' => 'sometimes|boolean',
        ]);

        $package = $this->monitorPackageService->getPackageById($id);

        if (! $package) {
            return response()->json([
                'message' => 'Monitor package not found',
            ], 404);
        }

        // Verify package belongs to user's commerce
        if ($user->commerce_id && $package->commerce_id !== $user->commerce_id) {
            return response()->json([
                'message' => 'Monitor package no pertenece a tu negocio',
            ], 403);
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
     * Automatically assigns to user's commerce.
     */
    public function bulkCreate(Request $request): JsonResponse
    {
        $user = $request->user();

        // Ensure user has commerce
        if (!$user->commerce_id) {
            return response()->json([
                'message' => 'Usuario debe pertenecer a un negocio para crear paquetes monitoreados',
            ], 400);
        }

        $request->validate([
            'packages' => 'required|array',
            'packages.*' => 'required|string|regex:/^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$/',
        ]);

        // Create packages with commerce_id
        $packageNames = $request->input('packages');
        $packages = collect();
        
        foreach ($packageNames as $packageName) {
            // Skip if already exists for this commerce
            if (MonitorPackage::where('package_name', $packageName)
                ->where('commerce_id', $user->commerce_id)
                ->exists()) {
                continue;
            }

            $packages->push(
                MonitorPackage::create([
                    'package_name' => $packageName,
                    'commerce_id' => $user->commerce_id,
                    'is_active' => true,
                ])
            );
        }

        return response()->json([
            'message' => 'Packages created successfully',
            'created_count' => $packages->count(),
            'packages' => $packages,
        ], 201);
    }
}

