package com.example.learnlog.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.learnlog.R
import com.example.learnlog.databinding.FragmentSettingsBinding
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.settingsViewpager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 5
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> AccountSettingsFragment()
                1 -> PreferencesSettingsFragment()
                2 -> NotificationsSettingsFragment()
                3 -> DataBackupSettingsFragment()
                else -> HelpAboutSettingsFragment()
            }
        }
        val titles = listOf(
            R.string.settings_account, R.string.settings_preferences, R.string.settings_notifications,
            R.string.settings_data_backup, R.string.settings_help
        )
        TabLayoutMediator(binding.settingsTabs, binding.settingsViewpager) { tab, position ->
            tab.setText(titles[position])
        }.attach()

        if (savedInstanceState == null) {
            binding.settingsViewpager.setCurrentItem(arguments?.getInt("section", 0) ?: 0, false)
        }
        binding.settingsViewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = viewModel.saveLastSection(position).let { }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
