package com.example.learnlog.ui.settings

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.learnlog.R
import com.example.learnlog.databinding.SettingsAccountBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AccountSettingsFragment : Fragment() {
    private var _binding: SettingsAccountBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels({ requireParentFragment() })

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Take persistable URI permission
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Permission not needed for some URIs
            }

            viewModel.updateAvatarUri(it.toString())
            loadAvatar(it.toString())

            Snackbar.make(binding.root, "Avatar updated", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeData()
    }

    private fun setupViews() {
        binding.btnChangeAvatar.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.displayName.collect { name ->
                if (binding.editDisplayName.text.toString() != name) {
                    binding.editDisplayName.setText(name)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.email.collect { email ->
                if (binding.editEmail.text.toString() != email) {
                    binding.editEmail.setText(email)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.avatarUri.collect { uri ->
                uri?.let { loadAvatar(it) }
            }
        }
    }

    private fun loadAvatar(uriString: String) {
        try {
            Glide.with(this)
                .load(Uri.parse(uriString))
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(binding.profileAvatar)
        } catch (e: Exception) {
            // Fallback to default
            binding.profileAvatar.setImageResource(R.drawable.ic_person)
        }
    }

    private fun saveProfile() {
        val displayName = binding.editDisplayName.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()

        // Validate display name
        if (displayName.isEmpty()) {
            binding.displayNameLayout.error = "Display name is required"
            return
        }

        // Validate email format if provided
        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Invalid email format"
            return
        }

        // Clear errors
        binding.displayNameLayout.error = null
        binding.emailLayout.error = null

        // Save to preferences
        viewModel.updateProfile(displayName, email)

        Snackbar.make(binding.root, "Profile updated successfully", Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

