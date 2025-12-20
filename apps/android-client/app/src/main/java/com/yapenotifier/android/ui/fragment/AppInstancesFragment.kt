package com.yapenotifier.android.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.databinding.FragmentAppInstancesBinding
import com.yapenotifier.android.ui.AppInstancesActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppInstancesFragment : Fragment() {
    private var _binding: FragmentAppInstancesBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var apiService: ApiService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppInstancesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesManager = PreferencesManager(requireContext())
        apiService = RetrofitClient.createApiService(requireContext())
        checkAppInstances()
        setupClickListeners()
    }

    private fun checkAppInstances() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                val deviceId = preferencesManager.deviceId.first()?.toLongOrNull()
                
                if (deviceId != null) {
                    val response = apiService.getDeviceAppInstances(deviceId)
                    
                    if (response.isSuccessful) {
                        val instances = response.body()?.instances ?: emptyList()
                        val hasUnnamedInstances = instances.any { 
                            val label = it.label
                            label.isNullOrBlank()
                        }
                        
                        if (instances.isEmpty()) {
                            binding.tvStatus.text = "No se encontraron instancias de aplicaciones"
                            binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#757575"))
                            binding.btnManageInstances.visibility = View.GONE
                        } else if (hasUnnamedInstances) {
                            binding.tvStatus.text = "⚠️ Hay ${instances.count { val label = it.label; label.isNullOrBlank() }} instancias sin nombre"
                            binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#FF9800"))
                            binding.btnManageInstances.visibility = View.VISIBLE
                        } else {
                            binding.tvStatus.text = "✅ Todas las instancias tienen nombre asignado"
                            binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                            binding.btnManageInstances.visibility = View.VISIBLE
                            binding.btnManageInstances.text = "Ver/Editar Instancias"
                        }
                    } else {
                        binding.tvStatus.text = "Error al verificar instancias: ${response.code()}"
                        binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#F44336"))
                        binding.btnManageInstances.visibility = View.GONE
                    }
                } else {
                    binding.tvStatus.text = "No se pudo obtener el ID del dispositivo"
                    binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#F44336"))
                    binding.btnManageInstances.visibility = View.GONE
                }
            } catch (e: Exception) {
                binding.tvStatus.text = "Error de conexión: ${e.message}"
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#F44336"))
                binding.btnManageInstances.visibility = View.GONE
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnManageInstances.setOnClickListener {
            val intent = Intent(requireContext(), AppInstancesActivity::class.java)
            startActivity(intent)
        }
    }

    fun hasUnnamedInstances(): Boolean {
        // This will be checked asynchronously, return false for now
        // The actual check happens in checkAppInstances()
        return false
    }

    override fun onResume() {
        super.onResume()
        checkAppInstances()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
