<?php

namespace App\Http\Controllers\SuperAdmin;

use App\Http\Controllers\Controller;
use App\Models\Plan;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class PlanController extends Controller
{
    public function index(): JsonResponse
    {
        $plans = Plan::withCount('commerces')->orderBy('price')->get();

        return response()->json(['plans' => $plans]);
    }

    public function store(Request $request): JsonResponse
    {
        $request->validate([
            'name'                      => 'required|string|max:255',
            'slug'                      => 'required|string|unique:plans,slug|max:100',
            'max_devices'               => 'nullable|integer|min:1',
            'max_notifications_per_day' => 'nullable|integer|min:1',
            'price'                     => 'required|numeric|min:0',
        ]);

        $plan = Plan::create($request->only([
            'name', 'slug', 'max_devices', 'max_notifications_per_day', 'price',
        ]));

        return response()->json(['plan' => $plan], 201);
    }

    public function update(Request $request, int $id): JsonResponse
    {
        $plan = Plan::findOrFail($id);

        $validated = $request->validate([
            'name'                      => 'sometimes|string|max:255',
            'max_devices'               => 'nullable|integer|min:1',
            'max_notifications_per_day' => 'nullable|integer|min:1',
            'price'                     => 'sometimes|numeric|min:0',
            'is_active'                 => 'sometimes|boolean',
        ]);

        $plan->update($validated);

        return response()->json(['plan' => $plan->fresh()]);
    }
}
