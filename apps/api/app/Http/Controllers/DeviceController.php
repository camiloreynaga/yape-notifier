<?php

namespace App\Http\Controllers;

use App\Http\Requests\Device\CreateDeviceRequest;
use App\Http\Requests\Device\UpdateDeviceRequest;
use App\Services\DeviceService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class DeviceController extends Controller
{
    public function __construct(
        protected DeviceService $deviceService
    ) {}

    /**
     * List all devices for the authenticated user.
     */
    public function index(Request $request): JsonResponse
    {
        $activeOnly = $request->boolean('active_only', false);
        $devices = $this->deviceService->getUserDevices($request->user(), $activeOnly);

        return response()->json([
            'devices' => $devices,
        ]);
    }

    /**
     * Create a new device.
     */
    public function store(CreateDeviceRequest $request): JsonResponse
    {
        $device = $this->deviceService->createDevice($request->user(), $request->validated());

        return response()->json([
            'message' => 'Device created successfully',
            'device' => $device,
        ], 201);
    }

    /**
     * Get a specific device.
     */
    public function show(Request $request, int $id): JsonResponse
    {
        $device = $request->user()->devices()->findOrFail($id);

        return response()->json([
            'device' => $device,
        ]);
    }

    /**
     * Update a device.
     */
    public function update(UpdateDeviceRequest $request, int $id): JsonResponse
    {
        $device = $request->user()->devices()->findOrFail($id);
        $device = $this->deviceService->updateDevice($device, $request->validated());

        return response()->json([
            'message' => 'Device updated successfully',
            'device' => $device,
        ]);
    }

    /**
     * Delete a device.
     */
    public function destroy(Request $request, int $id): JsonResponse
    {
        $device = $request->user()->devices()->findOrFail($id);
        $device->delete();

        return response()->json([
            'message' => 'Device deleted successfully',
        ]);
    }

    /**
     * Toggle device active status.
     */
    public function toggleStatus(Request $request, int $id): JsonResponse
    {
        $device = $request->user()->devices()->findOrFail($id);
        $isActive = $request->boolean('is_active', ! $device->is_active);

        $device = $this->deviceService->toggleDeviceStatus($device, $isActive);

        return response()->json([
            'message' => 'Device status updated',
            'device' => $device,
        ]);
    }

    /**
     * Unlink a device from its commerce.
     * 
     * Professional Architecture Approach (QR Authorization):
     * - Allows device to be re-linked to a different commerce
     * - Requires authentication and commerce ownership
     * - Maintains audit trail via logging
     * - Resets device to "unlinked" state (no commerce, no user)
     * 
     * Security:
     * - User must be authenticated
     * - User must belong to the device's commerce (commerce admin)
     * - Device must exist
     * - Operation is logged for audit
     * 
     * Use cases:
     * - Transfer device to another commerce
     * - Fix incorrect commerce linkage
     * - Remove device from commerce fleet
     * - Reset device to allow re-linking
     * 
     * POST /api/devices/{id}/unlink
     */
    public function unlink(Request $request, int $id): JsonResponse
    {
        $user = $request->user();
        
        // Find device by ID
        $device = Device::findOrFail($id);
        
        // Security validation: User must belong to device's commerce
        // This ensures only commerce admins can unlink devices from their commerce
        if (!$device->commerce_id) {
            return response()->json([
                'message' => 'Dispositivo ya está desvinculado',
            ], 400);
        }
        
        if ($device->commerce_id !== $user->commerce_id) {
            Log::warning('Unauthorized unlink attempt', [
                'user_id' => $user->id,
                'user_commerce_id' => $user->commerce_id,
                'device_id' => $device->id,
                'device_commerce_id' => $device->commerce_id,
            ]);
            
            return response()->json([
                'message' => 'No tienes permiso para desvincular este dispositivo',
            ], 403);
        }

        // Check if device is already unlinked
        if (!$device->commerce_id) {
            return response()->json([
                'message' => 'El dispositivo ya está desvinculado',
                'device' => $device,
            ], 400);
        }

        // Unlink device
        $device = $this->deviceService->unlinkDevice($device);

        return response()->json([
            'message' => 'Dispositivo desvinculado exitosamente. Ahora puede vincularse a otro negocio.',
            'device' => $device,
        ]);
    }
}
