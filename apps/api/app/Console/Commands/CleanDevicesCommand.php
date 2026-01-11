<?php

namespace App\Console\Commands;

use App\Models\Device;
use Illuminate\Console\Command;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

class CleanDevicesCommand extends Command
{
    /**
     * The name and signature of the console command.
     *
     * @var string
     */
    protected $signature = 'devices:clean 
                            {--force : Force deletion without confirmation}
                            {--dry-run : Show what would be deleted without actually deleting}';

    /**
     * The console command description.
     *
     * @var string
     */
    protected $description = 'Elimina todos los dispositivos de la base de datos de forma segura';

    /**
     * Execute the console command.
     */
    public function handle(): int
    {
        $this->info('🔍 Analizando base de datos...');

        // Contar registros relacionados
        $deviceCount = Device::count();
        $notificationCount = DB::table('notifications')->count();
        $appInstanceCount = DB::table('app_instances')->count();
        $monitoredAppCount = DB::table('device_monitored_apps')->count();
        $linkCodeCount = DB::table('device_link_codes')
            ->whereNotNull('device_id')
            ->count();

        $this->table(
            ['Tabla', 'Registros'],
            [
                ['devices', number_format($deviceCount, 0, ',', '.')],
                ['notifications', number_format($notificationCount, 0, ',', '.')],
                ['app_instances', number_format($appInstanceCount, 0, ',', '.')],
                ['device_monitored_apps', number_format($monitoredAppCount, 0, ',', '.')],
                ['device_link_codes (con device_id)', number_format($linkCodeCount, 0, ',', '.')],
            ]
        );

        if ($deviceCount === 0) {
            $this->info('✅ No hay dispositivos para eliminar.');
            return Command::SUCCESS;
        }

        // Dry run mode
        if ($this->option('dry-run')) {
            $this->warn('🔍 MODO DRY-RUN: No se eliminarán registros.');
            $this->info("Se eliminarían {$deviceCount} dispositivos y sus registros relacionados.");
            return Command::SUCCESS;
        }

        // Confirmación
        if (!$this->option('force')) {
            $this->warn('⚠️  ADVERTENCIA: Esta operación eliminará:');
            $this->warn("   - {$deviceCount} dispositivos");
            $this->warn("   - {$notificationCount} notificaciones (CASCADE)");
            $this->warn("   - {$appInstanceCount} instancias de apps (CASCADE)");
            $this->warn("   - {$monitoredAppCount} apps monitoreadas (CASCADE)");
            $this->warn("   - Los device_id en device_link_codes se pondrán en NULL");

            if (!$this->confirm('¿Estás seguro de que deseas continuar?', false)) {
                $this->info('❌ Operación cancelada.');
                return Command::FAILURE;
            }
        }

        // Ejecutar eliminación en transacción
        $this->info('🗑️  Eliminando dispositivos...');

        try {
            DB::beginTransaction();

            $deletedCount = Device::query()->delete();

            DB::commit();

            $this->info("✅ Eliminados {$deletedCount} dispositivos exitosamente.");
            Log::info("CleanDevicesCommand: Eliminados {$deletedCount} dispositivos");

            return Command::SUCCESS;
        } catch (\Exception $e) {
            DB::rollBack();
            $this->error("❌ Error al eliminar dispositivos: {$e->getMessage()}");
            Log::error("CleanDevicesCommand: Error al eliminar dispositivos", [
                'error' => $e->getMessage(),
                'trace' => $e->getTraceAsString(),
            ]);

            return Command::FAILURE;
        }
    }
}

