<?php

namespace Tests\Unit;

use App\Jobs\SuspendExpiredCommercesJob;
use App\Models\Commerce;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class SuspendExpiredCommercesJobTest extends TestCase
{
    use RefreshDatabase;

    public function test_suspends_active_commerces_past_grace_period(): void
    {
        $expired = Commerce::factory()->expiredPostGrace()->create();
        $inGrace = Commerce::factory()->inGrace()->create();
        $active = Commerce::factory()->create();

        (new SuspendExpiredCommercesJob())->handle();

        $this->assertEquals('suspended', $expired->fresh()->status);
        $this->assertEquals('active', $inGrace->fresh()->status);
        $this->assertEquals('active', $active->fresh()->status);
    }
}
