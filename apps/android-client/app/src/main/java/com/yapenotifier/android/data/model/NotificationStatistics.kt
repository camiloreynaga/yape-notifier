package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo de respuesta para estadísticas de notificaciones
 * Corresponde a la respuesta del endpoint /api/notifications/statistics
 */
data class NotificationStatistics(
    @SerializedName("total")
    val total: Int,
    
    @SerializedName("total_amount")
    val totalAmount: Double,
    
    @SerializedName("by_source_app")
    val bySourceApp: Map<String, SourceAppStats>,
    
    @SerializedName("by_device")
    val byDevice: Map<String, DeviceStats>,
    
    @SerializedName("by_date")
    val byDate: Map<String, DateStats>,
    
    @SerializedName("by_status")
    val byStatus: Map<String, Int>,
    
    @SerializedName("duplicates")
    val duplicates: Int
)

data class SourceAppStats(
    @SerializedName("count")
    val count: Int,
    
    @SerializedName("total_amount")
    val totalAmount: Double
)

data class DeviceStats(
    @SerializedName("count")
    val count: Int,
    
    @SerializedName("total_amount")
    val totalAmount: Double
)

data class DateStats(
    @SerializedName("count")
    val count: Int,
    
    @SerializedName("total_amount")
    val totalAmount: Double
)

