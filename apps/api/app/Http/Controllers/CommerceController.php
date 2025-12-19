<?php

namespace App\Http\Controllers;

use App\Http\Requests\Commerce\CreateCommerceRequest;
use App\Services\CommerceService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class CommerceController extends Controller
{
    public function __construct(
        protected CommerceService $commerceService
    ) {}

    /**
     * Create a new commerce.
     */
    public function store(CreateCommerceRequest $request): JsonResponse
    {
        $user = $request->user();

        // Check if user already has a commerce
        if ($user->commerce_id) {
            return response()->json([
                'message' => 'User already belongs to a commerce',
            ], 400);
        }

        $commerce = $this->commerceService->createCommerce($user, $request->validated());

        return response()->json([
            'message' => 'Commerce created successfully',
            'commerce' => $commerce->load('owner'),
        ], 201);
    }

    /**
     * Get current user's commerce.
     */
    public function show(Request $request): JsonResponse
    {
        $user = $request->user();
        $commerce = $this->commerceService->getUserCommerce($user);

        if (!$commerce) {
            return response()->json([
                'message' => 'User does not belong to a commerce',
            ], 404);
        }

        return response()->json([
            'commerce' => $commerce,
        ]);
    }

    /**
     * Check if current user has a commerce.
     * Returns a simple boolean response for quick verification.
     */
    public function check(Request $request): JsonResponse
    {
        $user = $request->user();
        $hasCommerce = !is_null($user->commerce_id);

        return response()->json([
            'has_commerce' => $hasCommerce,
            'commerce_id' => $user->commerce_id,
        ]);
    }
}



