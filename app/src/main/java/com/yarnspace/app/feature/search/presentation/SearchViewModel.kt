package com.yarnspace.app.feature.search.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yarnspace.app.core.model.UserSummary
import com.yarnspace.app.feature.search.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: UserRepository,
) : ViewModel() {

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

