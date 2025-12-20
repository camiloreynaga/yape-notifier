package com.yapenotifier.android.ui.permissions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.yapenotifier.android.databinding.FragmentMonitoredAppsBinding
import com.yapenotifier.android.ui.MonitoredAppsAdapter
import com.yapenotifier.android.ui.viewmodel.MonitoredAppsViewModel

class MonitoredAppsFragment : Fragment() {

    private var _binding: FragmentMonitoredAppsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MonitoredAppsViewModel
    private lateinit var adapter: MonitoredAppsAdapter

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
        viewModel = ViewModelProvider(this).get(MonitoredAppsViewModel::class.java)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        viewModel.loadMonitoredApps()
    }

    private fun setupRecyclerView() {
        binding.rvMonitoredApps.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupObservers() {
        viewModel.appsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MonitoredAppsViewModel.MonitoredAppsState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is MonitoredAppsViewModel.MonitoredAppsState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    adapter = MonitoredAppsAdapter(state.items.toMutableList())
                    binding.rvMonitoredApps.adapter = adapter
                }
                is MonitoredAppsViewModel.MonitoredAppsState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.saveState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MonitoredAppsViewModel.SaveState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSaveMonitoredApps.isEnabled = false
                }
                is MonitoredAppsViewModel.SaveState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Selección guardada con éxito", Toast.LENGTH_SHORT).show()
                    // TODO: Navigate to the next step in the wizard or finish
                }
                is MonitoredAppsViewModel.SaveState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSaveMonitoredApps.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSaveMonitoredApps.setOnClickListener {
            if (::adapter.isInitialized) {
                val selected = adapter.getSelectedPackages()
                viewModel.saveMonitoredApps(selected)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
