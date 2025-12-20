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
     * POST /api/devices/{id}/health
     */
    public function update(UpdateDeviceHealthRequest $request, int $id): JsonResponse
    {
        try {
            $user = $request->user();
            $device = $user->devices()->findOrFail($id);

            $device = $this->deviceService->updateHealth($device, $request->validated());

            return response()->json([
                'message' => 'Device health updated successfully',
                'device' => $device,
                'health' => $device->getHealthStatus(),
            ]);
        } catch (\Exception $e) {
            Log::error('Failed to update device health', [
                'user_id' => $request->user()->id,
                'device_id' => $id,
                'error' => $e->getMessage(),
                'trace' => $e->getTraceAsString(),
            ]);

            return response()->json([
                'message' => 'Failed to update device health',
                'error' => config('app.debug') ? $e->getMessage() : null,
            ], 500);
        }
    }
}

