<?php

namespace App\Http\Controllers;

use App\Http\Requests\Device\CreateDeviceRequest;
use App\Http\Requests\Device\UpdateDeviceRequest;
use App\Models\Device;
use App\Services\DeviceService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;

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
     * Allows access to devices in user's commerce (admin view) or user's own devices.
     */
    public function show(Request $request, int $id): JsonResponse
    {
        $device = $this->findDeviceForUser($request->user(), $id);

        return response()->json([
            'device' => $device,
        ]);
    }

    /**
     * Update a device.
     * Allows updating devices in user's commerce (admin view) or user's own devices.
     */
    public function update(UpdateDeviceRequest $request, int $id): JsonResponse
    {
        $device = $this->findDeviceForUser($request->user(), $id);
        $device = $this->deviceService->updateDevice($device, $request->validated());

        return response()->json([
            'message' => 'Device updated successfully',
            'device' => $device,
        ]);
    }

    /**
     * Delete a device.
     * Allows deleting devices in user's commerce (admin view) or user's own devices.
     */
    public function destroy(Request $request, int $id): JsonResponse
    {
        $user = $request->user();
        $device = $this->findDeviceForUser($user, $id);

        Log::info('Device deleted', [
            'device_id' => $device->id,
            'device_uuid' => $device->uuid,
            'device_name' => $device->name,
            'deleted_by_user_id' => $user->id,
            'commerce_id' => $device->commerce_id,
        ]);

        $device->delete();

        return response()->json([
            'message' => 'Device deleted successfully',
        ]);
    }

    /**
     * Toggle device active status.
     * Allows toggling devices in user's commerce (admin view) or user's own devices.
     */
    public function toggleStatus(Request $request, int $id): JsonResponse
    {
        $device = $this->findDeviceForUser($request->user(), $id);
        $isActive = $request->boolean('is_active', ! $device->is_active);

        $device = $this->deviceService->toggleDeviceStatus($device, $isActive);

        return response()->json([
            'message' => 'Device status updated',
            'device' => $device,
        ]);
    }

    /**
     * Find a device that belongs to user's commerce or directly to the user.
     * This enables commerce-level device management (admin view).
     *
     * @param \App\Models\User $user
     * @param int $id
     * @return Device
     * @throws \Illuminate\Database\Eloquent\ModelNotFoundException
     */
    private function findDeviceForUser($user, int $id): Device
    {
        // If user has commerce_id, allow access to all devices in that commerce
        if ($user->commerce_id) {
            return Device::where('commerce_id', $user->commerce_id)
                ->findOrFail($id);
        }

        // Fallback: only user's own devices
        return Device::where('user_id', $user->id)
            ->findOrFail($id);
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
