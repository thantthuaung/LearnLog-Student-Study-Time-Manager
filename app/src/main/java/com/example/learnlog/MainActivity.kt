package com.example.learnlog

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
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
                R.id.registerFragment
            )
        )

        // Set up Bottom Navigation
        binding.bottomNavigation.apply {
            setupWithNavController(navController)
            setOnItemReselectedListener { /* Prevent reselection reload */ }
        }

        // Hide bottom navigation on auth screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.registerFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
            }
        }

        // Handle notification intent to open Timer tab
        handleNotificationIntent()
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
}
