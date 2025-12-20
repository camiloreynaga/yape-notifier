<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * Middleware to ensure user has a commerce before accessing certain routes.
 */
class RequiresCommerce
{
    /**
     * Handle an incoming request.
     *
     * @param  \Closure(\Illuminate\Http\Request): (\Symfony\Component\HttpFoundation\Response)  $next
     */
    public function handle(Request $request, Closure $next): Response
    {
        $user = $request->user();

        if (!$user || !$user->commerce_id) {
            return response()->json([
                'message' => 'Debes pertenecer a un negocio para realizar esta acción. Por favor, crea un negocio primero.',
                'error' => 'commerce_required',
            ], 403);
        }

        return $next($request);
    }
}

