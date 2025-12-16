package com.yapenotifier.android.util

import android.util.Log
import kotlin.text.RegexOption

data class PaymentDetails(val sender: String, val amount: Double, val currency: String)

object PaymentNotificationParser {

    private const val TAG = "PaymentParser"

    // Use a raw string (triple quotes) for the regex to avoid illegal escape errors.
    private val yapePattern = """^(?:Yape! )?(.*?) te envió un pago por (S/|\$) (\d+\.?\d*).*""".toRegex(RegexOption.IGNORE_CASE)

    fun parse(title: String, text: String): PaymentDetails? {
        Log.d(TAG, "Attempting to parse notification: Title='$title', Text='$text'")

        if ("Yape" in title || "Yape" in text || "te envió un pago" in text) {
            val matchResult = yapePattern.find(text)
            if (matchResult != null) {
                try {
                    val (sender, currency, amountStr) = matchResult.destructured
                    val amount = amountStr.toDouble()
                    val cleanedSender = sender.trim()
                    Log.i(TAG, "Successfully parsed Yape payment: Sender='$cleanedSender', Amount=$amount, Currency='$currency'")
                    return PaymentDetails(cleanedSender, amount, currency)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing matched Yape notification", e)
                }
            } else {
                 Log.w(TAG, "Text matched Yape keywords but failed to match regex pattern.")
            }
        }

        Log.d(TAG, "Notification did not match any known payment patterns.")
        return null
    }
}
