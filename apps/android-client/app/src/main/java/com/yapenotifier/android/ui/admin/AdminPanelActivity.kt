package com.yapenotifier.android.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.yapenotifier.android.R
import com.yapenotifier.android.databinding.ActivityAdminPanelBinding
import com.yapenotifier.android.ui.admin.adapter.NotificationAdapter
import com.yapenotifier.android.ui.admin.viewmodel.AdminPanelViewModel
import dagger.hilt.android.AndroidEntryPoint
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
        adapter = NotificationAdapter(
            onItemClick = { notification ->
                // Navigate to detail
                val intent = Intent(this, AdminNotificationDetailActivity::class.java)
                intent.putExtra("notification_id", notification.id)
                startActivity(intent)
            },
            onValidateClick = { notification ->
                // Validate notification (quick action)
                viewModel.validateNotification(notification.id)
                Snackbar.make(binding.root, "Notificación validada", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getColor(R.color.badge_original_bg))
                    .setTextColor(getColor(R.color.badge_original_text))
                    .show()
            },
            onMarkInconsistent = { notification ->
                // Mark as inconsistent
                viewModel.updateNotificationStatus(notification.id, "inconsistent")
                Snackbar.make(binding.root, "Marcado como inconsistente", Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo) {
                        viewModel.updateNotificationStatus(notification.id, "pending")
                    }
                    .show()
            },
            onMarkPending = { notification ->
                // Mark as pending
                viewModel.updateNotificationStatus(notification.id, "pending")
                Snackbar.make(binding.root, "Marcado como pendiente", Snackbar.LENGTH_SHORT).show()
            }
        )

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

            // Show skeleton while loading and no data yet
            if (state.loading && state.notifications.isEmpty()) {
                binding.skeletonContainer.visibility = View.VISIBLE
                binding.emptyStateContainer.visibility = View.GONE
                binding.rvNotifications.visibility = View.GONE
                binding.sectionHeader.visibility = View.GONE
            } else {
                binding.skeletonContainer.visibility = View.GONE
            }

            adapter.submitList(state.notifications)

            // Update KPI cards: count + sum of amounts of currently visible notifications
            val ops = state.notifications.size
            val total = state.notifications
                .mapNotNull { it.amount }
                .sumOf { it.toDouble() }
            binding.tvKpiOpsValue.text = ops.toString()
            binding.tvKpiAmountValue.text = String.format("S/ %.2f", total)

            // Show empty state when no data and not loading
            if (state.notifications.isEmpty() && !state.loading) {
                binding.emptyStateContainer.visibility = View.VISIBLE
                binding.rvNotifications.visibility = View.GONE
                binding.sectionHeader.visibility = View.GONE
                Timber.d("AdminPanelActivity: Mostrando empty state")
            } else if (state.notifications.isNotEmpty()) {
                binding.emptyStateContainer.visibility = View.GONE
                binding.rvNotifications.visibility = View.VISIBLE
                binding.sectionHeader.visibility = View.VISIBLE
            }

            // Show error with Snackbar for better UX
            state.error?.let { error ->
                Timber.e("AdminPanelActivity Error: $error")
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry) { viewModel.refresh() }
                    .show()
            }
        }

        viewModel.pollingState.observe(this) { state ->
            Timber.d("AdminPanelActivity Polling State: $state")
        }
    }

    private fun setupFilters() {
        binding.chipGroup.removeAllViews()

        // "Todos" filter (default)
        val chipAll = Chip(this).apply {
            text = "Todos"
            isChecked = true
            isCheckable = true
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    viewModel.clearAllFilters()
                    // Uncheck other date chips
                    uncheckAllChipsExcept(this)
                }
            }
        }
        binding.chipGroup.addView(chipAll)

        // === FILTROS DE FECHA ===

        // "Hoy" filter
        val chipToday = Chip(this).apply {
            text = "Hoy"
            isCheckable = true
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    val today = viewModel.getTodayDateFilter()
                    viewModel.setFilter("start_date", today)
                    viewModel.setFilter("end_date", null) // Remove end_date for single day
                    uncheckAllChipsExcept(this)
                }
            }
        }
        binding.chipGroup.addView(chipToday)

        // "Ayer" filter
        val chipYesterday = Chip(this).apply {
            text = "Ayer"
            isCheckable = true
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    val yesterday = viewModel.getYesterdayDateFilter()
                    viewModel.setFilter("start_date", yesterday)
                    viewModel.setFilter("end_date", null) // Remove end_date for single day
                    uncheckAllChipsExcept(this)
                }
            }
        }
        binding.chipGroup.addView(chipYesterday)

        // "Últimos 7 días" filter
        val chipLast7Days = Chip(this).apply {
            text = "Últimos 7 días"
            isCheckable = true
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    val (startDate, endDate) = viewModel.getDateRangeFilter(7)
                    viewModel.setFilter("start_date", startDate)
                    viewModel.setFilter("end_date", endDate)
                    uncheckAllChipsExcept(this)
                }
            }
        }
        binding.chipGroup.addView(chipLast7Days)

        // === FILTROS DE ESTADO ===

        // "Pendientes" filter
        val chipPending = Chip(this).apply {
            text = "Pendientes"
            isCheckable = true
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    viewModel.setFilter("status", "pending")
                } else {
                    viewModel.setFilter("status", null)
                }
            }
        }
        binding.chipGroup.addView(chipPending)

        // === FILTROS DE INSTANCIAS ===
        // Agregar filtros dinámicos por instancia cuando se carguen las notificaciones
        viewModel.uiState.observe(this) { state ->
            // Extraer instancias únicas con sus labels
            val instances = state.notifications
                .mapNotNull { it.appInstance }
                .distinctBy { it.id }
                .sortedBy { it.label }

            // Solo agregar chips de instancias si hay más de una
            if (instances.size > 1) {
                // Remover chips de instancias anteriores (mantener los primeros 6 chips: Todos, Hoy, Ayer, 7 días, Pendientes + separador)
                while (binding.chipGroup.childCount > 5) {
                    binding.chipGroup.removeViewAt(5)
                }

                // Agregar chips para cada instancia
                instances.forEach { instance ->
                    val chipInstance = Chip(this).apply {
                        text = instance.label ?: "Instancia ${instance.id}"
                        isCheckable = true
                        setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                viewModel.setFilter("app_instance_id", instance.id)
                            } else {
                                viewModel.setFilter("app_instance_id", null)
                            }
                        }
                    }
                    binding.chipGroup.addView(chipInstance)
                }
            }
        }
    }

    /**
     * Helper para desmarcar todos los chips excepto el especificado
     * Solo desmarca chips de fecha (Todos, Hoy, Ayer, Últimos 7 días)
     */
    private fun uncheckAllChipsExcept(exceptChip: Chip) {
        for (i in 0 until minOf(4, binding.chipGroup.childCount)) {
            val chip = binding.chipGroup.getChildAt(i) as? Chip
            if (chip != null && chip != exceptChip) {
                chip.isChecked = false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_admin_panel, menu)
        return true
    }
    
    private fun setupClickListeners() {
        binding.tvMarkAllRead.setOnClickListener {
            viewModel.markAllAsRead()
            Toast.makeText(this, "Todas las notificaciones marcadas como leídas", Toast.LENGTH_SHORT).show()
        }

        binding.btnRefresh.setOnClickListener {
            viewModel.refresh()
            Toast.makeText(this, "Actualizando...", Toast.LENGTH_SHORT).show()
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
