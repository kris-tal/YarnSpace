package com.yarnspace.app.feature.auth.presentation

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.yarnspace.app.MainActivity
import com.yarnspace.app.R
import com.yarnspace.app.data.RegisterRequest
import com.yarnspace.app.core.auth.TokenManager
import com.yarnspace.app.core.network.RetrofitClient
import kotlinx.coroutines.launch

class SigninFragment : Fragment(R.layout.fragment_signin) {

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
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    Snackbar.make(view, getString(R.string.error_invalid_email), Snackbar.LENGTH_SHORT).show()
                }
                password.length < 6 -> {    //backend expects min 6 as per schemas.py
                    Snackbar.make(view, getString(R.string.error_password_length), Snackbar.LENGTH_SHORT).show()
                }
                password != confirm -> {
                    Snackbar.make(view, getString(R.string.error_passwords_not_match), Snackbar.LENGTH_SHORT).show()
                }
                else -> {
                    registerUser(view, username, email, password)
                }
            }
        }

        goToLogin.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun registerUser(view: View, username: String, email: String, pass: String) {
        lifecycleScope.launch {
            try {
                val request = RegisterRequest(
                    username = username,
                    email = email,
                    displayName = username,
                    password = pass
                )
                val apiService = RetrofitClient.getInstance(requireContext())
                val response = apiService.register(request)

                TokenManager.saveToken(requireContext(), response.accessToken)

                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish()
            } catch (e: Exception) {
                val message = "Registration failed: ${e.localizedMessage ?: "Unknown error"}"
                Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}

