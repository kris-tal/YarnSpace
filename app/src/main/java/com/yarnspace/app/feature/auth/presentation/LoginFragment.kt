package com.yarnspace.app.feature.auth.presentation

import android.content.Intent
import android.os.Bundle
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
class LoginFragment : Fragment(R.layout.fragment_login) {
    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val identifierInput = view.findViewById<TextInputEditText>(R.id.etLoginIdentifier)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.etLoginPassword)
        val loginButton = view.findViewById<Button>(R.id.btnLogin)
        val goToSignin = view.findViewById<TextView>(R.id.tvGoToSignin)

        loginButton.setOnClickListener {
            val identifier = identifierInput.trimmedText()
            val password = passwordInput.rawText()

            viewModel.login(identifier, password)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is AuthUiEvent.LoginSuccess -> {
                            startActivity(Intent(requireContext(), MainActivity::class.java))
                            requireActivity().finish()
                        }
                        is AuthUiEvent.Error -> {
                            Snackbar.make(view, event.message, Snackbar.LENGTH_SHORT).show()
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