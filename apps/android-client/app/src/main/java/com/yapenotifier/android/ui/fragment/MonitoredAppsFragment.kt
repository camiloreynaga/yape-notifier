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
import com.yapenotifier.android.data.model.MonitorPackage
import com.yapenotifier.android.databinding.FragmentMonitoredAppsBinding
import com.yapenotifier.android.ui.MonitoredAppsAdapter
import com.yapenotifier.android.ui.MonitoredAppCheckableItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MonitoredAppsFragment : Fragment() {
    private var _binding: FragmentMonitoredAppsBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var apiService: ApiService
    private lateinit var adapter: MonitoredAppsAdapter
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
        adapter = MonitoredAppsAdapter { item, isChecked ->
            val newList = adapter.currentList.map {
                if (it.packageName == item.packageName) {
                    it.copy(isChecked = isChecked)
                } else {
                    it
                }
            }
            adapter.submitList(newList)
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
                val response = apiService.getMonitorPackages(activeOnly = false)
                
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

    private fun displayPackages(packages: List<MonitorPackage>) {
        val items = packages.map { packageItem ->
            MonitoredAppCheckableItem(
                packageName = packageItem.packageName,
                isChecked = selectedPackages.contains(packageItem.packageName) || packageItem.isActive
            )
        }
        adapter.submitList(items)
    }

    private fun setupClickListeners() {
        binding.btnSaveMonitoredApps.setOnClickListener {
            saveSelectedPackages()
        }
    }

    private fun saveSelectedPackages() {
        lifecycleScope.launch {
            // Get selected packages from adapter
            val selected = adapter.getSelectedPackages().toSet()
            preferencesManager.saveSelectedMonitoredPackages(selected)
            android.widget.Toast.makeText(
                requireContext(),
                "Apps seleccionadas guardadas",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun getSelectedPackages(): Set<String> {
        return adapter.getSelectedPackages().toSet()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
