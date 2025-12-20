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
        val oem = OemDetector.detectOem()
        val oemName = OemDetector.getOemDisplayName(oem)
        val recommendations = OemDetector.getOemRecommendations(oem)

        binding.tvOemInfo.text = "Dispositivo detectado: $oemName"
        binding.tvRecommendations.text = recommendations.joinToString("\n")
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

