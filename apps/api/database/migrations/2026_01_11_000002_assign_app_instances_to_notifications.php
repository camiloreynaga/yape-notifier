<?php

use App\Models\AppInstance;
use App\Models\Notification;
use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

return new class extends Migration
{
    /**
     * Run the migrations.
     *
     * This migration assigns app_instance_id to existing notifications
     * that have package_name and android_user_id but no app_instance_id.
     *
     * Logic:
     * 1. Find notifications with package_name + android_user_id but NULL app_instance_id
     * 2. For each unique combination of (device_id, package_name, android_user_id):
     *    - Find or create the corresponding AppInstance
     *    - Update all matching notifications with the app_instance_id
     */
    public function up(): void
    {
        // Get unique combinations that need app instances
        $combinations = DB::table('notifications')
            ->select('device_id', 'package_name', 'android_user_id', 'commerce_id')
            ->whereNotNull('package_name')
            ->whereNotNull('android_user_id')
            ->whereNull('app_instance_id')
            ->whereNotNull('commerce_id')
            ->groupBy('device_id', 'package_name', 'android_user_id', 'commerce_id')
            ->get();

        $createdInstances = 0;
        $updatedNotifications = 0;

        foreach ($combinations as $combo) {
            // Skip if missing required fields
            if (!$combo->device_id || !$combo->package_name || $combo->android_user_id === null || !$combo->commerce_id) {
                continue;
            }

            // Find or create app instance
            $appInstance = AppInstance::firstOrCreate(
                [
                    'device_id' => $combo->device_id,
                    'package_name' => $combo->package_name,
                    'android_user_id' => $combo->android_user_id,
                ],
                [
                    'commerce_id' => $combo->commerce_id,
                    'instance_label' => null, // User can set this later
                ]
            );

            if ($appInstance->wasRecentlyCreated) {
                $createdInstances++;
            }

            // Update all notifications matching this combination
            $updated = DB::table('notifications')
                ->where('device_id', $combo->device_id)
                ->where('package_name', $combo->package_name)
                ->where('android_user_id', $combo->android_user_id)
                ->whereNull('app_instance_id')
                ->update(['app_instance_id' => $appInstance->id]);

            $updatedNotifications += $updated;
        }

        Log::info('Migration: assign_app_instances_to_notifications completed', [
            'combinations_processed' => $combinations->count(),
            'instances_created' => $createdInstances,
            'notifications_updated' => $updatedNotifications,
        ]);
    }

    /**
     * Reverse the migrations.
     *
     * Note: This does NOT delete the created app_instances as they may have
     * been modified by users (instance_label). It only removes the association.
     */
    public function down(): void
    {
        // We don't delete app_instances as they may have user-defined labels
        // Just log that this migration was rolled back
        Log::info('Migration: assign_app_instances_to_notifications rolled back');

        // Optionally: Set app_instance_id back to NULL for notifications
        // that were updated by this migration. However, this is risky as
        // we can't distinguish between migrations and normal app operation.
        //
        // If you need to rollback, manually handle this case.
    }
};
