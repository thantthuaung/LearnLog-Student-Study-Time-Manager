package com.example.learnlog

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.example.learnlog.auth.AuthManager
import com.example.learnlog.data.preferences.UserPreferences
import com.example.learnlog.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    @Inject
    lateinit var authManager: AuthManager

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
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

        // Configure action bar
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.tasksFragment,
                R.id.plannerFragment,
                R.id.timerFragment,
                R.id.insightsFragment,
                R.id.notesFragment,
                R.id.loginFragment,
                R.id.registerFragment,
                R.id.settingsFragment
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

        // Hide bottom navigation and drawer on auth screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.registerFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                    binding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                }
                else -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED)
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

        lifecycleScope.launch {
            userPreferences.displayName.collect { name ->
                displayNameTextView.text = name.ifEmpty { "Student" }
            }
        }

        lifecycleScope.launch {
            userPreferences.email.collect { email ->
                emailTextView.text = email.ifEmpty { "student@learnlog.app" }
            }
        }

        lifecycleScope.launch {
            userPreferences.avatarUri.collect { uri ->
                if (uri != null) {
                    try {
                        Glide.with(this@MainActivity)
                            .load(Uri.parse(uri))
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .circleCrop()
                            .into(avatarImageView)
                    } catch (e: Exception) {
                        avatarImageView.setImageResource(R.drawable.ic_person)
                    }
                } else {
                    avatarImageView.setImageResource(R.drawable.ic_person)
                }
            }
        }
    }

    fun openDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.START)
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

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
