package com.example.learnlog.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.learnlog.R
import com.example.learnlog.databinding.FragmentSettingsBinding
import com.example.learnlog.ui.base.BaseFragment
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : BaseFragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupHeader()
        setupViewPager()

        // Navigate to specific section if passed in arguments
        arguments?.getInt("section", 0)?.let { section ->
            binding.settingsViewpager.setCurrentItem(section, false)
        }
    }

    private fun setupHeader() {
        view?.findViewById<TextView>(R.id.page_title)?.text = getString(R.string.page_settings_title)
    }

    private fun setupViewPager() {
        val adapter = SettingsPagerAdapter(this)
        binding.settingsViewpager.adapter = adapter

        TabLayoutMediator(binding.settingsTabs, binding.settingsViewpager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.settings_account)
                1 -> getString(R.string.settings_preferences)
                2 -> getString(R.string.settings_notifications)
                3 -> getString(R.string.settings_data_backup)
                4 -> getString(R.string.settings_help)
                else -> ""
            }
        }.attach()

        // Save last opened section
        binding.settingsViewpager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.saveLastSection(position)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class SettingsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 5

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> AccountSettingsFragment()
                1 -> PreferencesSettingsFragment()
                2 -> NotificationsSettingsFragment()
                3 -> DataBackupSettingsFragment()
                4 -> HelpAboutSettingsFragment()
                else -> AccountSettingsFragment()
            }
        }
    }
}

