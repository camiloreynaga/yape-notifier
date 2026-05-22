package com.yapenotifier.android.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.yapenotifier.android.databinding.FragmentBatteryOptimizationBinding
import com.yapenotifier.android.util.OemDetector
import com.yapenotifier.android.util.OEMGuideHelper

class BatteryOptimizationFragment : Fragment() {
    private var _binding: FragmentBatteryOptimizationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBatteryOptimizationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        checkBatteryOptimizationStatus()
        setupClickListeners()
    }

    private fun setupUI() {
        val guide = OEMGuideHelper.getBatteryOptimizationGuide(requireContext())
        val oemName = OemDetector.getOemDisplayName(guide.oem)

        binding.tvOemInfo.text = "Dispositivo detectado: $oemName"
        binding.tvGuideTitle.text = guide.title
        
        // Display step-by-step guide
        val guideText = buildString {
            guide.steps.forEach { step ->
                append("${step.number}. ${step.title}\n")
                append("   ${step.description}\n\n")
            }
        }
        binding.tvRecommendations.text = guideText.trim()
    }

    private fun checkBatteryOptimizationStatus() {
        val powerManager = requireContext().getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
        val isIgnoringOptimizations = powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)
        updateUI(isIgnoringOptimizations)
    }

    private fun updateUI(isIgnoringOptimizations: Boolean) {
        if (isIgnoringOptimizations) {
            binding.tvStatus.text = "✅ Optimización de Batería: Desactivada"
            binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            binding.btnOpenSettings.text = "Configuración Correcta"
            binding.btnOpenSettings.isEnabled = false
        } else {
            binding.tvStatus.text = "❌ Optimización de Batería: Activada (puede detener el servicio)"
            binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#F44336"))
            binding.btnOpenSettings.text = "Abrir Configuración"
            binding.btnOpenSettings.isEnabled = true
        }
    }

    private fun setupClickListeners() {
        binding.btnOpenSettings.setOnClickListener {
            openBatteryOptimizationSettings()
        }
    }

    private fun openBatteryOptimizationSettings() {
        val guide = OEMGuideHelper.getBatteryOptimizationGuide(requireContext())
        // Try to open the first step's action intent if available
        val firstStepWithAction = guide.steps.firstOrNull { it.actionIntent != null }
        
        if (firstStepWithAction != null) {
            OEMGuideHelper.openGuideStep(requireContext(), firstStepWithAction)
        } else {
            // Fallback to standard battery optimization settings
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${requireContext().packageName}")
                }
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback to battery settings
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)
                } catch (e2: Exception) {
                    // Last resort: general settings
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    startActivity(intent)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkBatteryOptimizationStatus()
    }

    fun isBatteryOptimizationDisabled(): Boolean {
        val powerManager = requireContext().getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

