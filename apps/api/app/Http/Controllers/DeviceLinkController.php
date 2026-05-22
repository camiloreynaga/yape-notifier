<?php

namespace App\Http\Controllers;

use App\Http\Requests\Device\LinkDeviceByCodeRequest;
use App\Services\DeviceLinkService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;

class DeviceLinkController extends Controller
{
    public function __construct(
        protected DeviceLinkService $deviceLinkService
    ) {}

    /**
     * Generate a link code for the authenticated user's commerce.
     * Requires user to be admin of the commerce.
     *
     * POST /api/devices/generate-link-code
     */
    public function generateLinkCode(Request $request): JsonResponse
    {
        try {
            $user = $request->user();

            // Check if user has commerce
            if (!$user->commerce_id) {
                return response()->json([
                    'message' => 'Usuario no pertenece a un negocio',
                ], 400);
            }

            // Check if user is admin
            if (!$user->isAdmin()) {
                return response()->json([
                    'message' => 'Solo los administradores pueden generar códigos de vinculación',
                ], 403);
            }

            // Check device limit based on plan
            $commerce = $user->commerce()->with('plan')->first();
            if ($commerce && !$commerce->canAddDevice()) {
                return response()->json([
                    'message' => 'Has alcanzado el límite de dispositivos de tu plan (' . $commerce->plan->max_devices . '). Actualiza tu plan para añadir más dispositivos.',
                    'plan' => $commerce->plan->name,
                    'max_devices' => $commerce->plan->max_devices,
                    'current_devices' => $commerce->activeDevicesCount(),
                ], 403);
            }

            $linkCode = $this->deviceLinkService->generateLinkCode($user->commerce_id);
            
            // Get device alias from request if provided (for mobile admin app)
            $deviceAlias = $request->input('device_alias', null);

            return response()->json([
                'message' => 'Código de vinculación generado exitosamente',
                'code' => $linkCode->code,
                'expires_at' => $linkCode->expires_at->toIso8601String(),
                // For web compatibility: return code as string
                'link_code' => $linkCode->code,
                // QR code data: the code itself (string format for scanning)
                'qr_code_data' => $linkCode->code,
                // Device alias from request (optional, for mobile admin app)
                'device_alias' => $deviceAlias,
            ], 201);
        } catch (\Exception $e) {
            Log::error('Failed to generate device link code', [
                'user_id' => $request->user()->id,
                'error' => $e->getMessage(),
                'trace' => $e->getTraceAsString(),
            ]);

            return response()->json([
                'message' => 'Error al generar código de vinculación',
                'error' => config('app.debug') ? $e->getMessage() : null,
            ], 500);
        }
    }

    /**
     * Validate a link code (public endpoint).
     * Returns commerce information if code is valid.
     *
     * GET /api/devices/link-code/{code}
     */
    public function validateLinkCode(Request $request, string $code): JsonResponse
    {
        try {
            // Normalize code to uppercase
            $code = strtoupper($code);

            $validation = $this->deviceLinkService->validateCode($code);

            if (!$validation['valid']) {
                return response()->json([
                    'valid' => false,
                    'message' => $validation['message'],
                ], 400);
            }

            // Get commerce information
            $commerce = \App\Models\Commerce::find($validation['commerce_id']);

            return response()->json([
                'valid' => true,
                'message' => 'Código válido',
                'commerce' => [
                    'id' => $commerce->id,
                    'name' => $commerce->name,
                ],
            ]);
        } catch (\Exception $e) {
            Log::error('Failed to validate device link code', [
                'code' => $code,
                'error' => $e->getMessage(),
            ]);

            return response()->json([
                'valid' => false,
                'message' => 'Error al validar código',
                'error' => config('app.debug') ? $e->getMessage() : null,
            ], 500);
        }
    }

    /**
     * Link a device to a commerce using a link code.
     * 
     * Professional Architecture Approach:
     * - Authentication is OPTIONAL (flexible UX for capturer mode)
     * - The link code itself is the authorization mechanism
     * - If user is authenticated, device is associated with user (traceability)
     * - If device doesn't exist, it's created automatically
     *
     * POST /api/devices/link-by-code
     */
    public function linkByCode(LinkDeviceByCodeRequest $request): JsonResponse
    {
        try {
            // Authentication is optional - link code is the authorization mechanism
            $user = $request->user(); // Will be null if not authenticated
            $code = $request->input('code');
            $deviceUuid = $request->input('device_uuid');
            $deviceName = $request->input('device_name'); // Optional device name

            $result = $this->deviceLinkService->linkDevice($code, $deviceUuid, $user, $deviceName);

            if (!$result['success']) {
                return response()->json([
                    'message' => $result['message'],
                ], 400);
            }

            return response()->json([
                'message' => $result['message'],
                'device' => $result['device'],
            ], 200);
        } catch (\Exception $e) {
            Log::error('Failed to link device by code', [
                'user_id' => $request->user()?->id,
                'code' => $request->input('code'),
                'device_uuid' => $request->input('device_uuid'),
                'device_name' => $request->input('device_name'),
                'error' => $e->getMessage(),
                'file' => $e->getFile(),
                'line' => $e->getLine(),
                'trace' => $e->getTraceAsString(),
            ]);

            // Professional approach: Always return error details in development, sanitized in production
            $errorDetails = config('app.debug') ? [
                'message' => $e->getMessage(),
                'file' => $e->getFile(),
                'line' => $e->getLine(),
                'exception' => get_class($e),
            ] : [
                'message' => 'Error al vincular dispositivo. Por favor, contacta al soporte.',
            ];

            return response()->json([
                'message' => 'Error al vincular dispositivo',
                'error' => $errorDetails,
            ], 500);
        }
    }

    /**
     * Get active link codes for the authenticated user's commerce.
     * Requires admin role.
     *
     * GET /api/devices/link-codes
     */
    public function getActiveCodes(Request $request): JsonResponse
    {
        try {
            $user = $request->user();

            if (!$user->commerce_id) {
                return response()->json([
                    'message' => 'Usuario no pertenece a un negocio',
                ], 400);
            }

            if (!$user->isAdmin()) {
                return response()->json([
                    'message' => 'Solo los administradores pueden ver los códigos',
                ], 403);
            }

            $codes = $this->deviceLinkService->getActiveCodes($user->commerce_id);

            return response()->json([
                'codes' => $codes,
            ]);
        } catch (\Exception $e) {
            Log::error('Failed to get active link codes', [
                'user_id' => $request->user()->id,
                'error' => $e->getMessage(),
            ]);

            return response()->json([
                'message' => 'Error al obtener códigos',
                'error' => config('app.debug') ? $e->getMessage() : null,
            ], 500);
        }
    }
}

