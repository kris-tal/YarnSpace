package com.yarnspace.app.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yarnspace.app.core.model.UserSummary
import com.yarnspace.app.feature.search.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: UserRepository,
) : ViewModel() {

    data class SearchUiState(
        val query: String = "",
        val isLoading: Boolean = false,
        val results: List<UserSummary> = emptyList(),
    )

    sealed interface SearchUiEvent {
        data class Error(val message: String) : SearchUiEvent
    }

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SearchUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SearchUiEvent> = _events.asSharedFlow()

    private var searchJob: Job? = null

    fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()

        val sanitizedQuery = query.trim().removePrefix("@")

        _uiState.value = _uiState.value.copy(
            query = query,
            isLoading = sanitizedQuery.isNotBlank(),
            results = if (sanitizedQuery.isBlank()) emptyList() else _uiState.value.results,
        )

        if (sanitizedQuery.isBlank()) {
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            try {
                val results = repository.searchUsers(sanitizedQuery)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = results,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = emptyList(),
                )
                _events.tryEmit(SearchUiEvent.Error(e.message ?: "Search failed"))
            }
        }
    }
}

