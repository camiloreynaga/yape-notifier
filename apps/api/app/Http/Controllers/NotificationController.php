<?php

namespace App\Http\Controllers;

use App\Http\Requests\Notification\CreateNotificationRequest;
use App\Services\DeviceService;
use App\Services\NotificationService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class NotificationController extends Controller
{
    public function __construct(
        protected NotificationService $notificationService,
        protected DeviceService $deviceService
    ) {}

    /**
     * Create a new notification.
     */
    public function store(CreateNotificationRequest $request): JsonResponse
    {
        $user = $request->user();
        $deviceUuid = $request->input('device_id');

        // Find device by UUID
        $device = $this->deviceService->findDeviceByUuid($user, $deviceUuid);

        if (! $device) {
            return response()->json([
                'message' => 'Device not found',
            ], 404);
        }

        if (! $device->is_active) {
            return response()->json([
                'message' => 'Device is not active',
            ], 403);
        }

        $notification = $this->notificationService->createNotification(
            $request->validated(),
            $device
        );

        return response()->json([
            'message' => $notification->is_duplicate
                ? 'Notification received (duplicate detected)'
                : 'Notification created successfully',
            'notification' => $notification,
        ], 201);
    }

    /**
     * List notifications for the authenticated user.
     */
    public function index(Request $request): JsonResponse
    {
        $filters = $request->only([
            'device_id',
            'source_app',
            'start_date',
            'end_date',
            'status',
            'exclude_duplicates',
        ]);

        $perPage = $request->integer('per_page', 50);
        $notifications = $this->notificationService
            ->getUserNotifications($request->user(), $filters)
            ->paginate($perPage);

        return response()->json($notifications);
    }

    /**
     * Get a specific notification.
     */
    public function show(Request $request, int $id): JsonResponse
    {
        $notification = $request->user()
            ->notifications()
            ->with('device')
            ->findOrFail($id);

        return response()->json([
            'notification' => $notification,
        ]);
    }

    /**
     * Get statistics for notifications.
     */
    public function statistics(Request $request): JsonResponse
    {
        $filters = $request->only(['start_date', 'end_date']);

        $stats = $this->notificationService->getStatistics(
            $request->user(),
            $filters
        );

        return response()->json($stats);
    }

    /**
     * Update notification status.
     */
    public function updateStatus(Request $request, int $id): JsonResponse
    {
        $request->validate([
            'status' => 'required|in:pending,validated,inconsistent',
        ]);

        $notification = $request->user()
            ->notifications()
            ->findOrFail($id);

        $notification->update(['status' => $request->status]);

        return response()->json([
            'message' => 'Notification status updated',
            'notification' => $notification,
        ]);
    }
}
