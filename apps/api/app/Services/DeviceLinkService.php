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
     * Professional Architecture Approach:
     * - The link code is the primary authorization mechanism (not user authentication)
     * - Device can be linked without prior registration (flexible UX)
     * - If user is authenticated, device is also associated with user (optional traceability)
     * - If device doesn't exist, it's created automatically (find-or-create pattern)
     *
     * @param string $code
     * @param string $deviceUuid
     * @param User|null $user Optional authenticated user for traceability
     * @param string|null $deviceName Optional device name from request
     * @return array{success: bool, device: Device|null, message: string}
     */
    public function linkDevice(string $code, string $deviceUuid, ?User $user = null, ?string $deviceName = null): array
    {
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
            // Device doesn't exist - create it automatically
            // This enables flexible UX: devices can be linked without prior registration
            Log::info('Device not found, creating automatically during link', [
                'device_uuid' => $deviceUuid,
                'user_id' => $user?->id,
                'commerce_id' => $linkCode->commerce_id,
                'code' => $code,
            ]);

            $device = Device::create([
                'uuid' => $deviceUuid,
                'user_id' => $user?->id, // Optional: associate with user if authenticated
                'commerce_id' => $linkCode->commerce_id,
                'name' => $deviceName ?? 'Android Device',
                'platform' => 'android',
                'is_active' => true,
                'last_seen_at' => now(),
            ]);
            
            $wasCreated = true;

            Log::info('Device created automatically during link', [
                'device_id' => $device->id,
                'device_uuid' => $deviceUuid,
                'commerce_id' => $linkCode->commerce_id,
                'user_id' => $user?->id,
            ]);
        } else {
            // Device exists - verify and update
            // Check if device already belongs to a different commerce
            if ($device->commerce_id && $device->commerce_id !== $linkCode->commerce_id) {
                return [
                    'success' => false,
                    'device' => null,
                    'message' => 'El dispositivo ya pertenece a otro negocio',
                ];
            }

            // Update device with commerce_id and optionally user_id
            $updateData = [
                'commerce_id' => $linkCode->commerce_id,
                'last_seen_at' => now(),
            ];

            // If user is authenticated and device doesn't have user_id, associate it
            if ($user && !$device->user_id) {
                $updateData['user_id'] = $user->id;
                Log::info('Associating existing device with authenticated user', [
                    'device_id' => $device->id,
                    'user_id' => $user->id,
                ]);
            }

            $device->update($updateData);
        }

        // Mark code as used
        $linkCode->markAsUsed();
        $linkCode->update(['device_id' => $device->id]);

        Log::info('Device linked to commerce via code', [
            'device_id' => $device->id,
            'device_uuid' => $deviceUuid,
            'commerce_id' => $linkCode->commerce_id,
            'code' => $code,
            'user_id' => $user?->id,
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

