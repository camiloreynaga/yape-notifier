package com.yapenotifier.android.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import android.text.TextWatcher
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.yapenotifier.android.R
import com.yapenotifier.android.databinding.ActivityAdminPanelBinding
import com.yapenotifier.android.ui.admin.adapter.NotificationAdapter
import com.yapenotifier.android.ui.admin.viewmodel.AdminPanelViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class AdminPanelActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminPanelBinding
    private val viewModel: AdminPanelViewModel by viewModels()
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Timber.d("AdminPanelActivity onCreate - iniciando")
            binding = ActivityAdminPanelBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // ViewModel inyectado automáticamente por Hilt

            Timber.d("AdminPanelActivity ViewModel creado")
            setupToolbar()
            setupRecyclerView()
            setupSwipeRefresh()
            setupBottomNavigation()
            setupSearch()
            setupClickListeners()
            setupObservers()
            setupFilters()
            Timber.d("AdminPanelActivity setup completo")
        } catch (e: Exception) {
            Timber.e(e, "AdminPanelActivity: Error crítico en onCreate")
            Toast.makeText(this, "Error al iniciar el panel: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
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
            Timber.d("AdminPanelActivity UI State actualizado: loading=${state.loading}, notifications=${state.notifications.size}, error=${state.error}, total=${state.total}")
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
                Timber.d("AdminPanelActivity: Mostrando mensaje vacío")
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.rvNotifications.visibility = View.VISIBLE
            }

            state.error?.let { error ->
                Timber.e("AdminPanelActivity Error: $error")
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
        
        viewModel.pollingState.observe(this) { state ->
            Timber.d("AdminPanelActivity Polling State: $state")
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
        return true
    }
    
    private fun setupSearch() {
        // Setup search from EditText in layout
        binding.etSearch.setOnEditorActionListener { _, _, _ ->
            viewModel.setSearchQuery(binding.etSearch.text.toString())
            true
        }
        
        // Debounce search input
        var searchJob: Job? = null
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s?.toString() ?: ""
                // Pausar polling cuando usuario está escribiendo
                viewModel.setUserTyping(query.isNotEmpty())
                
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(500)
                    viewModel.setSearchQuery(query)
                }
            }
        })
    }
    
    private fun setupClickListeners() {
        binding.tvMarkAllRead.setOnClickListener {
            viewModel.markAllAsRead()
            Toast.makeText(this, "Todas las notificaciones marcadas como leídas", Toast.LENGTH_SHORT).show()
        }
        
        binding.ivProfile.setOnClickListener {
            val intent = Intent(this, AdminSettingsActivity::class.java)
            startActivity(intent)
        }
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

    override fun onResume() {
        super.onResume()
        Timber.d("AdminPanelActivity onResume - iniciando polling")
        viewModel.startPolling() // Iniciar polling cuando Activity está visible
    }

    override fun onPause() {
        super.onPause()
        Timber.d("AdminPanelActivity onPause - deteniendo polling")
        viewModel.stopPolling() // Detener polling cuando Activity está en background
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("AdminPanelActivity onDestroy - deteniendo polling")
        // Safe access: viewModel is initialized by Hilt when first accessed
        try {
            viewModel.stopPolling() // Asegurar que se detiene al destruir
        } catch (e: Exception) {
            Timber.e(e, "Error stopping polling in onDestroy")
        }
    }
}
