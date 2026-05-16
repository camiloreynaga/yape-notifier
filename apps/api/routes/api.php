<?php

use App\Http\Controllers\AppInstanceController;
use App\Http\Controllers\AuthController;
use App\Http\Controllers\CommerceController;
use App\Http\Controllers\Api\DeviceLogController;
use App\Http\Controllers\DeviceController;
use App\Http\Controllers\DeviceHealthController;
use App\Http\Controllers\DeviceLinkController;
use App\Http\Controllers\DeviceMonitoredAppController;
use App\Http\Controllers\MonitorPackageController;
use App\Http\Controllers\NotificationController;
use App\Http\Controllers\PinAuthController;
use App\Http\Controllers\UserController;
use App\Http\Controllers\PayoutAccountController;
use App\Http\Controllers\SuperAdmin\CommerceManagementController;
use App\Http\Controllers\SuperAdmin\CommissionsController;
use App\Http\Controllers\SuperAdmin\DashboardController;
use App\Http\Controllers\SuperAdmin\PlanController;
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

// Public settings endpoint (used by Android clients)
Route::get('/settings/monitored-packages', [MonitorPackageController::class, 'getActivePackages']);

// Public device link code validation endpoint
Route::get('/devices/link-code/{code}', [DeviceLinkController::class, 'validateLinkCode']);

// Self-service commerce endpoints — only auth, NOT commerce.active.
// A user with a pending/suspended commerce must still be able to read their
// own commerce status, otherwise they get bounced to /create-commerce forever.
Route::middleware('auth:sanctum')->group(function () {
    Route::post('/commerces', [CommerceController::class, 'store']);
    Route::get('/commerces/me', [CommerceController::class, 'show']);
    Route::get('/commerces/check', [CommerceController::class, 'check']);
});

// Protected routes (auth + commerce must be active)
Route::middleware(['auth:sanctum', 'commerce.active'])->group(function () {
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
    Route::get('/notifications/by-instance', [NotificationController::class, 'byInstance']);
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

    // Employee management — only admins / super admins
    Route::middleware('require_admin')->group(function () {
        Route::apiResource('users', UserController::class);
        Route::post('/users/{id}/regenerate-pin', [UserController::class, 'regeneratePin']);
        Route::post('/users/{id}/regenerate-password', [UserController::class, 'regeneratePassword']);

        Route::get('/commerces/me/payout-account', [PayoutAccountController::class, 'show']);
        Route::put('/commerces/me/payout-account', [PayoutAccountController::class, 'update']);

        Route::get('/referrals/stats', [\App\Http\Controllers\ReferralController::class, 'stats']);
        Route::get('/referrals/referrals', [\App\Http\Controllers\ReferralController::class, 'referrals']);
        Route::get('/referrals/commissions', [\App\Http\Controllers\ReferralController::class, 'commissions']);
    });
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

// Device logs endpoint (REQUIRES authentication)
// Used by Android app to send critical logs for debugging
Route::middleware('auth:sanctum')->post('/device-logs', [DeviceLogController::class, 'store']);

// Device logs viewing endpoints (REQUIRES authentication)
Route::middleware('auth:sanctum')->group(function () {
    Route::get('/device-logs/summary', [DeviceLogController::class, 'summary']);
    Route::get('/device-logs/commerce', [DeviceLogController::class, 'commerceLogs']);
    Route::get('/devices/{deviceId}/logs', [DeviceLogController::class, 'index']);
});

// Super Admin routes (REQUIRES authentication + super_admin role)
Route::middleware(['auth:sanctum', 'super_admin'])->prefix('admin')->group(function () {
    // Commerce management
    Route::get('/commerces', [CommerceManagementController::class, 'index']);
    Route::get('/commerces/pending', [CommerceManagementController::class, 'pending']);
    Route::get('/commerces/{id}', [CommerceManagementController::class, 'show']);
    Route::patch('/commerces/{id}/approve', [CommerceManagementController::class, 'approve']);
    Route::patch('/commerces/{id}/suspend', [CommerceManagementController::class, 'suspend']);
    Route::patch('/commerces/{id}/reactivate', [CommerceManagementController::class, 'reactivate']);
    Route::patch('/commerces/{id}/plan', [CommerceManagementController::class, 'changePlan']);
    Route::patch('/commerces/{id}/renew', [CommerceManagementController::class, 'renew']);

    // Plan management
    Route::get('/plans', [PlanController::class, 'index']);
    Route::post('/plans', [PlanController::class, 'store']);
    Route::patch('/plans/{id}', [PlanController::class, 'update']);

    // Dashboard KPIs
    Route::get('/dashboard/kpis', [DashboardController::class, 'kpis']);

    // Commission management
    Route::get('/commissions', [CommissionsController::class, 'index']);
    Route::post('/commissions/{id}/approve', [CommissionsController::class, 'approve']);
    Route::post('/commissions/{id}/pay', [CommissionsController::class, 'pay']);
    Route::post('/commissions/{id}/void', [CommissionsController::class, 'void']);
});
