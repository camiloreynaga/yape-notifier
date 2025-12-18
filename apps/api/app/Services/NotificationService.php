<?php

namespace App\Services;

use App\Models\AppInstance;
use App\Models\Device;
use App\Models\Notification;
use App\Models\User;
use Carbon\Carbon;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

class NotificationService
{
    public function __construct(
        protected ?AppInstanceService $appInstanceService = null
    ) {
        // Lazy load to avoid circular dependency
        if (!$this->appInstanceService) {
            $this->appInstanceService = app(AppInstanceService::class);
        }
    }

    /**
     * Create a notification from device data.
     */
    public function createNotification(array $data, Device $device): Notification
    {
        // Find or create app instance if dual app identifiers are provided
        $appInstance = null;
        if (isset($data['package_name']) && isset($data['android_user_id'])) {
            $appInstance = $this->appInstanceService->findOrCreate(
                $device,
                $data['package_name'],
                $data['android_user_id']
            );
        }

        // Check for duplicates
        $isDuplicate = $this->checkDuplicate($data, $device, $appInstance);

        $commerceId = $device->commerce_id ?? $device->user->commerce_id;

        $notification = Notification::create([
            'user_id' => $device->user_id,
            'commerce_id' => $commerceId,
            'device_id' => $device->id,
            'source_app' => $data['source_app'],
            'package_name' => $data['package_name'] ?? null,
            'android_user_id' => $data['android_user_id'] ?? null,
            'android_uid' => $data['android_uid'] ?? null,
            'app_instance_id' => $appInstance?->id,
            'title' => $data['title'] ?? null,
            'body' => $data['body'],
            'amount' => $data['amount'] ?? null,
            'currency' => $data['currency'] ?? 'PEN',
            'payer_name' => $data['payer_name'] ?? null,
            'posted_at' => isset($data['posted_at'])
                ? Carbon::parse($data['posted_at'])
                : null,
            'received_at' => isset($data['received_at'])
                ? Carbon::parse($data['received_at'])
                : now(),
            'raw_json' => $data['raw_json'] ?? null,
            'status' => $data['status'] ?? 'pending',
            'is_duplicate' => $isDuplicate,
        ]);

        // Update device last seen
        $device->update(['last_seen_at' => now()]);

        if ($isDuplicate) {
            Log::info('Duplicate notification detected', [
                'device_id' => $device->id,
                'package_name' => $data['package_name'] ?? null,
                'android_user_id' => $data['android_user_id'] ?? null,
                'received_at' => $notification->received_at,
            ]);
        }

        return $notification;
    }

    /**
     * Check if a notification is a duplicate.
     * Uses package_name + android_user_id + posted_at + body for deduplication.
     */
    protected function checkDuplicate(array $data, Device $device, ?AppInstance $appInstance = null): bool
    {
        $postedAt = isset($data['posted_at'])
            ? Carbon::parse($data['posted_at'])
            : (isset($data['received_at']) ? Carbon::parse($data['received_at']) : now());

        // Check for duplicates within a 5-second window
        $startTime = $postedAt->copy()->subSeconds(5);
        $endTime = $postedAt->copy()->addSeconds(5);

        $query = Notification::where('device_id', $device->id)
            ->whereBetween('posted_at', [$startTime, $endTime])
            ->where('body', $data['body']);

        // If we have dual app identifiers, use them for more precise deduplication
        if (isset($data['package_name']) && isset($data['android_user_id'])) {
            $query->where('package_name', $data['package_name'])
                ->where('android_user_id', $data['android_user_id']);
        } else {
            // Fallback to source_app for backward compatibility
            $query->where('source_app', $data['source_app']);
        }

        $existing = $query->first();

        return $existing !== null;
    }

    /**
     * Get notifications for a user with filters.
     */
    public function getUserNotifications(
        User $user,
        array $filters = []
    ) {
        $query = Notification::where('user_id', $user->id)
            ->with(['device', 'appInstance'])
            ->orderBy('received_at', 'desc');

        // Filter by commerce if user has one
        if ($user->commerce_id) {
            $query->where('commerce_id', $user->commerce_id);
        }

        // Filter by device
        if (isset($filters['device_id'])) {
            $query->where('device_id', $filters['device_id']);
        }

        // Filter by source app
        if (isset($filters['source_app'])) {
            $query->where('source_app', $filters['source_app']);
        }

        // Filter by package name
        if (isset($filters['package_name'])) {
            $query->where('package_name', $filters['package_name']);
        }

        // Filter by app instance
        if (isset($filters['app_instance_id'])) {
            $query->where('app_instance_id', $filters['app_instance_id']);
        }

        // Filter by date range
        if (isset($filters['start_date'])) {
            $query->where('received_at', '>=', $filters['start_date']);
        }

        if (isset($filters['end_date'])) {
            $query->where('received_at', '<=', $filters['end_date']);
        }

        // Filter by status
        if (isset($filters['status'])) {
            $query->where('status', $filters['status']);
        }

        // Filter out duplicates if requested
        if (isset($filters['exclude_duplicates']) && $filters['exclude_duplicates']) {
            $query->where('is_duplicate', false);
        }

        return $query;
    }

    /**
     * Get statistics for a user.
     */
    public function getStatistics(User $user, array $filters = []): array
    {
        $baseQuery = Notification::where('notifications.user_id', $user->id);

        // Apply date filters
        if (isset($filters['start_date'])) {
            $baseQuery->where('notifications.received_at', '>=', $filters['start_date']);
        }

        if (isset($filters['end_date'])) {
            $baseQuery->where('notifications.received_at', '<=', $filters['end_date'].' 23:59:59');
        }

        // Total statistics (excluding duplicates)
        $query = (clone $baseQuery)->where('notifications.is_duplicate', false);
        $totalNotifications = $query->count();
        $totalAmount = (float) ($query->sum('notifications.amount') ?? 0);

        // By source app
        $bySourceApp = (clone $query)
            ->select('notifications.source_app', DB::raw('count(*) as count'), DB::raw('COALESCE(sum(notifications.amount), 0) as total_amount'))
            ->groupBy('notifications.source_app')
            ->get()
            ->mapWithKeys(function ($item) {
                return [$item->source_app ?? 'unknown' => [
                    'count' => (int) $item->count,
                    'total_amount' => (float) $item->total_amount,
                ]];
            })
            ->toArray();

        // By device
        $byDevice = (clone $query)
            ->join('devices', 'notifications.device_id', '=', 'devices.id')
            ->select('devices.name', DB::raw('count(*) as count'), DB::raw('COALESCE(sum(notifications.amount), 0) as total_amount'))
            ->groupBy('devices.id', 'devices.name')
            ->get()
            ->mapWithKeys(function ($item) {
                return [$item->name => [
                    'count' => (int) $item->count,
                    'total_amount' => (float) $item->total_amount,
                ]];
            })
            ->toArray();

        // By date - using Carbon for compatibility
        $byDate = [];
        (clone $query)->get()->groupBy(function ($notification) {
            return $notification->received_at->format('Y-m-d');
        })->each(function ($notifications, $date) use (&$byDate) {
            $byDate[$date] = [
                'count' => $notifications->count(),
                'total_amount' => (float) $notifications->sum('amount'),
            ];
        });

        // By status
        $byStatus = (clone $query)
            ->select(DB::raw('COALESCE(notifications.status, \'pending\') as status'), DB::raw('count(*) as count'))
            ->groupBy('notifications.status')
            ->get()
            ->mapWithKeys(function ($item) {
                return [$item->status => (int) $item->count];
            })
            ->toArray();

        // Duplicates count
        $duplicates = (clone $baseQuery)->where('notifications.is_duplicate', true)->count();

        return [
            'total' => $totalNotifications,
            'total_amount' => $totalAmount,
            'by_source_app' => $bySourceApp,
            'by_device' => $byDevice,
            'by_date' => $byDate,
            'by_status' => $byStatus,
            'duplicates' => $duplicates,
        ];
    }
}
