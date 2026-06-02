package com.yarnspace.app.feature.notifs.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.yarnspace.app.R
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotifsFragment : Fragment() {

    private val viewModel: NotifsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = NotifsAdapter()
        view.findViewById<RecyclerView>(R.id.rvNotifs).adapter = adapter

        val emptyView = view.findViewById<View>(R.id.tvNotifsEmpty)
        val loadingView = view.findViewById<View>(R.id.pbNotifsLoading)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        adapter.submitNotifications(state.items)
                        emptyView.visibility = if (!state.isLoading && state.items.isEmpty()) View.VISIBLE else View.GONE
                        loadingView.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is NotifsViewModel.NotifsUiEvent.Error ->
                                Snackbar.make(view, event.message, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.markAsViewed()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadNotifications()
    }
}
