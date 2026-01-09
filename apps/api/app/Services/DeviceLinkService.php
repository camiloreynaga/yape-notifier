<?php

namespace App\Services;

use App\Models\Device;
use App\Models\DeviceLinkCode;
use App\Models\User;
use Carbon\Carbon;
use Illuminate\Support\Facades\Log;

class DeviceLinkService
{
    /**
     * Generate a link code for a commerce.
     * Code expires in 24 hours.
     *
     * @param int $commerceId
     * @return DeviceLinkCode
     * @throws \Exception
     */
    public function generateLinkCode(int $commerceId): DeviceLinkCode
    {
        $code = DeviceLinkCode::generateUniqueCode();
        $expiresAt = now()->addHours(24);

        $linkCode = DeviceLinkCode::create([
            'commerce_id' => $commerceId,
            'code' => $code,
            'expires_at' => $expiresAt,
        ]);

        Log::info('Device link code generated', [
            'code' => $code,
            'commerce_id' => $commerceId,
            'expires_at' => $expiresAt,
        ]);

        return $linkCode;
    }

    /**
     * Validate a link code and return commerce_id if valid.
     *
     * @param string $code
     * @return array{valid: bool, commerce_id: int|null, message: string}
     */
    public function validateCode(string $code): array
    {
        $linkCode = DeviceLinkCode::where('code', strtoupper($code))->first();

        if (!$linkCode) {
            return [
                'valid' => false,
                'commerce_id' => null,
                'message' => 'Código no encontrado',
            ];
        }

        if ($linkCode->isUsed()) {
            return [
                'valid' => false,
                'commerce_id' => null,
                'message' => 'Código ya utilizado',
            ];
        }

        if ($linkCode->isExpired()) {
            return [
                'valid' => false,
                'commerce_id' => null,
                'message' => 'Código expirado',
            ];
        }

        Log::info('Device link code validated', [
            'code' => $code,
            'commerce_id' => $linkCode->commerce_id,
        ]);

        return [
            'valid' => true,
            'commerce_id' => $linkCode->commerce_id,
            'message' => 'Código válido',
        ];
    }

    /**
     * Link a device to a commerce using a link code.
     * 
     * Professional Architecture Approach (WITH PIN AUTHENTICATION):
     * - User authentication is REQUIRED (via PIN)
     * - Every device MUST have an owner (user_id NOT NULL)
     * - Complete traceability: who owns which device
     * - The link code validates the commerce, the user provides accountability
     *
     * @param string $code
     * @param string $deviceUuid
     * @param User $user REQUIRED authenticated user (from PIN login)
     * @param string|null $deviceName Optional device name from request
     * @return array{success: bool, device: Device|null, message: string}
     */
    public function linkDevice(string $code, string $deviceUuid, User $user, ?string $deviceName = null): array
    {
        // User is now REQUIRED (not nullable)
        if (!$user) {
            return [
                'success' => false,
                'device' => null,
                'message' => 'Autenticación requerida. Por favor, inicia sesión con tu PIN.',
            ];
        }

        // Validate code
        $validation = $this->validateCode($code);

        if (!$validation['valid']) {
            return [
                'success' => false,
                'device' => null,
                'message' => $validation['message'],
            ];
        }

        $linkCode = DeviceLinkCode::where('code', strtoupper($code))->first();

        // Validate UUID format before querying database
        $uuidPattern = '/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i';
        if (!preg_match($uuidPattern, $deviceUuid)) {
            return [
                'success' => false,
                'device' => null,
                'message' => 'Formato de UUID inválido',
            ];
        }

        // Find device by UUID (without user_id filter for flexibility)
        // Professional approach: UUID is the primary identifier, user_id is optional
        $device = Device::where('uuid', $deviceUuid)->first();

        $wasCreated = false;
        
        if (!$device) {
            // Device doesn't exist - create it WITH user_id (REQUIRED)
            Log::info('Device not found, creating with authenticated user', [
                'device_uuid' => $deviceUuid,
                'user_id' => $user->id,
                'user_name' => $user->name,
                'commerce_id' => $linkCode->commerce_id,
                'code' => $code,
            ]);

            try {
                $device = Device::create([
                    'uuid' => $deviceUuid,
                    'user_id' => $user->id, // REQUIRED: Every device has an owner
                    'commerce_id' => $linkCode->commerce_id,
                    'name' => $deviceName ?? 'Android Device',
                    'alias' => $user->name ? "Teléfono de {$user->name}" : null,
                    'platform' => 'android',
                    'is_active' => true,
                    'last_seen_at' => now(),
                ]);
            } catch (\Illuminate\Database\QueryException $e) {
                // Log the specific database error for debugging
                Log::error('Failed to create device during link', [
                    'device_uuid' => $deviceUuid,
                    'user_id' => $user->id,
                    'commerce_id' => $linkCode->commerce_id,
                    'error' => $e->getMessage(),
                    'sql_state' => $e->getSqlState() ?? null,
                    'error_code' => $e->getCode(),
                ]);
                
                // Re-throw with a more user-friendly message
                throw new \RuntimeException(
                    'Error al crear el dispositivo. Verifica que la base de datos esté correctamente configurada.',
                    $e->getCode(),
                    $e
                );
            }
            
            $wasCreated = true;

            Log::info('Device created with authenticated user', [
                'device_id' => $device->id,
                'device_uuid' => $deviceUuid,
                'commerce_id' => $linkCode->commerce_id,
                'user_id' => $user->id,
                'user_name' => $user->name,
            ]);
        } else {
            // Device exists - verify ownership and update
            
            // Security check: Verify device belongs to the same user
            if ($device->user_id !== $user->id) {
                Log::warning('Device ownership mismatch during link', [
                    'device_id' => $device->id,
                    'device_user_id' => $device->user_id,
                    'attempting_user_id' => $user->id,
                ]);
                
                return [
                    'success' => false,
                    'device' => null,
                    'message' => 'Este dispositivo ya está vinculado a otro usuario',
                ];
            }
            
            // Check if device already belongs to a different commerce
            if ($device->commerce_id && $device->commerce_id !== $linkCode->commerce_id) {
                return [
                    'success' => false,
                    'device' => null,
                    'message' => 'El dispositivo ya pertenece a otro negocio',
                ];
            }

            // Update device with new commerce_id
            $device->update([
                'commerce_id' => $linkCode->commerce_id,
                'last_seen_at' => now(),
            ]);
            
            Log::info('Device updated with new commerce', [
                'device_id' => $device->id,
                'user_id' => $user->id,
                'commerce_id' => $linkCode->commerce_id,
            ]);
        }

        // Mark code as used
        $linkCode->markAsUsed();
        $linkCode->update(['device_id' => $device->id]);

        Log::info('Device linked to commerce via code with user', [
            'device_id' => $device->id,
            'device_uuid' => $deviceUuid,
            'commerce_id' => $linkCode->commerce_id,
            'code' => $code,
            'user_id' => $user->id,
            'user_name' => $user->name,
            'was_created' => $wasCreated,
        ]);

        return [
            'success' => true,
            'device' => $device->fresh(),
            'message' => 'Dispositivo vinculado exitosamente',
        ];
    }

    /**
     * Get active link codes for a commerce.
     *
     * @param int $commerceId
     * @return \Illuminate\Database\Eloquent\Collection
     */
    public function getActiveCodes(int $commerceId)
    {
        return DeviceLinkCode::where('commerce_id', $commerceId)
            ->valid()
            ->orderBy('created_at', 'desc')
            ->get();
    }

    /**
     * Clean up expired codes (can be called by a scheduled job).
     *
     * @return int Number of codes cleaned up
     */
    public function cleanupExpiredCodes(): int
    {
        $expiredCount = DeviceLinkCode::expired()
            ->whereNull('used_at')
            ->count();

        DeviceLinkCode::expired()
            ->whereNull('used_at')
            ->delete();

        if ($expiredCount > 0) {
            Log::info('Expired device link codes cleaned up', [
                'count' => $expiredCount,
            ]);
        }

        return $expiredCount;
    }
}

