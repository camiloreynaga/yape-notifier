<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class EnsureCommerceActive
{
    public function handle(Request $request, Closure $next): Response
    {
        $user = $request->user();

        // Super admins bypass this check
        if ($user->isSuperAdmin()) {
            return $next($request);
        }

        $commerce = $user->commerce;

        if (!$commerce) {
            return response()->json([
                'message' => 'No commerce associated with this account.',
            ], 403);
        }

        if ($commerce->isPending()) {
            return response()->json([
                'message' => 'Your commerce is pending approval. Please wait for a super admin to activate your account.',
                'status' => 'pending',
            ], 403);
        }

        if ($commerce->isSuspended()) {
            return response()->json([
                'message' => 'Your commerce has been suspended. Please contact support.',
                'status' => 'suspended',
            ], 403);
        }

        return $next($request);
    }
}
