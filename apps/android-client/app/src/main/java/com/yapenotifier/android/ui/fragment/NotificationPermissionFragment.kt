package com.yapenotifier.android.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.yapenotifier.android.databinding.FragmentNotificationPermissionBinding
import com.yapenotifier.android.util.NotificationAccessChecker
import com.yapenotifier.android.util.OemDetector
import com.yapenotifier.android.util.OEMGuideHelper

class NotificationPermissionFragment : Fragment() {
    private var _binding: FragmentNotificationPermissionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationPermissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        checkPermissionStatus()
        setupClickListeners()
    }

    private fun setupUI() {
        val guide = OEMGuideHelper.getNotificationGuide(requireContext())
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

    private fun checkPermissionStatus() {
        val isEnabled = NotificationAccessChecker.isNotificationAccessEnabled(requireContext())
        updateUI(isEnabled)
    }

    private fun updateUI(isEnabled: Boolean) {
        if (isEnabled) {
            binding.tvStatus.text = "✅ Permiso de Notificaciones: Activado"
            binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            binding.btnOpenSettings.text = "Permiso Concedido"
            binding.btnOpenSettings.isEnabled = false
        } else {
            binding.tvStatus.text = "❌ Permiso de Notificaciones: Desactivado"
            binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#F44336"))
            binding.btnOpenSettings.text = "Abrir Configuración"
            binding.btnOpenSettings.isEnabled = true
        }
    }

    private fun setupClickListeners() {
        binding.btnOpenSettings.setOnClickListener {
            openNotificationSettings()
        }
    }

    private fun openNotificationSettings() {
        val guide = OEMGuideHelper.getNotificationGuide(requireContext())
        // Try to open the first step's action intent if available
        val firstStepWithAction = guide.steps.firstOrNull { it.actionIntent != null }
        
        if (firstStepWithAction != null) {
            OEMGuideHelper.openGuideStep(requireContext(), firstStepWithAction)
        } else {
            // Fallback to standard notification listener settings
            NotificationAccessChecker.openNotificationListenerSettings(requireContext())
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionStatus()
    }

    fun isPermissionGranted(): Boolean {
        return NotificationAccessChecker.isNotificationAccessEnabled(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

