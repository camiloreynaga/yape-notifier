package com.yapenotifier.android.data.parser

import com.yapenotifier.android.data.model.NotificationData
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class NotificationParser {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parseNotification(
        packageName: String,
        title: String?,
        body: String
    ): NotificationData? {
        // --- TEST BYPASS ---
        // If this is a test notification from our own app, create a mock object and return immediately.
        if (packageName == "com.yapenotifier.android") {
            return NotificationData(
                deviceId = "", // Will be set by repository
                sourceApp = "yape-test",
                title = title,
                body = body,
                amount = 1.00,
                currency = "PEN",
                payerName = "Juan Pérez (Prueba)",
                receivedAt = dateFormat.format(Date()),
                rawJson = mapOf(
                    "package_name" to packageName,
                    "title" to (title ?: ""),
                    "body" to body
                ),
                status = "pending"
            )
        }
        
        // --- PRODUCTION LOGIC ---
        val sourceApp = mapPackageToSourceApp(packageName) ?: return null

        if (!isPaymentNotification(body)) {
            return null
        }

        val amount = extractAmount(body)
        val payerName = extractPayerName(body)
        val currency = extractCurrency(body) ?: "PEN"

        // Do not proceed if critical information is missing
        if (amount == null || payerName == null) {
            return null
        }

        return NotificationData(
            deviceId = "", // Will be set by repository
            sourceApp = sourceApp,
            title = title,
            body = body,
            amount = amount,
            currency = currency,
            payerName = payerName,
            receivedAt = dateFormat.format(Date()),
            rawJson = mapOf(
                "package_name" to packageName,
                "title" to (title ?: ""),
                "body" to body
            ),
            status = "pending"
        )
    }

    private fun mapPackageToSourceApp(packageName: String): String? {
        return when {
            packageName.contains("yape", ignoreCase = true) -> "yape"
            packageName.contains("plin", ignoreCase = true) -> "plin"
            packageName.contains("bcp", ignoreCase = true) -> "bcp"
            packageName.contains("interbank", ignoreCase = true) -> "interbank"
            packageName.contains("bbva", ignoreCase = true) -> "bbva"
            packageName.contains("scotiabank", ignoreCase = true) -> "scotiabank"
            else -> null
        }
    }

    private fun isPaymentNotification(text: String): Boolean {
        val paymentKeywords = listOf(
            "recibiste", "recibió", "recibido",
            "depósito", "deposito",
            "transferencia",
            "pago recibido",
            "ingresó", "ingreso",
            "s/", "soles", "pen"
        )
        
        val lowerText = text.lowercase()
        return paymentKeywords.any { keyword -> lowerText.contains(keyword) }
    }

    private fun extractAmount(text: String): Double? {
        val patterns = listOf(
            Pattern.compile("s/\\s*([\\d,]+(?:\\.[\\d]{1,2})?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([\\d,]+(?:\\.[\\d]{1,2})?)\\s*soles?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b([\\d,]+(?:\\.[\\d]{1,2})?)\\b")
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "") ?: continue
                return try {
                    amountStr.toDouble()
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }

        return null
    }

    private fun extractPayerName(text: String): String? {
        val patterns = listOf(
            Pattern.compile("de\\s+([A-ZÁÉÍÓÚÑ][a-záéíóúñ]+(?:\\s+[A-ZÁÉÍÓÚÑ][a-záéíóúñ]+)*)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([A-ZÁÉÍÓÚÑ][a-záéíóúñ]+(?:\\s+[A-ZÁÉÍÓÚÑ][a-záéíóúñ]+)*)\\s+te", Pattern.CASE_INSENSITIVE),
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val name = matcher.group(1)?.trim()
                if (name != null && name.length > 2 && name.length < 50) {
                    return name
                }
            }
        }

        return null
    }

    private fun extractCurrency(text: String): String? {
        val lowerText = text.lowercase()
        return when {
            lowerText.contains("s/") || lowerText.contains("soles") || lowerText.contains("pen") -> "PEN"
            lowerText.contains("usd") || lowerText.contains("dólares") || lowerText.contains("dolares") -> "USD"
            else -> null
        }
    }
}
