package com.yarnspace.app.feature.auth.presentation

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.yarnspace.app.MainActivity
import com.yarnspace.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SigninFragment : Fragment(R.layout.fragment_signin) {

    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val usernameInput = view.findViewById<TextInputEditText>(R.id.etSigninUsername)
        val emailInput = view.findViewById<TextInputEditText>(R.id.etSigninEmail)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.etSigninPassword)
        val confirmInput = view.findViewById<TextInputEditText>(R.id.etSigninConfirmPassword)
        val createAccountButton = view.findViewById<Button>(R.id.btnCreateAccount)
        val goToLogin = view.findViewById<TextView>(R.id.tvGoToLogin)

        createAccountButton.setOnClickListener {
            val username = usernameInput.trimmedText()
            val email = emailInput.trimmedText()
            val password = passwordInput.rawText()
            val confirm = confirmInput.rawText()

            when {
                username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty() -> {
                    Snackbar.make(view, getString(R.string.error_fill_all_fields), Snackbar.LENGTH_SHORT).show()
                }
                username.length < 3 -> {
                    Snackbar.make(view, getString(R.string.error_username_length), Snackbar.LENGTH_SHORT).show()
                }
                username.contains('@') -> {
                    Snackbar.make(view, getString(R.string.error_username_is_email), Snackbar.LENGTH_SHORT).show()
                }
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    Snackbar.make(view, getString(R.string.error_invalid_email), Snackbar.LENGTH_SHORT).show()
                }
                password.length < 8 -> {
                    Snackbar.make(view, getString(R.string.error_password_length), Snackbar.LENGTH_SHORT).show()
                }
                password != confirm -> {
                    Snackbar.make(view, getString(R.string.error_passwords_not_match), Snackbar.LENGTH_SHORT).show()
                }
                else -> { viewModel.register(username, email, password) }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is AuthUiEvent.LoginSuccess -> {
                            startActivity(Intent(requireContext(), MainActivity::class.java))
                            requireActivity().finish()
                        }
                        is AuthUiEvent.Error -> { Snackbar.make(view, event.message, Snackbar.LENGTH_LONG).show() }
                    }
                }
            }
        }

        goToLogin.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}