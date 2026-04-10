package com.yarnspace.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

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
                    // TODO: backend login call
                    startActivity(Intent(requireContext(), MainActivity::class.java))
                    requireActivity().finish()
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
