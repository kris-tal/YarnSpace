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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.search.SearchView
import com.yarnspace.app.R
import com.yarnspace.app.feature.profile.presentation.ProfileFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private val viewModel: SearchViewModel by viewModels()

    private lateinit var adapter: UserSearchAdapter

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

        adapter = UserSearchAdapter { user ->
            val fragment = ProfileFragment.newPublicInstance(user)

            searchView.hide()
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

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

        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            adapter.submitList(results)

            val queryText = searchView.editText.text.toString()
            val sanitizedQuery = queryText.trim().removePrefix("@")

            when {
                sanitizedQuery.isEmpty() -> {
                    emptyStateText.visibility = View.VISIBLE
                    emptyStateText.setText(R.string.search_empty_state)
                    recyclerView.visibility = View.GONE
                }
                results.isEmpty() -> {
                    emptyStateText.visibility = View.VISIBLE
                    emptyStateText.text = getString(R.string.search_no_results, sanitizedQuery)
                    recyclerView.visibility = View.GONE
                }
                else -> {
                    emptyStateText.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
        }
    }
}
