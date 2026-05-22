<?php
namespace Tests\Feature\Referral;

use App\Models\Commerce;
use App\Models\ReferralCommission;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Crypt;
use Illuminate\Support\Facades\DB;
use Tests\TestCase;

class CommerceReferralModelTest extends TestCase
{
    use RefreshDatabase;

    public function test_payout_account_number_is_encrypted_in_db(): void
    {
        $owner = User::factory()->create();
        $commerce = Commerce::create([
            'name' => 'Test Commerce',
            'owner_user_id' => $owner->id,
            'status' => 'active',
            'referral_code' => 'test-aaaa',
            'payout_account_number' => '00112233445566778899',
        ]);

        $raw = DB::table('commerces')->where('id', $commerce->id)->value('payout_account_number');
        $this->assertNotSame('00112233445566778899', $raw);
        $this->assertSame('00112233445566778899', Crypt::decryptString($raw));
        $this->assertSame('00112233445566778899', $commerce->fresh()->payout_account_number);
    }

    public function test_referrer_and_referrals_relationships(): void
    {
        $u1 = User::factory()->create();
        $u2 = User::factory()->create();
        $referrer = Commerce::create([
            'name' => 'Karol',
            'owner_user_id' => $u1->id,
            'status' => 'active',
            'referral_code' => 'karol-bbbb',
        ]);
        $referred = Commerce::create([
            'name' => 'Pepe',
            'owner_user_id' => $u2->id,
            'status' => 'active',
            'referral_code' => 'pepe-cccc',
            'referred_by_commerce_id' => $referrer->id,
        ]);

        $this->assertEquals($referrer->id, $referred->referrer->id);
        $this->assertTrue($referrer->referrals->contains('id', $referred->id));
    }

    public function test_has_payout_account_returns_true_only_when_all_fields_present(): void
    {
        $u = User::factory()->create();
        $c = Commerce::create([
            'name' => 'X',
            'owner_user_id' => $u->id,
            'status' => 'active',
            'referral_code' => 'x-dddd',
        ]);
        $this->assertFalse($c->hasPayoutAccount());

        $c->fill([
            'payout_bank' => 'BCP',
            'payout_account_type' => 'cci',
            'payout_account_number' => '0021901',
            'payout_account_holder' => 'X',
            'payout_account_holder_doc' => '11111111',
        ])->save();

        $this->assertTrue($c->fresh()->hasPayoutAccount());
    }

    public function test_referral_commission_has_relationships_and_constants(): void
    {
        $u1 = User::factory()->create();
        $u2 = User::factory()->create();
        $r1 = Commerce::create(['name'=>'A','owner_user_id'=>$u1->id,'status'=>'active','referral_code'=>'a-eeee']);
        $r2 = Commerce::create(['name'=>'B','owner_user_id'=>$u2->id,'status'=>'active','referral_code'=>'b-ffff','referred_by_commerce_id'=>$r1->id]);

        $rc = ReferralCommission::create([
            'referrer_commerce_id' => $r1->id,
            'referred_commerce_id' => $r2->id,
            'commerce_renewal_id' => null,
            'base_amount' => 49.00,
            'commission_rate' => 0.2000,
            'amount' => 9.80,
            'status' => ReferralCommission::STATUS_PENDING,
        ]);

        $this->assertEquals($r1->id, $rc->referrer->id);
        $this->assertEquals($r2->id, $rc->referred->id);
        $this->assertSame('pending', ReferralCommission::STATUS_PENDING);
        $this->assertSame('approved', ReferralCommission::STATUS_APPROVED);
        $this->assertSame('paid', ReferralCommission::STATUS_PAID);
        $this->assertSame('void', ReferralCommission::STATUS_VOID);
    }
}
