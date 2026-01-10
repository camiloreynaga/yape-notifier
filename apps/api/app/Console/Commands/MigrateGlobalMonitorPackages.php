<?php

namespace App\Console\Commands;

use App\Models\Commerce;
use App\Models\MonitorPackage;
use Database\Seeders\MonitorPackageSeeder;
use Illuminate\Console\Command;
use Illuminate\Support\Facades\DB;

class MigrateGlobalMonitorPackages extends Command
{
    /**
     * The name and signature of the console command.
     *
     * @var string
     */
    protected $signature = 'monitor-packages:migrate-global 
                            {--delete-global : Delete global packages after migration}
                            {--force : Force migration even if packages already exist}';

    /**
     * The console command description.
     *
     * @var string
     */
    protected $description = 'Migrate global monitor packages (commerce_id = null) to all existing commerces';

    /**
     * Execute the console command.
     */
    public function handle(): int
    {
        $this->info('🔄 Iniciando migración de paquetes globales a commerces...');
        $this->newLine();

        // Get all global packages (commerce_id = null)
        $globalPackages = MonitorPackage::whereNull('commerce_id')->get();

        if ($globalPackages->isEmpty()) {
            $this->warn('⚠️  No se encontraron paquetes globales para migrar.');
            $this->info('💡 Los paquetes globales ya fueron migrados o no existen.');
            return Command::SUCCESS;
        }

        $this->info("📦 Encontrados {$globalPackages->count()} paquetes globales:");
        foreach ($globalPackages as $pkg) {
            $this->line("   - {$pkg->package_name} ({$pkg->app_name})");
        }
        $this->newLine();

        // Get all commerces
        $commerces = Commerce::all();

        if ($commerces->isEmpty()) {
            $this->warn('⚠️  No se encontraron commerces.');
            $this->info('💡 Crea un commerce primero para migrar los paquetes.');
            return Command::SUCCESS;
        }

        $this->info("🏢 Encontrados {$commerces->count()} commerces:");
        foreach ($commerces as $commerce) {
            $this->line("   - {$commerce->name} (ID: {$commerce->id})");
        }
        $this->newLine();

        // Ask for confirmation
        if (!$this->option('force') && !$this->confirm('¿Deseas continuar con la migración?', true)) {
            $this->info('❌ Migración cancelada.');
            return Command::SUCCESS;
        }

        $this->info('🚀 Iniciando migración...');
        $this->newLine();

        $totalCreated = 0;
        $totalSkipped = 0;
        $errors = [];

        DB::beginTransaction();

        try {
            foreach ($commerces as $commerce) {
                $this->line("📋 Procesando commerce: {$commerce->name} (ID: {$commerce->id})");

                foreach ($globalPackages as $globalPackage) {
                    // Check if package already exists for this commerce
                    $exists = MonitorPackage::where('commerce_id', $commerce->id)
                        ->where('package_name', $globalPackage->package_name)
                        ->exists();

                    if ($exists && !$this->option('force')) {
                        $this->line("   ⏭️  Saltando {$globalPackage->package_name} (ya existe)");
                        $totalSkipped++;
                        continue;
                    }

                    try {
                        // Create package for this commerce
                        MonitorPackage::updateOrCreate(
                            [
                                'commerce_id' => $commerce->id,
                                'package_name' => $globalPackage->package_name,
                            ],
                            [
                                'app_name' => $globalPackage->app_name,
                                'description' => $globalPackage->description,
                                'is_active' => $globalPackage->is_active,
                                'priority' => $globalPackage->priority,
                                'enabled_default' => $globalPackage->enabled_default ?? true,
                            ]
                        );

                        $this->line("   ✅ Creado: {$globalPackage->package_name}");
                        $totalCreated++;
                    } catch (\Exception $e) {
                        $errorMsg = "Error al crear {$globalPackage->package_name} para commerce {$commerce->id}: {$e->getMessage()}";
                        $this->error("   ❌ {$errorMsg}");
                        $errors[] = $errorMsg;
                    }
                }

                $this->newLine();
            }

            // Optionally delete global packages
            if ($this->option('delete-global')) {
                $this->info('🗑️  Eliminando paquetes globales...');
                $deleted = MonitorPackage::whereNull('commerce_id')->delete();
                $this->info("   ✅ Eliminados {$deleted} paquetes globales.");
            }

            DB::commit();

            $this->newLine();
            $this->info('✅ Migración completada exitosamente!');
            $this->newLine();
            $this->table(
                ['Métrica', 'Valor'],
                [
                    ['Paquetes globales encontrados', $globalPackages->count()],
                    ['Commerces procesados', $commerces->count()],
                    ['Paquetes creados', $totalCreated],
                    ['Paquetes omitidos', $totalSkipped],
                    ['Errores', count($errors)],
                ]
            );

            if (!empty($errors)) {
                $this->newLine();
                $this->warn('⚠️  Se encontraron algunos errores:');
                foreach ($errors as $error) {
                    $this->error("   - {$error}");
                }
            }

            return Command::SUCCESS;
        } catch (\Exception $e) {
            DB::rollBack();
            $this->error("❌ Error durante la migración: {$e->getMessage()}");
            $this->error("   Stack trace: {$e->getTraceAsString()}");
            return Command::FAILURE;
        }
    }
}

