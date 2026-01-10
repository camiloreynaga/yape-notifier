package com.yapenotifier.android.util

import android.util.Log
import kotlin.text.RegexOption

data class PaymentDetails(
    val sender: String,
    val amount: Double,
    val currency: String,
    val securityCode: String? = null  // Código de seguridad (solo Yape -> Yape)
)

object PaymentNotificationParser {

    private const val TAG = "PaymentParser"
    
    /**
     * Converts currency symbols to ISO 4217 currency codes.
     * The backend requires 3-character currency codes (e.g., "PEN", "USD").
     * 
     * @param currencySymbol The currency symbol extracted from notification (e.g., "S/", "S/.", "$")
     * @return The ISO currency code (e.g., "PEN", "USD")
     */
    private fun normalizeCurrency(currencySymbol: String): String {
        // FIXED: Normalizar removiendo puntos y espacios para manejar S/. y S/
        val normalized = currencySymbol.trim().uppercase().replace(".", "").replace(" ", "")
        return when (normalized) {
            "S/", "S", "SOL", "SOLES", "PEN" -> "PEN"  // Peruvian Sol
            "$", "USD", "US$", "DOLAR", "DOLARES" -> "USD"  // US Dollar
            else -> {
                Log.w(TAG, "Unknown currency symbol: '$currencySymbol', defaulting to PEN")
                "PEN"  // Default to PEN for Peru
            }
        }
    }
    
    /**
     * FIXED: Helper para parsear montos con soporte para comas y puntos decimales
     */
    private fun parseAmount(amountStr: String): Double? {
        return try {
            // Reemplazar comas por puntos para decimales (formato europeo/latinoamericano)
            amountStr.replace(",", ".").toDouble()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing amount: '$amountStr'", e)
            null
        }
    }

    /**
     * Extrae el código de seguridad de una notificación de Yape.
     * Ejemplo: "El cód. de seguridad es: 693" -> "693"
     * Ejemplo: "El código de seguridad es: 161" -> "161"
     *
     * IMPORTANTE: Solo las transferencias Yape -> Yape incluyen código de seguridad.
     * Transferencias de otros bancos/billeteras -> Yape NO tienen código.
     *
     * @param text El texto completo de la notificación
     * @return El código de seguridad o null si no se encuentra
     */
    private fun extractSecurityCode(text: String): String? {
        // Patrón para capturar el código de seguridad
        // Soporta variaciones: "cód.", "código", "cod.", "codigo"
        val securityCodePattern = """c[óo]d(?:igo|\.)?\s+de\s+seguridad\s+es:\s*(\d{2,4})""".toRegex(RegexOption.IGNORE_CASE)

        val matchResult = securityCodePattern.find(text)
        return matchResult?.groupValues?.get(1)?.also {
            Log.d(TAG, "Security code extracted: $it")
        }
    }

    // FIXED: Patrones actualizados para soportar S/. (con punto) y variaciones de espacios
    // Yape patterns
    private val yapePattern = """^(?:Yape!?\s*)?(.*?)\s+te envió un pago por\s*(S/\.?|\$)\s*(\d+[.,]?\d*).*""".toRegex(RegexOption.IGNORE_CASE)
    private val yapePatternAlt = """(.*?)\s+te envió\s+(?:un pago\s+)?(?:por\s+)?(S/\.?|\$)\s*(\d+[.,]?\d*).*""".toRegex(RegexOption.IGNORE_CASE)
    private val yapePatternAlt2 = """(.*?)\s+te ha enviado\s+(S/\.?|\$)\s*(\d+[.,]?\d*).*""".toRegex(RegexOption.IGNORE_CASE)
    
    // Plin patterns
    private val plinPattern = """(.*?)\s+te ha plineado\s+(S/\.?|\$)\s*(\d+[.,]?\d*).*""".toRegex(RegexOption.IGNORE_CASE)
    private val plinPatternAlt = """(.*?)\s+te plineó\s+(S/\.?|\$)\s*(\d+[.,]?\d*).*""".toRegex(RegexOption.IGNORE_CASE)
    private val plinPatternAlt2 = """(.*?)\s+te (?:plinó|plineo)\s+(S/\.?|\$)\s*(\d+[.,]?\d*).*""".toRegex(RegexOption.IGNORE_CASE)
    
    // BCP patterns
    private val bcpPattern = """(.*?)\s+te (?:envió|transferió|envio|transfirio)\s+(?:un pago|dinero)\s*(?:por\s+)?(?:de\s+)?(S/\.?|\$)\s*(\d+[.,]?\d*).*""".toRegex(RegexOption.IGNORE_CASE)
    
    // Interbank patterns
    private val interbankPattern = """(.*?)\s+te (?:envió|transferió|envio|transfirio)\s+(?:un pago|dinero)\s*(?:por\s+)?(?:de\s+)?(S/\.?|\$)\s*(\d+[.,]?\d*).*""".toRegex(RegexOption.IGNORE_CASE)
    
    // BBVA patterns
    private val bbvaPattern = """(.*?)\s+te (?:envió|transferió|envio|transfirio)\s+(?:un pago|dinero)\s*(?:por\s+)?(?:de\s+)?(S/\.?|\$)\s*(\d+[.,]?\d*).*""".toRegex(RegexOption.IGNORE_CASE)
    
    // Scotiabank patterns
    private val scotiabankPattern = """(.*?)\s+te (?:envió|transferió|envio|transfirio)\s+(?:un pago|dinero)\s*(?:por\s+)?(?:de\s+)?(S/\.?|\$)\s*(\d+[.,]?\d*).*""".toRegex(RegexOption.IGNORE_CASE)
    
    // FIXED: Generic pattern más flexible para formatos desconocidos
    private val genericPattern = """(.*?)(?:\s+te\s+(?:envió|transferió|ha\s+(?:plineado|enviado|transferido))|\s+recibiste|\s+recibió)\s*(?:un pago|dinero|pago)?\s*(?:por\s+|de\s+)?(S/\.?|\$)\s*(\d+[.,]?\d*).*""".toRegex(RegexOption.IGNORE_CASE)

    /**
     * Parses a notification to extract payment details.
     * Supports multiple payment apps: Yape, Plin, BCP, Interbank, BBVA, Scotiabank.
     * 
     * @param title The notification title
     * @param text The notification body text
     * @return PaymentDetails if a payment pattern is found, null otherwise
     */
    fun parse(title: String, text: String): PaymentDetails? {
        Log.d(TAG, "Attempting to parse notification: Title='$title', Text='$text'")
        
        // STEP 1: Filter out advertisements, promotions, and reminders
        val filterResult = PaymentNotificationFilter.validatePaymentNotification(title, text)
        if (!filterResult.isValid) {
            val reason = filterResult.reason ?: "Unknown reason"
            Log.d(TAG, "Notification excluded by filter: Title='$title', Text='$text', Reason='$reason'")
            return null
        }

        // Try Yape patterns first
        if ("Yape" in title || "Yape" in text || "te envió un pago" in text || "te envió" in text) {
            val matchResult = yapePattern.find(text) ?: yapePatternAlt.find(text) ?: yapePatternAlt2.find(text)
            if (matchResult != null) {
                return try {
                    val (sender, currencySymbol, amountStr) = matchResult.destructured
                    val amount = parseAmount(amountStr) ?: return null
                    val cleanedSender = sender.trim()
                    val currency = normalizeCurrency(currencySymbol)

                    // Extract security code (only for Yape -> Yape transfers)
                    val securityCode = extractSecurityCode(text)

                    Log.i(TAG, "✅ Parsed Yape payment: Sender='$cleanedSender', Amount=$amount, Currency='$currency', SecurityCode='$securityCode'")
                    PaymentDetails(cleanedSender, amount, currency, securityCode)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing matched Yape notification", e)
                    null
                }
            }
        }

        // Try Plin patterns
        if ("Plin" in title || "Plin" in text || "plineado" in text.lowercase() || "plineó" in text.lowercase() || "plineo" in text.lowercase()) {
            val matchResult = plinPattern.find(text) ?: plinPatternAlt.find(text) ?: plinPatternAlt2.find(text)
            if (matchResult != null) {
                return try {
                    val (sender, currencySymbol, amountStr) = matchResult.destructured
                    val amount = parseAmount(amountStr) ?: return null
                    val cleanedSender = sender.trim()
                    val currency = normalizeCurrency(currencySymbol)
                    Log.i(TAG, "✅ Parsed Plin payment: Sender='$cleanedSender', Amount=$amount, Currency='$currency'")
                    PaymentDetails(cleanedSender, amount, currency)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing matched Plin notification", e)
                    null
                }
            }
        }

        // Try BCP patterns
        if ("BCP" in title || "BCP" in text || "bancadigital" in text.lowercase() || "bcp" in text.lowercase()) {
            val matchResult = bcpPattern.find(text)
            if (matchResult != null) {
                return try {
                    val (sender, currencySymbol, amountStr) = matchResult.destructured
                    val amount = parseAmount(amountStr) ?: return null
                    val cleanedSender = sender.trim()
                    val currency = normalizeCurrency(currencySymbol)
                    Log.i(TAG, "✅ Parsed BCP payment: Sender='$cleanedSender', Amount=$amount, Currency='$currency'")
                    PaymentDetails(cleanedSender, amount, currency)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing matched BCP notification", e)
                    null
                }
            }
        }

        // Try Interbank patterns
        if ("Interbank" in title || "Interbank" in text || "interbank" in text.lowercase()) {
            val matchResult = interbankPattern.find(text)
            if (matchResult != null) {
                return try {
                    val (sender, currencySymbol, amountStr) = matchResult.destructured
                    val amount = parseAmount(amountStr) ?: return null
                    val cleanedSender = sender.trim()
                    val currency = normalizeCurrency(currencySymbol)
                    Log.i(TAG, "✅ Parsed Interbank payment: Sender='$cleanedSender', Amount=$amount, Currency='$currency'")
                    PaymentDetails(cleanedSender, amount, currency)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing matched Interbank notification", e)
                    null
                }
            }
        }

        // Try BBVA patterns
        if ("BBVA" in title || "BBVA" in text || "bbvacontinental" in text.lowercase() || "bbva" in text.lowercase()) {
            val matchResult = bbvaPattern.find(text)
            if (matchResult != null) {
                return try {
                    val (sender, currencySymbol, amountStr) = matchResult.destructured
                    val amount = parseAmount(amountStr) ?: return null
                    val cleanedSender = sender.trim()
                    val currency = normalizeCurrency(currencySymbol)
                    Log.i(TAG, "✅ Parsed BBVA payment: Sender='$cleanedSender', Amount=$amount, Currency='$currency'")
                    PaymentDetails(cleanedSender, amount, currency)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing matched BBVA notification", e)
                    null
                }
            }
        }

        // Try Scotiabank patterns
        if ("Scotiabank" in title || "Scotiabank" in text || "scotiabank" in text.lowercase()) {
            val matchResult = scotiabankPattern.find(text)
            if (matchResult != null) {
                return try {
                    val (sender, currencySymbol, amountStr) = matchResult.destructured
                    val amount = parseAmount(amountStr) ?: return null
                    val cleanedSender = sender.trim()
                    val currency = normalizeCurrency(currencySymbol)
                    Log.i(TAG, "✅ Parsed Scotiabank payment: Sender='$cleanedSender', Amount=$amount, Currency='$currency'")
                    PaymentDetails(cleanedSender, amount, currency)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing matched Scotiabank notification", e)
                    null
                }
            }
        }

        // Try generic pattern as last resort (for unknown formats)
        if (containsPaymentKeywords(text)) {
            val matchResult = genericPattern.find(text)
            if (matchResult != null) {
                return try {
                    val (sender, currencySymbol, amountStr) = matchResult.destructured
                    val amount = parseAmount(amountStr) ?: return null
                    val cleanedSender = sender.trim()
                    val currency = normalizeCurrency(currencySymbol)
                    Log.i(TAG, "✅ Parsed generic payment: Sender='$cleanedSender', Amount=$amount, Currency='$currency'")
                    PaymentDetails(cleanedSender, amount, currency)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing matched generic notification", e)
                    null
                }
            }
        }

        Log.d(TAG, "⚠️ Notification did not match any known payment patterns.")
        return null
    }
    
    /**
     * Checks if text contains common payment keywords.
     */
    private fun containsPaymentKeywords(text: String): Boolean {
        val keywords = listOf(
            "pago", "pagó", "envió", "transferió", "recibiste", "recibió",
            "dinero", "monto", "S/", "$", "soles", "depósito"
        )
        val lowerText = text.lowercase()
        return keywords.any { it.lowercase() in lowerText }
    }
}
