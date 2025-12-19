<?php

namespace Database\Seeders;

use App\Models\Commerce;
use App\Models\Device;
use App\Models\User;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

class UpdateExistingUsersCommerceSeeder extends Seeder
{
    /**
     * Run the database seeds.
     * 
     * This seeder creates a commerce for all existing users that don't have one.
     * It also updates their devices to have the same commerce_id.
     */
    public function run(): void
    {
        $this->command->info('Starting migration of existing users to commerce system...');

        // Get all users without commerce_id
        $usersWithoutCommerce = User::whereNull('commerce_id')->get();

        if ($usersWithoutCommerce->isEmpty()) {
            $this->command->info('No users without commerce found. Migration not needed.');
            return;
        }

        $this->command->info("Found {$usersWithoutCommerce->count()} users without commerce.");

        $created = 0;
        $failed = 0;

        DB::beginTransaction();

        try {
            foreach ($usersWithoutCommerce as $user) {
                try {
                    // Create commerce for user
                    $commerce = Commerce::create([
                        'name' => $user->name . ' - Negocio',
                        'owner_user_id' => $user->id,
                    ]);

                    // Update user with commerce_id and set role to admin
                    $user->update([
                        'commerce_id' => $commerce->id,
                        'role' => 'admin',
                    ]);

                    // Update all devices belonging to this user
                    $devicesUpdated = Device::where('user_id', $user->id)
                        ->whereNull('commerce_id')
                        ->update(['commerce_id' => $commerce->id]);

                    $created++;
                    
                    Log::info('Commerce created for existing user', [
                        'user_id' => $user->id,
                        'commerce_id' => $commerce->id,
                        'devices_updated' => $devicesUpdated,
                    ]);

                    $this->command->info("✓ Created commerce for user: {$user->email} (ID: {$user->id})");
                } catch (\Exception $e) {
                    $failed++;
                    
                    Log::error('Failed to create commerce for user', [
                        'user_id' => $user->id,
                        'error' => $e->getMessage(),
                    ]);

                    $this->command->error("✗ Failed to create commerce for user: {$user->email} (ID: {$user->id}) - {$e->getMessage()}");
                }
            }

            DB::commit();

            $this->command->info("\nMigration completed!");
            $this->command->info("✓ Successfully created commerce for {$created} users");
            
            if ($failed > 0) {
                $this->command->warn("✗ Failed to create commerce for {$failed} users");
            }
        } catch (\Exception $e) {
            DB::rollBack();
            
            Log::error('Commerce migration failed', [
                'error' => $e->getMessage(),
                'trace' => $e->getTraceAsString(),
            ]);

            $this->command->error("Migration failed: {$e->getMessage()}");
            throw $e;
        }
    }
}

