package com.yarnspace.app.feature.feed.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.yarnspace.app.R
import com.yarnspace.app.feature.feed.data.RemoteFeedRepository
import kotlinx.coroutines.launch

class FeedFragment : Fragment() {
    private lateinit var viewModel: FeedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = RemoteFeedRepository(requireContext())
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FeedViewModel(repository) as T
            }
        })[FeedViewModel::class.java]

        val adapter = FeedAdapter(
            onProjectClick = { project ->
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
            }
        )

        view.findViewById<RecyclerView>(R.id.rvFeed).adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.feedItems.collect { items ->
                adapter.submitList(items)
            }
        }

        viewModel.refreshFeed()
    }
}
