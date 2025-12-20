<?php

namespace App\Services;

use Illuminate\Support\Str;

class PaymentNotificationValidator
{
    /**
     * Maximum valid amount (1,000,000)
     */
    private const MAX_AMOUNT = 1000000;

    /**
     * Minimum valid amount (0.01)
     */
    private const MIN_AMOUNT = 0.01;

    /**
     * Exclusion keywords (publicidad, promociones, recordatorios)
     */
    private const EXCLUSION_KEYWORDS = [
        // Publicidad y promociones
        'descuento', 'dscto', 'oferta', 'promoción', 'promocion', 'aprovecha', 'solo hoy',
        'exclusivo', 'campaña', 'gana', 'participa', 'sorteo', 'regalo', 'gratis',
        'hasta', 'desde', 'despegar', 'booking', 'trivago', 'viaje', 'vuelo', 'hotel',
        
        // Recordatorios e informativos
        'recuerda', 'recordatorio', 'no dejes', 'venza', 'vencer', 'revisa', 'ingresa',
        'ya te depositaron', 'disponible', 'úsalo', 'cuando quieras', 'cambia ahora',
        'vender dólares', 'comprar dólares', 'cambio', 'tipo de cambio',
        
        // Mensajes genéricos sin pago
        'realizaste un consumo', 'consumo con tu tarjeta', 'movimiento', 'saldo',
        'tu saldo', 'revisa tu', 'consulta', 'información',
    ];

    /**
     * Exclusion patterns (regex)
     */
    private const EXCLUSION_PATTERNS = [
        // Patrones de publicidad/promoción
        '/hasta.*(S\/|\$|soles|dólares).*(dscto|descuento)/i',
        '/solo (hoy|mañana|esta semana)/i',
        '/ya te depositaron|revisa tu dinero|ingresa al app/i',
        '/recuerda que puedes|no dejes que.*venza/i',
        '/por vender|comprar.*dólares/i',
        '/realizaste un consumo|movimiento en tu/i',
        
        // Patrones de ofertas temporales
        '/hasta.*% (dscto|descuento)/i',
        '/aprovecha.*(hoy|mañana|esta semana)/i',
    ];

    /**
     * Inclusion patterns (solo estos son pagos reales)
     */
    private const INCLUSION_PATTERNS = [
        // Yape
        '/.*te envió un pago por (S\/|\$).*/i',
        '/.*te envió (S\/|\$).*/i',
        '/.*recibiste un pago de.*(S\/|\$).*/i',
        
        // Plin
        '/.*te ha plineado (S\/|\$).*/i',
        '/.*te plineó (S\/|\$).*/i',
        
        // Bancos (genérico)
        '/.*te (envió|transferió) (un pago|dinero) (por|de) (S\/|\$).*/i',
        '/.*recibiste (un pago|dinero) (de|por) (S\/|\$).*/i',
        '/.*pago recibido.*(S\/|\$).*/i',
        '/.*transferencia recibida.*(S\/|\$).*/i',
        '/.*depósito recibido.*(S\/|\$).*/i',
        
        // Patrones genéricos de pago
        '/.*(recibiste|recibió|recibido).*(pago|transferencia|depósito).*(S\/|\$).*/i',
        '/.*(S\/|\$).*(recibido|enviado|transferido).*/i',
    ];

    /**
     * Validate if notification is a valid payment.
     *
     * @param array $data Notification data
     * @return array{valid: bool, reason: string|null}
     */
    public static function isValid(array $data): array
    {
        $body = strtolower($data['body'] ?? '');
        $title = strtolower($data['title'] ?? '');
        $combinedText = $body . ' ' . $title;

        // Check exclusion keywords
        $exclusionCount = 0;
        foreach (self::EXCLUSION_KEYWORDS as $keyword) {
            if (Str::contains($combinedText, $keyword)) {
                $exclusionCount++;
            }
        }

        // If 2+ exclusion keywords found, reject
        if ($exclusionCount >= 2) {
            return [
                'valid' => false,
                'reason' => "Contains {$exclusionCount} exclusion keywords (publicity/promotion)",
            ];
        }

        // Check exclusion patterns
        foreach (self::EXCLUSION_PATTERNS as $pattern) {
            if (preg_match($pattern, $combinedText)) {
                return [
                    'valid' => false,
                    'reason' => "Matches exclusion pattern: {$pattern}",
                ];
            }
        }

        // Check if amount is valid
        $amount = $data['amount'] ?? null;
        if ($amount !== null) {
            $amount = (float) $amount;
            if ($amount < self::MIN_AMOUNT || $amount > self::MAX_AMOUNT) {
                return [
                    'valid' => false,
                    'reason' => "Invalid amount: {$amount} (must be between " . self::MIN_AMOUNT . " and " . self::MAX_AMOUNT . ")",
                ];
            }
        }

        // Check inclusion patterns (must match at least one)
        $matchesInclusion = false;
        foreach (self::INCLUSION_PATTERNS as $pattern) {
            if (preg_match($pattern, $combinedText)) {
                $matchesInclusion = true;
                break;
            }
        }

        // If no inclusion pattern matches and we have exclusion keywords, reject
        if (!$matchesInclusion && $exclusionCount > 0) {
            return [
                'valid' => false,
                'reason' => "No inclusion pattern matched and contains exclusion keywords",
            ];
        }

        // If no inclusion pattern and no amount, might not be a payment
        if (!$matchesInclusion && $amount === null) {
            return [
                'valid' => false,
                'reason' => "No inclusion pattern matched and no amount specified",
            ];
        }

        // If we have an amount and no exclusion patterns, it's likely valid
        if ($amount !== null && $exclusionCount === 0) {
            return [
                'valid' => true,
                'reason' => null,
            ];
        }

        // If inclusion pattern matches, it's valid
        if ($matchesInclusion) {
            return [
                'valid' => true,
                'reason' => null,
            ];
        }

        // Default: reject if uncertain
        return [
            'valid' => false,
            'reason' => "Uncertain payment notification (no clear payment pattern)",
        ];
    }

    /**
     * Get exclusion keywords (for testing/debugging).
     *
     * @return array
     */
    public static function getExclusionKeywords(): array
    {
        return self::EXCLUSION_KEYWORDS;
    }

    /**
     * Get exclusion patterns (for testing/debugging).
     *
     * @return array
     */
    public static function getExclusionPatterns(): array
    {
        return self::EXCLUSION_PATTERNS;
    }

    /**
     * Get inclusion patterns (for testing/debugging).
     *
     * @return array
     */
    public static function getInclusionPatterns(): array
    {
        return self::INCLUSION_PATTERNS;
    }
}

