<?php
namespace App\Http\Controllers;

use App\Http\Requests\Commerce\UpdatePayoutAccountRequest;
use App\Models\Commerce;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class PayoutAccountController extends Controller
{
    public function show(Request $request): JsonResponse
    {
        $commerce = $this->resolveCommerce($request);
        return response()->json([
            'payout_bank' => $commerce->payout_bank,
            'payout_account_type' => $commerce->payout_account_type,
            'payout_account_number' => $commerce->payout_account_number,
            'payout_account_holder' => $commerce->payout_account_holder,
            'payout_account_holder_doc' => $commerce->payout_account_holder_doc,
            'is_complete' => $commerce->hasPayoutAccount(),
        ]);
    }

    public function update(UpdatePayoutAccountRequest $request): JsonResponse
    {
        $commerce = $this->resolveCommerce($request);
        $commerce->fill($request->validated())->save();

        return response()->json([
            'message' => 'Cuenta de pago actualizada',
            'is_complete' => true,
        ]);
    }

    private function resolveCommerce(Request $request): Commerce
    {
        $user = $request->user();
        $commerce = $user->commerce_id ? Commerce::find($user->commerce_id) : null;
        abort_unless($commerce, 404, 'Usuario no pertenece a un comercio');
        return $commerce;
    }
}
