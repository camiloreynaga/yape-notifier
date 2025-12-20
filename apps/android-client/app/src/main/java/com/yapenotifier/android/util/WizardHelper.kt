package com.yapenotifier.android.util

import android.content.Context
import android.content.Intent
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.ui.AppInstancesActivity
import com.yapenotifier.android.ui.MainActivity
import com.yapenotifier.android.ui.PermissionsWizardActivity
import com.yapenotifier.android.data.api.RetrofitClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object WizardHelper {
    /**
     * Checks if the wizard is completed.
     */
    suspend fun checkWizardCompleted(context: Context): Boolean {
        val preferencesManager = PreferencesManager(context)
        return preferencesManager.wizardCompleted.first()
    }

    /**
     * Marks the wizard as completed.
     */
    suspend fun markWizardCompleted(context: Context) {
        val preferencesManager = PreferencesManager(context)
        preferencesManager.setWizardCompleted(true)
    }

    /**
     * Checks if there are unnamed app instances.
     */
    suspend fun hasUnnamedInstances(context: Context): Boolean {
        return try {
            val preferencesManager = PreferencesManager(context)
            val deviceId = preferencesManager.deviceId.first()?.toLongOrNull()
            
            if (deviceId == null) {
                return false
            }

            val apiService = RetrofitClient.createApiService(context)
            val response = apiService.getDeviceAppInstances(deviceId)
            
            if (response.isSuccessful) {
                val instances = response.body()?.instances ?: emptyList()
                instances.any {
                    val label = it.label
                    label.isNullOrBlank()
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if the wizard should be shown and navigates to it if needed.
     * Returns true if wizard was shown, false otherwise.
     */
    suspend fun checkAndShowWizard(context: Context): Boolean {
        val wizardCompleted = checkWizardCompleted(context)
        
        if (!wizardCompleted) {
            val intent = Intent(context, PermissionsWizardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
            return true
        }
        return false
    }

    /**
     * Navigates to the appropriate next screen based on wizard and instances status.
     * Returns true if navigation was performed, false if should go to MainActivity.
     */
    suspend fun navigateToNextScreen(context: Context): Boolean {
        val wizardCompleted = checkWizardCompleted(context)
        
        if (!wizardCompleted) {
            // Wizard not completed, show wizard
            val intent = Intent(context, PermissionsWizardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
            return true
        } else {
            // Wizard completed, check for unnamed instances
            val hasUnnamed = hasUnnamedInstances(context)
            if (hasUnnamed) {
                val intent = Intent(context, AppInstancesActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
                return true
            }
        }
        return false
    }
}

