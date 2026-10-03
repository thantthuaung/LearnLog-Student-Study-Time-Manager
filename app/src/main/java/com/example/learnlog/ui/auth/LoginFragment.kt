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
import com.example.learnlog.databinding.FragmentLoginBinding
import com.example.learnlog.ui.common.Motion
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    @Inject lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        AuthNavigation.handleGoogleResult(it, viewModel)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeAuthState()
        if (savedInstanceState == null) {
            Motion.staggerIn(listOf(binding.logo, binding.formCard, binding.footer), startDelay = 80, step = 90)
        }
    }

    private fun setupClickListeners() = with(binding) {
        signInBtn.setOnClickListener { submit() }
        passwordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { submit(); true } else false
        }
        emailInput.doAfterTextChanged { emailLayout.error = null }
        passwordInput.doAfterTextChanged { passwordLayout.error = null }

        googleSignInBtn.setOnClickListener {
            googleSignInLauncher.launch(AuthNavigation.googleIntent(googleSignInClient))
        }
        signUpBtn.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
        forgotPasswordBtn.setOnClickListener {
            viewModel.sendPasswordResetEmail(emailInput.text.toString().trim())
        }
    }

    private fun submit() {
        viewModel.signInWithEmail(
            binding.emailInput.text.toString().trim(),
            binding.passwordInput.text.toString()
        )
    }

    private fun observeAuthState() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Success -> AuthNavigation.enterApp(this, state.user, R.id.action_loginFragment_to_tasksFragment)
                is AuthState.Error -> {
                    val message = if (state.messageRes != 0) getString(state.messageRes) else state.message.orEmpty()
                    when (state.field) {
                        AuthField.EMAIL -> binding.emailLayout.error = message
                        AuthField.PASSWORD -> binding.passwordLayout.error = message
                        else -> Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                    }
                    viewModel.resetAuthState()
                }
                is AuthState.PasswordResetSent -> {
                    Snackbar.make(binding.root, R.string.auth_reset_sent, Snackbar.LENGTH_LONG).show()
                    viewModel.resetAuthState()
                }
                else -> Unit
            }
        }
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
            binding.signInBtn.isEnabled = !loading
            binding.signInBtn.text = if (loading) "" else getString(R.string.auth_sign_in)
            binding.googleSignInBtn.isEnabled = !loading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
