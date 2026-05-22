<?php

namespace App\Http\Controllers;

use App\Http\Requests\Device\UpdateDeviceMonitoredAppsRequest;
use App\Models\Device;
use App\Models\DeviceMonitoredApp;
use App\Services\DeviceService;
use Illuminate\Http\JsonResponse;
use Illuminate\Support\Facades\Log;

class DeviceMonitoredAppController extends Controller
{
    public function __construct(
        protected DeviceService $deviceService
    ) {}

    /**
     * Get monitored apps for a device.
     * GET /api/devices/{id}/monitored-apps
     */
    public function index(int $id): JsonResponse
    {
        try {
            $user = request()->user();
            
            // Find device and verify it belongs to the user
            $device = Device::where('id', $id)
                ->where('user_id', $user->id)
                ->firstOrFail();

            // Verify device belongs to user's commerce
            if ($user->commerce_id && $device->commerce_id !== $user->commerce_id) {
                return response()->json([
                    'message' => 'Dispositivo no pertenece a tu negocio',
                ], 403);
            }

            // Get monitored apps with monitor package information
            $monitoredApps = DeviceMonitoredApp::where('device_id', $device->id)
                ->with(['device'])
                ->get()
                ->map(function ($monitoredApp) use ($device) {
                    // Get monitor package for this app
                    $monitorPackage = null;
                    if ($device->commerce_id) {
                        $monitorPackage = \App\Models\MonitorPackage::where('package_name', $monitoredApp->package_name)
                            ->where('commerce_id', $device->commerce_id)
                            ->first();
                    }

                    return [
                        'id' => $monitoredApp->id,
                        'device_id' => $monitoredApp->device_id,
                        'package_name' => $monitoredApp->package_name,
                        'enabled' => $monitoredApp->enabled,
                        'created_at' => $monitoredApp->created_at?->toIso8601String(),
                        'updated_at' => $monitoredApp->updated_at?->toIso8601String(),
                        'monitor_package' => $monitorPackage ? [
                            'id' => $monitorPackage->id,
                            'app_name' => $monitorPackage->app_name,
                            'description' => $monitorPackage->description,
                            'is_active' => $monitorPackage->is_active,
                            'priority' => $monitorPackage->priority,
                        ] : null,
                    ];
                });

            return response()->json([
                'monitored_apps' => $monitoredApps,
            ]);
        } catch (\Illuminate\Database\Eloquent\ModelNotFoundException $e) {
            return response()->json([
                'message' => 'Dispositivo no encontrado',
            ], 404);
        } catch (\Exception $e) {
            Log::error('Failed to get monitored apps', [
                'user_id' => request()->user()->id,
                'device_id' => $id,
                'error' => $e->getMessage(),
                'trace' => $e->getTraceAsString(),
            ]);

            return response()->json([
                'message' => 'Error al obtener apps monitoreadas',
                'error' => config('app.debug') ? $e->getMessage() : null,
            ], 500);
        }
    }

    /**
     * Sync monitored apps for a device.
     * POST /api/devices/{id}/monitored-apps
     */
    public function store(UpdateDeviceMonitoredAppsRequest $request, int $id): JsonResponse
    {
        try {
            $user = $request->user();
            
            // Find device and verify it belongs to the user
            $device = Device::where('id', $id)
                ->where('user_id', $user->id)
                ->firstOrFail();

            // Verify device has commerce_id first (before checking if it matches user's commerce)
            if (!$device->commerce_id) {
                return response()->json([
                    'message' => 'El dispositivo debe estar vinculado a un negocio',
                ], 400);
            }

            // Verify device belongs to user's commerce
            if ($user->commerce_id && $device->commerce_id !== $user->commerce_id) {
                return response()->json([
                    'message' => 'Dispositivo no pertenece a tu negocio',
                ], 403);
            }

            // Verify all package names exist in MonitorPackage for this commerce
            $packageNames = $request->input('package_names');
            $existingPackages = \App\Models\MonitorPackage::where('commerce_id', $device->commerce_id)
                ->where('is_active', true)
                ->whereIn('package_name', $packageNames)
                ->pluck('package_name')
                ->toArray();

            $missingPackages = array_diff($packageNames, $existingPackages);
            if (!empty($missingPackages)) {
                return response()->json([
                    'message' => 'Algunos paquetes no existen o no están activos en tu negocio',
                    'missing_packages' => $missingPackages,
                ], 422);
            }

            // Sync monitored apps
            $this->deviceService->syncMonitoredApps($device, $packageNames);

            // Get updated list
            $monitoredApps = DeviceMonitoredApp::where('device_id', $device->id)->get();

            Log::info('Monitored apps synced for device', [
                'device_id' => $device->id,
                'user_id' => $user->id,
                'package_names' => $packageNames,
                'total_apps' => $monitoredApps->count(),
            ]);

            return response()->json([
                'message' => 'Apps monitoreadas actualizadas exitosamente',
                'monitored_apps' => $monitoredApps,
            ], 200);
        } catch (\Illuminate\Database\Eloquent\ModelNotFoundException $e) {
            return response()->json([
                'message' => 'Dispositivo no encontrado',
            ], 404);
        } catch (\Exception $e) {
            Log::error('Failed to sync monitored apps', [
                'user_id' => $request->user()->id,
                'device_id' => $id,
                'package_names' => $request->input('package_names', []),
                'error' => $e->getMessage(),
                'trace' => $e->getTraceAsString(),
            ]);

            return response()->json([
                'message' => 'Error al actualizar apps monitoreadas',
                'error' => config('app.debug') ? $e->getMessage() : null,
            ], 500);
        }
    }
}

