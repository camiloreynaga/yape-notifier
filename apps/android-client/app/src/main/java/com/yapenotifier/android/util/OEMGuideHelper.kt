package com.yapenotifier.android.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Provides OEM-specific step-by-step guides for permissions and settings.
 */
object OEMGuideHelper {

    data class GuideStep(
        val number: Int,
        val title: String,
        val description: String,
        val actionIntent: Intent? = null
    )

    data class OEMGuide(
        val oem: OemDetector.OemType,
        val title: String,
        val steps: List<GuideStep>
    )

    /**
     * Gets detailed step-by-step guide for notification permission based on OEM.
     */
    fun getNotificationGuide(context: Context): OEMGuide {
        val oem = OemDetector.detectOem()
        return when (oem) {
            OemDetector.OemType.MIUI -> getMIUINotificationGuide(context)
            OemDetector.OemType.COLOROS -> getColorOSNotificationGuide(context)
            OemDetector.OemType.ONEUI -> getOneUINotificationGuide(context)
            OemDetector.OemType.ONEPLUS -> getOnePlusNotificationGuide(context)
            OemDetector.OemType.EMUI -> getEMUINotificationGuide(context)
            OemDetector.OemType.STOCK_ANDROID -> getStockAndroidNotificationGuide(context)
            OemDetector.OemType.OTHER -> getGenericNotificationGuide(context)
        }
    }

    /**
     * Gets detailed step-by-step guide for battery optimization based on OEM.
     */
    fun getBatteryOptimizationGuide(context: Context): OEMGuide {
        val oem = OemDetector.detectOem()
        return when (oem) {
            OemDetector.OemType.MIUI -> getMIUIBatteryGuide(context)
            OemDetector.OemType.COLOROS -> getColorOSBatteryGuide(context)
            OemDetector.OemType.ONEUI -> getOneUIBatteryGuide(context)
            OemDetector.OemType.ONEPLUS -> getOnePlusBatteryGuide(context)
            OemDetector.OemType.EMUI -> getEMUIBatteryGuide(context)
            OemDetector.OemType.STOCK_ANDROID -> getStockAndroidBatteryGuide(context)
            OemDetector.OemType.OTHER -> getGenericBatteryGuide(context)
        }
    }

    // --- MIUI Guides ---
    private fun getMIUINotificationGuide(context: Context): OEMGuide {
        val packageName = context.packageName
        return OEMGuide(
            oem = OemDetector.OemType.MIUI,
            title = "Configurar Permisos en MIUI",
            steps = listOf(
                GuideStep(
                    1,
                    "Abrir Configuración de Aplicaciones",
                    "Ve a Configuración > Aplicaciones > Administrar aplicaciones",
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                ),
                GuideStep(
                    2,
                    "Buscar Yape Notifier",
                    "Busca y selecciona 'Yape Notifier' en la lista de aplicaciones"
                ),
                GuideStep(
                    3,
                    "Habilitar Notificaciones",
                    "Toca en 'Notificaciones' y activa 'Mostrar notificaciones'"
                ),
                GuideStep(
                    4,
                    "Habilitar Acceso a Notificaciones",
                    "Toca en 'Acceso a notificaciones' y activa el acceso para Yape Notifier"
                )
            )
        )
    }

    private fun getMIUIBatteryGuide(context: Context): OEMGuide {
        val packageName = context.packageName
        return OEMGuide(
            oem = OemDetector.OemType.MIUI,
            title = "Desactivar Optimización de Batería en MIUI",
            steps = listOf(
                GuideStep(
                    1,
                    "Abrir Configuración de Batería",
                    "Ve a Configuración > Batería y rendimiento",
                    Intent().apply {
                        setClassName("com.miui.securitycenter", "com.miui.powercenter.PowerMainActivity")
                    }
                ),
                GuideStep(
                    2,
                    "Abrir Ahorro de Batería",
                    "Toca en 'Ahorro de batería' o 'Battery saver'"
                ),
                GuideStep(
                    3,
                    "Buscar Yape Notifier",
                    "Busca 'Yape Notifier' en la lista de aplicaciones"
                ),
                GuideStep(
                    4,
                    "Seleccionar Sin Restricciones",
                    "Selecciona 'Sin restricciones' o 'No restrictions' para Yape Notifier"
                )
            )
        )
    }

    // --- ColorOS Guides ---
    private fun getColorOSNotificationGuide(context: Context): OEMGuide {
        val packageName = context.packageName
        return OEMGuide(
            oem = OemDetector.OemType.COLOROS,
            title = "Configurar Permisos en ColorOS",
            steps = listOf(
                GuideStep(
                    1,
                    "Abrir Configuración de Aplicaciones",
                    "Ve a Configuración > Aplicaciones > Administrar aplicaciones",
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                ),
                GuideStep(
                    2,
                    "Buscar Yape Notifier",
                    "Busca y selecciona 'Yape Notifier' en la lista"
                ),
                GuideStep(
                    3,
                    "Habilitar Notificaciones",
                    "Toca en 'Notificaciones' y activa todas las opciones"
                ),
                GuideStep(
                    4,
                    "Habilitar Acceso a Notificaciones",
                    "Ve a Configuración > Aplicaciones especiales > Acceso a notificaciones y activa Yape Notifier"
                )
            )
        )
    }

    private fun getColorOSBatteryGuide(context: Context): OEMGuide {
        val packageName = context.packageName
        return OEMGuide(
            oem = OemDetector.OemType.COLOROS,
            title = "Desactivar Optimización de Batería en ColorOS",
            steps = listOf(
                GuideStep(
                    1,
                    "Abrir Configuración de Batería",
                    "Ve a Configuración > Batería",
                    Intent().apply {
                        setClassName("com.coloros.oppoguardelf", "com.coloros.powermanager.PowerConsumptionActivity")
                    }
                ),
                GuideStep(
                    2,
                    "Abrir Optimización de Batería",
                    "Toca en 'Optimización de batería' o 'Battery optimization'"
                ),
                GuideStep(
                    3,
                    "Buscar Yape Notifier",
                    "Busca 'Yape Notifier' en la lista"
                ),
                GuideStep(
                    4,
                    "Seleccionar Permitir",
                    "Selecciona 'Permitir' o 'Allow' para Yape Notifier"
                )
            )
        )
    }

    // --- One UI Guides ---
    private fun getOneUINotificationGuide(context: Context): OEMGuide {
        val packageName = context.packageName
        return OEMGuide(
            oem = OemDetector.OemType.ONEUI,
            title = "Configurar Permisos en One UI",
            steps = listOf(
                GuideStep(
                    1,
                    "Abrir Configuración de Aplicaciones",
                    "Ve a Configuración > Aplicaciones",
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                ),
                GuideStep(
                    2,
                    "Buscar Yape Notifier",
                    "Busca y selecciona 'Yape Notifier'"
                ),
                GuideStep(
                    3,
                    "Habilitar Notificaciones",
                    "Toca en 'Notificaciones' y activa todas las opciones"
                ),
                GuideStep(
                    4,
                    "Habilitar Acceso a Notificaciones",
                    "Ve a Configuración > Aplicaciones > Acceso especial > Acceso a notificaciones y activa Yape Notifier"
                )
            )
        )
    }

    private fun getOneUIBatteryGuide(context: Context): OEMGuide {
        val packageName = context.packageName
        return OEMGuide(
            oem = OemDetector.OemType.ONEUI,
            title = "Desactivar Optimización de Batería en One UI",
            steps = listOf(
                GuideStep(
                    1,
                    "Abrir Configuración de Batería",
                    "Ve a Configuración > Cuidado del dispositivo > Batería",
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                ),
                GuideStep(
                    2,
                    "Abrir Optimización de Batería",
                    "Toca en 'Optimización de batería' o 'Battery optimization'"
                ),
                GuideStep(
                    3,
                    "Buscar Yape Notifier",
                    "Busca 'Yape Notifier' en la lista"
                ),
                GuideStep(
                    4,
                    "Seleccionar No Optimizar",
                    "Selecciona 'No optimizar' o 'Don't optimize' para Yape Notifier"
                )
            )
        )
    }

    // --- OnePlus Guides ---
    private fun getOnePlusNotificationGuide(context: Context): OEMGuide {
        val packageName = context.packageName
        return OEMGuide(
            oem = OemDetector.OemType.ONEPLUS,
            title = "Configurar Permisos en OxygenOS",
            steps = listOf(
                GuideStep(
                    1,
                    "Abrir Configuración de Aplicaciones",
                    "Ve a Configuración > Aplicaciones",
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                ),
                GuideStep(
                    2,
                    "Buscar Yape Notifier",
                    "Busca y selecciona 'Yape Notifier'"
                ),
                GuideStep(
                    3,
                    "Habilitar Notificaciones",
                    "Toca en 'Notificaciones' y activa todas las opciones"
                ),
                GuideStep(
                    4,
                    "Habilitar Acceso a Notificaciones",
                    "Ve a Configuración > Aplicaciones > Acceso especial > Acceso a notificaciones y activa Yape Notifier"
                )
            )
        )
    }

    private fun getOnePlusBatteryGuide(context: Context): OEMGuide {
        val packageName = context.packageName
        return OEMGuide(
            oem = OemDetector.OemType.ONEPLUS,
            title = "Desactivar Optimización de Batería en OxygenOS",
            steps = listOf(
                GuideStep(
                    1,
                    "Abrir Configuración de Batería",
                    "Ve a Configuración > Batería",
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                ),
                GuideStep(
                    2,
                    "Abrir Optimización de Batería",
                    "Toca en 'Optimización de batería'"
                ),
                GuideStep(
                    3,
                    "Buscar Yape Notifier",
                    "Busca 'Yape Notifier' en la lista"
                ),
                GuideStep(
                    4,
                    "Seleccionar No Optimizar",
                    "Selecciona 'No optimizar' para Yape Notifier"
                )
            )
        )
    }

    // --- EMUI Guides ---
    private fun getEMUINotificationGuide(context: Context): OEMGuide {
        val packageName = context.packageName
        return OEMGuide(
            oem = OemDetector.OemType.EMUI,
            title = "Configurar Permisos en EMUI",
            steps = listOf(
                GuideStep(
                    1,
                    "Abrir Configuración de Aplicaciones",
                    "Ve a Configuración > Aplicaciones",
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                ),
                GuideStep(
                    2,
                    "Buscar Yape Notifier",
                    "Busca y selecciona 'Yape Notifier'"
                ),
                GuideStep(
                    3,
                    "Habilitar Notificaciones",
                    "Toca en 'Notificaciones' y activa todas las opciones"
                ),
                GuideStep(
                    4,
                    "Habilitar Acceso a Notificaciones",
                    "Ve a Configuración > Aplicaciones > Acceso especial > Acceso a notificaciones y activa Yape Notifier"
                )
            )
        )
    }

    private fun getEMUIBatteryGuide(context: Context): OEMGuide {
        val packageName = context.packageName
        return OEMGuide(
            oem = OemDetector.OemType.EMUI,
            title = "Desactivar Optimización de Batería en EMUI",
            steps = listOf(
                GuideStep(
                    1,
                    "Abrir Configuración de Batería",
                    "Ve a Configuración > Batería",
                    Intent().apply {
                        setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.power.ui.HwPowerManagerActivity")
                    }
                ),
                GuideStep(
                    2,
                    "Abrir Protección de Aplicaciones",
                    "Toca en 'Protección de aplicaciones' o 'App launch'"
                ),
                GuideStep(
                    3,
                    "Buscar Yape Notifier",
                    "Busca 'Yape Notifier' en la lista"
                ),
                GuideStep(
                    4,
                    "Habilitar Inicio Automático",
                    "Activa 'Inicio automático' y 'Ejecutar en segundo plano' para Yape Notifier"
                )
            )
        )
    }

    // --- Stock Android Guides ---
    private fun getStockAndroidNotificationGuide(context: Context): OEMGuide {
        val packageName = context.packageName
        return OEMGuide(
            oem = OemDetector.OemType.STOCK_ANDROID,
            title = "Configurar Permisos en Android",
            steps = listOf(
                GuideStep(
                    1,
                    "Abrir Configuración de Aplicaciones",
                    "Ve a Configuración > Aplicaciones",
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                ),
                GuideStep(
                    2,
                    "Buscar Yape Notifier",
                    "Busca y selecciona 'Yape Notifier'"
                ),
                GuideStep(
                    3,
                    "Habilitar Notificaciones",
                    "Toca en 'Notificaciones' y activa todas las opciones"
                ),
                GuideStep(
                    4,
                    "Habilitar Acceso a Notificaciones",
                    "Ve a Configuración > Aplicaciones > Acceso especial > Acceso a notificaciones y activa Yape Notifier"
                )
            )
        )
    }

    private fun getStockAndroidBatteryGuide(context: Context): OEMGuide {
        val packageName = context.packageName
        return OEMGuide(
            oem = OemDetector.OemType.STOCK_ANDROID,
            title = "Desactivar Optimización de Batería",
            steps = listOf(
                GuideStep(
                    1,
                    "Abrir Configuración de Batería",
                    "Ve a Configuración > Batería",
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                ),
                GuideStep(
                    2,
                    "Buscar Yape Notifier",
                    "Busca 'Yape Notifier' en la lista de aplicaciones"
                ),
                GuideStep(
                    3,
                    "Seleccionar No Optimizar",
                    "Toca en 'Yape Notifier' y selecciona 'No optimizar'"
                )
            )
        )
    }

    // --- Generic Guides ---
    private fun getGenericNotificationGuide(context: Context): OEMGuide {
        val packageName = context.packageName
        return OEMGuide(
            oem = OemDetector.OemType.OTHER,
            title = "Configurar Permisos",
            steps = listOf(
                GuideStep(
                    1,
                    "Abrir Configuración de Aplicaciones",
                    "Ve a Configuración > Aplicaciones",
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                ),
                GuideStep(
                    2,
                    "Buscar Yape Notifier",
                    "Busca y selecciona 'Yape Notifier'"
                ),
                GuideStep(
                    3,
                    "Habilitar Notificaciones",
                    "Toca en 'Notificaciones' y activa todas las opciones"
                ),
                GuideStep(
                    4,
                    "Habilitar Acceso a Notificaciones",
                    "Ve a Configuración > Aplicaciones > Acceso especial > Acceso a notificaciones y activa Yape Notifier"
                )
            )
        )
    }

    private fun getGenericBatteryGuide(context: Context): OEMGuide {
        val packageName = context.packageName
        return OEMGuide(
            oem = OemDetector.OemType.OTHER,
            title = "Desactivar Optimización de Batería",
            steps = listOf(
                GuideStep(
                    1,
                    "Abrir Configuración de Batería",
                    "Ve a Configuración > Batería",
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                ),
                GuideStep(
                    2,
                    "Buscar Yape Notifier",
                    "Busca 'Yape Notifier' en la lista"
                ),
                GuideStep(
                    3,
                    "Seleccionar No Optimizar",
                    "Selecciona 'No optimizar' para Yape Notifier"
                )
            )
        )
    }

    /**
     * Opens the appropriate settings screen based on the guide step's action intent.
     */
    fun openGuideStep(context: Context, step: GuideStep) {
        step.actionIntent?.let { intent ->
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to generic settings
                try {
                    val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
                    context.startActivity(fallbackIntent)
                } catch (e2: Exception) {
                    // Last resort: do nothing
                }
            }
        }
    }
}



