<?php
namespace Tests\Feature\Referral;

use App\Models\Commerce;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class PayoutAccountEndpointsTest extends TestCase
{
    use RefreshDatabase;

    private function makeAdminWithActiveCommerce(): array
    {
        $admin = User::factory()->create(['role' => 'admin']);
        $commerce = Commerce::create([
            'name' => 'C',
            'owner_user_id' => $admin->id,
            'status' => 'active',
            'referral_code' => 'c-zzzz',
        ]);
        $admin->update(['commerce_id' => $commerce->id]);
        return [$admin->fresh(), $commerce];
    }

    public function test_admin_can_update_own_payout_account(): void
    {
        [$admin, $commerce] = $this->makeAdminWithActiveCommerce();
        Sanctum::actingAs($admin);

        $response = $this->putJson('/api/commerces/me/payout-account', [
            'payout_bank' => 'BCP',
            'payout_account_type' => 'cci',
            'payout_account_number' => '00219012345678901234',
            'payout_account_holder' => 'Karol Mendoza',
            'payout_account_holder_doc' => '12345678',
        ]);

        $response->assertOk();
        $commerce->refresh();
        $this->assertSame('BCP', $commerce->payout_bank);
        $this->assertSame('cci', $commerce->payout_account_type);
        $this->assertSame('00219012345678901234', $commerce->payout_account_number);
        $this->assertSame('Karol Mendoza', $commerce->payout_account_holder);
        $this->assertSame('12345678', $commerce->payout_account_holder_doc);
    }

    public function test_get_returns_decrypted_account(): void
    {
        [$admin, $commerce] = $this->makeAdminWithActiveCommerce();
        $commerce->update([
            'payout_bank' => 'BBVA',
            'payout_account_type' => 'ahorros',
            'payout_account_number' => '11122233344455',
            'payout_account_holder' => 'X',
            'payout_account_holder_doc' => '11111111',
        ]);
        Sanctum::actingAs($admin);

        $response = $this->getJson('/api/commerces/me/payout-account');
        $response->assertOk()
            ->assertJsonPath('payout_account_number', '11122233344455')
            ->assertJsonPath('is_complete', true);
    }

    public function test_get_returns_is_complete_false_when_empty(): void
    {
        [$admin, $commerce] = $this->makeAdminWithActiveCommerce();
        Sanctum::actingAs($admin);

        $response = $this->getJson('/api/commerces/me/payout-account');
        $response->assertOk()->assertJsonPath('is_complete', false);
    }

    public function test_partial_payload_is_rejected(): void
    {
        [$admin] = $this->makeAdminWithActiveCommerce();
        Sanctum::actingAs($admin);

        $response = $this->putJson('/api/commerces/me/payout-account', [
            'payout_bank' => 'BCP',
        ]);
        $response->assertStatus(422);
    }

    public function test_invalid_account_type_is_rejected(): void
    {
        [$admin] = $this->makeAdminWithActiveCommerce();
        Sanctum::actingAs($admin);

        $response = $this->putJson('/api/commerces/me/payout-account', [
            'payout_bank' => 'BCP',
            'payout_account_type' => 'crypto', // invalid
            'payout_account_number' => '0021',
            'payout_account_holder' => 'X',
            'payout_account_holder_doc' => '1',
        ]);
        $response->assertStatus(422);
    }

    public function test_captador_cannot_update_payout_account(): void
    {
        $admin = User::factory()->create(['role' => 'admin']);
        $commerce = Commerce::create([
            'name' => 'C',
            'owner_user_id' => $admin->id,
            'status' => 'active',
            'referral_code' => 'c-yyyy',
        ]);
        $captador = User::factory()->create(['role' => 'captador', 'commerce_id' => $commerce->id]);
        Sanctum::actingAs($captador);

        $response = $this->putJson('/api/commerces/me/payout-account', [
            'payout_bank' => 'BCP',
            'payout_account_type' => 'cci',
            'payout_account_number' => '0021',
            'payout_account_holder' => 'X',
            'payout_account_holder_doc' => '1',
        ]);
        $response->assertStatus(403);
    }
}
