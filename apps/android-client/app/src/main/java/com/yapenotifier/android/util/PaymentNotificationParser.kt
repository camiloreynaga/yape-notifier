package com.yapenotifier.android.util

import android.util.Log
import kotlin.text.RegexOption

data class PaymentDetails(val sender: String, val amount: Double, val currency: String)

object PaymentNotificationParser {

    private const val TAG = "PaymentParser"
    
    /**
     * Converts currency symbols to ISO 4217 currency codes.
     * The backend requires 3-character currency codes (e.g., "PEN", "USD").
     * 
     * @param currencySymbol The currency symbol extracted from notification (e.g., "S/", "$")
     * @return The ISO currency code (e.g., "PEN", "USD")
     */
    private fun normalizeCurrency(currencySymbol: String): String {
        return when (currencySymbol.trim().uppercase()) {
            "S/", "S/.", "SOL", "SOLES" -> "PEN"  // Peruvian Sol
            "$", "USD", "DOLAR", "DOLARES" -> "USD"  // US Dollar
            else -> {
                Log.w(TAG, "Unknown currency symbol: '$currencySymbol', defaulting to PEN")
                "PEN"  // Default to PEN for Peru
            }
        }
    }

    // Yape patterns
    private val yapePattern = """^(?:Yape! )?(.*?) te envió un pago por (S/|\$) (\d+\.?\d*).*""".toRegex(RegexOption.IGNORE_CASE)
    private val yapePatternAlt = """(.*?) te envió (?:un pago|S/|\$) (?:por )?(S/|\$) (\d+\.?\d*).*""".toRegex(RegexOption.IGNORE_CASE)
    
    // Plin patterns
    private val plinPattern = """(.*?) te ha plineado (S/|\$) (\d+\.?\d*).*""".toRegex(RegexOption.IGNORE_CASE)
    private val plinPatternAlt = """(.*?) te plineó (S/|\$) (\d+\.?\d*).*""".toRegex(RegexOption.IGNORE_CASE)
    
    // BCP patterns
    private val bcpPattern = """(.*?) te (?:envió|transferió) (?:un pago|dinero) (?:por )?(?:de )?(S/|\$) (\d+\.?\d*).*""".toRegex(RegexOption.IGNORE_CASE)
    
    // Interbank patterns
    private val interbankPattern = """(.*?) te (?:envió|transferió) (?:un pago|dinero) (?:por )?(?:de )?(S/|\$) (\d+\.?\d*).*""".toRegex(RegexOption.IGNORE_CASE)
    
    // BBVA patterns
    private val bbvaPattern = """(.*?) te (?:envió|transferió) (?:un pago|dinero) (?:por )?(?:de )?(S/|\$) (\d+\.?\d*).*""".toRegex(RegexOption.IGNORE_CASE)
    
    // Scotiabank patterns
    private val scotiabankPattern = """(.*?) te (?:envió|transferió) (?:un pago|dinero) (?:por )?(?:de )?(S/|\$) (\d+\.?\d*).*""".toRegex(RegexOption.IGNORE_CASE)
    
    // Generic pattern for common payment notifications
    private val genericPattern = """(.*?)(?: te (?:envió|transferió|ha (?:plineado|enviado))| recibiste| recibió) (?:un pago|dinero|pago) (?:por |de )?(S/|\$) (\d+\.?\d*).*""".toRegex(RegexOption.IGNORE_CASE)

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

        // Try Yape patterns first
        if ("Yape" in title || "Yape" in text || "te envió un pago" in text) {
            val matchResult = yapePattern.find(text) ?: yapePatternAlt.find(text)
            if (matchResult != null) {
                return try {
                    val (sender, currencySymbol, amountStr) = matchResult.destructured
                    val amount = amountStr.toDouble()
                    val cleanedSender = sender.trim()
                    val currency = normalizeCurrency(currencySymbol)
                    Log.i(TAG, "Successfully parsed Yape payment: Sender='$cleanedSender', Amount=$amount, Currency='$currency' (from '$currencySymbol')")
                    PaymentDetails(cleanedSender, amount, currency)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing matched Yape notification", e)
                    null
                }
            }
        }

        // Try Plin patterns
        if ("Plin" in title || "Plin" in text || "plineado" in text || "plineó" in text) {
            val matchResult = plinPattern.find(text) ?: plinPatternAlt.find(text)
            if (matchResult != null) {
                return try {
                    val (sender, currencySymbol, amountStr) = matchResult.destructured
                    val amount = amountStr.toDouble()
                    val cleanedSender = sender.trim()
                    val currency = normalizeCurrency(currencySymbol)
                    Log.i(TAG, "Successfully parsed Plin payment: Sender='$cleanedSender', Amount=$amount, Currency='$currency' (from '$currencySymbol')")
                    PaymentDetails(cleanedSender, amount, currency)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing matched Plin notification", e)
                    null
                }
            }
        }

        // Try BCP patterns
        if ("BCP" in title || "BCP" in text || "bancadigital" in text.lowercase()) {
            val matchResult = bcpPattern.find(text)
            if (matchResult != null) {
                return try {
                    val (sender, currencySymbol, amountStr) = matchResult.destructured
                    val amount = amountStr.toDouble()
                    val cleanedSender = sender.trim()
                    val currency = normalizeCurrency(currencySymbol)
                    Log.i(TAG, "Successfully parsed BCP payment: Sender='$cleanedSender', Amount=$amount, Currency='$currency' (from '$currencySymbol')")
                    PaymentDetails(cleanedSender, amount, currency)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing matched BCP notification", e)
                    null
                }
            }
        }

        // Try Interbank patterns
        if ("Interbank" in title || "Interbank" in text) {
            val matchResult = interbankPattern.find(text)
            if (matchResult != null) {
                return try {
                    val (sender, currencySymbol, amountStr) = matchResult.destructured
                    val amount = amountStr.toDouble()
                    val cleanedSender = sender.trim()
                    val currency = normalizeCurrency(currencySymbol)
                    Log.i(TAG, "Successfully parsed Interbank payment: Sender='$cleanedSender', Amount=$amount, Currency='$currency' (from '$currencySymbol')")
                    PaymentDetails(cleanedSender, amount, currency)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing matched Interbank notification", e)
                    null
                }
            }
        }

        // Try BBVA patterns
        if ("BBVA" in title || "BBVA" in text || "bbvacontinental" in text.lowercase()) {
            val matchResult = bbvaPattern.find(text)
            if (matchResult != null) {
                return try {
                    val (sender, currencySymbol, amountStr) = matchResult.destructured
                    val amount = amountStr.toDouble()
                    val cleanedSender = sender.trim()
                    val currency = normalizeCurrency(currencySymbol)
                    Log.i(TAG, "Successfully parsed BBVA payment: Sender='$cleanedSender', Amount=$amount, Currency='$currency' (from '$currencySymbol')")
                    PaymentDetails(cleanedSender, amount, currency)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing matched BBVA notification", e)
                    null
                }
            }
        }

        // Try Scotiabank patterns
        if ("Scotiabank" in title || "Scotiabank" in text) {
            val matchResult = scotiabankPattern.find(text)
            if (matchResult != null) {
                return try {
                    val (sender, currencySymbol, amountStr) = matchResult.destructured
                    val amount = amountStr.toDouble()
                    val cleanedSender = sender.trim()
                    val currency = normalizeCurrency(currencySymbol)
                    Log.i(TAG, "Successfully parsed Scotiabank payment: Sender='$cleanedSender', Amount=$amount, Currency='$currency' (from '$currencySymbol')")
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
                    val amount = amountStr.toDouble()
                    val cleanedSender = sender.trim()
                    val currency = normalizeCurrency(currencySymbol)
                    Log.i(TAG, "Successfully parsed generic payment: Sender='$cleanedSender', Amount=$amount, Currency='$currency' (from '$currencySymbol')")
                    PaymentDetails(cleanedSender, amount, currency)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing matched generic notification", e)
                    null
                }
            }
        }

        Log.d(TAG, "Notification did not match any known payment patterns.")
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
