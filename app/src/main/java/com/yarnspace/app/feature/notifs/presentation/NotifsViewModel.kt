package com.yarnspace.app.feature.notifs.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yarnspace.app.feature.notifs.domain.NotifsRepository
import com.yarnspace.app.feature.notifs.domain.model.Notif
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotifsViewModel @Inject constructor(
    private val repository: NotifsRepository,
) : ViewModel() {

    data class NotifsUiState(
        val items: List<Notif> = emptyList(),
        val isLoading: Boolean = false,
    )

    sealed interface NotifsUiEvent {
        data class Error(val message: String) : NotifsUiEvent
    }

    private val _uiState = MutableStateFlow(NotifsUiState())
    val uiState: StateFlow<NotifsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<NotifsUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<NotifsUiEvent> = _events.asSharedFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                _uiState.value = _uiState.value.copy(items = repository.listMyNotifications())
            } catch (e: Exception) {
                e.printStackTrace()
                _events.tryEmit(NotifsUiEvent.Error(e.message ?: "Failed to load notifications"))
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            repository.markAllRead()
                .onSuccess { refresh() }
                .onFailure {
                    it.printStackTrace()
                    _events.tryEmit(NotifsUiEvent.Error(it.message ?: "Failed to mark notifications as read"))
                }
        }
    }
}

