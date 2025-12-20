package com.yapenotifier.android.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.model.MonitoredPackagesResponse
import com.yapenotifier.android.databinding.FragmentMonitoredAppsBinding
import com.yapenotifier.android.ui.adapter.MonitoredAppAdapter
import com.yapenotifier.android.ui.adapter.MonitoredAppItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MonitoredAppsFragment : Fragment() {
    private var _binding: FragmentMonitoredAppsBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var apiService: ApiService
    private lateinit var adapter: MonitoredAppAdapter
    private val selectedPackages = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMonitoredAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesManager = PreferencesManager(requireContext())
        apiService = RetrofitClient.createApiService(requireContext())
        setupRecyclerView()
        loadSelectedPackages()
        loadAvailablePackages()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = MonitoredAppAdapter { packageName, isChecked ->
            if (isChecked) {
                selectedPackages.add(packageName)
            } else {
                selectedPackages.remove(packageName)
            }
        }
        binding.rvMonitoredApps.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMonitoredApps.adapter = adapter
    }

    private fun loadSelectedPackages() {
        lifecycleScope.launch {
            val selected = preferencesManager.selectedMonitoredPackages.first()
            selectedPackages.clear()
            selectedPackages.addAll(selected)
        }
    }

    private fun loadAvailablePackages() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                val response = apiService.getMonitoredPackages()
                
                if (response.isSuccessful) {
                    val packages = response.body()?.packages ?: emptyList()
                    displayPackages(packages)
                } else {
                    // Show error if needed
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Error al cargar apps: ${response.code()}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Error de conexión: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun displayPackages(packages: List<String>) {
        val items = packages.map { packageName ->
            MonitoredAppItem(
                packageName = packageName,
                displayName = getAppDisplayName(packageName),
                isSelected = selectedPackages.contains(packageName)
            )
        }
        adapter.submitList(items)
    }

    private fun getAppDisplayName(packageName: String): String {
        return when (packageName) {
            "com.bcp.innovacxion.yape.movil" -> "Yape"
            "pe.com.interbank.mobilebanking" -> "Interbank"
            "com.scotiabank.mobile.android" -> "Scotiabank"
            else -> packageName
        }
    }

    private fun setupClickListeners() {
        binding.btnSaveMonitoredApps.setOnClickListener {
            saveSelectedPackages()
        }
    }

    private fun saveSelectedPackages() {
        lifecycleScope.launch {
            preferencesManager.saveSelectedMonitoredPackages(selectedPackages)
            android.widget.Toast.makeText(
                requireContext(),
                "Apps seleccionadas guardadas",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun getSelectedPackages(): Set<String> = selectedPackages

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
