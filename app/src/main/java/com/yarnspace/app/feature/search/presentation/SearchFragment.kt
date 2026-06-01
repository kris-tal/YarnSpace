package com.yarnspace.app.feature.search.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.search.SearchView
import com.yarnspace.app.R
import com.yarnspace.app.feature.feed.presentation.FeedAdapter
import com.yarnspace.app.feature.feed.presentation.ProjectDetailsFragment
import com.yarnspace.app.feature.profile.presentation.ProfileFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private val viewModel: SearchViewModel by viewModels()

    private lateinit var userAdapter: UserSearchAdapter
    private lateinit var projectAdapter: FeedAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchView = view.findViewById<SearchView>(R.id.searchView)
        val recyclerView = view.findViewById<RecyclerView>(R.id.searchResultsRecycler)
        val emptyStateText = view.findViewById<TextView>(R.id.searchEmptyState)

        val chipGroupDateFilter = view.findViewById<ChipGroup>(R.id.chipGroupDateFilter)
        val chipDate24h = view.findViewById<Chip>(R.id.chipDate24h)
        val chipDate7d = view.findViewById<Chip>(R.id.chipDate7d)
        val chipDate30d = view.findViewById<Chip>(R.id.chipDate30d)
        val cbHasPattern = view.findViewById<MaterialCheckBox>(R.id.cbHasPattern)

        var ignoreChipCallback = false
        var ignorePatternCallback = false

        userAdapter = UserSearchAdapter { user ->
            val fragment = ProfileFragment.newPublicInstance(user)

            searchView.hide()
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        projectAdapter = FeedAdapter(
            onProjectClick = { project ->
                searchView.hide()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, ProjectDetailsFragment.newInstance(project))
                    .addToBackStack(null)
                    .commit()
            },
            onReblogClick = { project ->
                viewModel.reblogProject(project)
            },
            onSaveClick = { project ->
                viewModel.toggleSaveProject(project)
            },
            onAuthorClick = { author ->
                val profileFragment = ProfileFragment.newPublicInstance(author)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, profileFragment)
                    .addToBackStack(null)
                    .commit()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = projectAdapter

        cbHasPattern.setOnCheckedChangeListener { _, isChecked ->
            if (ignorePatternCallback) return@setOnCheckedChangeListener
            viewModel.onHasPatternOnlyChanged(isChecked)
        }

        chipGroupDateFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (ignoreChipCallback) return@setOnCheckedStateChangeListener
            val checkedId = checkedIds.firstOrNull() ?: -1
            val filter = when (checkedId) {
                chipDate24h.id -> SearchViewModel.DateAddedFilter.LAST_24H
                chipDate7d.id -> SearchViewModel.DateAddedFilter.LAST_7D
                chipDate30d.id -> SearchViewModel.DateAddedFilter.LAST_30D
                else -> SearchViewModel.DateAddedFilter.ANY
            }
            viewModel.onDateAddedFilterChanged(filter)
        }

        searchView.editText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString()
                viewModel.onSearchQueryChanged(query)
                true
            } else {
                false
            }
        }

        searchView.editText.addTextChangedListener {
            val query = it?.toString() ?: ""
            viewModel.onSearchQueryChanged(query)

            val sanitized = query.trim().removePrefix("@")
            if (sanitized.isEmpty()) {
                emptyStateText.visibility = View.VISIBLE
                emptyStateText.setText(R.string.search_empty_state)
                recyclerView.visibility = View.GONE
            } else {
                if (emptyStateText.text == getString(R.string.search_empty_state)) {
                    emptyStateText.visibility = View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        val filtersVisible = state.mode == SearchViewModel.SearchMode.PROJECTS
                        chipGroupDateFilter.visibility = if (filtersVisible) View.VISIBLE else View.GONE
                        cbHasPattern.visibility = if (filtersVisible) View.VISIBLE else View.GONE

                        // Keep chip selection in sync with state.
                        // None selected == ANY.
                        ignoreChipCallback = true
                        when (state.dateAddedFilter) {
                            SearchViewModel.DateAddedFilter.ANY -> {
                                chipDate24h.isChecked = false
                                chipDate7d.isChecked = false
                                chipDate30d.isChecked = false
                            }

                            SearchViewModel.DateAddedFilter.LAST_24H -> chipDate24h.isChecked = true
                            SearchViewModel.DateAddedFilter.LAST_7D -> chipDate7d.isChecked = true
                            SearchViewModel.DateAddedFilter.LAST_30D -> chipDate30d.isChecked = true
                        }
                        ignoreChipCallback = false

                        if (cbHasPattern.isChecked != state.hasPatternOnly) {
                            ignorePatternCallback = true
                            cbHasPattern.isChecked = state.hasPatternOnly
                            ignorePatternCallback = false
                        }

                        when (state.mode) {
                            SearchViewModel.SearchMode.USERS -> {
                                if (recyclerView.adapter !== userAdapter) recyclerView.adapter = userAdapter
                                userAdapter.submitList(state.userResults)
                            }

                            SearchViewModel.SearchMode.PROJECTS -> {
                                if (recyclerView.adapter !== projectAdapter) recyclerView.adapter = projectAdapter
                                projectAdapter.submitList(state.projectResults)
                            }
                        }

                        val sanitizedQuery = state.query.trim().removePrefix("@")
                        val resultsEmpty = when (state.mode) {
                            SearchViewModel.SearchMode.USERS -> state.userResults.isEmpty()
                            SearchViewModel.SearchMode.PROJECTS -> state.projectResults.isEmpty()
                        }

                        when {
                            sanitizedQuery.isEmpty() -> {
                                emptyStateText.visibility = View.VISIBLE
                                emptyStateText.setText(R.string.search_empty_state)
                                recyclerView.visibility = View.GONE
                            }

                            resultsEmpty -> {
                                emptyStateText.visibility = View.VISIBLE
                                val noResString = when (state.mode) {
                                    SearchViewModel.SearchMode.USERS -> R.string.search_no_users_results
                                    SearchViewModel.SearchMode.PROJECTS -> R.string.search_no_projects_results
                                }
                                emptyStateText.text = getString(noResString, sanitizedQuery)
                                recyclerView.visibility = View.GONE
                            }

                            else -> {
                                emptyStateText.visibility = View.GONE
                                recyclerView.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is SearchViewModel.SearchUiEvent.Error -> {
                                Snackbar.make(view, event.message, Snackbar.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }
}
