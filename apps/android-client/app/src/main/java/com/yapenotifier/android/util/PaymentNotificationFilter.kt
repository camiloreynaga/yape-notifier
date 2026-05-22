package com.yapenotifier.android.util

import android.util.Log
import kotlin.text.RegexOption

/**
 * Filters payment notifications to exclude advertisements, promotions, reminders, and other non-payment notifications.
 * 
 * This filter ensures that only real payment/transfer notifications are processed and sent to the backend,
 * excluding marketing messages, promotional offers, and informational reminders.
 */
object PaymentNotificationFilter {
    private const val TAG = "PaymentNotificationFilter"
    
    // Maximum valid amount (1,000,000 in any currency)
    private const val MAX_VALID_AMOUNT = 1_000_000.0
    
    // Minimum valid amount (must be > 0)
    private const val MIN_VALID_AMOUNT = 0.0
    
    /**
     * Keywords that indicate the notification is NOT a real payment (exclusion list).
     * If a notification contains multiple of these keywords, it should be excluded.
     */
    private val exclusionKeywords = listOf(
        // Publicidad y promociones
        "descuento", "dscto", "oferta", "promoción", "promocion", "aprovecha", "solo hoy",
        "exclusivo", "campaña", "gana", "participa", "sorteo", "regalo", "gratis",
        "hasta", "desde", "despegar", "booking", "trivago", "viaje", "vuelo", "hotel",
        
        // Recordatorios e informativos
        "recuerda", "recordatorio", "no dejes", "venza", "vencer", "revisa", "ingresa",
        "ya te depositaron", "disponible", "úsalo", "cuando quieras", "cambia ahora",
        "vender dólares", "comprar dólares", "cambio", "tipo de cambio",
        
        // Mensajes genéricos sin pago
        "realizaste un consumo", "consumo con tu tarjeta", "movimiento", "saldo",
        "tu saldo", "revisa tu", "consulta", "información",
        
        // Emojis comunes en publicidad (como indicador adicional)
        "💰💰", "👀👀", "⭐", "💸", "🎁", "🎉"
    )
    
    /**
     * Regex patterns that indicate the notification is NOT a real payment (exclusion patterns).
     */
    private val exclusionPatterns = listOf(
        // Patrones de publicidad/promoción
        """hasta.*(S/|\$|soles|dólares).*dscto|descuento""".toRegex(RegexOption.IGNORE_CASE),
        """solo hoy|mañana|esta semana""".toRegex(RegexOption.IGNORE_CASE),
        """ya te depositaron|revisa tu dinero|ingresa al app""".toRegex(RegexOption.IGNORE_CASE),
        """recuerda que puedes|no dejes que.*venza""".toRegex(RegexOption.IGNORE_CASE),
        """por vender|comprar.*dólares""".toRegex(RegexOption.IGNORE_CASE),
        """realizaste un consumo|movimiento en tu""".toRegex(RegexOption.IGNORE_CASE),
        
        // Patrones de recordatorios
        """¿ya te depositaron\?""".toRegex(RegexOption.IGNORE_CASE),
        """revisa tu.*disponible""".toRegex(RegexOption.IGNORE_CASE),
        """no dejes que tu recibo venza""".toRegex(RegexOption.IGNORE_CASE),
        
        // Patrones de cambio de moneda
        """por vender dólares|comprar dólares""".toRegex(RegexOption.IGNORE_CASE),
        """cambio.*moneda|tipo de cambio""".toRegex(RegexOption.IGNORE_CASE)
    )
    
    /**
     * Regex patterns that indicate the notification IS a real payment (inclusion patterns).
     * These patterns take precedence over exclusion patterns.
     * FIXED: Soporte para S/. (con punto) además de S/
     */
    private val inclusionPatterns = listOf(
        // Patrones que SÍ indican pago real recibido - FIXED: S/\.? para soportar S/ y S/.
        """.*te envió un pago por (S/\.?|\$).*""".toRegex(RegexOption.IGNORE_CASE),
        """.*te ha plineado (S/\.?|\$).*""".toRegex(RegexOption.IGNORE_CASE),
        """.*te plineó (S/\.?|\$).*""".toRegex(RegexOption.IGNORE_CASE),
        """.*te (envió|transferió|envio|transfirio) (un pago|dinero) (por|de) (S/\.?|\$).*""".toRegex(RegexOption.IGNORE_CASE),
        """.*recibiste (un pago|dinero) (de|por) (S/\.?|\$).*""".toRegex(RegexOption.IGNORE_CASE),
        """.*pago recibido.*(S/\.?|\$).*""".toRegex(RegexOption.IGNORE_CASE),
        """.*transferencia recibida.*(S/\.?|\$).*""".toRegex(RegexOption.IGNORE_CASE),
        """.*te (envió|transferió|envio|transfirio).*(S/\.?|\$).*\d+.*""".toRegex(RegexOption.IGNORE_CASE),
        // FIXED: Nuevos patrones adicionales
        """.*te ha enviado (S/\.?|\$).*""".toRegex(RegexOption.IGNORE_CASE),
        """.*te mandó (S/\.?|\$).*""".toRegex(RegexOption.IGNORE_CASE),
        // Patrón genérico: Alguien + te + verbo + S/ + monto
        """.+\s+te\s+\w+.*(S/\.?|\$)\s*\d+.*""".toRegex(RegexOption.IGNORE_CASE)
    )
    
    /**
     * Result of filtering a notification.
     */
    data class FilterResult(
        val isValid: Boolean,
        val reason: String? = null
    )
    
    /**
     * Validates if a notification is a valid payment notification and returns the reason.
     * 
     * @param title The notification title
     * @param text The notification body text
     * @return FilterResult with isValid flag and reason for rejection (if invalid)
     */
    fun validatePaymentNotification(title: String, text: String): FilterResult {
        val combinedText = "$title $text"
        val lowerText = combinedText.lowercase()
        
        // STEP 1: Check inclusion patterns first (these take precedence)
        val matchesInclusionPattern = inclusionPatterns.any { pattern ->
            pattern.containsMatchIn(combinedText)
        }
        
        if (matchesInclusionPattern) {
            // If it matches an inclusion pattern, validate amount and structure
            val structureResult = validatePaymentStructure(combinedText)
            if (structureResult.isValid) {
                Log.d(TAG, "Notification INCLUDED by inclusion pattern: Title='$title', Text='$text'")
                return FilterResult(true)
            } else {
                Log.d(TAG, "Notification EXCLUDED: Matches inclusion pattern but lacks valid structure: Title='$title', Text='$text', Reason='${structureResult.reason}'")
                return FilterResult(false, "Matches inclusion pattern but ${structureResult.reason}")
            }
        }
        
        // STEP 2: Check exclusion patterns
        val matchingExclusionPattern = exclusionPatterns.firstOrNull { pattern ->
            pattern.containsMatchIn(combinedText)
        }
        
        if (matchingExclusionPattern != null) {
            val reason = "Matches exclusion pattern: ${matchingExclusionPattern.pattern}"
            Log.d(TAG, "Notification EXCLUDED by exclusion pattern: Title='$title', Text='$text', Reason='$reason'")
            return FilterResult(false, reason)
        }
        
        // STEP 3: Check exclusion keywords (count occurrences)
        val exclusionKeywordCount = exclusionKeywords.count { keyword ->
            lowerText.contains(keyword.lowercase())
        }
        
        // If notification contains 2+ exclusion keywords, exclude it
        if (exclusionKeywordCount >= 2) {
            val reason = "Contains $exclusionKeywordCount exclusion keywords"
            Log.d(TAG, "Notification EXCLUDED: $reason: Title='$title', Text='$text'")
            return FilterResult(false, reason)
        }
        
        // STEP 4: Validate payment structure (must have sender + amount + payment action)
        val structureResult = validatePaymentStructure(combinedText)
        if (!structureResult.isValid) {
            Log.d(TAG, "Notification EXCLUDED: Lacks valid payment structure: Title='$title', Text='$text', Reason='${structureResult.reason}'")
            return FilterResult(false, structureResult.reason)
        }
        
        // STEP 5: If it passed all checks, it's a valid payment notification
        Log.d(TAG, "Notification INCLUDED: Passed all validation checks: Title='$title', Text='$text'")
        return FilterResult(true)
    }
    
    /**
     * Validates if a notification is a valid payment notification.
     * Convenience method that returns only boolean.
     * 
     * @param title The notification title
     * @param text The notification body text
     * @return true if the notification is a valid payment, false otherwise
     */
    fun isValidPaymentNotification(title: String, text: String): Boolean {
        return validatePaymentNotification(title, text).isValid
    }
    
    /**
     * Validates that the notification has a valid payment structure:
     * - Contains a sender/remitter
     * - Contains a valid amount (> 0 and < MAX_VALID_AMOUNT)
     * - Contains a payment action (envió, transferió, plineado, etc.)
     * 
     * @return FilterResult with validation result and reason if invalid
     */
    private fun validatePaymentStructure(text: String): FilterResult {
        // Check for payment action keywords -- THIS CHECK IS TOO STRICT AND IS BEING REMOVED
        /*
        val paymentActions = listOf(
            "te envió", "te transferió", "te ha plineado", "te plineó",
            "recibiste", "pago recibido", "transferencia recibida"
        )
        val hasPaymentAction = paymentActions.any { action ->
            text.contains(action, ignoreCase = true)
        }
        
        if (!hasPaymentAction) {
            return FilterResult(false, "Missing payment action (envió, transferió, plineado, etc.)")
        }
        */
        
        // FIXED: Check for amount pattern - ahora soporta S/. (con punto) y comas decimales
        val amountPattern = """(S/\.?|\$)\s*(\d+(?:[.,]\d+)?)""".toRegex(RegexOption.IGNORE_CASE)
        val amountMatch = amountPattern.find(text)
        
        if (amountMatch == null) {
            return FilterResult(false, "No valid amount found (S/, S/. or $ followed by digits)")
        }
        
        // Extract and validate amount - FIXED: soporte para comas decimales
        val amountStr = amountMatch.groupValues[2].replace(",", ".")
        val amount = try {
            amountStr.toDouble()
        } catch (e: NumberFormatException) {
            return FilterResult(false, "Invalid amount format: '$amountStr'")
        }
        
        // Validate amount range
        if (amount <= MIN_VALID_AMOUNT) {
            return FilterResult(false, "Amount must be > $MIN_VALID_AMOUNT (got $amount)")
        }
        if (amount >= MAX_VALID_AMOUNT) {
            return FilterResult(false, "Amount must be < $MAX_VALID_AMOUNT (got $amount)")
        }
        
        // Check for sender (should have text before the payment action)
        // This is a basic check - the parser will do more detailed validation
        val hasPotentialSender = text.length > 20 // Basic heuristic: payment notifications are usually longer
        
        if (!hasPotentialSender) {
            return FilterResult(false, "Text too short to contain sender information")
        }
        
        return FilterResult(true)
    }
    
    /**
     * Convenience method for backward compatibility.
     */
    private fun hasValidPaymentStructure(text: String): Boolean {
        return validatePaymentStructure(text).isValid
    }
}
