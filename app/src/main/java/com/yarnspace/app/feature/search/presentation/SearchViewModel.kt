package com.yarnspace.app.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yarnspace.app.core.model.UserSummary
import com.yarnspace.app.core.model.FeedItem
import com.yarnspace.app.feature.feed.data.FeedRepository
import com.yarnspace.app.feature.search.data.ProjectSearchRepository
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
    private val userRepository: UserRepository,
    private val projectRepository: ProjectSearchRepository,
    private val feedRepository: FeedRepository,
) : ViewModel() {

    enum class SearchMode {
        USERS,
        PROJECTS,
    }

    data class SearchUiState(
        val query: String = "",
        val mode: SearchMode = SearchMode.PROJECTS,
        val isLoading: Boolean = false,
        val userResults: List<UserSummary> = emptyList(),
        val projectResults: List<FeedItem.Project> = emptyList(),
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

        val trimmed = query.trim()
        val mode = if (trimmed.startsWith("@")) SearchMode.USERS else SearchMode.PROJECTS
        val sanitizedQuery = if (mode == SearchMode.USERS) trimmed.removePrefix("@").trim() else trimmed

        _uiState.value = _uiState.value.copy(
            query = query,
            mode = mode,
            isLoading = sanitizedQuery.isNotBlank(),
            userResults = if (sanitizedQuery.isBlank()) emptyList() else _uiState.value.userResults,
            projectResults = if (sanitizedQuery.isBlank()) emptyList() else _uiState.value.projectResults,
        )

        if (sanitizedQuery.isBlank()) {
            return
        }

        val modeAtStart = mode
        val sanitizedAtStart = sanitizedQuery

        searchJob = viewModelScope.launch {
            delay(300)
            try {
                fun stillCurrent(): Boolean {
                    val current = _uiState.value
                    if (current.mode != modeAtStart) return false
                    val currentTrimmed = current.query.trim()
                    val currentSanitized = if (modeAtStart == SearchMode.USERS) {
                        currentTrimmed.removePrefix("@").trim()
                    } else {
                        currentTrimmed
                    }
                    return currentSanitized == sanitizedAtStart
                }

                if (!stillCurrent()) return@launch

                when (modeAtStart) {
                    SearchMode.USERS -> {
                        val results = userRepository.searchUsers(sanitizedAtStart)
                        if (!stillCurrent()) return@launch
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            userResults = results,
                        )
                    }

                    SearchMode.PROJECTS -> {
                        val results = projectRepository.searchProjects(sanitizedAtStart)
                        if (!stillCurrent()) return@launch
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            projectResults = results,
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userResults = emptyList(),
                    projectResults = emptyList(),
                )
                _events.tryEmit(SearchUiEvent.Error(e.message ?: "Search failed"))
            }
        }
    }

    fun toggleSaveProject(project: FeedItem.Project) {
        viewModelScope.launch {
            val result = if (project.isSavedByMe) {
                feedRepository.unsaveProject(project.id)
            } else {
                feedRepository.saveProject(project.id)
            }

            result.onSuccess {
                updateProjectSavedState(project.id, !project.isSavedByMe)
            }.onFailure {
                it.printStackTrace()
                _events.tryEmit(SearchUiEvent.Error(it.message ?: "Failed to update saved state"))
            }
        }
    }

    fun reblogProject(project: FeedItem.Project) {
        if (project.isRebloggedByMe) return

        viewModelScope.launch {
            feedRepository.reblogProject(project.id)
                .onSuccess {
                    updateProjectRebloggedState(project.id, true)
                }
                .onFailure {
                    it.printStackTrace()
                    _events.tryEmit(SearchUiEvent.Error(it.message ?: "Failed to reblog project"))
                }
        }
    }

    private fun updateProjectSavedState(projectId: Long, isSaved: Boolean) {
        _uiState.value = _uiState.value.copy(
            projectResults = _uiState.value.projectResults.map { p ->
                if (p.id == projectId) p.copy(isSavedByMe = isSaved) else p
            }
        )
    }

    private fun updateProjectRebloggedState(projectId: Long, isReblogged: Boolean) {
        _uiState.value = _uiState.value.copy(
            projectResults = _uiState.value.projectResults.map { p ->
                if (p.id == projectId) p.copy(isRebloggedByMe = isReblogged) else p
            }
        )
    }
}

