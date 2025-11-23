package com.example.learnlog

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.example.learnlog.auth.AuthManager
import com.example.learnlog.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    @Inject
    lateinit var authManager: AuthManager


    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate
        installSplashScreen()

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Check if user is logged in and set start destination accordingly
        val startDestination = if (authManager.isUserLoggedIn) {
            R.id.tasksFragment
        } else {
            R.id.loginFragment
        }

        // Update navigation graph if needed
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
        navGraph.setStartDestination(startDestination)
        navController.graph = navGraph

        // Configure AppBarConfiguration with ONLY top-level destinations (bottom nav tabs)
        // Settings is NOT a top-level destination, so it will show back arrow
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.tasksFragment,
                R.id.plannerFragment,
                R.id.timerFragment,
                R.id.insightsFragment,
                R.id.notesFragment,
                R.id.loginFragment,
                R.id.registerFragment
            ),
            binding.drawerLayout
        )

        // Set up Bottom Navigation
        binding.bottomNavigation.apply {
            setupWithNavController(navController)
            setOnItemReselectedListener { /* Prevent reselection reload */ }
        }

        // Set up Navigation Drawer
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_account, R.id.nav_preferences, R.id.nav_notifications,
                R.id.nav_data_backup, R.id.nav_help -> {
                    // Navigate to settings with specific section
                    val section = when (menuItem.itemId) {
                        R.id.nav_account -> 0
                        R.id.nav_preferences -> 1
                        R.id.nav_notifications -> 2
                        R.id.nav_data_backup -> 3
                        R.id.nav_help -> 4
                        else -> 0
                    }
                    val bundle = Bundle().apply {
                        putInt("section", section)
                    }
                    navController.navigate(R.id.settingsFragment, bundle)
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_sign_out -> {
                    // Handle sign out
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Sign Out")
                        .setMessage("Are you sure you want to sign out?")
                        .setPositiveButton("Sign Out") { _, _ ->
                            authManager.signOut()
                            navController.navigate(R.id.loginFragment)
                            binding.drawerLayout.closeDrawer(GravityCompat.START)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                }
                else -> false
            }
        }

        // Setup drawer header profile updates
        setupDrawerHeader()

        // Handle navigation destination changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.registerFragment -> {
                    // Hide bottom nav and lock drawer on auth screens
                    binding.bottomNavigation.visibility = View.GONE
                    binding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                }
                R.id.settingsFragment -> {
                    // Lock drawer on Settings (so back arrow works, not hamburger)
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                }
                else -> {
                    // Normal screens: show bottom nav and unlock drawer
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED)
                    // Refresh drawer header when navigating to main screens (after login/register)
                    setupDrawerHeader()
                }
            }
        }

        // Handle notification intent to open Timer tab
        handleNotificationIntent()
    }

    private fun setupDrawerHeader() {
        val headerView = binding.navView.getHeaderView(0)
        val avatarImageView = headerView.findViewById<ImageView>(R.id.drawer_avatar)
        val displayNameTextView = headerView.findViewById<TextView>(R.id.drawer_display_name)
        val emailTextView = headerView.findViewById<TextView>(R.id.drawer_email)

        // Get user info from Firebase Auth
        val currentUser = authManager.currentUser

        // Set display name from Firebase Auth
        val displayName = currentUser?.displayName
        displayNameTextView.text = displayName?.takeIf { it.isNotEmpty() } ?: "User"

        // Set email from Firebase Auth
        val email = currentUser?.email
        emailTextView.text = email?.takeIf { it.isNotEmpty() } ?: "No email"

        // Set profile photo from Firebase Auth or use default
        val photoUrl = currentUser?.photoUrl
        if (photoUrl != null) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(avatarImageView)
        } else {
            avatarImageView.setImageResource(R.drawable.ic_person)
        }
    }

    fun openDrawer() {
        // Open drawer immediately without checking if it's already open
        if (!binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.openDrawer(GravityCompat.START, true)
        }
    }

    fun getNavController() = (supportFragmentManager
        .findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

    override fun onResume() {
        super.onResume()
        // Refresh drawer header when activity resumes to show updated user info
        setupDrawerHeader()
    }

    private fun handleNotificationIntent() {
        if (intent?.getBooleanExtra("open_timer_tab", false) == true) {
            // Navigate to Timer tab
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController
            navController.navigate(R.id.timerFragment)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
