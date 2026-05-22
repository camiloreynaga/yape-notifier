<?php

namespace App\Jobs;

use App\Models\Commerce;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Queue\SerializesModels;
use Illuminate\Support\Facades\Log;

class SuspendExpiredCommercesJob implements ShouldQueue
{
    use Dispatchable, InteractsWithQueue, Queueable, SerializesModels;

    public function handle(): void
    {
        $cutoff = now()->subDays(Commerce::GRACE_DAYS);

        Commerce::where('status', 'active')
            ->whereNotNull('plan_expires_at')
            ->where('plan_expires_at', '<', $cutoff)
            ->each(function (Commerce $commerce) {
                $commerce->update(['status' => 'suspended']);
                Log::info('Auto-suspended commerce after grace period', [
                    'commerce_id' => $commerce->id,
                    'name'        => $commerce->name,
                    'expired_at'  => $commerce->plan_expires_at?->toDateTimeString(),
                ]);
            });
    }
}
