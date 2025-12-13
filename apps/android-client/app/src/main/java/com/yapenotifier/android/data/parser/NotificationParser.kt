package com.yapenotifier.android.data.parser

import android.util.Log
import com.yapenotifier.android.data.model.NotificationData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

class NotificationParser {

    private data class PaymentPattern(
        val sourceApp: String,
        val keywords: List<String>,
        val amountRegex: Regex,
        val payerRegex: Regex,
        val currencyRegex: Regex? = null
    )

    private val paymentPatterns: List<PaymentPattern>

    init {
        paymentPatterns = listOf(
            // Pattern for Yape: "JUAN PEREZ te yapeó S/ 1.00"
            PaymentPattern(
                sourceApp = "yape",
                keywords = listOf("yapeó", "yapeo"),
                amountRegex = Regex("[sS/.]\\s*([\\d,]+\\.[\\d]{2})|([\\d,]+\\.[\\d]{2})\\s*[sS/.]"),
                payerRegex = Regex("(.*?)\\s+te\\s+yapeó", RegexOption.IGNORE_CASE)
            ),
            // Pattern for Plin: "JOHN DOE te ha plineado S/ 5.50"
            PaymentPattern(
                sourceApp = "plin",
                keywords = listOf("plineado", "plin"),
                amountRegex = Regex("[sS/.]\\s*([\\d,]+\\.[\\d]{2})|([\\d,]+\\.[\\d]{2})\\s*[sS/.]"),
                payerRegex = Regex("(.*?)\\s+te\\s+ha\\s+plineado", RegexOption.IGNORE_CASE)
            ),
            // Add other general transfer patterns if needed
            PaymentPattern(
                sourceApp = "transferencia",
                keywords = listOf("transferencia de", "recibiste una transferencia"),
                amountRegex = Regex("[sS/.]\\s*([\\d,]+\\.[\\d]{2})|([\\d,]+\\.[\\d]{2})\\s*[sS/.]"),
                payerRegex = Regex("transferencia de\\s+(.*?)\\s*por", RegexOption.IGNORE_CASE)
            )
        )
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parseNotification(
        packageName: String,
        title: String?,
        body: String
    ): NotificationData? {
        val textToParse = "$title\n$body".lowercase(Locale.getDefault())

        val matchedPattern = paymentPatterns.firstOrNull { pattern ->
            pattern.keywords.any { keyword -> textToParse.contains(keyword) }
        } ?: return null

        Log.d("NotificationParser", "Matched pattern: ${matchedPattern.sourceApp}")

        val amount = extract(textToParse, matchedPattern.amountRegex)?.replace(",", "")?.toDoubleOrNull()
        val payerName = extract(textToParse, matchedPattern.payerRegex)?.trim()?.let { formatPayerName(it) }

        if (amount == null || payerName == null) {
            Log.w("NotificationParser", "Parse failed. Amount or PayerName is null. Amount: $amount, PayerName: $payerName for text: $textToParse")
            return null
        }

        return NotificationData(
            deviceId = "", // Will be set by repository
            sourceApp = matchedPattern.sourceApp,
            title = title,
            body = body,
            amount = amount,
            currency = "PEN", // Assuming PEN for now
            payerName = payerName,
            receivedAt = dateFormat.format(Date()),
            rawJson = mapOf("package_name" to packageName, "title" to (title ?: ""), "body" to body),
            status = "pending"
        )
    }

    private fun extract(text: String, regex: Regex): String? {
        // Find the last match in the string for cases where text might be complex.
        return regex.findAll(text).lastOrNull()?.groups?.get(1)?.value
    }
    
    private fun formatPayerName(name: String): String {
        return name.split(' ').joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() } }.trim()
    }
}
