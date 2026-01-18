<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Device;
use App\Models\DeviceLog;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Validator;
use Carbon\Carbon;

class DeviceLogController extends Controller
{
    /**
     * Store logs from a device.
     * Called by the Android app to send critical logs.
     *
     * @param Request $request
     * @return \Illuminate\Http\JsonResponse
     */
    public function store(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'device_uuid' => 'required|string|exists:devices,uuid',
            'logs' => 'required|array|max:50', // Max 50 logs per request
            'logs.*.category' => 'required|string|in:' . implode(',', DeviceLog::CATEGORIES),
            'logs.*.level' => 'sometimes|string|in:' . implode(',', DeviceLog::LEVELS),
            'logs.*.message' => 'required|string|max:500',
            'logs.*.details' => 'sometimes|string|max:2000',
            'logs.*.timestamp' => 'sometimes|integer', // Unix timestamp in milliseconds
        ]);

        if ($validator->fails()) {
            return response()->json([
                'message' => 'Validation failed',
                'errors' => $validator->errors(),
            ], 422);
        }

        $device = Device::where('uuid', $request->device_uuid)->first();

        if (!$device) {
            return response()->json([
                'message' => 'Device not found',
            ], 404);
        }

        $logsCreated = 0;

        foreach ($request->logs as $logData) {
            try {
                // Convert timestamp from milliseconds to Carbon datetime
                $deviceTimestamp = null;
                if (isset($logData['timestamp'])) {
                    $deviceTimestamp = Carbon::createFromTimestampMs($logData['timestamp']);
                }

                DeviceLog::create([
                    'device_id' => $device->id,
                    'commerce_id' => $device->commerce_id,
                    'category' => $logData['category'],
                    'level' => $logData['level'] ?? 'info',
                    'message' => $logData['message'],
                    'details' => $logData['details'] ?? null,
                    'device_timestamp' => $deviceTimestamp,
                ]);

                $logsCreated++;
            } catch (\Exception $e) {
                Log::error('Error creating device log', [
                    'device_id' => $device->id,
                    'error' => $e->getMessage(),
                    'log_data' => $logData,
                ]);
            }
        }

        Log::info('Device logs received', [
            'device_id' => $device->id,
            'device_uuid' => $device->uuid,
            'logs_received' => count($request->logs),
            'logs_created' => $logsCreated,
        ]);

        return response()->json([
            'message' => 'Logs received',
            'logs_created' => $logsCreated,
        ]);
    }

    /**
     * Get logs for a device.
     * Used by the dashboard to view device logs.
     *
     * @param Request $request
     * @param int $deviceId
     * @return \Illuminate\Http\JsonResponse
     */
    public function index(Request $request, $deviceId)
    {
        $device = Device::findOrFail($deviceId);

        // Verify user has access to this device's commerce
        $user = $request->user();
        if ($user->commerce_id && $user->commerce_id !== $device->commerce_id) {
            return response()->json([
                'message' => 'Unauthorized',
            ], 403);
        }

        $query = DeviceLog::where('device_id', $deviceId)
            ->orderBy('device_timestamp', 'desc')
            ->orderBy('created_at', 'desc');

        // Filter by category
        if ($request->has('category')) {
            $query->where('category', $request->category);
        }

        // Filter by level
        if ($request->has('level')) {
            $query->where('level', $request->level);
        }

        // Filter by time range (hours)
        $hours = $request->get('hours', 24);
        $query->where('created_at', '>=', now()->subHours($hours));

        // Paginate
        $perPage = min($request->get('per_page', 50), 100);
        $logs = $query->paginate($perPage);

        return response()->json($logs);
    }

    /**
     * Get logs for all devices in a commerce.
     * Used by the dashboard for overview.
     *
     * @param Request $request
     * @return \Illuminate\Http\JsonResponse
     */
    public function commerceLogs(Request $request)
    {
        $user = $request->user();

        if (!$user->commerce_id) {
            return response()->json([
                'message' => 'No commerce associated',
            ], 400);
        }

        $query = DeviceLog::where('commerce_id', $user->commerce_id)
            ->with('device:id,name,alias,uuid')
            ->orderBy('device_timestamp', 'desc')
            ->orderBy('created_at', 'desc');

        // Filter by category
        if ($request->has('category')) {
            $query->where('category', $request->category);
        }

        // Filter by level
        if ($request->has('level')) {
            $query->where('level', $request->level);
        }

        // Filter critical only
        if ($request->boolean('critical_only')) {
            $query->critical();
        }

        // Filter by time range (hours)
        $hours = $request->get('hours', 24);
        $query->where('created_at', '>=', now()->subHours($hours));

        // Paginate
        $perPage = min($request->get('per_page', 50), 100);
        $logs = $query->paginate($perPage);

        return response()->json($logs);
    }

    /**
     * Get summary of logs by device.
     * Shows count of critical events per device.
     *
     * @param Request $request
     * @return \Illuminate\Http\JsonResponse
     */
    public function summary(Request $request)
    {
        $user = $request->user();

        if (!$user->commerce_id) {
            return response()->json([
                'message' => 'No commerce associated',
            ], 400);
        }

        $hours = $request->get('hours', 24);

        $summary = DeviceLog::where('commerce_id', $user->commerce_id)
            ->where('created_at', '>=', now()->subHours($hours))
            ->selectRaw('device_id, category, count(*) as count')
            ->groupBy('device_id', 'category')
            ->get()
            ->groupBy('device_id')
            ->map(function ($logs) {
                return $logs->pluck('count', 'category');
            });

        // Get device info
        $deviceIds = $summary->keys()->toArray();
        $devices = Device::whereIn('id', $deviceIds)
            ->select('id', 'name', 'alias', 'uuid')
            ->get()
            ->keyBy('id');

        $result = [];
        foreach ($summary as $deviceId => $counts) {
            $device = $devices->get($deviceId);
            if ($device) {
                $result[] = [
                    'device' => $device,
                    'counts' => $counts,
                    'total' => $counts->sum(),
                    'has_disconnects' => ($counts->get('DISCONNECT', 0) > 0),
                    'has_errors' => ($counts->get('ERROR', 0) > 0),
                ];
            }
        }

        return response()->json([
            'hours' => $hours,
            'devices' => $result,
        ]);
    }
}
