<?php

namespace App\Http\Controllers\SuperAdmin;

use App\Http\Controllers\Controller;
use App\Models\Commerce;
use App\Models\Plan;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class CommerceManagementController extends Controller
{
    /**
     * List all commerces with filters.
     * GET /api/admin/commerces?status=pending|active|suspended
     */
    public function index(Request $request): JsonResponse
    {
        $query = Commerce::with(['owner', 'plan', 'approvedBy'])
            ->withCount(['devices', 'users', 'notifications']);

        if ($request->filled('status')) {
            $query->where('status', $request->status);
        }

        $commerces = $query->orderBy('created_at', 'desc')->paginate(20);

        return response()->json($commerces);
    }

    /**
     * List only pending commerces.
     * GET /api/admin/commerces/pending
     */
    public function pending(): JsonResponse
    {
        $commerces = Commerce::with(['owner', 'plan'])
            ->withCount(['devices', 'users'])
            ->where('status', 'pending')
            ->orderBy('created_at', 'asc')
            ->get();

        return response()->json([
            'commerces' => $commerces,
            'total' => $commerces->count(),
        ]);
    }

    /**
     * Approve a commerce and assign a plan.
     * PATCH /api/admin/commerces/{id}/approve
     */
    public function approve(Request $request, int $id): JsonResponse
    {
        $request->validate([
            'plan_slug' => 'required|string|exists:plans,slug',
        ]);

        $commerce = Commerce::findOrFail($id);

        if ($commerce->isActive()) {
            return response()->json([
                'message' => 'Commerce is already active.',
            ], 400);
        }

        $plan = Plan::where('slug', $request->plan_slug)->where('is_active', true)->firstOrFail();
        $commerce = app(\App\Services\CommerceService::class)->approve($commerce, $plan, $request->user());

        return response()->json([
            'message'  => 'Commerce approved successfully.',
            'commerce' => $commerce->load(['owner', 'plan', 'approvedBy']),
        ]);
    }

    /**
     * Suspend a commerce.
     * PATCH /api/admin/commerces/{id}/suspend
     */
    public function suspend(Request $request, int $id): JsonResponse
    {
        $commerce = Commerce::findOrFail($id);

        if ($commerce->isSuspended()) {
            return response()->json([
                'message' => 'Commerce is already suspended.',
            ], 400);
        }

        $commerce->update(['status' => 'suspended']);

        return response()->json([
            'message'  => 'Commerce suspended.',
            'commerce' => $commerce->fresh()->load(['owner', 'plan']),
        ]);
    }

    /**
     * Reactivate a suspended commerce.
     * PATCH /api/admin/commerces/{id}/reactivate
     */
    public function reactivate(Request $request, int $id): JsonResponse
    {
        $commerce = Commerce::findOrFail($id);

        if (!$commerce->isSuspended()) {
            return response()->json([
                'message' => 'Commerce is not suspended.',
            ], 400);
        }

        $commerce->update(['status' => 'active']);

        return response()->json([
            'message'  => 'Commerce reactivated.',
            'commerce' => $commerce->fresh()->load(['owner', 'plan']),
        ]);
    }

    /**
     * Change the plan of an active commerce.
     * PATCH /api/admin/commerces/{id}/plan
     */
    public function changePlan(Request $request, int $id): JsonResponse
    {
        $request->validate([
            'plan_slug' => 'required|string|exists:plans,slug',
        ]);

        $commerce = Commerce::findOrFail($id);
        $plan = Plan::where('slug', $request->plan_slug)->where('is_active', true)->firstOrFail();

        $commerce->update(['plan_id' => $plan->id]);

        return response()->json([
            'message'  => 'Plan updated successfully.',
            'commerce' => $commerce->fresh()->load(['owner', 'plan']),
        ]);
    }

    /**
     * Show details of a single commerce.
     * GET /api/admin/commerces/{id}
     */
    public function show(int $id): JsonResponse
    {
        $commerce = Commerce::with(['owner', 'plan', 'approvedBy', 'devices', 'users'])
            ->withCount(['notifications'])
            ->findOrFail($id);

        return response()->json(['commerce' => $commerce]);
    }
}
