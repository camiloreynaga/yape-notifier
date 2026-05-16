<?php
namespace Tests\Feature\Referral;

use App\Models\Commerce;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class CustomizeReferralCodeTest extends TestCase
{
    use RefreshDatabase;

    private function makeAdminWithCommerce(?string $code = null, $customizedAt = null): array
    {
        $admin = User::factory()->create(['role' => 'admin']);
        $commerce = Commerce::create([
            'name' => 'Karol',
            'owner_user_id' => $admin->id,
            'status' => 'active',
            'referral_code' => $code ?? 'karol-' . uniqid(),
            'referral_code_customized_at' => $customizedAt,
        ]);
        $admin->update(['commerce_id' => $commerce->id]);
        return [$admin->fresh(), $commerce];
    }

    public function test_admin_can_customize_code_once(): void
    {
        [$admin, $commerce] = $this->makeAdminWithCommerce();
        Sanctum::actingAs($admin);

        $response = $this->patchJson('/api/referrals/code', ['referral_code' => 'karol-bodega']);
        $response->assertOk()->assertJsonPath('referral_code', 'karol-bodega');

        $commerce->refresh();
        $this->assertSame('karol-bodega', $commerce->referral_code);
        $this->assertNotNull($commerce->referral_code_customized_at);
    }

    public function test_second_customization_is_rejected(): void
    {
        [$admin, $commerce] = $this->makeAdminWithCommerce(customizedAt: now());
        Sanctum::actingAs($admin);

        $response = $this->patchJson('/api/referrals/code', ['referral_code' => 'karol-otro']);
        $response->assertStatus(422);

        $commerce->refresh();
        $this->assertNotSame('karol-otro', $commerce->referral_code);
    }

    public function test_code_must_be_unique(): void
    {
        $u1 = User::factory()->create();
        Commerce::create([
            'name' => 'Other',
            'owner_user_id' => $u1->id,
            'status' => 'active',
            'referral_code' => 'taken-code',
        ]);

        [$admin] = $this->makeAdminWithCommerce();
        Sanctum::actingAs($admin);

        $response = $this->patchJson('/api/referrals/code', ['referral_code' => 'taken-code']);
        $response->assertStatus(422);
    }

    public function test_code_format_lowercase_alnum_dashes(): void
    {
        [$admin] = $this->makeAdminWithCommerce();
        Sanctum::actingAs($admin);

        // Uppercase rejected
        $this->patchJson('/api/referrals/code', ['referral_code' => 'KarolBodega1'])->assertStatus(422);
        // Spaces rejected
        $this->patchJson('/api/referrals/code', ['referral_code' => 'karol bodega'])->assertStatus(422);
        // Special chars rejected
        $this->patchJson('/api/referrals/code', ['referral_code' => 'karol@bodega'])->assertStatus(422);
    }

    public function test_code_must_have_dash_or_digit(): void
    {
        [$admin] = $this->makeAdminWithCommerce();
        Sanctum::actingAs($admin);

        // Plain single word → reject
        $response = $this->patchJson('/api/referrals/code', ['referral_code' => 'karol']);
        $response->assertStatus(422);

        // With digit → accept
        $this->patchJson('/api/referrals/code', ['referral_code' => 'karol2026'])->assertOk();
    }

    public function test_code_length_4_to_20(): void
    {
        [$admin] = $this->makeAdminWithCommerce();
        Sanctum::actingAs($admin);

        // Too short
        $this->patchJson('/api/referrals/code', ['referral_code' => 'ab1'])->assertStatus(422);
        // Too long
        $this->patchJson('/api/referrals/code', ['referral_code' => str_repeat('a', 19) . '-2'])->assertStatus(422);
        // OK length
        $this->patchJson('/api/referrals/code', ['referral_code' => 'kar-2026'])->assertOk();
    }

    public function test_captador_forbidden(): void
    {
        $admin = User::factory()->create(['role' => 'admin']);
        $commerce = Commerce::create([
            'name' => 'K',
            'owner_user_id' => $admin->id,
            'status' => 'active',
            'referral_code' => 'k-' . substr(uniqid(), -6),
        ]);
        $captador = User::factory()->create(['role' => 'captador', 'commerce_id' => $commerce->id]);
        Sanctum::actingAs($captador);

        $this->patchJson('/api/referrals/code', ['referral_code' => 'karol-new'])->assertStatus(403);
    }

    public function test_stats_includes_customized_at(): void
    {
        [$admin, $commerce] = $this->makeAdminWithCommerce(customizedAt: now()->subDay());
        Sanctum::actingAs($admin);

        $response = $this->getJson('/api/referrals/stats');
        $response->assertOk()->assertJsonStructure(['referral_code', 'referral_code_customized_at']);
        $this->assertNotNull($response->json('referral_code_customized_at'));
    }
}
