package com.example.learnlog.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.learnlog.R
import com.example.learnlog.databinding.FragmentLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.let { viewModel.signInWithGoogle(it) }
            } catch (e: ApiException) {
                Toast.makeText(requireContext(), "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeAuthState()
    }

    private fun setupClickListeners() {
        binding.signInBtn.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()
            viewModel.signInWithEmail(email, password)
        }

        binding.googleSignInBtn.setOnClickListener {
            signInWithGoogle()
        }

        binding.signUpBtn.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.forgotPasswordBtn.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            if (email.isNotEmpty()) {
                viewModel.sendPasswordResetEmail(email)
            } else {
                Toast.makeText(requireContext(), "Please enter your email", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeAuthState() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Success -> {
                    Toast.makeText(requireContext(), "Welcome back!", Toast.LENGTH_SHORT).show()
                    // Navigate to main app
                    findNavController().navigate(R.id.action_loginFragment_to_tasksFragment)
                }
                is AuthState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    viewModel.resetAuthState()
                }
                is AuthState.PasswordResetSent -> {
                    Toast.makeText(requireContext(), "Password reset email sent!", Toast.LENGTH_SHORT).show()
                    viewModel.resetAuthState()
                }
                else -> {
                    // Idle or other states
                }
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.signInBtn.isEnabled = !isLoading
            binding.googleSignInBtn.isEnabled = !isLoading
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

