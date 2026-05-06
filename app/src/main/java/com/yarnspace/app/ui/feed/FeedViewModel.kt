package com.yarnspace.app.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yarnspace.app.data.feed.FeedRepository
import com.yarnspace.app.domain.feed.FeedItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FeedViewModel(private val repository: FeedRepository) : ViewModel() {

    private val _feedItems = MutableStateFlow<List<FeedItem>>(emptyList())
    val feedItems: StateFlow<List<FeedItem>> = _feedItems

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        refreshFeed()
    }

    fun refreshFeed() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _feedItems.value = repository.getFeedItems()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
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
        _feedItems.value = _feedItems.value.map { item ->
            when (item) {
                is FeedItem.Project -> if (item.id == projectId) item.copy(isSavedByMe = isSaved) else item
                is FeedItem.Post -> if (item.rebloggedProject?.id == projectId) {
                    item.copy(rebloggedProject = item.rebloggedProject.copy(isSavedByMe = isSaved))
                } else item
                else -> item
            }
        }
    }

    private fun updateProjectRebloggedState(projectId: Long, isReblogged: Boolean) {
        _feedItems.value = _feedItems.value.map { item ->
            when (item) {
                is FeedItem.Project -> if (item.id == projectId) item.copy(isRebloggedByMe = isReblogged) else item
                is FeedItem.Post -> if (item.rebloggedProject?.id == projectId) {
                    item.copy(rebloggedProject = item.rebloggedProject.copy(isRebloggedByMe = isReblogged))
                } else item
                else -> item
            }
        }
    }
}
