<?php
namespace Tests\Feature\Referral;

use App\Models\Commerce;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class CommerceStoreReferralTest extends TestCase
{
    use RefreshDatabase;

    public function test_valid_referral_code_sets_referred_by_commerce_id(): void
    {
        $referrerOwner = User::factory()->create();
        $referrer = Commerce::create([
            'name' => 'Karol',
            'owner_user_id' => $referrerOwner->id,
            'status' => 'active',
            'referral_code' => 'karol-xxx1',
        ]);

        $newOwner = User::factory()->create(['commerce_id' => null]);
        Sanctum::actingAs($newOwner);

        $response = $this->postJson('/api/commerces', [
            'name' => 'Pepe Bodega',
            'referral_code' => 'karol-xxx1',
        ]);

        $response->assertCreated();
        $newCommerce = Commerce::where('name', 'Pepe Bodega')->firstOrFail();
        $this->assertEquals($referrer->id, $newCommerce->referred_by_commerce_id);
    }

    public function test_nonexistent_referral_code_is_ignored_silently(): void
    {
        $newOwner = User::factory()->create(['commerce_id' => null]);
        Sanctum::actingAs($newOwner);

        $response = $this->postJson('/api/commerces', [
            'name' => 'New Co',
            'referral_code' => 'does-not-exist',
        ]);

        $response->assertCreated();
        $this->assertNull(Commerce::where('name', 'New Co')->firstOrFail()->referred_by_commerce_id);
    }

    public function test_self_referral_is_ignored(): void
    {
        $owner = User::factory()->create(['commerce_id' => null]);
        $existing = Commerce::create([
            'name' => 'Mine',
            'owner_user_id' => $owner->id,
            'status' => 'active',
            'referral_code' => 'mine-yyy2',
        ]);
        // Allow the user to create another commerce (simulate by clearing commerce_id)
        $owner->update(['commerce_id' => null]);

        Sanctum::actingAs($owner);
        $response = $this->postJson('/api/commerces', [
            'name' => 'Another',
            'referral_code' => 'mine-yyy2',
        ]);

        // The store endpoint blocks if user already has commerce_id; we cleared it above
        $response->assertCreated();
        $this->assertNull(Commerce::where('name', 'Another')->firstOrFail()->referred_by_commerce_id);
    }

    public function test_no_referral_code_means_no_referrer(): void
    {
        $newOwner = User::factory()->create(['commerce_id' => null]);
        Sanctum::actingAs($newOwner);

        $response = $this->postJson('/api/commerces', ['name' => 'Solo']);
        $response->assertCreated();
        $this->assertNull(Commerce::where('name', 'Solo')->firstOrFail()->referred_by_commerce_id);
    }
}
