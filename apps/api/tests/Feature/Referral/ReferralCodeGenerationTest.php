<?php
namespace Tests\Feature\Referral;

use App\Models\Commerce;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class ReferralCodeGenerationTest extends TestCase
{
    use RefreshDatabase;

    public function test_creating_commerce_without_code_auto_generates_one(): void
    {
        $owner = User::factory()->create();
        $commerce = Commerce::create([
            'name' => 'Karol Pharmacy',
            'owner_user_id' => $owner->id,
            'status' => 'pending',
        ]);

        $this->assertNotNull($commerce->referral_code);
        $this->assertMatchesRegularExpression('/^[a-z0-9-]+$/', $commerce->referral_code);
        $this->assertStringStartsWith('karol-ph', $commerce->referral_code);
    }

    public function test_referral_code_is_unique_across_commerces(): void
    {
        $a = Commerce::create([
            'name' => 'Test',
            'owner_user_id' => User::factory()->create()->id,
            'status' => 'pending',
        ]);
        $b = Commerce::create([
            'name' => 'Test',
            'owner_user_id' => User::factory()->create()->id,
            'status' => 'pending',
        ]);
        $this->assertNotSame($a->referral_code, $b->referral_code);
    }

    public function test_existing_referral_code_is_preserved(): void
    {
        $commerce = Commerce::create([
            'name' => 'Test',
            'owner_user_id' => User::factory()->create()->id,
            'status' => 'pending',
            'referral_code' => 'custom-aaaa',
        ]);
        $this->assertSame('custom-aaaa', $commerce->fresh()->referral_code);
    }

    public function test_empty_name_falls_back_to_default_prefix(): void
    {
        // Name with only special chars should still generate a valid code
        $commerce = Commerce::create([
            'name' => '???',
            'owner_user_id' => User::factory()->create()->id,
            'status' => 'pending',
        ]);
        $this->assertNotNull($commerce->referral_code);
        $this->assertMatchesRegularExpression('/^[a-z0-9-]+$/', $commerce->referral_code);
    }
}
