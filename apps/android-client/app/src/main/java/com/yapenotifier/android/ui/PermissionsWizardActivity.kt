package com.yapenotifier.android.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.databinding.ActivityPermissionsWizardBinding
import com.yapenotifier.android.ui.adapter.PermissionsWizardAdapter
import com.yapenotifier.android.ui.fragment.BatteryOptimizationFragment
import com.yapenotifier.android.ui.fragment.MonitoredAppsFragment
import com.yapenotifier.android.ui.fragment.NotificationPermissionFragment
import com.yapenotifier.android.util.DeviceHealthWorkerHelper
import com.yapenotifier.android.util.WizardHelper
import kotlinx.coroutines.launch

class PermissionsWizardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPermissionsWizardBinding
    private lateinit var adapter: PermissionsWizardAdapter
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionsWizardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(this)
        
        setupViewPager()
        setupClickListeners()
    }

    private fun setupViewPager() {
        adapter = PermissionsWizardAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false // Disable swiping, use buttons only

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = adapter.getFragmentTitle(position)
        }.attach()

        // Update button visibility based on position
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateButtonVisibility(position)
            }
        })

        updateButtonVisibility(0)
    }

    private fun updateButtonVisibility(position: Int) {
        binding.btnPrevious.visibility = if (position > 0) View.VISIBLE else View.GONE
        binding.btnNext.visibility = if (position < adapter.itemCount - 1) View.VISIBLE else View.GONE
        binding.btnFinish.visibility = if (position == adapter.itemCount - 1) View.VISIBLE else View.GONE
    }

    private fun setupClickListeners() {
        binding.btnPrevious.setOnClickListener {
            if (binding.viewPager.currentItem > 0) {
                binding.viewPager.currentItem = binding.viewPager.currentItem - 1
            }
        }

        binding.btnNext.setOnClickListener {
            if (canProceedToNextStep(binding.viewPager.currentItem)) {
                if (binding.viewPager.currentItem < adapter.itemCount - 1) {
                    binding.viewPager.currentItem = binding.viewPager.currentItem + 1
                }
            }
        }

        binding.btnFinish.setOnClickListener {
            finishWizard()
        }

        binding.btnSkip.setOnClickListener {
            finishWizard()
        }
    }

    private fun canProceedToNextStep(currentPosition: Int): Boolean {
        return when (currentPosition) {
            0 -> {
                val notificationFragment = supportFragmentManager.findFragmentByTag("f$currentPosition") as? NotificationPermissionFragment
                notificationFragment?.isPermissionGranted() ?: false
            }
            1 -> {
                val batteryFragment = supportFragmentManager.findFragmentByTag("f$currentPosition") as? BatteryOptimizationFragment
                batteryFragment?.isBatteryOptimizationDisabled() ?: false
            }
            2 -> {
                val appsFragment = supportFragmentManager.findFragmentByTag("f$currentPosition") as? MonitoredAppsFragment
                appsFragment?.getSelectedPackages()?.isNotEmpty() ?: false
            }
            else -> true
        }
    }

    private fun finishWizard() {
        lifecycleScope.launch {
            // Mark wizard as completed
            WizardHelper.markWizardCompleted(this@PermissionsWizardActivity)
            
            // Start device health worker
            DeviceHealthWorkerHelper.scheduleDeviceHealthWorker(this@PermissionsWizardActivity)
            
            // Check if there are unnamed instances
            val hasUnnamed = WizardHelper.hasUnnamedInstances(this@PermissionsWizardActivity)
            
            if (hasUnnamed) {
                // Navigate to AppInstancesActivity
                val intent = Intent(this@PermissionsWizardActivity, AppInstancesActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                // Navigate to MainActivity
                navigateToMain()
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Refresh current fragment when returning from settings
        val currentFragment = supportFragmentManager.findFragmentByTag("f${binding.viewPager.currentItem}")
        when (currentFragment) {
            is NotificationPermissionFragment -> {
                // Fragment will check onResume
            }
            is BatteryOptimizationFragment -> {
                // Fragment will check onResume
            }
        }
    }
}
