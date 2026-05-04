package com.yarnspace.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.search.SearchView
import com.yarnspace.app.data.user.RemoteUserRepository
import com.yarnspace.app.retrofit.RetrofitClient
import com.yarnspace.app.ui.search.SearchViewModel
import com.yarnspace.app.ui.search.UserSearchAdapter

class SearchFragment : Fragment() {

    private val viewModel: SearchViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val apiService = RetrofitClient.getInstance(requireContext())
                val repository = RemoteUserRepository(apiService)
                @Suppress("UNCHECKED_CAST")
                return SearchViewModel(repository) as T
            }
        }
    }

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
            // TODO: navigate to user profile
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
