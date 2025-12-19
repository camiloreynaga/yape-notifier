<?php

use App\Http\Controllers\AppInstanceController;
use App\Http\Controllers\AuthController;
use App\Http\Controllers\CommerceController;
use App\Http\Controllers\DeviceController;
use App\Http\Controllers\MonitorPackageController;
use App\Http\Controllers\NotificationController;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| API Routes
|--------------------------------------------------------------------------
*/

// Public routes
Route::post('/register', [AuthController::class, 'register']);
Route::post('/login', [AuthController::class, 'login']);

// Public settings endpoint (used by Android clients)
Route::get('/settings/monitored-packages', [MonitorPackageController::class, 'getActivePackages']);

// Protected routes
Route::middleware('auth:sanctum')->group(function () {
    // Auth routes
    Route::post('/logout', [AuthController::class, 'logout']);
    Route::get('/me', [AuthController::class, 'me']);

    // Device routes
    Route::apiResource('devices', DeviceController::class);
    Route::post('/devices/{id}/toggle-status', [DeviceController::class, 'toggleStatus']);

    // Notification routes
    Route::post('/notifications', [NotificationController::class, 'store']);
    Route::get('/notifications', [NotificationController::class, 'index']);
    Route::get('/notifications/statistics', [NotificationController::class, 'statistics']);
    Route::get('/notifications/{id}', [NotificationController::class, 'show']);
    Route::patch('/notifications/{id}/status', [NotificationController::class, 'updateStatus']);

    // Monitor Package routes (admin/management)
    Route::apiResource('monitor-packages', MonitorPackageController::class);
    Route::post('/monitor-packages/{id}/toggle-status', [MonitorPackageController::class, 'toggleStatus']);
    Route::post('/monitor-packages/bulk-create', [MonitorPackageController::class, 'bulkCreate']);

    // App Instance routes (for dual apps management)
    Route::get('/app-instances', [AppInstanceController::class, 'index']);
    Route::get('/devices/{deviceId}/app-instances', [AppInstanceController::class, 'getDeviceInstances']);
    Route::patch('/app-instances/{id}/label', [AppInstanceController::class, 'updateLabel']);

    // Commerce routes
    Route::post('/commerces', [CommerceController::class, 'store']);
    Route::get('/commerces/me', [CommerceController::class, 'show']);
    Route::get('/commerces/check', [CommerceController::class, 'check']);
});
