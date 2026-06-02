package com.yarnspace.app.feature.notifs.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yarnspace.app.feature.notifs.data.NotifsRepository
import com.yarnspace.app.feature.notifs.domain.model.Notif
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotifsViewModel @Inject constructor(
    private val repository: NotifsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotifsUiState(isLoading = true))
    val uiState: StateFlow<NotifsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<NotifsUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    data class NotifsUiState(
        val items: List<Notif> = emptyList(),
        val isLoading: Boolean = false,
    )

    sealed interface NotifsUiEvent {
        data class Error(val message: String) : NotifsUiEvent
    }

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.getNotifications()
                .catch { e ->
                    e.printStackTrace()
                    _events.tryEmit(NotifsUiEvent.Error(e.message ?: "Failed to load notifications"))
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .collect { notifications ->
                    _uiState.value = NotifsUiState(
                        items = notifications,
                        isLoading = false
                    )
                }
        }
    }

    fun markAsViewed() {
        viewModelScope.launch {
            repository.markAllRead()
        }
    }
}