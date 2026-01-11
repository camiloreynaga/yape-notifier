<?php

namespace App\Console\Commands;

use App\Models\AppInstance;
use Illuminate\Console\Command;
use Illuminate\Support\Facades\DB;

class AssignAppInstances extends Command
{
    /**
     * The name and signature of the console command.
     *
     * @var string
     */
    protected $signature = 'notifications:assign-instances
                            {--dry-run : Show what would be done without making changes}
                            {--commerce= : Only process notifications for a specific commerce ID}';

    /**
     * The console command description.
     *
     * @var string
     */
    protected $description = 'Assign app_instance_id to notifications that have package_name and android_user_id but no instance assigned';

    /**
     * Execute the console command.
     */
    public function handle(): int
    {
        $dryRun = $this->option('dry-run');
        $commerceId = $this->option('commerce');

        if ($dryRun) {
            $this->info('🔍 DRY RUN MODE - No changes will be made');
        }

        $this->info('Searching for notifications without app_instance_id...');

        // Build query for unique combinations
        $query = DB::table('notifications')
            ->select('device_id', 'package_name', 'android_user_id', 'commerce_id', DB::raw('COUNT(*) as notification_count'))
            ->whereNotNull('package_name')
            ->whereNotNull('android_user_id')
            ->whereNull('app_instance_id')
            ->whereNotNull('commerce_id');

        if ($commerceId) {
            $query->where('commerce_id', $commerceId);
        }

        $combinations = $query
            ->groupBy('device_id', 'package_name', 'android_user_id', 'commerce_id')
            ->get();

        if ($combinations->isEmpty()) {
            $this->info('✅ No notifications found that need app_instance_id assignment.');
            return Command::SUCCESS;
        }

        $this->info("Found {$combinations->count()} unique device/package/user combinations to process.");
        $this->newLine();

        // Show table of what will be processed
        $tableData = $combinations->map(function ($combo) {
            return [
                'device_id' => $combo->device_id,
                'package_name' => $combo->package_name,
                'android_user_id' => $combo->android_user_id,
                'commerce_id' => $combo->commerce_id,
                'notifications' => $combo->notification_count,
            ];
        })->toArray();

        $this->table(
            ['Device ID', 'Package Name', 'Android User ID', 'Commerce ID', 'Notifications'],
            $tableData
        );

        if ($dryRun) {
            $this->newLine();
            $this->info('🔍 DRY RUN completed. Run without --dry-run to apply changes.');
            return Command::SUCCESS;
        }

        if (!$this->confirm('Do you want to proceed with assigning app instances?')) {
            $this->info('Operation cancelled.');
            return Command::SUCCESS;
        }

        $this->newLine();
        $progressBar = $this->output->createProgressBar($combinations->count());
        $progressBar->start();

        $createdInstances = 0;
        $updatedNotifications = 0;

        foreach ($combinations as $combo) {
            // Find or create app instance
            $appInstance = AppInstance::firstOrCreate(
                [
                    'device_id' => $combo->device_id,
                    'package_name' => $combo->package_name,
                    'android_user_id' => $combo->android_user_id,
                ],
                [
                    'commerce_id' => $combo->commerce_id,
                    'instance_label' => null,
                ]
            );

            if ($appInstance->wasRecentlyCreated) {
                $createdInstances++;
            }

            // Update notifications
            $updated = DB::table('notifications')
                ->where('device_id', $combo->device_id)
                ->where('package_name', $combo->package_name)
                ->where('android_user_id', $combo->android_user_id)
                ->whereNull('app_instance_id')
                ->update(['app_instance_id' => $appInstance->id]);

            $updatedNotifications += $updated;

            $progressBar->advance();
        }

        $progressBar->finish();
        $this->newLine(2);

        $this->info("✅ Process completed!");
        $this->info("   - App instances created: {$createdInstances}");
        $this->info("   - Notifications updated: {$updatedNotifications}");

        return Command::SUCCESS;
    }
}
