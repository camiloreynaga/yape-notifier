<?php
namespace Tests\Feature\SuperAdmin;

use App\Models\Commerce;
use App\Models\ReferralCommission;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class CommissionsTest extends TestCase
{
    use RefreshDatabase;

    private function makeCommission(string $status = 'pending', bool $withAccount = true): ReferralCommission
    {
        $u1 = User::factory()->create();
        $u2 = User::factory()->create();
        $refCode = 'ref-' . uniqid();
        $rfdCode = 'rfd-' . uniqid();
        $referrer = Commerce::create([
            'name' => 'Referrer',
            'owner_user_id' => $u1->id,
            'status' => 'active',
            'referral_code' => $refCode,
        ] + ($withAccount ? [
            'payout_bank' => 'BCP',
            'payout_account_type' => 'cci',
            'payout_account_number' => '00210123',
            'payout_account_holder' => 'X',
            'payout_account_holder_doc' => '11111111',
        ] : []));

        $referred = Commerce::create([
            'name' => 'Referred',
            'owner_user_id' => $u2->id,
            'status' => 'active',
            'referral_code' => $rfdCode,
            'referred_by_commerce_id' => $referrer->id,
        ]);

        return ReferralCommission::create([
            'referrer_commerce_id' => $referrer->id,
            'referred_commerce_id' => $referred->id,
            'commerce_renewal_id' => null,
            'base_amount' => 49.00,
            'commission_rate' => 0.2000,
            'amount' => 9.80,
            'status' => $status,
        ]);
    }

    public function test_super_admin_can_list_commissions(): void
    {
        $sa = User::factory()->create(['role' => 'super_admin']);
        $this->makeCommission('pending');
        $this->makeCommission('paid');

        Sanctum::actingAs($sa);
        $response = $this->getJson('/api/admin/commissions');
        $response->assertOk();
        $this->assertCount(2, $response->json('data'));
    }

    public function test_list_filter_by_status(): void
    {
        $sa = User::factory()->create(['role' => 'super_admin']);
        $this->makeCommission('pending');
        $this->makeCommission('paid');

        Sanctum::actingAs($sa);
        $response = $this->getJson('/api/admin/commissions?status=paid');
        $response->assertOk();
        $this->assertCount(1, $response->json('data'));
        $this->assertSame('paid', $response->json('data.0.status'));
    }

    public function test_approve_moves_pending_to_approved(): void
    {
        $sa = User::factory()->create(['role' => 'super_admin']);
        $c = $this->makeCommission('pending');

        Sanctum::actingAs($sa);
        $response = $this->postJson("/api/admin/commissions/{$c->id}/approve");
        $response->assertOk();
        $c->refresh();
        $this->assertSame('approved', $c->status);
        $this->assertEquals($sa->id, $c->approved_by_user_id);
    }

    public function test_approve_blocked_when_not_pending(): void
    {
        $sa = User::factory()->create(['role' => 'super_admin']);
        $c = $this->makeCommission('approved');

        Sanctum::actingAs($sa);
        $this->postJson("/api/admin/commissions/{$c->id}/approve")->assertStatus(422);
    }

    public function test_pay_with_reference_when_account_complete(): void
    {
        $sa = User::factory()->create(['role' => 'super_admin']);
        $c = $this->makeCommission('approved', withAccount: true);

        Sanctum::actingAs($sa);
        $response = $this->postJson("/api/admin/commissions/{$c->id}/pay", ['payout_reference' => 'OP-12345']);
        $response->assertOk();
        $c->refresh();
        $this->assertSame('paid', $c->status);
        $this->assertSame('OP-12345', $c->payout_reference);
        $this->assertEquals($sa->id, $c->paid_by_user_id);
        $this->assertNotNull($c->paid_at);
    }

    public function test_pay_blocked_when_no_account(): void
    {
        $sa = User::factory()->create(['role' => 'super_admin']);
        $c = $this->makeCommission('approved', withAccount: false);

        Sanctum::actingAs($sa);
        $response = $this->postJson("/api/admin/commissions/{$c->id}/pay", ['payout_reference' => 'OP-1']);
        $response->assertStatus(422);
        $c->refresh();
        $this->assertSame('approved', $c->status);
    }

    public function test_pay_blocked_when_not_approved(): void
    {
        $sa = User::factory()->create(['role' => 'super_admin']);
        $c = $this->makeCommission('pending');

        Sanctum::actingAs($sa);
        $this->postJson("/api/admin/commissions/{$c->id}/pay", ['payout_reference' => 'OP-1'])->assertStatus(422);
    }

    public function test_void_with_reason(): void
    {
        $sa = User::factory()->create(['role' => 'super_admin']);
        $c = $this->makeCommission('pending');

        Sanctum::actingAs($sa);
        $response = $this->postJson("/api/admin/commissions/{$c->id}/void", ['reason' => 'fraude']);
        $response->assertOk();
        $c->refresh();
        $this->assertSame('void', $c->status);
        $this->assertSame('fraude', $c->voided_reason);
    }

    public function test_void_requires_reason(): void
    {
        $sa = User::factory()->create(['role' => 'super_admin']);
        $c = $this->makeCommission('pending');

        Sanctum::actingAs($sa);
        $this->postJson("/api/admin/commissions/{$c->id}/void", [])->assertStatus(422);
    }

    public function test_non_super_admin_forbidden(): void
    {
        $admin = User::factory()->create(['role' => 'admin']);
        $c = $this->makeCommission('pending');

        Sanctum::actingAs($admin);
        $this->getJson('/api/admin/commissions')->assertStatus(403);
        $this->postJson("/api/admin/commissions/{$c->id}/approve")->assertStatus(403);
        $this->postJson("/api/admin/commissions/{$c->id}/pay", ['payout_reference' => 'X'])->assertStatus(403);
        $this->postJson("/api/admin/commissions/{$c->id}/void", ['reason' => 'X'])->assertStatus(403);
    }
}
