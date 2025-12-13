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
        OPPO,      // OPPO, realme, OnePlus (all use ColorOS/realme UI)
        XIAOMI,    // Xiaomi, Redmi, POCO (MIUI)
        HUAWEI,    // Huawei, Honor (EMUI)
        SAMSUNG,   // Samsung (One UI)
        OTHER
    }

    /**
     * Detects the OEM type based on Build.MANUFACTURER.
     */
    fun detectOem(): OemType {
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        return when {
            manufacturer.contains("oppo") || 
            manufacturer.contains("realme") || 
            manufacturer.contains("oneplus") -> {
                Log.d(TAG, "Detected OPPO/realme/OnePlus device")
                OemType.OPPO
            }
            manufacturer.contains("xiaomi") || 
            manufacturer.contains("redmi") || 
            manufacturer.contains("poco") -> {
                Log.d(TAG, "Detected Xiaomi/Redmi/POCO device")
                OemType.XIAOMI
            }
            manufacturer.contains("huawei") || 
            manufacturer.contains("honor") -> {
                Log.d(TAG, "Detected Huawei/Honor device")
                OemType.HUAWEI
            }
            manufacturer.contains("samsung") -> {
                Log.d(TAG, "Detected Samsung device")
                OemType.SAMSUNG
            }
            else -> {
                Log.d(TAG, "Detected other OEM: $manufacturer")
                OemType.OTHER
            }
        }
    }

    /**
     * Gets OEM-specific recommendations for enabling notification access.
     */
    fun getOemRecommendations(oem: OemType): List<String> {
        return when (oem) {
            OemType.OPPO -> listOf(
                "1. Desactiva la optimización de batería para esta app",
                "2. Permite el auto-inicio (Auto-start) de la app",
                "3. Permite la ejecución en segundo plano",
                "4. Asegúrate de que 'Acceso a notificaciones' esté habilitado",
                "5. Si el servicio se desconecta, usa el botón 'Reiniciar listener'"
            )
            OemType.XIAOMI -> listOf(
                "1. Desactiva la optimización de batería (MIUI Battery Saver)",
                "2. Permite el auto-inicio en Configuración de apps",
                "3. Habilita 'Mostrar notificaciones' y 'Acceso a notificaciones'",
                "4. Desactiva 'Restricción de actividad en segundo plano'"
            )
            OemType.HUAWEI -> listOf(
                "1. Desactiva la optimización de batería",
                "2. Permite el inicio automático",
                "3. Habilita 'Permitir notificaciones' y 'Acceso a notificaciones'",
                "4. Añade la app a la lista de apps protegidas"
            )
            OemType.SAMSUNG -> listOf(
                "1. Desactiva la optimización de batería",
                "2. Permite la ejecución en segundo plano",
                "3. Habilita 'Acceso a notificaciones'"
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
            OemType.OPPO -> "OPPO/realme/OnePlus (ColorOS)"
            OemType.XIAOMI -> "Xiaomi/Redmi/POCO (MIUI)"
            OemType.HUAWEI -> "Huawei/Honor (EMUI)"
            OemType.SAMSUNG -> "Samsung (One UI)"
            OemType.OTHER -> "Otro fabricante"
        }
    }
}

