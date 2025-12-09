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
}
