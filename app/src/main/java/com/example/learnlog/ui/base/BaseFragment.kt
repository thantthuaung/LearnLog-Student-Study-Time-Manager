package com.example.learnlog.ui.base

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.learnlog.MainActivity
import com.example.learnlog.R

/**
 * Base fragment that sets up the hamburger/back button behavior
 */
abstract class BaseFragment : Fragment {

    constructor() : super()

    constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupNavigationButton(view)
    }

    private fun setupNavigationButton(view: View) {
        view.findViewById<ImageButton>(R.id.hamburger_button)?.let { button ->
            val navController = findNavController()

            // Determine if this is a top-level destination
            val topLevelDestinations = setOf(
                R.id.tasksFragment,
                R.id.plannerFragment,
                R.id.timerFragment,
                R.id.insightsFragment,
                R.id.notesFragment
            )

            val currentDestination = navController.currentDestination?.id
            val isTopLevel = currentDestination in topLevelDestinations

            // Clear any existing click listeners
            button.setOnClickListener(null)
            button.setOnLongClickListener(null)

            // Make button clickable and focusable
            button.isClickable = true
            button.isFocusable = true

            if (isTopLevel) {
                // Show hamburger icon and open drawer
                button.setImageResource(R.drawable.ic_menu)
                button.contentDescription = getString(R.string.open_drawer)
                button.setOnClickListener {
                    // Open drawer immediately on click
                    (activity as? MainActivity)?.openDrawer()
                }
            } else {
                // Show back arrow and navigate up
                button.setImageResource(R.drawable.ic_arrow_back)
                button.contentDescription = getString(R.string.navigate_up)
                button.setOnClickListener {
                    navController.navigateUp()
                }
            }
        }
    }
}
