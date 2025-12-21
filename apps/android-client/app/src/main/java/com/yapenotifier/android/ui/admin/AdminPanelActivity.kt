package com.yapenotifier.android.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.yapenotifier.android.R
import com.yapenotifier.android.databinding.ActivityAdminPanelBinding
import com.yapenotifier.android.ui.admin.adapter.NotificationAdapter
import com.yapenotifier.android.ui.admin.viewmodel.AdminPanelViewModel

class AdminPanelActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminPanelBinding
    private lateinit var viewModel: AdminPanelViewModel
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminPanelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[AdminPanelViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        setupBottomNavigation()
        setupObservers()
        setupFilters()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbarTitle.text = getString(R.string.admin_panel_title)
        binding.toolbarSubtitle.text = getString(R.string.admin_panel_subtitle)
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter { notification ->
            // Navigate to detail
            val intent = Intent(this, AdminNotificationDetailActivity::class.java)
            intent.putExtra("notification_id", notification.id)
            startActivity(intent)
        }

        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter

        // Pagination
        binding.rvNotifications.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                val lastVisiblePosition = layoutManager?.findLastVisibleItemPosition() ?: 0
                val totalItems = layoutManager?.itemCount ?: 0

                if (lastVisiblePosition >= totalItems - 5) {
                    viewModel.loadMore()
                }
            }
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_notifications -> {
                    // Already on notifications tab
                    true
                }
                R.id.nav_devices -> {
                    val intent = Intent(this, AdminDevicesActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_settings -> {
                    val intent = Intent(this, AdminSettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            binding.swipeRefresh.isRefreshing = state.loading

            if (state.loading && state.notifications.isEmpty()) {
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
            }

            adapter.submitList(state.notifications)

            if (state.notifications.isEmpty() && !state.loading) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvNotifications.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.rvNotifications.visibility = View.VISIBLE
            }

            state.error?.let { error ->
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupFilters() {
        // "Todos" filter (default)
        val chipAll = Chip(this).apply {
            text = getString(R.string.filter_all)
            isChecked = true
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    viewModel.setFilter("device_id", null)
                    viewModel.setFilter("source_app", null)
                    viewModel.setFilter("start_date", null)
                }
            }
        }
        binding.chipGroup.addView(chipAll)

        // "Hoy" filter
        val chipToday = Chip(this).apply {
            text = getString(R.string.filter_today)
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    viewModel.setFilter("start_date", viewModel.getTodayDateFilter())
                    chipAll.isChecked = false
                }
            }
        }
        binding.chipGroup.addView(chipToday)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_admin_panel, menu)

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
            R.id.action_mark_all_read -> {
                viewModel.markAllAsRead()
                Toast.makeText(this, getString(R.string.mark_all_as_read_toast), Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_profile -> {
                // Open profile/settings
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
