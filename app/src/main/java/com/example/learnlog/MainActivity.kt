package com.example.learnlog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.learnlog.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Configure action bar
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.tasksFragment,
                R.id.plannerFragment,
                R.id.timerFragment,
                R.id.insightsFragment,
                R.id.notesFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Set up Bottom Navigation
        binding.bottomNavigation.apply {
            setupWithNavController(navController)
            setOnItemReselectedListener { /* Prevent reselection reload */ }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
