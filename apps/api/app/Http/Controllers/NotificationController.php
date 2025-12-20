<?php

namespace App\Http\Controllers;

use App\Http\Requests\Notification\CreateNotificationRequest;
use App\Http\Requests\Notification\ListNotificationsRequest;
use App\Services\AppInstanceService;
use App\Services\DeviceService;
use App\Services\NotificationService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;
use Illuminate\Validation\ValidationException;

class NotificationController extends Controller
{
    public function __construct(
        protected NotificationService $notificationService,
        protected DeviceService $deviceService,
        protected AppInstanceService $appInstanceService
    ) {}

    /**
     * Create a new notification.
     */
    public function store(CreateNotificationRequest $request): JsonResponse
    {
        try {
            $user = $request->user();
            $deviceUuid = $request->input('device_id');

            // Validate user has commerce (critical operation)
            if (!$user->commerce_id) {
                Log::warning('Notification creation attempted without commerce', [
                    'user_id' => $user->id,
                    'device_uuid' => $deviceUuid,
                ]);

                return response()->json([
                    'message' => 'Debes pertenecer a un negocio para recibir notificaciones. Por favor, crea un negocio primero.',
                    'error' => 'commerce_required',
                ], 403);
            }

            Log::info('Notification creation request received', [
                'user_id' => $user->id,
                'device_uuid' => $deviceUuid,
                'has_commerce' => !is_null($user->commerce_id),
            ]);

            // Find device by UUID
            $device = $this->deviceService->findDeviceByUuid($user, $deviceUuid);

            if (! $device) {
                Log::warning('Device not found for notification', [
                    'user_id' => $user->id,
                    'device_uuid' => $deviceUuid,
                ]);

                return response()->json([
                    'message' => 'Device not found',
                ], 404);
            }

            if (! $device->is_active) {
                Log::warning('Inactive device attempted to create notification', [
                    'user_id' => $user->id,
                    'device_id' => $device->id,
                ]);

                return response()->json([
                    'message' => 'Device is not active',
                ], 403);
            }

            // Ensure device has commerce_id
            if (!$device->commerce_id && $user->commerce_id) {
                $device->update(['commerce_id' => $user->commerce_id]);
                Log::info('Device commerce_id updated from user', [
                    'device_id' => $device->id,
                    'commerce_id' => $user->commerce_id,
                ]);
            }

            $notification = $this->notificationService->createNotification(
                $request->validated(),
                $device
            );

            Log::info('Notification created successfully', [
                'notification_id' => $notification->id,
                'user_id' => $user->id,
                'device_id' => $device->id,
                'is_duplicate' => $notification->is_duplicate,
            ]);

            return response()->json([
                'message' => $notification->is_duplicate
                    ? 'Notification received (duplicate detected)'
                    : 'Notification created successfully',
                'notification' => $notification,
            ], 201);
        } catch (ValidationException $e) {
            Log::warning('Notification validation failed', [
                'user_id' => $request->user()->id,
                'errors' => $e->errors(),
            ]);

            throw $e;
        } catch (\Exception $e) {
            Log::error('Failed to create notification', [
                'user_id' => $request->user()->id,
                'device_uuid' => $request->input('device_id'),
                'error' => $e->getMessage(),
                'trace' => $e->getTraceAsString(),
            ]);

            return response()->json([
                'message' => 'Failed to create notification. Please try again.',
                'error' => config('app.debug') ? $e->getMessage() : null,
            ], 500);
        }
    }

    /**
     * List notifications for the authenticated user.
     */
    public function index(ListNotificationsRequest $request): JsonResponse
    {
        try {
            $filters = $request->only([
                'device_id',
                'source_app',
                'package_name',
                'app_instance_id',
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
        } catch (\Exception $e) {
            Log::error('Failed to retrieve notifications', [
                'user_id' => $request->user()->id,
                'error' => $e->getMessage(),
            ]);

            return response()->json([
                'message' => 'Failed to retrieve notifications.',
                'error' => config('app.debug') ? $e->getMessage() : null,
            ], 500);
        }
    }

    /**
     * Get a specific notification.
     */
    public function show(Request $request, int $id): JsonResponse
    {
        $notification = $request->user()
            ->notifications()
            ->with(['device', 'appInstance'])
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
        try {
            $filters = $request->only(['start_date', 'end_date']);

            $stats = $this->notificationService->getStatistics(
                $request->user(),
                $filters
            );

            return response()->json($stats);
        } catch (\Exception $e) {
            Log::error('Failed to retrieve notification statistics', [
                'user_id' => $request->user()->id,
                'error' => $e->getMessage(),
            ]);

            return response()->json([
                'message' => 'Failed to retrieve statistics.',
                'error' => config('app.debug') ? $e->getMessage() : null,
            ], 500);
        }
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
