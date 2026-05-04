package com.yarnspace.app.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yarnspace.app.data.user.UserRepository
import com.yarnspace.app.domain.feed.UserSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel(private val repository: UserRepository) : ViewModel() {

    private val _searchResults = MutableLiveData<List<UserSummary>>()
    val searchResults: LiveData<List<UserSummary>> = _searchResults

    private var searchJob: Job? = null

    fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()

        val sanitizedQuery = query.trim().removePrefix("@")

        if (sanitizedQuery.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            val results = repository.searchUsers(sanitizedQuery)
            _searchResults.value = results
        }
    }
}
