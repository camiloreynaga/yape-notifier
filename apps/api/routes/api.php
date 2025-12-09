<?php

use App\Http\Controllers\AuthController;
use App\Http\Controllers\DeviceController;
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
});
