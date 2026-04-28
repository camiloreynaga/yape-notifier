<?php

namespace Tests\Unit;

use App\Models\Commerce;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class CommerceExpiryStatusTest extends TestCase
{
    use RefreshDatabase;

    public function test_pending_commerce_returns_pending_status(): void
    {
        $commerce = Commerce::factory()->pending()->create();
        $this->assertEquals('pending', $commerce->expiryStatus());
    }

    public function test_suspended_commerce_returns_suspended_status(): void
    {
        $commerce = Commerce::factory()->suspended()->create();
        $this->assertEquals('suspended', $commerce->expiryStatus());
    }

    public function test_active_with_more_than_7_days_returns_active(): void
    {
        $commerce = Commerce::factory()->create([
            'plan_expires_at' => now()->addDays(15),
        ]);
        $this->assertEquals('active', $commerce->expiryStatus());
    }

    public function test_active_with_less_than_7_days_returns_expiring_soon(): void
    {
        $commerce = Commerce::factory()->expiringSoon()->create();
        $this->assertEquals('expiring_soon', $commerce->expiryStatus());
    }

    public function test_active_past_expiry_within_grace_returns_in_grace(): void
    {
        $commerce = Commerce::factory()->inGrace()->create();
        $this->assertEquals('in_grace', $commerce->expiryStatus());
    }

    public function test_active_past_grace_returns_expired(): void
    {
        $commerce = Commerce::factory()->expiredPostGrace()->create();
        $this->assertEquals('expired', $commerce->expiryStatus());
    }
}
