package com.yapenotifier.android.util

import android.os.Build
import android.util.Log

/**
 * Detects OEM (Original Equipment Manufacturer) to provide specific
 * recommendations and workarounds for known issues.
 */
object OemDetector {
    private const val TAG = "OemDetector"

    enum class OemType {
        MIUI,           // Xiaomi, Redmi, POCO (MIUI)
        COLOROS,        // OPPO, realme (ColorOS/realme UI)
        ONEPLUS,        // OnePlus (OxygenOS)
        ONEUI,          // Samsung (One UI)
        EMUI,           // Huawei, Honor (EMUI)
        STOCK_ANDROID,  // Stock Android (Google Pixel, etc.)
        OTHER
    }

    /**
     * Detects the OEM type based on Build.MANUFACTURER and Build.BRAND.
     */
    fun detectOem(): OemType {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        
        return when {
            // OnePlus (OxygenOS) - check first as it can be confused with OPPO
            manufacturer.contains("oneplus") || brand.contains("oneplus") -> {
                Log.d(TAG, "Detected OnePlus device (OxygenOS)")
                OemType.ONEPLUS
            }
            // OPPO/realme (ColorOS)
            manufacturer.contains("oppo") || brand.contains("oppo") ||
            manufacturer.contains("realme") || brand.contains("realme") -> {
                Log.d(TAG, "Detected OPPO/realme device (ColorOS)")
                OemType.COLOROS
            }
            // Xiaomi/Redmi/POCO (MIUI)
            manufacturer.contains("xiaomi") || brand.contains("xiaomi") ||
            manufacturer.contains("redmi") || brand.contains("redmi") ||
            manufacturer.contains("poco") || brand.contains("poco") -> {
                Log.d(TAG, "Detected Xiaomi/Redmi/POCO device (MIUI)")
                OemType.MIUI
            }
            // Samsung (One UI)
            manufacturer.contains("samsung") || brand.contains("samsung") -> {
                Log.d(TAG, "Detected Samsung device (One UI)")
                OemType.ONEUI
            }
            // Huawei/Honor (EMUI)
            manufacturer.contains("huawei") || brand.contains("huawei") ||
            manufacturer.contains("honor") || brand.contains("honor") -> {
                Log.d(TAG, "Detected Huawei/Honor device (EMUI)")
                OemType.EMUI
            }
            // Google Pixel or other stock Android
            manufacturer.contains("google") || brand.contains("google") -> {
                Log.d(TAG, "Detected Google device (Stock Android)")
                OemType.STOCK_ANDROID
            }
            else -> {
                Log.d(TAG, "Detected other OEM: manufacturer=$manufacturer, brand=$brand")
                OemType.OTHER
            }
        }
    }

    /**
     * Gets OEM-specific recommendations for enabling notification access.
     * @deprecated Use OEMGuideHelper for detailed step-by-step guides
     */
    @Deprecated("Use OEMGuideHelper.getNotificationGuide() for detailed guides")
    fun getOemRecommendations(oem: OemType): List<String> {
        return when (oem) {
            OemType.COLOROS -> listOf(
                "1. Desactiva la optimización de batería para esta app",
                "2. Permite el auto-inicio (Auto-start) de la app",
                "3. Permite la ejecución en segundo plano",
                "4. Asegúrate de que 'Acceso a notificaciones' esté habilitado"
            )
            OemType.MIUI -> listOf(
                "1. Desactiva la optimización de batería (MIUI Battery Saver)",
                "2. Permite el auto-inicio en Configuración de apps",
                "3. Habilita 'Mostrar notificaciones' y 'Acceso a notificaciones'",
                "4. Desactiva 'Restricción de actividad en segundo plano'"
            )
            OemType.EMUI -> listOf(
                "1. Desactiva la optimización de batería",
                "2. Permite el inicio automático",
                "3. Habilita 'Permitir notificaciones' y 'Acceso a notificaciones'",
                "4. Añade la app a la lista de apps protegidas"
            )
            OemType.ONEUI -> listOf(
                "1. Desactiva la optimización de batería",
                "2. Permite la ejecución en segundo plano",
                "3. Habilita 'Acceso a notificaciones'"
            )
            OemType.ONEPLUS -> listOf(
                "1. Desactiva la optimización de batería",
                "2. Permite la ejecución en segundo plano",
                "3. Habilita 'Acceso a notificaciones'"
            )
            OemType.STOCK_ANDROID -> listOf(
                "1. Desactiva la optimización de batería",
                "2. Habilita 'Acceso a notificaciones'"
            )
            OemType.OTHER -> listOf(
                "1. Desactiva la optimización de batería",
                "2. Permite la ejecución en segundo plano",
                "3. Habilita 'Acceso a notificaciones'"
            )
        }
    }

    /**
     * Gets a user-friendly OEM name.
     */
    fun getOemDisplayName(oem: OemType): String {
        return when (oem) {
            OemType.MIUI -> "Xiaomi/Redmi/POCO (MIUI)"
            OemType.COLOROS -> "OPPO/realme (ColorOS)"
            OemType.ONEPLUS -> "OnePlus (OxygenOS)"
            OemType.ONEUI -> "Samsung (One UI)"
            OemType.EMUI -> "Huawei/Honor (EMUI)"
            OemType.STOCK_ANDROID -> "Google (Stock Android)"
            OemType.OTHER -> "Otro fabricante"
        }
    }
}

