package com.yapenotifier.android.util

import android.util.Log

/**
 * Utility object to map Android package names to standardized source_app identifiers
 * that match the backend API validation rules.
 * 
 * The backend expects source_app to be one of:
 * "yape", "plin", "bcp", "interbank", "bbva", "scotiabank"
 */
object SourceAppMapper {
    
    private const val TAG = "SourceAppMapper"
    
    /**
     * Maps a package name to its corresponding source_app identifier.
     * 
     * @param packageName The Android package name (e.g., "com.yape.android")
     * @return The standardized source_app identifier, or null if no mapping exists
     */
    fun mapPackageToSourceApp(packageName: String?): String? {
        if (packageName.isNullOrBlank()) {
            Log.w(TAG, "Package name is null or blank")
            return null
        }
        
        // Normalize package name to lowercase for case-insensitive matching
        val normalizedPackage = packageName.lowercase().trim()
        
        // Exclude our own app - it's not a payment app, it's the notification listener
        if (normalizedPackage == "com.yapenotifier.android") {
            Log.d(TAG, "Package is our own app (com.yapenotifier.android), cannot map to source_app")
            return null
        }
        
        return when {
            // Yape variants
            normalizedPackage.contains("yape") -> {
                when {
                    normalizedPackage.contains("bcp") || normalizedPackage.contains("innovacxion") -> {
                        // BCP's Yape app - could be "yape" or "bcp", defaulting to "yape" as it's Yape functionality
                        "yape"
                    }
                    else -> "yape"
                }
            }
            
            // Plin
            normalizedPackage.contains("plin") -> "plin"
            
            // BCP (excluding Yape variants already handled)
            normalizedPackage.contains("bcp") && !normalizedPackage.contains("yape") -> "bcp"
            normalizedPackage.contains("bancadigital") -> "bcp"
            
            // Interbank
            normalizedPackage.contains("interbank") -> "interbank"
            
            // BBVA
            normalizedPackage.contains("bbva") -> "bbva"
            normalizedPackage.contains("bbvacontinental") -> "bbva"
            
            // Scotiabank
            normalizedPackage.contains("scotiabank") -> "scotiabank"
            
            else -> {
                Log.w(TAG, "No mapping found for package: $packageName")
                null
            }
        }
    }
    
    /**
     * Maps a package name to source_app with a fallback value.
     * 
     * @param packageName The Android package name
     * @param fallback The fallback value if no mapping exists (default: "yape")
     * @return The mapped source_app or fallback
     */
    fun mapPackageToSourceAppWithFallback(packageName: String?, fallback: String = "yape"): String {
        return mapPackageToSourceApp(packageName) ?: fallback
    }
    
    /**
     * Checks if a package name is a known payment app.
     */
    fun isKnownPaymentApp(packageName: String?): Boolean {
        return mapPackageToSourceApp(packageName) != null
    }
    
    /**
     * Infers source_app from notification content (title and body).
     * This is useful when the package_name is our own app (com.yapenotifier.android)
     * or when the package cannot be mapped directly.
     * 
     * @param title The notification title
     * @param body The notification body text
     * @return The inferred source_app identifier, or null if cannot be determined
     */
    fun inferSourceAppFromContent(title: String?, body: String?): String? {
        val titleLower = title?.lowercase() ?: ""
        val bodyLower = body?.lowercase() ?: ""
        val combined = "$titleLower $bodyLower"
        
        return when {
            // Plin indicators
            "plin" in titleLower || "plineado" in bodyLower || "plineó" in bodyLower -> {
                Log.d(TAG, "Inferred source_app: plin from content")
                "plin"
            }
            
            // Yape indicators
            "yape" in titleLower || "yape" in bodyLower || "te envió un pago" in bodyLower -> {
                Log.d(TAG, "Inferred source_app: yape from content")
                "yape"
            }
            
            // BCP indicators
            "bcp" in combined || "bancadigital" in combined -> {
                Log.d(TAG, "Inferred source_app: bcp from content")
                "bcp"
            }
            
            // Interbank indicators
            "interbank" in combined -> {
                Log.d(TAG, "Inferred source_app: interbank from content")
                "interbank"
            }
            
            // BBVA indicators
            "bbva" in combined || "bbvacontinental" in combined -> {
                Log.d(TAG, "Inferred source_app: bbva from content")
                "bbva"
            }
            
            // Scotiabank indicators
            "scotiabank" in combined -> {
                Log.d(TAG, "Inferred source_app: scotiabank from content")
                "scotiabank"
            }
            
            else -> {
                Log.w(TAG, "Could not infer source_app from content. Title: '$title', Body: '$body'")
                null
            }
        }
    }
}

