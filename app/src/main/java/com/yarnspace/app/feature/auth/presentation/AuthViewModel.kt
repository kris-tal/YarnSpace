package com.yarnspace.app.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yarnspace.app.core.auth.TokenManager
import com.yarnspace.app.core.network.ApiService
import com.yarnspace.app.data.RegisterRequest
import com.yarnspace.app.core.theme.settings.ThemeSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

sealed interface AuthUiEvent {
    data object LoginSuccess : AuthUiEvent
    data class Error(val message: String) : AuthUiEvent
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val themeSettingsRepository: ThemeSettingsRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<AuthUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthUiEvent> = _events.asSharedFlow()

    fun login(identifier: String, pass: String) {
        if (identifier.isBlank() || pass.isBlank()) {
            _events.tryEmit(AuthUiEvent.Error("Username and password cannot be empty"))
            return
        }

        viewModelScope.launch {
            try {
                val response = apiService.login(identifier, pass)


                tokenManager.saveToken(response.accessToken)
                tokenManager.saveUsername(response.user.username)
                themeSettingsRepository.setAccentColorName(response.user.accentColor)

                _events.tryEmit(AuthUiEvent.LoginSuccess)

            } catch (e: Exception) {
                e.printStackTrace()
                _events.tryEmit(AuthUiEvent.Error(e.message ?: "Login failed."))
            }
        }
    }

    fun register(username: String, email: String, pass: String) {
        viewModelScope.launch {
            try {
                val request = RegisterRequest(
                    username = username,
                    email = email,
                    displayName = username,
                    password = pass
                )
                val response = apiService.register(request)

                tokenManager.saveToken(response.accessToken)
                tokenManager.saveUsername(response.user.username)
                themeSettingsRepository.setAccentColorName(response.user.accentColor)

                _events.tryEmit(AuthUiEvent.LoginSuccess)
            } catch (e: HttpException) {
                e.printStackTrace()
                if (e.code() == 409) {
                    _events.tryEmit(AuthUiEvent.Error("Username taken."))
                } else {
                    _events.tryEmit(AuthUiEvent.Error("Server error: ${e.code()}"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _events.tryEmit(AuthUiEvent.Error(e.message ?: "Registration failed."))
            }
        }
    }
}