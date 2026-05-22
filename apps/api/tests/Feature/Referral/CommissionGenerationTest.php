<?php
namespace Tests\Feature\Referral;

use App\Models\Commerce;
use App\Models\CommerceRenewal;
use App\Models\Plan;
use App\Models\ReferralCommission;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class CommissionGenerationTest extends TestCase
{
    use RefreshDatabase;

    private function makePair(string $referredStatus = 'active'): array
    {
        $u1 = User::factory()->create();
        $u2 = User::factory()->create();
        $referrer = Commerce::create([
            'name' => 'Karol',
            'owner_user_id' => $u1->id,
            'status' => 'active',
            'referral_code' => 'karol-rxxx',
        ]);
        $referred = Commerce::create([
            'name' => 'Pepe',
            'owner_user_id' => $u2->id,
            'status' => $referredStatus,
            'referral_code' => 'pepe-rxxx',
            'referred_by_commerce_id' => $referrer->id,
        ]);
        return [$referrer, $referred];
    }

    private function makeRenewal(Commerce $commerce, float $amount = 49.00, ?Plan $plan = null): CommerceRenewal
    {
        $plan = $plan ?? Plan::factory()->create(['price' => 49.00]);
        $admin = User::factory()->create(['role' => 'super_admin']);
        return CommerceRenewal::create([
            'commerce_id' => $commerce->id,
            'plan_id' => $plan->id,
            'renewed_by_user_id' => $admin->id,
            'new_expires_at' => now()->addMonth(),
            'amount_paid' => $amount,
        ]);
    }

    public function test_active_referred_renewal_generates_pending_commission_at_20pct(): void
    {
        [$referrer, $referred] = $this->makePair();
        $renewal = $this->makeRenewal($referred, 49.00);

        $c = ReferralCommission::where('commerce_renewal_id', $renewal->id)->first();
        $this->assertNotNull($c);
        $this->assertSame('pending', $c->status);
        $this->assertEquals(49.00, (float) $c->base_amount);
        $this->assertEquals(0.2000, (float) $c->commission_rate);
        $this->assertEquals(9.80, (float) $c->amount);
        $this->assertEquals($referrer->id, $c->referrer_commerce_id);
        $this->assertEquals($referred->id, $c->referred_commerce_id);
    }

    public function test_no_commission_when_no_referrer(): void
    {
        $u = User::factory()->create();
        $commerce = Commerce::create([
            'name' => 'Solo',
            'owner_user_id' => $u->id,
            'status' => 'active',
            'referral_code' => 'solo-rxxx',
        ]);
        $this->makeRenewal($commerce);
        $this->assertSame(0, ReferralCommission::count());
    }

    public function test_no_commission_when_referred_not_active(): void
    {
        [$referrer, $referred] = $this->makePair('pending');
        $this->makeRenewal($referred);
        $this->assertSame(0, ReferralCommission::count());
    }

    public function test_no_commission_when_amount_paid_is_zero(): void
    {
        [$referrer, $referred] = $this->makePair();
        $this->makeRenewal($referred, 0);
        $this->assertSame(0, ReferralCommission::count());
    }

    public function test_deleting_renewal_voids_pending_commission(): void
    {
        [$referrer, $referred] = $this->makePair();
        $renewal = $this->makeRenewal($referred, 49.00);
        $commission = ReferralCommission::where('commerce_renewal_id', $renewal->id)->first();
        $this->assertNotNull($commission);

        $renewal->delete();

        $commission->refresh();
        $this->assertSame('void', $commission->status);
        $this->assertSame('Renewal eliminado', $commission->voided_reason);
    }

    public function test_amount_rounds_to_two_decimals(): void
    {
        [$referrer, $referred] = $this->makePair();
        $renewal = $this->makeRenewal($referred, 33.33);
        $c = ReferralCommission::where('commerce_renewal_id', $renewal->id)->first();
        // 33.33 * 0.20 = 6.666 → 6.67
        $this->assertEquals(6.67, (float) $c->amount);
    }
}
