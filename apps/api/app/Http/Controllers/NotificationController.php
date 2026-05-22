<?php

namespace App\Http\Controllers;

use App\Http\Requests\Notification\CreateNotificationRequest;
use App\Http\Requests\Notification\ListNotificationsRequest;
use App\Models\Notification;
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
     * 
     * Professional Architecture Approach:
     * - Authentication is OPTIONAL (device linking via QR is the authorization)
     * - Device must have commerce_id (from QR linking)
     * - Device UUID is the primary authorization mechanism
     * - User association is optional (for traceability only)
     * 
     * This allows "capturer mode" where devices send notifications without user accounts.
     * The QR code linking is the authorization - if device has commerce_id, it's authorized.
     */
    public function store(CreateNotificationRequest $request): JsonResponse
    {
        try {
            $user = $request->user(); // Nullable - authentication is optional
            $deviceUuid = $request->input('device_id');

            Log::info('Notification creation request received', [
                'user_id' => $user?->id,
                'device_uuid' => $deviceUuid,
                'authenticated' => !is_null($user),
            ]);

            // Find device by UUID (primary authorization mechanism)
            // If user is authenticated, verify device belongs to same commerce
            // If user is NOT authenticated, just verify device exists and has commerce_id
            if ($user) {
                // Authenticated: use existing method with user validation
                $device = $this->deviceService->findDeviceByUuid($user, $deviceUuid);
            } else {
                // Not authenticated: find device by UUID only
                $device = \App\Models\Device::where('uuid', $deviceUuid)
                    ->where('is_active', true)
                    ->first();
            }

            if (! $device) {
                Log::warning('Device not found for notification', [
                    'user_id' => $user?->id,
                    'device_uuid' => $deviceUuid,
                    'authenticated' => !is_null($user),
                ]);

                return response()->json([
                    'message' => 'Dispositivo no encontrado. Por favor, vincula tu dispositivo con un código QR.',
                    'error' => 'device_not_found',
                ], 404);
            }

            // Check daily notification limit based on plan (only for Starter plan)
            $commerce = \App\Models\Commerce::with('plan')->find($device->commerce_id);
            if ($commerce && !$commerce->canSendNotificationToday()) {
                Log::warning('Daily notification limit reached', [
                    'commerce_id' => $commerce->id,
                    'plan' => $commerce->plan->name,
                    'limit' => $commerce->plan->max_notifications_per_day,
                    'today_count' => $commerce->todayNotificationsCount(),
                ]);

                return response()->json([
                    'message' => 'Has alcanzado el límite de ' . $commerce->plan->max_notifications_per_day . ' notificaciones por día de tu plan ' . $commerce->plan->name . '. Actualiza tu plan para continuar.',
                    'plan' => $commerce->plan->name,
                    'limit' => $commerce->plan->max_notifications_per_day,
                    'error' => 'daily_limit_reached',
                ], 429);
            }

            // Critical validation: Device MUST have commerce_id (from QR linking)
            // This is the authorization mechanism - QR linking authorizes the device
            if (!$device->commerce_id) {
                Log::error('Device has no commerce_id - not linked via QR', [
                    'device_id' => $device->id,
                    'device_uuid' => $deviceUuid,
                    'user_id' => $user?->id,
                ]);
                
                return response()->json([
                    'message' => 'Dispositivo no vinculado. Por favor, escanea el código QR para vincular tu dispositivo a un negocio.',
                    'error' => 'device_not_linked',
                ], 403);
            }

            if (! $device->is_active) {
                Log::warning('Inactive device attempted to create notification', [
                    'user_id' => $user?->id,
                    'device_id' => $device->id,
                ]);

                return response()->json([
                    'message' => 'Dispositivo inactivo',
                    'error' => 'device_inactive',
                ], 403);
            }

            $notification = $this->notificationService->createNotification(
                $request->validated(),
                $device
            );

            Log::info('Notification created successfully', [
                'notification_id' => $notification->id,
                'user_id' => $user?->id,
                'device_id' => $device->id,
                'commerce_id' => $device->commerce_id,
                'is_duplicate' => $notification->is_duplicate,
                'authenticated' => !is_null($user),
            ]);

            return response()->json([
                'message' => $notification->is_duplicate
                    ? 'Notification received (duplicate detected)'
                    : 'Notification created successfully',
                'notification' => $notification,
            ], 201);
        } catch (ValidationException $e) {
            Log::warning('Notification validation failed', [
                'user_id' => $request->user()?->id,
                'errors' => $e->errors(),
            ]);

            throw $e;
        } catch (\Exception $e) {
            $userId = $request->user()?->id ?? 'unknown';
            
            Log::error('Failed to create notification', [
                'user_id' => $userId,
                'device_uuid' => $request->input('device_id'),
                'error' => $e->getMessage(),
                'file' => $e->getFile(),
                'line' => $e->getLine(),
                'trace' => $e->getTraceAsString(),
            ]);

            return response()->json([
                'message' => 'Server Error',
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
            $filters = [
                'device_id'          => $request->input('device_id'),
                'app_instance_id'    => $request->input('app_instance_id'),
                'instance_id'        => $request->input('instance_id'), // accept alias
                'source_app'         => $request->input('source_app'),
                'package_name'       => $request->input('package_name'),
                'start_date'         => $request->input('start_date'),
                'end_date'           => $request->input('end_date'),
                'status'             => $request->input('status'),
                'exclude_duplicates' => $request->boolean('exclude_duplicates'),
            ];

            // Allow instance_id as alias of app_instance_id (frontend uses instance_id[])
            if (! empty($filters['instance_id']) && empty($filters['app_instance_id'])) {
                $filters['app_instance_id'] = $filters['instance_id'];
            }
            unset($filters['instance_id']);

            // Drop nulls so the service doesn't apply empty filters
            $filters = array_filter($filters, fn ($v) => $v !== null && $v !== '');

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

    public function byInstance(Request $request): JsonResponse
    {
        $user = $request->user();
        $commerceId = $user->commerce_id;

        $startDate = $request->input('start_date');
        $endDate = $request->input('end_date');

        $query = \App\Models\Notification::query()
            ->select([
                'app_instance_id',
                \DB::raw('COUNT(*) as total'),
                \DB::raw("COUNT(CASE WHEN status = 'validated' THEN 1 END) as validated"),
                \DB::raw("COUNT(CASE WHEN status = 'pending' THEN 1 END) as pending"),
                \DB::raw("COUNT(CASE WHEN status = 'inconsistent' THEN 1 END) as inconsistent"),
                \DB::raw('COALESCE(SUM(amount), 0) as amount_total'),
            ])
            ->whereNotNull('app_instance_id')
            ->where('commerce_id', $commerceId)
            ->groupBy('app_instance_id');

        if ($startDate) {
            $query->where('created_at', '>=', $startDate);
        }
        if ($endDate) {
            $query->where('created_at', '<=', $endDate);
        }

        $rows = $query->get();
        $instanceIds = $rows->pluck('app_instance_id')->all();
        $labels = \App\Models\AppInstance::whereIn('id', $instanceIds)
            ->pluck('instance_label', 'id');

        $data = $rows->map(fn ($r) => [
            'instance_id'    => (int) $r->app_instance_id,
            'instance_label' => $labels[$r->app_instance_id] ?? 'Sin etiqueta',
            'total'          => (int) $r->total,
            'validated'      => (int) $r->validated,
            'pending'        => (int) $r->pending,
            'inconsistent'   => (int) $r->inconsistent,
            'amount_total'   => number_format((float) $r->amount_total, 2, '.', ''),
        ])->values();

        return response()->json(['data' => $data]);
    }

    /**
     * Update notification status.
     *
     * Professional Architecture Approach:
     * - Prioritize commerce_id over user_id (supports admin viewing all commerce notifications)
     * - Admin can update any notification from their commerce
     * - Ensures proper authorization through commerce ownership
     */
    public function updateStatus(Request $request, int $id): JsonResponse
    {
        $request->validate([
            'status' => 'required|in:pending,validated,inconsistent',
        ]);

        $user = $request->user();

        // Find notification by commerce_id (if user has commerce) or user_id
        if ($user->commerce_id) {
            // Admin mode: can update any notification from their commerce
            $notification = Notification::where('commerce_id', $user->commerce_id)
                ->findOrFail($id);
        } else {
            // User mode: can only update their own notifications
            $notification = $user->notifications()->findOrFail($id);
        }

        $notification->update(['status' => $request->status]);

        return response()->json([
            'message' => 'Notification status updated',
            'notification' => $notification,
        ]);
    }
}
