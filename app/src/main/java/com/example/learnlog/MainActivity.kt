package com.example.learnlog

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.learnlog.auth.AuthManager
import com.example.learnlog.data.preferences.UserPreferences
import com.example.learnlog.databinding.ActivityMainBinding
import com.example.learnlog.databinding.NavHeaderBinding
import com.example.learnlog.timer.FocusTimer
import com.example.learnlog.ui.common.ProfileAvatar
import com.example.learnlog.ui.common.Reselectable
import com.example.learnlog.util.AppRestarter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var header: NavHeaderBinding

    @Inject lateinit var authManager: AuthManager
    @Inject lateinit var focusTimer: FocusTimer
    @Inject lateinit var userPreferences: UserPreferences

    private var bottomNavShown = true
    private var keyboardVisible = false
    private var destinationWantsNav = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        header = NavHeaderBinding.bind(binding.navView.getHeaderView(0))

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController
        // The graph is set here (not in XML) so the start destination depends on sign-in state;
        // on re-creation NavController restores the saved back stack onto this graph.
        val graph = navController.navInflater.inflate(R.navigation.nav_graph)
        graph.setStartDestination(if (authManager.isUserLoggedIn) R.id.tasksFragment else R.id.loginFragment)
        navController.graph = graph

        setupBottomNav()
        setupDrawer()
        setupBackHandling()
        watchKeyboard()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            destinationWantsNav = destination.id in TOP_LEVEL
            updateBottomNavVisibility(animate = true)
            binding.drawerLayout.setDrawerLockMode(
                if (destinationWantsNav) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
            )
            if (destinationWantsNav) refreshDrawerHeader()
        }

        handleIntent(intent)
    }

    private fun setupBottomNav() {
        binding.bottomNavigation.setupWithNavController(navController)
        binding.bottomNavigation.setOnItemReselectedListener {
            val current = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment)
                .childFragmentManager.primaryNavigationFragment
            (current as? Reselectable)?.onReselected()
        }
    }

    private fun setupDrawer() {
        binding.navView.setNavigationItemSelectedListener { item ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (item.itemId) {
                R.id.nav_analytics -> navController.navigate(R.id.action_global_analytics)
                R.id.nav_account -> openSettings(SECTION_ACCOUNT)
                R.id.nav_preferences -> openSettings(SECTION_PREFERENCES)
                R.id.nav_notifications -> openSettings(SECTION_ALERTS)
                R.id.nav_data_backup -> openSettings(SECTION_DATA)
                R.id.nav_help -> openSettings(SECTION_ABOUT)
                R.id.nav_sign_out -> confirmSignOut()
            }
            true
        }
    }

    fun openSettings(section: Int) {
        navController.navigate(R.id.action_global_settings, bundleOf("section" to section))
    }

    fun openDrawer() {
        if (!binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    fun confirmSignOut() {
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_logout)
            .setTitle(R.string.sign_out_title)
            .setMessage(R.string.sign_out_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.drawer_sign_out) { _, _ ->
                // Keep any focus time already earned, then rebuild everything for the next account
                focusTimer.stop(save = true)
                authManager.signOut()
                window.decorView.postDelayed({ AppRestarter.restart(this) }, 250)
            }
            .show()
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    private fun watchKeyboard() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val visible = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (visible != keyboardVisible) {
                keyboardVisible = visible
                updateBottomNavVisibility(animate = !visible)
            }
            insets
        }
    }

    private fun updateBottomNavVisibility(animate: Boolean) {
        val show = destinationWantsNav && !keyboardVisible
        if (show == bottomNavShown && binding.bottomNavContainer.isVisible == show) return
        bottomNavShown = show
        val nav = binding.bottomNavContainer
        nav.animate().cancel()
        if (show) {
            nav.visibility = View.VISIBLE
            if (animate) {
                nav.translationY = nav.height.toFloat() + 80f
                nav.animate().translationY(0f).alpha(1f).setDuration(320)
                    .setInterpolator(android.view.animation.PathInterpolator(0.2f, 0f, 0f, 1f)).start()
            } else {
                nav.translationY = 0f
                nav.alpha = 1f
            }
        } else if (animate && nav.isVisible) {
            nav.animate().translationY(nav.height.toFloat() + 80f).alpha(0f).setDuration(200)
                .withEndAction { nav.visibility = View.GONE }.start()
        } else {
            nav.visibility = View.GONE
        }
    }

    private val View.isVisible get() = visibility == View.VISIBLE

    private fun refreshDrawerHeader() {
        val user = authManager.currentUser
        val name = user?.displayName?.takeIf { it.isNotBlank() } ?: getString(R.string.guest_name)
        header.drawerDisplayName.text = name
        header.drawerEmail.text = user?.email.orEmpty()
        lifecycleScope.launch {
            val local = userPreferences.avatarUri.first()
            ProfileAvatar.bind(header.drawerInitials, header.drawerAvatar, name, local ?: user?.photoUrl?.toString())
        }
    }

    override fun onResume() {
        super.onResume()
        if (::header.isInitialized && authManager.isUserLoggedIn) refreshDrawerHeader()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_TIMER, false) == true && authManager.isUserLoggedIn) {
            intent.removeExtra(EXTRA_OPEN_TIMER)
            binding.root.post { binding.bottomNavigation.selectedItemId = R.id.timerFragment }
        }
    }

    override fun onSupportNavigateUp(): Boolean = navController.navigateUp() || super.onSupportNavigateUp()

    companion object {
        const val EXTRA_OPEN_TIMER = "open_timer_tab"

        const val SECTION_ACCOUNT = 0
        const val SECTION_PREFERENCES = 1
        const val SECTION_ALERTS = 2
        const val SECTION_DATA = 3
        const val SECTION_ABOUT = 4

        val TOP_LEVEL = setOf(
            R.id.tasksFragment, R.id.plannerFragment, R.id.timerFragment,
            R.id.insightsFragment, R.id.notesFragment
        )
    }
}
