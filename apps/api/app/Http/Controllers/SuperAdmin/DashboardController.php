<?php

namespace App\Http\Controllers\SuperAdmin;

use App\Http\Controllers\Controller;
use App\Models\Commerce;
use Illuminate\Http\JsonResponse;

class DashboardController extends Controller
{
    public function kpis(): JsonResponse
    {
        $all = Commerce::all();
        $buckets = [
            'pending' => 0, 'active' => 0, 'expiring_soon' => 0,
            'in_grace' => 0, 'expired' => 0, 'suspended' => 0,
        ];
        foreach ($all as $commerce) {
            $status = $commerce->expiryStatus();
            if (isset($buckets[$status])) {
                $buckets[$status]++;
            }
        }
        return response()->json(array_merge(['total' => $all->count()], $buckets));
    }
}
