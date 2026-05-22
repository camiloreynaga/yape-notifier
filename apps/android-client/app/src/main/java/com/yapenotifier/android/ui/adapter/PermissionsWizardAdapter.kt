package com.yapenotifier.android.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.yapenotifier.android.ui.fragment.AppInstancesFragment
import com.yapenotifier.android.ui.fragment.BatteryOptimizationFragment
import com.yapenotifier.android.ui.fragment.MonitoredAppsFragment
import com.yapenotifier.android.ui.fragment.NotificationPermissionFragment

class PermissionsWizardAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    
    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> NotificationPermissionFragment()
            1 -> BatteryOptimizationFragment()
            2 -> MonitoredAppsFragment()
            3 -> AppInstancesFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }

    fun getFragmentTitle(position: Int): String {
        return when (position) {
            0 -> "Permisos de Notificaciones"
            1 -> "Optimización de Batería"
            2 -> "Apps a Monitorear"
            3 -> "Instancias Duales"
            else -> ""
        }
    }
}

