<?php
namespace App\Observers;

use App\Models\CommerceRenewal;
use App\Models\ReferralCommission;

class CommerceRenewalObserver
{
    public const COMMISSION_RATE = 0.20;

    public function created(CommerceRenewal $renewal): void
    {
        $commerce = $renewal->commerce;
        if (! $commerce || ! $commerce->referred_by_commerce_id) {
            return;
        }
        if ($commerce->status !== 'active') {
            return;
        }
        if ((float) $renewal->amount_paid <= 0) {
            return;
        }

        $base = (float) $renewal->amount_paid;
        $rate = self::COMMISSION_RATE;

        ReferralCommission::create([
            'referrer_commerce_id' => $commerce->referred_by_commerce_id,
            'referred_commerce_id' => $commerce->id,
            'commerce_renewal_id' => $renewal->id,
            'base_amount' => $base,
            'commission_rate' => $rate,
            'amount' => round($base * $rate, 2),
            'status' => ReferralCommission::STATUS_PENDING,
        ]);
    }

    public function deleting(CommerceRenewal $renewal): void
    {
        ReferralCommission::where('commerce_renewal_id', $renewal->id)
            ->whereIn('status', [ReferralCommission::STATUS_PENDING, ReferralCommission::STATUS_APPROVED])
            ->update([
                'status' => ReferralCommission::STATUS_VOID,
                'voided_reason' => 'Renewal eliminado',
            ]);
    }
}
