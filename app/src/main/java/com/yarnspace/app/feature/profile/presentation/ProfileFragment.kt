package com.yarnspace.app.feature.profile.presentation

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.yarnspace.app.R
import com.yarnspace.app.data.remote.dto.PostReadDto
import com.yarnspace.app.data.remote.dto.ProfilePublicDto
import com.yarnspace.app.data.remote.dto.ProjectReadDto
import com.yarnspace.app.data.remote.dto.UserPublicDto
import com.yarnspace.app.core.model.FeedItem
import com.yarnspace.app.core.model.UserSummary
import com.yarnspace.app.core.network.RetrofitClient
import com.yarnspace.app.feature.feed.data.RemoteFeedRepository
import com.yarnspace.app.feature.feed.presentation.FeedAdapter
import com.yarnspace.app.feature.feed.presentation.FeedViewModel
import com.yarnspace.app.feature.feed.presentation.ProjectDetailsFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private enum class ProfileMode { PRIVATE, PUBLIC }
    private enum class Tab { POSTS, PROJECTS, SAVED }

    companion object {
        private const val ARG_USERNAME = "arg_username"
        private const val ARG_FORCE_ME = "arg_force_me"

        private const val ARG_PREFILL_ID = "arg_prefill_id"
        private const val ARG_PREFILL_USERNAME = "arg_prefill_username"
        private const val ARG_PREFILL_DISPLAY_NAME = "arg_prefill_display_name"
        private const val ARG_PREFILL_AVATAR_URL = "arg_prefill_avatar_url"
        private const val ARG_PREFILL_ACCENT_COLOR = "arg_prefill_accent_color"

        fun newPublicInstance(user: UserSummary): ProfileFragment {
            return ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USERNAME, user.username)
                    putBoolean(ARG_FORCE_ME, false)

                    putLong(ARG_PREFILL_ID, user.id)
                    putString(ARG_PREFILL_USERNAME, user.username)
                    putString(ARG_PREFILL_DISPLAY_NAME, user.displayName)
                    putString(ARG_PREFILL_AVATAR_URL, user.avatarUrl)
                    putString(ARG_PREFILL_ACCENT_COLOR, user.accentColor)
                }
            }
        }

        fun newMeInstance(prefill: UserSummary? = null): ProfileFragment {
            return ProfileFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_FORCE_ME, true)
                    if (prefill != null) {
                        putLong(ARG_PREFILL_ID, prefill.id)
                        putString(ARG_PREFILL_USERNAME, prefill.username)
                        putString(ARG_PREFILL_DISPLAY_NAME, prefill.displayName)
                        putString(ARG_PREFILL_AVATAR_URL, prefill.avatarUrl)
                        putString(ARG_PREFILL_ACCENT_COLOR, prefill.accentColor)
                    }
                }
            }
        }
    }

    private val viewModel: ProfileViewModel by viewModels()

    private val feedViewModel: FeedViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return FeedViewModel(RemoteFeedRepository(requireContext())) as T
            }
        }
    }

    private lateinit var adapter: FeedAdapter
    private var profileMode: ProfileMode = ProfileMode.PRIVATE
    private var loadJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvDisplayName = view.findViewById<TextView>(R.id.profile_display_name)
        val tvUsername = view.findViewById<TextView>(R.id.profile_username_text)
        val ivAvatar = view.findViewById<ImageView>(R.id.ivProfileAvatar)
        val btnBack = view.findViewById<ImageButton>(R.id.btnProfileBack)
        val btnEdit = view.findViewById<ImageButton>(R.id.btnProfileEdit)
        val btnFollow = view.findViewById<MaterialButton>(R.id.btnProfileFollow)
        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.profileToggleGroup)
        val btnProjects = view.findViewById<MaterialButton>(R.id.profile_filter_projects)
        val btnPosts = view.findViewById<MaterialButton>(R.id.profile_filter_posts)
        val btnSaved = view.findViewById<MaterialButton>(R.id.profile_filter_saved)
        val tvFollowersCount = view.findViewById<TextView>(R.id.tvFollowersCount)
        val tvFollowingCount = view.findViewById<TextView>(R.id.tvFollowingCount)
        val tvPostsCount = view.findViewById<TextView>(R.id.tvPostsCount)
        val tvProjectsCount = view.findViewById<TextView>(R.id.tvProjectsCount)
        val tvSavedCount = view.findViewById<TextView>(R.id.tvSavedCount)
        val savedCountGroup = view.findViewById<View>(R.id.profileSavedCountGroup)

        fun styleTabButton(button: MaterialButton, selected: Boolean) {
            val primary = MaterialColors.getColor(button, com.google.android.material.R.attr.colorPrimary)
            val onPrimary = MaterialColors.getColor(button, com.google.android.material.R.attr.colorOnPrimary)
            val surface = MaterialColors.getColor(button, com.google.android.material.R.attr.colorSurface)
            val onSurface = MaterialColors.getColor(button, com.google.android.material.R.attr.colorOnSurface)

            if (selected) {
                button.backgroundTintList = ColorStateList.valueOf(primary)
                button.setTextColor(onPrimary)
                button.strokeWidth = 0
            } else {
                button.backgroundTintList = ColorStateList.valueOf(surface)
                button.setTextColor(onSurface)
                button.strokeColor = ColorStateList.valueOf(primary)
                button.strokeWidth = (1 * resources.displayMetrics.density).toInt()
            }
        }

        fun applyTabStyles() {
            val checkedId = toggleGroup.checkedButtonId
            styleTabButton(btnPosts, selected = checkedId == R.id.profile_filter_posts)
            styleTabButton(btnProjects, selected = checkedId == R.id.profile_filter_projects)
            styleTabButton(btnSaved, selected = checkedId == R.id.profile_filter_saved)
        }

        fun configureUiForMode(mode: ProfileMode, isViewingSelf: Boolean = false) {
            profileMode = mode
            when (mode) {
                ProfileMode.PRIVATE -> {
                    btnBack.visibility = View.GONE
                    btnEdit.visibility = View.VISIBLE
                    btnFollow.visibility = View.GONE
                    btnSaved.visibility = View.VISIBLE
                    savedCountGroup.visibility = View.GONE
                    btnFollow.alpha = 1.0f
                }
                ProfileMode.PUBLIC -> {
                    btnBack.visibility = View.VISIBLE

                    btnEdit.visibility = View.GONE

                    btnFollow.visibility = View.VISIBLE
                    btnFollow.isEnabled = !isViewingSelf
                    btnFollow.alpha = if (isViewingSelf) 0.5f else 1.0f

                    btnSaved.visibility = View.GONE
                    savedCountGroup.visibility = View.GONE
                    if (toggleGroup.checkedButtonId == R.id.profile_filter_saved) {
                        toggleGroup.check(R.id.profile_filter_posts)
                    }
                }
            }
            applyTabStyles()
        }

        fun updateCounts(profile: ProfilePublicDto) {
            tvFollowersCount.text = profile.followersCount.toString()
            tvFollowingCount.text = profile.followingCount.toString()
            tvPostsCount.text = profile.postsCount.toString()
            tvProjectsCount.text = profile.projectsCount.toString()
            tvSavedCount.text = profile.savedProjectsCount.toString()
        }

        fun updateFollowButton() {
            if (profileMode == ProfileMode.PUBLIC && btnFollow.visibility == View.VISIBLE) {
                btnFollow.text = if (viewModel.isFollowedByMe) getString(R.string.profile_unfollow) else getString(R.string.profile_follow)
            }
        }

        fun currentTab(): Tab {
            return when (toggleGroup.checkedButtonId) {
                R.id.profile_filter_projects -> Tab.PROJECTS
                R.id.profile_filter_saved -> Tab.SAVED
                else -> Tab.POSTS
            }
        }

        fun loadTab(tab: Tab, force: Boolean = false) {
            val username = viewModel.viewingUsername ?: return
            val pTab = when(tab) {
                Tab.POSTS -> ProfileViewModel.ProfileTab.POSTS
                Tab.PROJECTS -> ProfileViewModel.ProfileTab.PROJECTS
                Tab.SAVED -> ProfileViewModel.ProfileTab.SAVED
            }

            if (!force && viewModel.loadedTabs.contains(pTab)) {
                val cached = when(tab) {
                    Tab.POSTS -> viewModel.posts
                    Tab.PROJECTS -> viewModel.projects
                    Tab.SAVED -> viewModel.saved
                }
                adapter.submitList(cached)
                return
            }

            loadJob?.cancel()
            loadJob = viewLifecycleOwner.lifecycleScope.launch {
                val apiService = RetrofitClient.getInstance(requireContext())
                val me = try { apiService.getMe() } catch (_: Exception) { null }

                val items: List<FeedItem> = try {
                    when (tab) {
                        Tab.POSTS -> coroutineScope {
                            val posts = async { apiService.listUserPosts(username).map { it.toFeedItemPost() } }
                            val projects = async { apiService.listUserProjects(username).map { it.toFeedItemProject() } }
                            posts.await() + projects.await()
                        }
                        Tab.PROJECTS -> apiService.listUserProjects(username).map { it.toFeedItemProject() }
                        Tab.SAVED -> apiService.listMySavedProjects().map { it.toFeedItemProject() }
                    }
                } catch (_: Exception) {
                    emptyList()
                }

                val sorted = items
                    .mapNotNull { it as? FeedItem.Base }
                    .sortedByDescending { it.createdAt }
                    .map { it as FeedItem }

                when(tab) {
                    Tab.POSTS -> viewModel.posts = sorted
                    Tab.PROJECTS -> viewModel.projects = sorted
                    Tab.SAVED -> viewModel.saved = sorted
                }
                viewModel.loadedTabs.add(pTab)
                adapter.submitList(sorted)
            }
        }

        val args = arguments
        val requestedUsername = args?.getString(ARG_USERNAME)
        val forceMe = args?.getBoolean(ARG_FORCE_ME, false) ?: false
        val initialMode = if (forceMe || requestedUsername == null) ProfileMode.PRIVATE else ProfileMode.PUBLIC
        configureUiForMode(initialMode)

        arguments?.let { a ->
            val prefillDisplayName = a.getString(ARG_PREFILL_DISPLAY_NAME)
            val prefillUsername = a.getString(ARG_PREFILL_USERNAME)
            if (!prefillDisplayName.isNullOrBlank()) tvDisplayName.text = prefillDisplayName
            if (!prefillUsername.isNullOrBlank()) tvUsername.text = getString(R.string.username_format, prefillUsername)
            ivAvatar.setImageResource(R.drawable.ic_default_avatar)
        }

        adapter = FeedAdapter(
            onProjectClick = { project ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, ProjectDetailsFragment.newInstance(project))
                    .addToBackStack(null)
                    .commit()
            },
            onReblogClick = { project ->
                feedViewModel.reblogProject(project)
                if (currentTab() == Tab.POSTS) loadTab(Tab.POSTS, force = true)
            },
            onSaveClick = { project ->
                feedViewModel.toggleSaveProject(project)
                if (currentTab() == Tab.SAVED) loadTab(Tab.SAVED, force = true)
            }
        )
        view.findViewById<RecyclerView>(R.id.rvProfile).adapter = adapter

        btnEdit.setOnClickListener {
            Snackbar.make(view, getString(R.string.profile_edit_coming_soon), Snackbar.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener {
            if (!parentFragmentManager.popBackStackImmediate()) {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.profile_filter_posts -> loadTab(Tab.POSTS)
                R.id.profile_filter_projects -> loadTab(Tab.PROJECTS)
                R.id.profile_filter_saved -> if (profileMode == ProfileMode.PRIVATE) loadTab(Tab.SAVED)
            }
            applyTabStyles()
        }

        btnFollow.setOnClickListener {
            val username = viewModel.viewingUsername ?: return@setOnClickListener
            if (profileMode != ProfileMode.PUBLIC) return@setOnClickListener

            btnFollow.isEnabled = false
            lifecycleScope.launch {
                try {
                    val apiService = RetrofitClient.getInstance(requireContext())
                    if (viewModel.isFollowedByMe) {
                        apiService.unfollowUser(username)
                        viewModel.isFollowedByMe = false
                    } else {
                        apiService.followUser(username)
                        viewModel.isFollowedByMe = true
                    }
                    updateFollowButton()
                } catch (_: Exception) {
                } finally {
                    btnFollow.isEnabled = true
                }
            }
        }

        lifecycleScope.launch {
            val apiService = RetrofitClient.getInstance(requireContext())
            val me = try { apiService.getMe() } catch (_: Exception) { null }
            val myUsername = me?.username

            val resolvedMode = when {
                forceMe -> ProfileMode.PRIVATE
                requestedUsername.isNullOrBlank() -> ProfileMode.PRIVATE
                else -> ProfileMode.PUBLIC
            }
            val viewingSelf = !myUsername.isNullOrBlank() && (requestedUsername == myUsername || resolvedMode == ProfileMode.PRIVATE)

            val finalUsername = if (resolvedMode == ProfileMode.PRIVATE) myUsername else requestedUsername

            if (viewModel.viewingUsername == finalUsername && viewModel.profile != null) {
                configureUiForMode(resolvedMode, viewingSelf)
                val profile = viewModel.profile!!
                tvDisplayName.text = profile.displayName
                tvUsername.text = getString(R.string.username_format, profile.username)
                updateCounts(profile)
                if (profileMode == ProfileMode.PUBLIC) updateFollowButton()
                loadTab(currentTab())
                return@launch
            }

            viewModel.clearCache()
            viewModel.viewingUsername = finalUsername
            configureUiForMode(resolvedMode, viewingSelf)

            val usernameToLoad = viewModel.viewingUsername
            if (usernameToLoad.isNullOrBlank()) {
                tvDisplayName.text = getString(R.string.error_loading_profile)
                return@launch
            }

            try {
                val profile = apiService.getPublicProfile(usernameToLoad)
                viewModel.profile = profile
                tvDisplayName.text = profile.displayName
                tvUsername.text = getString(R.string.username_format, profile.username)
                updateCounts(profile)
                if (profileMode == ProfileMode.PUBLIC) {
                    viewModel.isFollowedByMe = profile.isFollowedByMe ?: false
                    updateFollowButton()
                }
                loadTab(currentTab())
            } catch (_: Exception) {
                tvDisplayName.text = getString(R.string.error_loading_profile)
            }
        }
    }

    private fun UserPublicDto.toUserSummary(): UserSummary {
        return UserSummary(
            id = id.toLong(),
            username = username,
            displayName = displayName,
            avatarUrl = avatarUrl,
            accentColor = accentColor,
        )
    }

    private fun PostReadDto.toFeedItemPost(): FeedItem.Post {
        return FeedItem.Post(
            id = id.toLong(),
            author = author.toUserSummary(),
            createdAt = createdAt,
            content = content,
            imageUrl = imageUrl,
            rebloggedProject = rebloggedProject?.toFeedItemProject()
        )
    }

    private fun ProjectReadDto.toFeedItemProject(): FeedItem.Project {
        return FeedItem.Project(
            id = id.toLong(),
            author = author.toUserSummary(),
            createdAt = createdAt,
            title = title,
            content = content,
            imageUrl = imageUrl,
            hookSize = hookSize,
            pattern = pattern,
            yarnType = yarnType,
            yarnAmount = yarnAmount,
            timeToComplete = timeToComplete,
            additionalMaterials = additionalMaterials,
            isSavedByMe = isSavedByMe ?: false,
            isRebloggedByMe = isRebloggedByMe ?: false
        )
    }
}

