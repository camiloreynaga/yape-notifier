package com.yapenotifier.android.util

import android.os.Build

/**
 * Best-effort OEM detection using MANUFACTURER + BRAND + MODEL.
 * Used to tailor user-facing recovery instructions for OEMs with
 * aggressive battery savers that kill NotificationListenerService
 * silently (MIUI/Xiaomi/Redmi/POCO especially).
 */
object OemDetection {

    enum class Oem { XIAOMI, HUAWEI, OPPO, VIVO, ONEPLUS, SAMSUNG, OTHER }

    fun current(): Oem {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val model = Build.MODEL.lowercase()

        return when {
            "xiaomi" in manufacturer ||
                brand in listOf("xiaomi", "redmi", "poco") ||
                "redmi" in model || "poco" in model -> Oem.XIAOMI

            "huawei" in manufacturer || "honor" in brand -> Oem.HUAWEI
            "oppo" in manufacturer || "realme" in brand -> Oem.OPPO
            "vivo" in manufacturer -> Oem.VIVO
            "oneplus" in manufacturer -> Oem.ONEPLUS
            "samsung" in manufacturer -> Oem.SAMSUNG
            else -> Oem.OTHER
        }
    }

    fun hasAggressiveBatterySaver(): Boolean = current() in setOf(
        Oem.XIAOMI, Oem.HUAWEI, Oem.OPPO, Oem.VIVO,
    )

    /** Hint adicional para mostrar al usuario después de los pasos genéricos de toggle. */
    fun extraHint(): String? = when (current()) {
        Oem.XIAOMI -> "En MIUI también activa Autoinicio y Sin restricción de batería para NotiCentral."
        Oem.HUAWEI -> "En EMUI también permite Autoinicio para NotiCentral en Ajustes."
        Oem.OPPO, Oem.VIVO -> "Permite Autoinicio y desactiva la optimización de batería para NotiCentral."
        else -> null
    }
}
