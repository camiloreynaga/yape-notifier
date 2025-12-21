package com.yapenotifier.android.util

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PaymentNotificationFilter.
 * 
 * Tests cover:
 * - Valid payment notifications (should pass)
 * - Advertisements and promotions (should be excluded)
 * - Reminders and informational messages (should be excluded)
 * - Invalid amounts (should be excluded)
 * - Edge cases
 */
class PaymentNotificationFilterTest {

    // ========== VALID PAYMENT NOTIFICATIONS (should pass) ==========
    
    @Test
    fun `valid Yape payment notification should pass`() {
        val title = "Yape"
        val text = "JOHN DOE te envió un pago por S/ 50. El cód. de seguridad es: 427"
        
        assertTrue("Valid Yape payment should pass", 
            PaymentNotificationFilter.isValidPaymentNotification(title, text))
    }
    
    @Test
    fun `valid Plin payment notification should pass`() {
        val title = "Plin"
        val text = "MARIA GARCIA te ha plineado S/ 25.50"
        
        assertTrue("Valid Plin payment should pass",
            PaymentNotificationFilter.isValidPaymentNotification(title, text))
    }
    
    @Test
    fun `valid bank transfer notification should pass`() {
        val title = "BCP"
        val text = "PEDRO LOPEZ te transferió un pago de S/ 100"
        
        assertTrue("Valid bank transfer should pass",
            PaymentNotificationFilter.isValidPaymentNotification(title, text))
    }
    
    @Test
    fun `valid payment with dollar currency should pass`() {
        val title = "Yape"
        val text = "JUAN PEREZ te envió un pago por $ 200"
        
        assertTrue("Valid payment with dollars should pass",
            PaymentNotificationFilter.isValidPaymentNotification(title, text))
    }
    
    @Test
    fun `valid payment with decimal amount should pass`() {
        val title = "Plin"
        val text = "ANA SANCHEZ te ha plineado S/ 99.99"
        
        assertTrue("Valid payment with decimal amount should pass",
            PaymentNotificationFilter.isValidPaymentNotification(title, text))
    }

    // ========== ADVERTISEMENTS AND PROMOTIONS (should be excluded) ==========
    
    @Test
    fun `advertisement with discount should be excluded`() {
        val title = "Yape"
        val text = "Hasta $150 dscto. 💸 Solo hoy 15/12 en Despegar exclusivo con Tarjetas Interbank..."
        
        assertFalse("Advertisement with discount should be excluded",
            PaymentNotificationFilter.isValidPaymentNotification(title, text))
    }
    
    @Test
    fun `promotional message should be excluded`() {
        val title = "Plin"
        val text = "¡Aprovecha nuestra oferta especial! Gana hasta S/ 500 en sorteos exclusivos"
        
        assertFalse("Promotional message should be excluded",
            PaymentNotificationFilter.isValidPaymentNotification(title, text))
    }
    
    @Test
    fun `advertisement with multiple exclusion keywords should be excluded`() {
        val title = "Yape"
        val text = "¿Ya te depositaron? 💰💰 👀👀 Ingresa al app y revisa tu dinero disponible..."
        
        assertFalse("Advertisement with multiple exclusion keywords should be excluded",
            PaymentNotificationFilter.isValidPaymentNotification(title, text))
    }
    
    @Test
    fun `promotional campaign should be excluded`() {
        val title = "BCP"
        val text = "Participa en nuestra campaña y gana regalos exclusivos. Solo hoy!"
        
        assertFalse("Promotional campaign should be excluded",
            PaymentNotificationFilter.isValidPaymentNotification(title, text))
    }

    // ========== REMINDERS AND INFORMATIONAL MESSAGES (should be excluded) ==========
    
    @Test
    fun `reminder message should be excluded`() {
        val title = "Yape"
        val text = "¡No dejes que tu recibo venza! Recuerda que puedes yapear tus pagos..."
        
        assertFalse("Reminder message should be excluded",
            PaymentNotificationFilter.isValidPaymentNotification(title, text))
    }
    
    @Test
    fun `informational message about deposit should be excluded`() {
        val title = "Plin"
        val text = "¿Ya te depositaron? Revisa tu dinero disponible en el app"
        
        assertFalse("Informational message should be excluded",
            PaymentNotificationFilter.isValidPaymentNotification(title, text))
    }
    
    @Test
    fun `currency exchange message should be excluded`() {
        val title = "Yape"
        val text = "¿Por vender dólares? 💰$ Hazlo al toque desde Yape. ¡Cambia ahora! ⭐💰"
        
        assertFalse("Currency exchange message should be excluded",
            PaymentNotificationFilter.isValidPaymentNotification(title, text))
    }
    
    @Test
    fun `balance check reminder should be excluded`() {
        val title = "BCP"
        val text = "Recuerda revisar tu saldo disponible. Ingresa al app cuando quieras"
        
        assertFalse("Balance check reminder should be excluded",
            PaymentNotificationFilter.isValidPaymentNotification(title, text))
    }

    // ========== CONSUMPTION NOTIFICATIONS (should be excluded) ==========
    
    @Test
    fun `card consumption notification should be excluded`() {
        val title = "Interbank"
        val text = "Realizaste un consumo con tu tarjeta por S/ 150.00"
        
        assertFalse("Card consumption should be excluded (not a received payment)",
            PaymentNotificationFilter.isValidPaymentNotification(title, text))
    }
    
    @Test
    fun `movement notification should be excluded`() {
        val title = "BBVA"
        val text = "Movimiento en tu cuenta: Consulta tu saldo disponible"
        
        assertFalse("Movement notification should be excluded",
            PaymentNotificationFilter.isValidPaymentNotification(title, text))
    }

    // ========== INVALID AMOUNTS (should be excluded) ==========
    
    @Test
    fun `notification with zero amount should be excluded`() {
        val title = "Yape"
        val text = "JOHN DOE te envió un pago por S/ 0"
        
        assertFalse("Notification with zero amount should be excluded",
            PaymentNotificationFilter.isValidPaymentNotification(title, text))
    }
    
    @Test
    fun `notification with excessive amount should be excluded`() {
        val title = "Plin"
        val text = "MARIA GARCIA te ha plineado S/ 2000000"
        
        assertFalse("Notification with excessive amount should be excluded",
            PaymentNotificationFilter.isValidPaymentNotification(title, text))
    }

    // ========== EDGE CASES ==========
    
    @Test
    fun `empty notification should be excluded`() {
        val title = ""
        val text = ""
        
        assertFalse("Empty notification should be excluded",
            PaymentNotificationFilter.isValidPaymentNotification(title, text))
    }
    
    @Test
    fun `notification without amount should be excluded`() {
        val title = "Yape"
        val text = "JOHN DOE te envió un mensaje"
        
        assertFalse("Notification without amount should be excluded",
            PaymentNotificationFilter.isValidPaymentNotification(title, text))
    }
    
    @Test
    fun `notification without payment action should be excluded`() {
        val title = "Plin"
        val text = "JOHN DOE tiene S/ 50 disponibles"
        
        assertFalse("Notification without payment action should be excluded",
            PaymentNotificationFilter.isValidPaymentNotification(title, text))
    }
    
    @Test
    fun `notification with single exclusion keyword should pass if valid structure`() {
        // Single exclusion keyword should not exclude if structure is valid
        val title = "Yape"
        val text = "JOHN DOE te envió un pago por S/ 50. Revisa tu saldo"
        
        // This should pass because it has valid payment structure despite having "revisa"
        assertTrue("Notification with single exclusion keyword but valid structure should pass",
            PaymentNotificationFilter.isValidPaymentNotification(title, text))
    }
    
    @Test
    fun `notification matching inclusion pattern should pass`() {
        val title = "Yape"
        val text = "PEDRO LOPEZ te envió un pago por S/ 75.50"
        
        assertTrue("Notification matching inclusion pattern should pass",
            PaymentNotificationFilter.isValidPaymentNotification(title, text))
    }
}


