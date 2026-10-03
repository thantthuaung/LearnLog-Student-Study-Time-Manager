package com.example.learnlog.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.learnlog.BuildConfig
import com.example.learnlog.R
import com.example.learnlog.databinding.FragmentSettingsPageBinding
import com.example.learnlog.databinding.SettingsAboutHeaderBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HelpAboutSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val builder = SettingsBuilder(binding.pageContainer)

        val header = SettingsAboutHeaderBinding.inflate(layoutInflater, binding.pageContainer, false)
        header.tvVersion.text = getString(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        builder.custom(header.root)

        builder.group(getString(R.string.about_tips_title)) {
            row(R.drawable.ic_checklist, getString(R.string.about_tip_1), chevron = false)
            row(R.drawable.ic_target, getString(R.string.about_tip_2), chevron = false)
            row(R.drawable.ic_timelapse, getString(R.string.about_tip_3), chevron = false)
        }
        builder.group(null) {
            row(R.drawable.ic_mail, getString(R.string.about_contact)) { sendEmail() }
            row(R.drawable.ic_privacy, getString(R.string.about_privacy)) { openUrl(getString(R.string.url_privacy_policy)) }
            row(R.drawable.ic_star, getString(R.string.about_rate)) { rateApp() }
        }
    }

    private fun openUrl(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { snack(getString(R.string.open_link_failed)) }
    }

    private fun sendEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${getString(R.string.support_email)}")
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject))
            putExtra(Intent.EXTRA_TEXT, getString(R.string.email_body_template, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toString()))
        }
        runCatching { startActivity(intent) }.onFailure { snack(getString(R.string.no_email_app)) }
    }

    private fun rateApp() {
        val pkg = requireContext().packageName
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))) }
            .onFailure { openUrl("https://play.google.com/store/apps/details?id=$pkg") }
    }

    private fun snack(message: String) = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
