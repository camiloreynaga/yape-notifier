<?php
namespace App\Http\Controllers\SuperAdmin;

use App\Http\Controllers\Controller;
use App\Models\ReferralCommission;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class CommissionsController extends Controller
{
    public function index(Request $request): JsonResponse
    {
        $q = ReferralCommission::with([
                'referrer:id,name,payout_bank,payout_account_type,payout_account_holder,payout_account_holder_doc',
                'referred:id,name',
                'renewal:id,new_expires_at,amount_paid',
            ])
            ->orderByDesc('created_at');

        if ($s = $request->query('status')) {
            $q->where('status', $s);
        }
        if ($m = $request->query('month')) {
            // month format YYYY-MM
            $q->whereYear('created_at', substr($m, 0, 4))
              ->whereMonth('created_at', substr($m, 5, 2));
        }
        if ($r = $request->query('referrer_id')) {
            $q->where('referrer_commerce_id', $r);
        }
        if ($r = $request->query('referred_id')) {
            $q->where('referred_commerce_id', $r);
        }

        return response()->json($q->paginate(20));
    }

    public function approve(Request $request, int $id): JsonResponse
    {
        $c = ReferralCommission::findOrFail($id);
        abort_if($c->status !== ReferralCommission::STATUS_PENDING, 422, 'Solo se pueden aprobar comisiones pendientes');

        $c->update([
            'status' => ReferralCommission::STATUS_APPROVED,
            'approved_by_user_id' => $request->user()->id,
        ]);
        return response()->json($c->fresh());
    }

    public function pay(Request $request, int $id): JsonResponse
    {
        $request->validate(['payout_reference' => 'required|string|max:100']);
        $c = ReferralCommission::with('referrer')->findOrFail($id);

        abort_if($c->status !== ReferralCommission::STATUS_APPROVED, 422, 'Solo se pueden pagar comisiones aprobadas');
        abort_unless(
            $c->referrer && $c->referrer->hasPayoutAccount(),
            422,
            'El comercio referidor no tiene cuenta de pago configurada'
        );

        $c->update([
            'status' => ReferralCommission::STATUS_PAID,
            'paid_at' => now(),
            'payout_reference' => $request->input('payout_reference'),
            'paid_by_user_id' => $request->user()->id,
        ]);
        return response()->json($c->fresh());
    }

    public function void(Request $request, int $id): JsonResponse
    {
        $request->validate(['reason' => 'required|string|max:500']);
        $c = ReferralCommission::findOrFail($id);
        abort_if($c->status === ReferralCommission::STATUS_VOID, 422, 'La comisión ya está anulada');

        $c->update([
            'status' => ReferralCommission::STATUS_VOID,
            'voided_reason' => $request->input('reason'),
        ]);
        return response()->json($c->fresh());
    }
}
