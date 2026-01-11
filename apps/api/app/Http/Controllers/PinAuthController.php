<?php

namespace App\Http\Controllers;

use App\Http\Requests\Auth\LoginPinRequest;
use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Log;

class PinAuthController extends Controller
{
    /**
     * Maximum login attempts before blocking.
     */
    private const MAX_ATTEMPTS = 5;

    /**
     * Lockout duration in seconds (15 minutes).
     */
    private const LOCKOUT_SECONDS = 900;
    /**
     * Login with PIN.
     * 
     * POST /api/auth/login-pin
     * Body: { "pin": "1234" }
     * 
     * @param LoginPinRequest $request
     * @return JsonResponse
     */
    public function loginWithPin(LoginPinRequest $request): JsonResponse
    {
        try {
            $ip = $request->ip();
            $cacheKey = 'pin_attempts:' . $ip;

            // Check rate limiting
            $attempts = Cache::get($cacheKey, 0);
            if ($attempts >= self::MAX_ATTEMPTS) {
                $ttl = Cache::ttl($cacheKey);
                Log::warning('PIN login blocked: too many attempts', [
                    'ip' => $ip,
                    'attempts' => $attempts,
                    'remaining_lockout' => $ttl,
                ]);

                return response()->json([
                    'message' => 'Demasiados intentos. Intente nuevamente en ' . ceil($ttl / 60) . ' minutos.',
                    'retry_after' => $ttl,
                ], 429);
            }

            $pin = $request->input('pin');

            // Buscar usuario por PIN activo
            $user = User::where('pin', $pin)
                ->where('is_active', true)
                ->first();

            if (!$user) {
                // Increment failed attempts
                Cache::put($cacheKey, $attempts + 1, self::LOCKOUT_SECONDS);

                Log::warning('PIN login failed: invalid PIN', [
                    'ip' => $ip,
                    'user_agent' => $request->userAgent(),
                    'attempt' => $attempts + 1,
                ]);

                return response()->json([
                    'message' => 'PIN inválido o usuario inactivo',
                ], 401);
            }
            
            // Verificar que el usuario tenga commerce_id
            if (!$user->commerce_id) {
                Log::error('User without commerce_id attempted login', [
                    'user_id' => $user->id,
                    'user_name' => $user->name,
                ]);
                
                return response()->json([
                    'message' => 'Usuario no asociado a un comercio',
                ], 403);
            }
            
            // Clear rate limiting on successful login
            Cache::forget($cacheKey);

            // Generar token de autenticación (válido por 30 días)
            $token = $user->createToken('android-device', ['*'], now()->addDays(30))->plainTextToken;

            Log::info('PIN login successful', [
                'user_id' => $user->id,
                'user_name' => $user->name,
                'commerce_id' => $user->commerce_id,
                'role' => $user->role,
                'ip' => $request->ip(),
            ]);
            
            return response()->json([
                'message' => 'Login exitoso',
                'token' => $token,
                'user' => [
                    'id' => $user->id,
                    'name' => $user->name,
                    'email' => $user->email,
                    'role' => $user->role,
                    'commerce_id' => $user->commerce_id,
                ],
            ], 200);
            
        } catch (\Exception $e) {
            Log::error('PIN login error', [
                'error' => $e->getMessage(),
                'trace' => $e->getTraceAsString(),
            ]);
            
            return response()->json([
                'message' => 'Error al iniciar sesión',
                'error' => config('app.debug') ? $e->getMessage() : null,
            ], 500);
        }
    }
}

