<?php

namespace App\Http\Controllers;

use App\Http\Requests\AppInstance\UpdateAppInstanceRequest;
use App\Services\AppInstanceService;
use App\Services\DeviceService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class AppInstanceController extends Controller
{
    public function __construct(
        protected AppInstanceService $appInstanceService,
        protected DeviceService $deviceService
    ) {}

    /**
     * Get app instances for a device.
     */
    public function getDeviceInstances(Request $request, int $deviceId): JsonResponse
    {
        $user = $request->user();
        $device = $user->devices()->findOrFail($deviceId);

        $instances = $this->appInstanceService->getDeviceInstances($device);

        return response()->json([
            'instances' => $instances,
        ]);
    }

    /**
     * Update app instance label.
     */
    public function updateLabel(UpdateAppInstanceRequest $request, int $id): JsonResponse
    {
        $user = $request->user();
        $commerceId = $user->commerce_id;

        if (!$commerceId) {
            return response()->json([
                'message' => 'User does not belong to a commerce',
            ], 403);
        }

        $instance = \App\Models\AppInstance::where('commerce_id', $commerceId)
            ->findOrFail($id);

        $instance = $this->appInstanceService->updateLabel(
            $instance,
            $request->validated()['instance_label']
        );

        return response()->json([
            'message' => 'App instance label updated',
            'instance' => $instance,
        ]);
    }

    /**
     * Get all app instances for user's commerce.
     */
    public function index(Request $request): JsonResponse
    {
        $user = $request->user();
        $commerceId = $user->commerce_id;

        if (!$commerceId) {
            return response()->json([
                'instances' => [],
            ]);
        }

        $deviceId = $request->query('device_id');
        $instances = $this->appInstanceService->getCommerceInstances($commerceId, $deviceId);

        return response()->json([
            'instances' => $instances,
        ]);
    }
}


