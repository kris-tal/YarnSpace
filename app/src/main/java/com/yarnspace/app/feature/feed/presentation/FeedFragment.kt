package com.yarnspace.app.feature.feed.presentation

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
import com.yarnspace.app.core.audio.UiSoundManager
import com.yarnspace.app.core.auth.TokenManager
import com.yarnspace.app.feature.profile.presentation.ProfileFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FeedFragment : Fragment() {
    private val viewModel: FeedViewModel by viewModels()

    @Inject
    lateinit var tokenManager: TokenManager
    @Inject
    lateinit var soundManager: UiSoundManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = FeedAdapter(
            currentUsername = tokenManager.getUsername(),
            onProjectClick = { project ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, ProjectDetailsFragment.newInstance(project))
                    .addToBackStack(null)
                    .commit()
            },
            onReblogClick = { project ->
                soundManager.play(soundManager.soundReblog)
                soundManager.play(soundManager.soundReblog)

                viewModel.reblogProject(project)
            },
            onSaveClick = { project ->

                if (project.isSavedByMe) {
                    soundManager.play(soundManager.soundUnsave)
                } else {
                    soundManager.play(soundManager.soundSave)
                }

                viewModel.toggleSaveProject(project)
            },
            onAuthorClick = { author ->
                val profileFragment = ProfileFragment.newPublicInstance(author)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, profileFragment)
                    .addToBackStack(null)
                    .commit()
            },
            onDeleteClick = { itemToDelete ->
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete post")
                    .setMessage("Are you sure you want to delete this? This action cannot be undone.")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteItem(itemToDelete)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        view.findViewById<RecyclerView>(R.id.rvFeed).adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        adapter.submitList(state.items)
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is FeedViewModel.FeedUiEvent.Error -> {
                                Snackbar.make(view, event.message, Snackbar.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        viewModel.refreshFeed()
    }
}