package com.yarnspace.app.feature.feed.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yarnspace.app.core.model.FeedItem
import com.yarnspace.app.feature.feed.data.FeedRepository
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
class FeedViewModel @Inject constructor(
    private val repository: FeedRepository,
) : ViewModel() {

    data class FeedUiState(
        val items: List<FeedItem> = emptyList(),
        val isLoading: Boolean = false,
    )

    sealed interface FeedUiEvent {
        data class Error(val message: String) : FeedUiEvent
    }

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<FeedUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<FeedUiEvent> = _events.asSharedFlow()

    init {
        refreshFeed()
    }

    fun refreshFeed() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                _uiState.value = _uiState.value.copy(items = repository.getFeedItems())
            } catch (e: Exception) {
                e.printStackTrace()
                _events.tryEmit(FeedUiEvent.Error(e.message ?: "Failed to load feed"))
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun deleteItem(item: FeedItem) {
        val previousItems = _uiState.value.items
        _uiState.value = _uiState.value.copy(
            items = previousItems.filterNot { it == item }
        )

        viewModelScope.launch {
            try {
                when (item) {
                    is FeedItem.Project -> {
                        repository.deleteProject(item.id)
                    }
                    is FeedItem.Post -> {
                        repository.deletePost(item.id)
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _events.tryEmit(FeedUiEvent.Error(e.message ?: "Failed to delete item"))
                refreshFeed()
            }
        }
    }

    fun reblogProject(project: FeedItem.Project) {
        viewModelScope.launch {
            repository.reblogProject(project.id).onSuccess {
                updateProjectRebloggedState(project.id, true)
                refreshFeed()
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    fun toggleSaveProject(project: FeedItem.Project) {
        viewModelScope.launch {
            if (project.isSavedByMe) {
                repository.unsaveProject(project.id).onSuccess {
                    updateProjectSavedState(project.id, false)
                }.onFailure { it.printStackTrace() }
            } else {
                repository.saveProject(project.id).onSuccess {
                    updateProjectSavedState(project.id, true)
                }.onFailure { it.printStackTrace() }
            }
        }
    }

    private fun updateProjectSavedState(projectId: Long, isSaved: Boolean) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map { item ->
                when (item) {
                    is FeedItem.Project -> if (item.id == projectId) item.copy(isSavedByMe = isSaved) else item
                    is FeedItem.Post -> if (item.rebloggedProject?.id == projectId) {
                        item.copy(rebloggedProject = item.rebloggedProject.copy(isSavedByMe = isSaved))
                    } else item
                    else -> item
                }
            }
        )
    }

    private fun updateProjectRebloggedState(projectId: Long, isReblogged: Boolean) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map { item ->
                when (item) {
                    is FeedItem.Project -> if (item.id == projectId) item.copy(isRebloggedByMe = isReblogged) else item
                    is FeedItem.Post -> if (item.rebloggedProject?.id == projectId) {
                        item.copy(rebloggedProject = item.rebloggedProject.copy(isRebloggedByMe = isReblogged))
                    } else item
                    else -> item
                }
            }
        )
    }
}