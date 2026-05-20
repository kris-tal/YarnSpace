package com.yarnspace.app.feature.auth.presentation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.yarnspace.app.MainActivity
import com.yarnspace.app.R
import com.yarnspace.app.core.auth.TokenManager
import com.yarnspace.app.core.network.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.lang.Exception

class LoginFragment : Fragment(R.layout.fragment_login) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val identifierInput = view.findViewById<TextInputEditText>(R.id.etLoginIdentifier)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.etLoginPassword)
        val loginButton = view.findViewById<Button>(R.id.btnLogin)
        val goToSignin = view.findViewById<TextView>(R.id.tvGoToSignin)

        loginButton.setOnClickListener {
            val identifier = identifierInput.trimmedText()
            val password = passwordInput.rawText()

            when {
                identifier.isBlank() || password.isBlank() ->
                    Snackbar.make(view, getString(R.string.error_fill_all_fields), Snackbar.LENGTH_SHORT).show()
                else -> {
                    lifecycleScope.launch {
                        try {
                            val apiService = RetrofitClient.getInstance(requireContext())
                            val authResponse = apiService.login(identifier, password)
                            TokenManager.saveToken(requireContext(), authResponse.accessToken)
                            startActivity(Intent(requireContext(), MainActivity::class.java))
                            requireActivity().finish()
                        } catch (e: Exception) {
                            val message = when (e) {
                                is HttpException -> {
                                    if (e.code() == 401) "Invalid credentials"
                                    else "Login failed: ${e.message()}"
                                }
                                else -> "Login failed: ${e.message}"
                            }
                            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        goToSignin.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.auth_container, SigninFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}

