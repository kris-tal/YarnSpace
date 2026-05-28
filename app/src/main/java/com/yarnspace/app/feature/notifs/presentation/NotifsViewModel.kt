package com.yarnspace.app.feature.notifs.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yarnspace.app.feature.notifs.domain.NotifsRepository
import com.yarnspace.app.feature.notifs.domain.model.Notif
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotifsViewModel @Inject constructor(
    private val repository: NotifsRepository,
) : ViewModel() {

    private val _isSyncing = kotlinx.coroutines.flow.MutableStateFlow(false)

    val uiState: StateFlow<NotifsUiState> = combine(
        repository.getNotifications(),
        _isSyncing
    ) { notifications, syncing ->
        NotifsUiState(
            items = notifications,
            isLoading = syncing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotifsUiState()
    )

    data class NotifsUiState(
        val items: List<Notif> = emptyList(),
        val isLoading: Boolean = false,
    )

    sealed interface NotifsUiEvent {
        data class Error(val message: String) : NotifsUiEvent
    }

    private val _events = MutableSharedFlow<NotifsUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun sync() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.syncNotifications()
            } catch (e: Exception) {
                e.printStackTrace()
                _events.tryEmit(NotifsUiEvent.Error(e.message ?: "Failed to sync"))
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun markAsViewed() {
        viewModelScope.launch {
            repository.markAllRead()
        }
    }
}
