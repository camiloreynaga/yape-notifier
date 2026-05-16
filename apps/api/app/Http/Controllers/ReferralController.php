<?php
namespace App\Http\Controllers;

use App\Models\Commerce;
use App\Models\ReferralCommission;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class ReferralController extends Controller
{
    public function stats(Request $request): JsonResponse
    {
        $commerce = $this->resolveCommerce($request);
        $startOfMonth = now()->startOfMonth();

        $monthEarnings = ReferralCommission::where('referrer_commerce_id', $commerce->id)
            ->whereIn('status', ['pending', 'approved', 'paid'])
            ->where('created_at', '>=', $startOfMonth)
            ->sum('amount');

        $pendingBalance = ReferralCommission::where('referrer_commerce_id', $commerce->id)
            ->whereIn('status', ['pending', 'approved'])
            ->sum('amount');

        $lifetimePaid = ReferralCommission::where('referrer_commerce_id', $commerce->id)
            ->where('status', 'paid')
            ->sum('amount');

        $activeReferralsCount = Commerce::where('referred_by_commerce_id', $commerce->id)
            ->where('status', 'active')
            ->count();

        return response()->json([
            'month_earnings' => round((float) $monthEarnings, 2),
            'pending_balance' => round((float) $pendingBalance, 2),
            'lifetime_paid' => round((float) $lifetimePaid, 2),
            'active_referrals_count' => $activeReferralsCount,
            'referral_code' => $commerce->referral_code,
        ]);
    }

    public function referrals(Request $request): JsonResponse
    {
        $commerce = $this->resolveCommerce($request);
        $rows = Commerce::where('referred_by_commerce_id', $commerce->id)
            ->with('plan:id,name,price')
            ->withSum('renewals as total_paid', 'amount_paid')
            ->orderByDesc('created_at')
            ->get(['id', 'name', 'status', 'plan_id', 'plan_expires_at', 'created_at']);

        return response()->json(['data' => $rows]);
    }

    public function commissions(Request $request): JsonResponse
    {
        $commerce = $this->resolveCommerce($request);
        $q = ReferralCommission::where('referrer_commerce_id', $commerce->id)
            ->with(['referred:id,name', 'renewal:id,new_expires_at,amount_paid'])
            ->orderByDesc('created_at');

        if ($status = $request->query('status')) {
            $q->where('status', $status);
        }
        return response()->json($q->paginate(20));
    }

    private function resolveCommerce(Request $request): Commerce
    {
        $user = $request->user();
        $commerce = $user->commerce_id ? Commerce::find($user->commerce_id) : null;
        abort_unless($commerce, 404, 'Usuario no pertenece a un comercio');
        return $commerce;
    }
}
