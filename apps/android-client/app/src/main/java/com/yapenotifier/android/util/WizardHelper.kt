package com.yapenotifier.android.util

import android.content.Context
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.ui.PermissionsWizardActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object WizardHelper {
    /**
     * Checks if the wizard should be shown and navigates to it if needed.
     * Returns true if wizard was shown, false otherwise.
     */
    suspend fun checkAndShowWizard(context: Context): Boolean {
        val preferencesManager = PreferencesManager(context)
        val wizardCompleted = preferencesManager.wizardCompleted.first()
        
        if (!wizardCompleted) {
            val intent = Intent(context, PermissionsWizardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
            return true
        }
        return false
    }
}

