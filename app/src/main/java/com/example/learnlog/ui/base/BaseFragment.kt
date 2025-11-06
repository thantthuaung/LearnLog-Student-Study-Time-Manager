package com.example.learnlog.ui.base

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.example.learnlog.MainActivity
import com.example.learnlog.R

/**
 * Base fragment that sets up the hamburger button to open the drawer
 */
abstract class BaseFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupHamburgerButton(view)
    }

    private fun setupHamburgerButton(view: View) {
        view.findViewById<ImageButton>(R.id.hamburger_button)?.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }
    }
}

