package com.yapenotifier.android.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.yapenotifier.android.R
import com.yapenotifier.android.databinding.ActivityMonitoredAppsSelectionBinding
import com.yapenotifier.android.ui.adapter.MonitoredAppAdapter
import com.yapenotifier.android.ui.viewmodel.FilterType
import com.yapenotifier.android.ui.viewmodel.MonitoredAppsSelectionViewModel

class MonitoredAppsSelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMonitoredAppsSelectionBinding
    private lateinit var viewModel: MonitoredAppsSelectionViewModel
    private lateinit var adapter: MonitoredAppAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonitoredAppsSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MonitoredAppsSelectionViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        setupFilters()
        setupObservers()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        supportActionBar?.title = getString(R.string.monitored_apps_title)
    }

    private fun setupRecyclerView() {
        adapter = MonitoredAppAdapter { packageId ->
            viewModel.togglePackageStatus(packageId)
        }

        binding.rvMonitoredApps.layoutManager = LinearLayoutManager(this)
        binding.rvMonitoredApps.adapter = adapter
    }

    private fun setupFilters() {
        binding.chipGroup.isSingleSelection = true
        // "Todas" filter (default)
        val chipAll = Chip(this).apply {
            text = getString(R.string.all_filter)
            isCheckable = true
            isChecked = true
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    viewModel.setFilterType(FilterType.ALL)
                }
            }
        }
        binding.chipGroup.addView(chipAll)

        // "Monitoreadas" filter
        val chipMonitored = Chip(this).apply {
            text = getString(R.string.monitored_filter)
            isCheckable = true
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    viewModel.setFilterType(FilterType.MONITORED)
                }
            }
        }
        binding.chipGroup.addView(chipMonitored)

        // "No monitoreadas" filter
        val chipNotMonitored = Chip(this).apply {
            text = getString(R.string.not_monitored_filter)
            isCheckable = true
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    viewModel.setFilterType(FilterType.NOT_MONITORED)
                }
            }
        }
        binding.chipGroup.addView(chipNotMonitored)
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            // Update loading state
            binding.progressBar.visibility = if (state.loading) View.VISIBLE else View.GONE

            // Update list
            adapter.submitList(state.filteredPackages)

            // Show/hide empty state
            if (state.filteredPackages.isEmpty() && !state.loading) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvMonitoredApps.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.rvMonitoredApps.visibility = View.VISIBLE
            }

            // Update statistics
            val monitoredCount = viewModel.getMonitoredCount()
            binding.tvStats.text = getString(R.string.monitored_apps_stats, monitoredCount, state.packages.size)

            // Update last updated
            state.lastUpdated?.let {
                binding.tvLastUpdated.text = getString(R.string.last_updated, it)
            }

            // Show error
            state.error?.let { error ->
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }

            // Show save error
            state.saveError?.let { error ->
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }

            // Show saving indicator
            if (state.saving) {
                Toast.makeText(this, getString(R.string.syncing), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_monitored_apps, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as? SearchView
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: "")
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.refresh()
                Toast.makeText(this, getString(R.string.updating), Toast.LENGTH_SHORT).show()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
