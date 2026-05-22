<?php

namespace App\Http\Controllers;

use App\Http\Requests\Device\UpdateDeviceHealthRequest;
use App\Services\DeviceService;
use Illuminate\Http\JsonResponse;
use Illuminate\Support\Facades\Log;

class DeviceHealthController extends Controller
{
    public function __construct(
        protected DeviceService $deviceService
    ) {}

    /**
     * Update device health information.
     * 
     * Professional Architecture Approach:
     * - Authentication is OPTIONAL (devices can be linked without user)
     * - If user is authenticated, verify device belongs to user (security)
     * - If not authenticated, allow health updates for any device (devices can exist without user)
     * 
     * POST /api/devices/{id}/health
     */
    public function update(UpdateDeviceHealthRequest $request, int $id): JsonResponse
    {
        try {
            $user = $request->user(); // Can be null if not authenticated
            
            // Find device directly (not filtered by user)
            $device = \App\Models\Device::findOrFail($id);
            
            // If user is authenticated, verify device belongs to user (security check)
            if ($user) {
                // Device must belong to the authenticated user OR have no user (linked without auth)
                if ($device->user_id !== null && $device->user_id !== $user->id) {
                    return response()->json([
                        'message' => 'No tienes permisos para actualizar este dispositivo',
                    ], 403);
                }
            }
            // If not authenticated, allow update (device may have user_id = null)

            $device = $this->deviceService->updateHealth($device, $request->validated());

            return response()->json([
                'message' => 'Device health updated successfully',
                'device' => $device,
                'health' => $device->getHealthStatus(),
            ]);
        } catch (\Illuminate\Database\Eloquent\ModelNotFoundException $e) {
            Log::warning('Device not found for health update', [
                'device_id' => $id,
                'user_id' => $request->user()?->id,
            ]);

            return response()->json([
                'message' => 'Dispositivo no encontrado',
            ], 404);
        } catch (\Exception $e) {
            Log::error('Failed to update device health', [
                'user_id' => $request->user()?->id,
                'device_id' => $id,
                'error' => $e->getMessage(),
                'file' => $e->getFile(),
                'line' => $e->getLine(),
                'trace' => $e->getTraceAsString(),
            ]);

            $errorDetails = config('app.debug') ? [
                'message' => $e->getMessage(),
                'file' => $e->getFile(),
                'line' => $e->getLine(),
                'exception' => get_class($e),
            ] : [
                'message' => 'Error al actualizar el estado del dispositivo. Por favor, contacta al soporte.',
            ];

            return response()->json([
                'message' => 'Failed to update device health',
                'error' => $errorDetails,
            ], 500);
        }
    }
}

