package com.example.learnlog.ui.settings

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.learnlog.MainActivity
import com.example.learnlog.R
import com.example.learnlog.databinding.FragmentSettingsPageBinding
import com.example.learnlog.databinding.SettingsAccountHeaderBinding
import com.example.learnlog.ui.common.ProfileAvatar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@AndroidEntryPoint
class AccountSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsPageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels({ requireParentFragment() })

    private lateinit var header: SettingsAccountHeaderBinding
    private lateinit var nameRow: SettingsBuilder.Row
    private var removePhotoRow: SettingsBuilder.Row? = null
    private var localAvatar: String? = null

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { savePhoto(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val builder = SettingsBuilder(binding.pageContainer)
        val user = viewModel.authManager.currentUser

        header = SettingsAccountHeaderBinding.inflate(layoutInflater, binding.pageContainer, false)
        builder.custom(header.root)
        header.headerEmail.text = user?.email.orEmpty()
        header.btnChangePhoto.setOnClickListener { choosePhoto() }
        header.avatarContainer.setOnClickListener { choosePhoto() }

        builder.group(getString(R.string.settings_account)) {
            nameRow = row(R.drawable.ic_person, getString(R.string.account_name), value = displayName()) { editName() }
            row(R.drawable.ic_mail, getString(R.string.account_email), value = user?.email.orEmpty(), chevron = false)
            row(R.drawable.ic_lock, getString(R.string.account_reset_password), getString(R.string.account_reset_password_sub)) { resetPassword() }
        }
        builder.group(null) {
            removePhotoRow = row(R.drawable.ic_delete, getString(R.string.account_remove_photo)) {
                viewModel.updateAvatarUri(null)
                File(requireContext().filesDir, AVATAR_FILE).delete()
            }
            row(R.drawable.ic_logout, getString(R.string.drawer_sign_out), destructive = true) {
                (activity as? MainActivity)?.confirmSignOut()
            }
        }

        renderHeader()
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.avatarUri.collect { uri ->
                localAvatar = uri
                removePhotoRow?.binding?.root?.visibility = if (uri != null) View.VISIBLE else View.GONE
                renderHeader()
            }
        }
    }

    private fun displayName(): String =
        viewModel.authManager.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: getString(R.string.guest_name)

    private fun renderHeader() {
        val user = viewModel.authManager.currentUser
        header.headerName.text = displayName()
        ProfileAvatar.bind(header.avatarInitials, header.avatarPhoto, displayName(), localAvatar ?: user?.photoUrl?.toString())
    }

    private fun choosePhoto() {
        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    /** Copies the picked image into app storage so it keeps working after restarts. */
    private fun savePhoto(source: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    val file = File(requireContext().filesDir, AVATAR_FILE)
                    requireContext().contentResolver.openInputStream(source)!!.use { input ->
                        file.outputStream().use { input.copyTo(it) }
                    }
                    // Cache-busting query so image loaders pick up a replaced photo
                    Uri.fromFile(file).buildUpon().appendQueryParameter("v", System.currentTimeMillis().toString()).build().toString()
                }.getOrNull()
            }
            if (saved != null) {
                com.bumptech.glide.Glide.get(requireContext()).clearMemory()
                viewModel.updateAvatarUri(saved)
                snack(getString(R.string.account_photo_updated))
            }
        }
    }

    private fun editName() {
        val layout = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.account_name)
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
        }
        val input = TextInputEditText(layout.context).apply {
            setText(viewModel.authManager.currentUser?.displayName.orEmpty())
            setSingleLine()
            selectAll()
        }
        layout.addView(input)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.account_name)
            .setView(layout)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.updateDisplayName(name).onSuccess {
                        nameRow.value = name
                        renderHeader()
                        snack(getString(R.string.account_saved))
                    }.onFailure { snack(it.localizedMessage ?: getString(R.string.auth_offline)) }
                }
            }
            .show()
        input.requestFocus()
    }

    private fun resetPassword() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sendPasswordReset()
                .onSuccess { snack(getString(R.string.auth_reset_sent)) }
                .onFailure { snack(it.localizedMessage ?: getString(R.string.auth_offline)) }
        }
    }

    private fun snack(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        removePhotoRow = null
    }

    companion object {
        private const val AVATAR_FILE = "avatar.jpg"
    }
}
