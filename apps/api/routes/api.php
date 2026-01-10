<?php

use App\Http\Controllers\AppInstanceController;
use App\Http\Controllers\AuthController;
use App\Http\Controllers\CommerceController;
use App\Http\Controllers\DeviceController;
use App\Http\Controllers\DeviceHealthController;
use App\Http\Controllers\DeviceLinkController;
use App\Http\Controllers\DeviceMonitoredAppController;
use App\Http\Controllers\MonitorPackageController;
use App\Http\Controllers\NotificationController;
use App\Http\Controllers\PinAuthController;
use App\Http\Controllers\UserController;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| API Routes
|--------------------------------------------------------------------------
*/

// Public routes
Route::post('/register', [AuthController::class, 'register']);
Route::post('/login', [AuthController::class, 'login']);

// PIN Authentication (public endpoint)
Route::post('/auth/login-pin', [PinAuthController::class, 'loginWithPin']);

// Settings endpoint (used by Android clients - requires authentication)
// Devices must be authenticated to get packages for their commerce
Route::middleware('auth:sanctum')->get('/settings/monitored-packages', [MonitorPackageController::class, 'getActivePackages']);

// Public device link code validation endpoint
Route::get('/devices/link-code/{code}', [DeviceLinkController::class, 'validateLinkCode']);

// Protected routes
Route::middleware('auth:sanctum')->group(function () {
    // Auth routes
    Route::post('/logout', [AuthController::class, 'logout']);
    Route::get('/me', [AuthController::class, 'me']);

    // Device routes
    Route::apiResource('devices', DeviceController::class);
    Route::post('/devices/{id}/toggle-status', [DeviceController::class, 'toggleStatus']);
    Route::post('/devices/{id}/unlink', [DeviceController::class, 'unlink']);
    Route::get('/devices/{id}/monitored-apps', [DeviceMonitoredAppController::class, 'index']);
    Route::post('/devices/{id}/monitored-apps', [DeviceMonitoredAppController::class, 'store']);

    // Device Link routes (for QR/code linking - admin only)
    Route::post('/devices/generate-link-code', [DeviceLinkController::class, 'generateLinkCode']);
    Route::get('/devices/link-codes', [DeviceLinkController::class, 'getActiveCodes']);

    // Notification routes (admin/management - require authentication)
    Route::get('/notifications', [NotificationController::class, 'index']);
    Route::get('/notifications/statistics', [NotificationController::class, 'statistics']);
    Route::get('/notifications/{id}', [NotificationController::class, 'show']);
    Route::patch('/notifications/{id}/status', [NotificationController::class, 'updateStatus']);

    // Monitor Package routes (admin/management)
    Route::apiResource('monitor-packages', MonitorPackageController::class);
    Route::post('/monitor-packages/{id}/toggle-status', [MonitorPackageController::class, 'toggleStatus']);
    Route::post('/monitor-packages/bulk-create', [MonitorPackageController::class, 'bulkCreate']);
    Route::get('/monitor-packages/detected', [MonitorPackageController::class, 'getDetectedPackages']);

    // App Instance routes (for dual apps management)
    Route::get('/app-instances', [AppInstanceController::class, 'index']);
    Route::get('/devices/{deviceId}/app-instances', [AppInstanceController::class, 'getDeviceInstances']);
    Route::patch('/app-instances/{id}/label', [AppInstanceController::class, 'updateLabel']);

    // Commerce routes
    Route::post('/commerces', [CommerceController::class, 'store']);
    Route::get('/commerces/me', [CommerceController::class, 'show']);
    Route::get('/commerces/check', [CommerceController::class, 'check']);

    // User/Employee routes (admin only)
    Route::apiResource('users', UserController::class);
    Route::post('/users/{id}/regenerate-pin', [UserController::class, 'regeneratePin']);
});

// Device linking endpoint (REQUIRES authentication with PIN)
// Professional Architecture: User must be authenticated to link device
// This ensures complete traceability: every device has an owner (user_id)
Route::middleware('auth:sanctum')->post('/devices/link-by-code', [DeviceLinkController::class, 'linkByCode']);

// Notification creation endpoint (REQUIRES authentication with PIN)
// Professional Architecture: User must be authenticated to send notifications
// This ensures complete traceability: every notification has a capturer (user_id)
Route::middleware('auth:sanctum')->post('/notifications', [NotificationController::class, 'store']);

// Device health endpoint (REQUIRES authentication)
// Professional Architecture: Only authenticated devices can report health
Route::middleware('auth:sanctum')->post('/devices/{id}/health', [DeviceHealthController::class, 'update']);
