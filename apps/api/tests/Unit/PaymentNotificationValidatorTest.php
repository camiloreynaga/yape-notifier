<?php

namespace Tests\Unit;

use App\Services\PaymentNotificationValidator;
use Tests\TestCase;

class PaymentNotificationValidatorTest extends TestCase
{
    /**
     * Test valid payment notifications (should pass)
     */
    public function test_it_accepts_valid_yape_payment(): void
    {
        $data = [
            'body' => 'JOHN DOE te envió un pago por S/ 50. El cód. de seguridad es: 427',
            'amount' => 50.00,
            'source_app' => 'yape',
        ];

        $result = PaymentNotificationValidator::isValid($data);

        $this->assertTrue($result['valid']);
        $this->assertNull($result['reason']);
    }

    public function test_it_accepts_valid_plin_payment(): void
    {
        $data = [
            'body' => 'MARIA GARCIA te ha plineado S/ 25.50',
            'amount' => 25.50,
            'source_app' => 'plin',
        ];

        $result = PaymentNotificationValidator::isValid($data);

        $this->assertTrue($result['valid']);
        $this->assertNull($result['reason']);
    }

    public function test_it_accepts_valid_bank_transfer(): void
    {
        $data = [
            'body' => 'PEDRO LOPEZ te transferió un pago de S/ 100',
            'amount' => 100.00,
            'source_app' => 'bcp',
        ];

        $result = PaymentNotificationValidator::isValid($data);

        $this->assertTrue($result['valid']);
        $this->assertNull($result['reason']);
    }

    public function test_it_accepts_payment_with_amount_only(): void
    {
        $data = [
            'body' => 'Recibiste un pago',
            'amount' => 50.00,
            'source_app' => 'yape',
        ];

        $result = PaymentNotificationValidator::isValid($data);

        $this->assertTrue($result['valid']);
    }

    public function test_it_accepts_valid_amount_range(): void
    {
        $data = [
            'body' => 'JUAN PEREZ te envió un pago por S/ 999999.99',
            'amount' => 999999.99,
            'source_app' => 'yape',
        ];

        $result = PaymentNotificationValidator::isValid($data);

        $this->assertTrue($result['valid']);
    }

    /**
     * Test invalid notifications (should be rejected)
     */
    public function test_it_rejects_advertisement_with_discount(): void
    {
        $data = [
            'body' => 'Hasta $150 dscto. 💸 Solo hoy 15/12 en Despegar exclusivo con Tarjetas Interbank...',
            'amount' => 150.00,
            'source_app' => 'interbank',
        ];

        $result = PaymentNotificationValidator::isValid($data);

        $this->assertFalse($result['valid']);
        $this->assertNotNull($result['reason']);
    }

    public function test_it_rejects_reminder_notification(): void
    {
        $data = [
            'body' => '¿Ya te depositaron? 💰💰 👀👀 Ingresa al app y revisa tu dinero disponible...',
            'source_app' => 'yape',
        ];

        $result = PaymentNotificationValidator::isValid($data);

        $this->assertFalse($result['valid']);
        $this->assertNotNull($result['reason']);
    }

    public function test_it_rejects_payment_reminder(): void
    {
        $data = [
            'body' => '¡No dejes que tu recibo venza! Recuerda que puedes yapear tus pagos...',
            'source_app' => 'yape',
        ];

        $result = PaymentNotificationValidator::isValid($data);

        $this->assertFalse($result['valid']);
        $this->assertNotNull($result['reason']);
    }

    public function test_it_rejects_currency_exchange_promotion(): void
    {
        $data = [
            'body' => '¿Por vender dólares? 💰$ Hazlo al toque desde Yape. ¡Cambia ahora! ⭐💰',
            'source_app' => 'yape',
        ];

        $result = PaymentNotificationValidator::isValid($data);

        $this->assertFalse($result['valid']);
        $this->assertNotNull($result['reason']);
    }

    public function test_it_rejects_card_consumption_notification(): void
    {
        $data = [
            'body' => 'Realizaste un consumo con tu tarjeta por S/ 200',
            'amount' => 200.00,
            'source_app' => 'bcp',
        ];

        $result = PaymentNotificationValidator::isValid($data);

        $this->assertFalse($result['valid']);
        $this->assertNotNull($result['reason']);
    }

    public function test_it_rejects_promotion_with_multiple_keywords(): void
    {
        $data = [
            'body' => 'Aprovecha esta oferta exclusiva con descuento solo hoy',
            'amount' => 100.00,
            'source_app' => 'yape',
        ];

        $result = PaymentNotificationValidator::isValid($data);

        $this->assertFalse($result['valid']);
        $this->assertStringContainsString('exclusion keywords', $result['reason']);
    }

    public function test_it_rejects_invalid_amount_too_high(): void
    {
        $data = [
            'body' => 'JUAN PEREZ te envió un pago por S/ 2000000',
            'amount' => 2000000.00,
            'source_app' => 'yape',
        ];

        $result = PaymentNotificationValidator::isValid($data);

        $this->assertFalse($result['valid']);
        $this->assertStringContainsString('Invalid amount', $result['reason']);
    }

    public function test_it_rejects_invalid_amount_too_low(): void
    {
        $data = [
            'body' => 'JUAN PEREZ te envió un pago por S/ 0.001',
            'amount' => 0.001,
            'source_app' => 'yape',
        ];

        $result = PaymentNotificationValidator::isValid($data);

        $this->assertFalse($result['valid']);
        $this->assertStringContainsString('Invalid amount', $result['reason']);
    }

    public function test_it_rejects_notification_without_amount_and_pattern(): void
    {
        $data = [
            'body' => 'Mensaje genérico sin patrón de pago',
            'source_app' => 'yape',
        ];

        $result = PaymentNotificationValidator::isValid($data);

        $this->assertFalse($result['valid']);
        $this->assertNotNull($result['reason']);
    }

    public function test_it_rejects_promotion_with_exclusion_pattern(): void
    {
        $data = [
            'body' => 'Hasta S/ 500 descuento solo hoy en nuestra tienda',
            'amount' => 500.00,
            'source_app' => 'yape',
        ];

        $result = PaymentNotificationValidator::isValid($data);

        $this->assertFalse($result['valid']);
        $this->assertStringContainsString('exclusion pattern', $result['reason']);
    }

    /**
     * Test edge cases
     */
    public function test_it_handles_empty_body(): void
    {
        $data = [
            'body' => '',
            'source_app' => 'yape',
        ];

        $result = PaymentNotificationValidator::isValid($data);

        $this->assertFalse($result['valid']);
    }

    public function test_it_handles_null_amount(): void
    {
        $data = [
            'body' => 'JUAN PEREZ te envió un pago por S/ 50',
            'amount' => null,
            'source_app' => 'yape',
        ];

        $result = PaymentNotificationValidator::isValid($data);

        // Should still be valid if it matches inclusion pattern
        $this->assertTrue($result['valid']);
    }

    public function test_it_handles_title_and_body_combined(): void
    {
        $data = [
            'title' => 'Yape',
            'body' => 'JOHN DOE te envió un pago por S/ 50',
            'amount' => 50.00,
            'source_app' => 'yape',
        ];

        $result = PaymentNotificationValidator::isValid($data);

        $this->assertTrue($result['valid']);
    }

    public function test_it_handles_case_insensitive_matching(): void
    {
        $data = [
            'body' => 'MARIA GARCIA TE HA PLINEADO S/ 25.50',
            'amount' => 25.50,
            'source_app' => 'plin',
        ];

        $result = PaymentNotificationValidator::isValid($data);

        $this->assertTrue($result['valid']);
    }

    public function test_it_rejects_single_exclusion_keyword_with_no_inclusion(): void
    {
        $data = [
            'body' => 'Revisa tu saldo disponible',
            'source_app' => 'yape',
        ];

        $result = PaymentNotificationValidator::isValid($data);

        $this->assertFalse($result['valid']);
    }

    /**
     * Test helper methods
     */
    public function test_get_exclusion_keywords_returns_array(): void
    {
        $keywords = PaymentNotificationValidator::getExclusionKeywords();

        $this->assertIsArray($keywords);
        $this->assertNotEmpty($keywords);
        $this->assertContains('descuento', $keywords);
    }

    public function test_get_exclusion_patterns_returns_array(): void
    {
        $patterns = PaymentNotificationValidator::getExclusionPatterns();

        $this->assertIsArray($patterns);
        $this->assertNotEmpty($patterns);
    }

    public function test_get_inclusion_patterns_returns_array(): void
    {
        $patterns = PaymentNotificationValidator::getInclusionPatterns();

        $this->assertIsArray($patterns);
        $this->assertNotEmpty($patterns);
    }

    /**
     * Test real-world examples from documentation
     */
    public function test_it_rejects_real_world_advertisement_examples(): void
    {
        $examples = [
            '¿Ya te depositaron? 💰💰 👀👀 Ingresa al app y revisa tu dinero disponible...',
            'Hasta $150 dscto. 💸 Solo hoy 15/12 en Despegar exclusivo con Tarjetas Interbank...',
            '¡No dejes que tu recibo venza! Recuerda que puedes yapear tus pagos...',
            '¿Por vender dólares? 💰$ Hazlo al toque desde Yape. ¡Cambia ahora! ⭐💰',
        ];

        foreach ($examples as $body) {
            $data = [
                'body' => $body,
                'source_app' => 'yape',
            ];

            $result = PaymentNotificationValidator::isValid($data);
            $this->assertFalse($result['valid'], "Should reject: {$body}");
        }
    }

    public function test_it_accepts_real_world_payment_examples(): void
    {
        $examples = [
            'JOHN DOE te envió un pago por S/ 50. El cód. de seguridad es: 427',
            'MARIA GARCIA te ha plineado S/ 25.50',
            'PEDRO LOPEZ te transferió un pago de S/ 100',
        ];

        foreach ($examples as $body) {
            $data = [
                'body' => $body,
                'source_app' => 'yape',
            ];

            $result = PaymentNotificationValidator::isValid($data);
            $this->assertTrue($result['valid'], "Should accept: {$body}");
        }
    }
}


