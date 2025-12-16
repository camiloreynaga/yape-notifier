package com.yapenotifier.android.data.parser

import android.content.Context
import android.util.Log
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.model.NotificationData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class NotificationParser(context: Context) {

    private val preferencesManager = PreferencesManager(context)

    private data class PaymentPattern(
        val sourceApp: String,
        val keywords: List<String>,
        val amountRegex: Regex,
        val payerRegex: Regex
    )

    private val paymentPatterns: List<PaymentPattern>

    init {
        paymentPatterns = listOf(
            // New, more robust Yape Pattern for "Confirmación de Pago"
            PaymentPattern(
                sourceApp = "yape",
                keywords = listOf("envió un pago por", "confirmación de pago"),
                // Handles amounts like S/ 1 and S/ 1.00
                amountRegex = Regex("[sS]\\/\\s*([\\d,]+(?:\\.[\\d]{1,2})?)"),
                payerRegex = Regex("yape!\\s+(.*?)\\s+te\\s+envió un pago por", RegexOption.IGNORE_CASE)
            ),
            // Original Yape Pattern: "JUAN PEREZ te yapeó S/ 1.00"
            PaymentPattern(
                sourceApp = "yape",
                keywords = listOf("yapeó", "yapeo"),
                // Corrected Regex to be more robust
                amountRegex = Regex("[sS]\\/\\s*([\\d,]+\\.[\\d]{2})"),
                payerRegex = Regex("(.*?)\\s+te\\s+yape[óo]", RegexOption.IGNORE_CASE)
            ),
            // Pattern for Plin: "JOHN DOE te ha plineado S/ 5.50"
            PaymentPattern(
                sourceApp = "plin",
                keywords = listOf("plineado", "plin"),
                // Corrected Regex to be more robust
                amountRegex = Regex("[sS]\\/\\s*([\\d,]+\\.[\\d]{2})"),
                payerRegex = Regex("(.*?)\\s+te\\s+ha\\s+plineado", RegexOption.IGNORE_CASE)
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

        // Find the first pattern that matches the notification content
        val matchedPattern = paymentPatterns.firstOrNull { pattern ->
            pattern.keywords.any { keyword -> textToParse.contains(keyword) }
        } ?: return null // If no pattern matches, ignore the notification

        Log.d("NotificationParser", "Matched pattern for source: ${matchedPattern.sourceApp}")

        val amount = extract(textToParse, matchedPattern.amountRegex)?.replace(",", "")?.toDoubleOrNull()
        val payerName = extract(textToParse, matchedPattern.payerRegex)?.trim()?.let { formatPayerName(it) }

        // If we can't extract the core info, mark as failed and return null
        if (amount == null || payerName == null) {
            Log.w("NotificationParser", "Parse failed. Amount or PayerName is null. Amount: $amount, PayerName: $payerName for text: $textToParse")
            return null
        }

        val deviceId = runBlocking {
            preferencesManager.deviceUuid.first() ?: ""
        }

        return NotificationData(
            deviceId = deviceId,
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
        return regex.find(text)?.groups?.get(1)?.value
    }
    
    private fun formatPayerName(name: String): String {
        // Capitalize each part of the name for consistent formatting
        return name.split(' ').joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() } }.trim()
    }
}
