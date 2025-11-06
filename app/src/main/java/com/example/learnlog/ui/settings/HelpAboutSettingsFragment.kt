package com.example.learnlog.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.learnlog.BuildConfig
import com.example.learnlog.databinding.SettingsHelpAboutBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HelpAboutSettingsFragment : Fragment() {
    private var _binding: SettingsHelpAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsHelpAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        // Set version info
        binding.tvVersion.text = BuildConfig.VERSION_NAME
        binding.tvBuild.text = BuildConfig.VERSION_CODE.toString()

        binding.linkPrivacy.setOnClickListener {
            openUrl("https://learnlog.app/privacy")
        }

        binding.linkContact.setOnClickListener {
            sendEmail()
        }

        binding.linkRate.setOnClickListener {
            rateApp()
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Unable to open link", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun sendEmail() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@learnlog.app")
                putExtra(Intent.EXTRA_SUBJECT, "LearnLog Support")
                putExtra(Intent.EXTRA_TEXT, "Version: ${BuildConfig.VERSION_NAME}\nBuild: ${BuildConfig.VERSION_CODE}\n\n")
            }
            startActivity(Intent.createChooser(intent, "Send Email"))
        } catch (e: Exception) {
            Snackbar.make(binding.root, "No email app found", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun rateApp() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${requireContext().packageName}"))
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to web version
            openUrl("https://play.google.com/store/apps/details?id=${requireContext().packageName}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

