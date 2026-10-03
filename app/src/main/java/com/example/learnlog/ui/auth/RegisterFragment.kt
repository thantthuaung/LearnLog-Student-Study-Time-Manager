package com.example.learnlog.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.learnlog.R
import com.example.learnlog.databinding.FragmentRegisterBinding
import com.example.learnlog.ui.common.Motion
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    @Inject lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        AuthNavigation.handleGoogleResult(it, viewModel)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeAuthState()
        if (savedInstanceState == null) {
            Motion.staggerIn(listOf(binding.formCard, binding.footer), startDelay = 120, step = 90)
        }
    }

    private fun setupClickListeners() = with(binding) {
        backBtn.setOnClickListener { findNavController().navigateUp() }
        signInBtn.setOnClickListener { findNavController().navigateUp() }
        signUpBtn.setOnClickListener { submit() }
        confirmPasswordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { submit(); true } else false
        }
        nameInput.doAfterTextChanged { nameLayout.error = null }
        emailInput.doAfterTextChanged { emailLayout.error = null }
        passwordInput.doAfterTextChanged { passwordLayout.error = null }
        confirmPasswordInput.doAfterTextChanged { confirmPasswordLayout.error = null }
        googleSignInBtn.setOnClickListener {
            googleSignInLauncher.launch(AuthNavigation.googleIntent(googleSignInClient))
        }
    }

    private fun submit() = with(binding) {
        viewModel.signUpWithEmail(
            nameInput.text.toString(),
            emailInput.text.toString().trim(),
            passwordInput.text.toString(),
            confirmPasswordInput.text.toString()
        )
    }

    private fun observeAuthState() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Success -> AuthNavigation.enterApp(this, state.user, R.id.action_registerFragment_to_tasksFragment)
                is AuthState.Error -> {
                    val message = if (state.messageRes != 0) getString(state.messageRes) else state.message.orEmpty()
                    when (state.field) {
                        AuthField.NAME -> binding.nameLayout.error = message
                        AuthField.EMAIL -> binding.emailLayout.error = message
                        AuthField.PASSWORD -> binding.passwordLayout.error = message
                        AuthField.CONFIRM -> binding.confirmPasswordLayout.error = message
                        AuthField.NONE -> Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                    }
                    viewModel.resetAuthState()
                }
                else -> Unit
            }
        }
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
            binding.signUpBtn.isEnabled = !loading
            binding.signUpBtn.text = if (loading) "" else getString(R.string.auth_sign_up)
            binding.googleSignInBtn.isEnabled = !loading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
