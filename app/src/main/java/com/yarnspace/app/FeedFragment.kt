package com.yarnspace.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.yarnspace.app.ui.feed.FeedAdapter
import com.yarnspace.app.ui.feed.FeedViewModel

class FeedFragment : Fragment() {
    private val viewModel by lazy { FeedViewModel() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = FeedAdapter(
            onProjectClick = { project ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, ProjectDetailsFragment.newInstance(project))
                    .addToBackStack(null)
                    .commit()
            },
        )

        view.findViewById<RecyclerView>(R.id.rvFeed).adapter = adapter
        adapter.submitList(viewModel.loadFeed())
    }
}