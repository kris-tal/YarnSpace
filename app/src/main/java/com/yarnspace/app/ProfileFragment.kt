package com.yarnspace.app

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.yarnspace.app.data.remote.dto.PostReadDto
import com.yarnspace.app.data.remote.dto.ProfilePublicDto
import com.yarnspace.app.data.remote.dto.ProjectReadDto
import com.yarnspace.app.data.remote.dto.UserPublicDto
import com.yarnspace.app.domain.feed.FeedItem
import com.yarnspace.app.domain.feed.UserSummary
import com.yarnspace.app.retrofit.RetrofitClient
import com.yarnspace.app.ui.feed.FeedAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private enum class ProfileMode { ME, PUBLIC }
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

    private lateinit var adapter: FeedAdapter
    private var profileMode: ProfileMode = ProfileMode.ME
    private var viewingUsername: String? = null
    private var isFollowedByMe: Boolean = false
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
        val btnFollow = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnProfileFollow)
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

        arguments?.let { args ->
            val prefillDisplayName = args.getString(ARG_PREFILL_DISPLAY_NAME)
            val prefillUsername = args.getString(ARG_PREFILL_USERNAME)
            if (!prefillDisplayName.isNullOrBlank()) tvDisplayName.text = prefillDisplayName
            if (!prefillUsername.isNullOrBlank()) tvUsername.text = getString(R.string.username_format, prefillUsername)

            ivAvatar.setImageResource(R.drawable.ic_default_avatar)
        }

        btnEdit.setOnClickListener {
            Snackbar.make(view, getString(R.string.profile_edit_coming_soon), Snackbar.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener {
            if (!parentFragmentManager.popBackStackImmediate()) {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }

        adapter = FeedAdapter(
            onProjectClick = { project ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, ProjectDetailsFragment.newInstance(project))
                    .addToBackStack(null)
                    .commit()
            },
        )

        view.findViewById<RecyclerView>(R.id.rvProfile).adapter = adapter

        fun dpToPx(dp: Int): Int {
            return (dp * resources.displayMetrics.density).toInt()
        }

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
                button.strokeWidth = dpToPx(1)
            }
        }

        fun applyTabStyles() {
            val checkedId = toggleGroup.checkedButtonId
            styleTabButton(btnPosts, selected = checkedId == R.id.profile_filter_posts)
            styleTabButton(btnProjects, selected = checkedId == R.id.profile_filter_projects)

            // Saved tab exists only in ME mode; if hidden, styling doesn't matter.
            styleTabButton(btnSaved, selected = checkedId == R.id.profile_filter_saved)
        }

        fun currentTab(): Tab {
            return when (toggleGroup.checkedButtonId) {
                R.id.profile_filter_projects -> Tab.PROJECTS
                R.id.profile_filter_saved -> Tab.SAVED
                else -> Tab.POSTS
            }
        }

        fun configureUiForMode(mode: ProfileMode) {
            profileMode = mode

            when (mode) {
                ProfileMode.ME -> {
                    btnBack.visibility = View.GONE
                    btnEdit.visibility = View.VISIBLE
                    btnFollow.visibility = View.GONE

                    btnSaved.visibility = View.VISIBLE
                    savedCountGroup.visibility = View.VISIBLE
                }

                ProfileMode.PUBLIC -> {
                    btnBack.visibility = View.VISIBLE
                    btnEdit.visibility = View.GONE
                    btnFollow.visibility = View.VISIBLE

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
            btnFollow.text = if (isFollowedByMe) getString(R.string.profile_unfollow) else getString(R.string.profile_follow)
        }

        fun loadTab(tab: Tab) {
            val username = viewingUsername ?: return

            loadJob?.cancel()
            loadJob = viewLifecycleOwner.lifecycleScope.launch {
                val apiService = RetrofitClient.getInstance(requireContext())

                val items: List<FeedItem> = try {
                    when (tab) {
                        Tab.POSTS -> apiService.listUserPosts(username).map { it.toFeedItemPost() }
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
                adapter.submitList(sorted)
            }
        }

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.profile_filter_posts -> loadTab(Tab.POSTS)
                R.id.profile_filter_projects -> loadTab(Tab.PROJECTS)
                R.id.profile_filter_saved -> if (profileMode == ProfileMode.ME) loadTab(Tab.SAVED)
            }

            applyTabStyles()
        }

        applyTabStyles()

        btnFollow.setOnClickListener {
            val username = viewingUsername ?: return@setOnClickListener
            if (profileMode != ProfileMode.PUBLIC) return@setOnClickListener

            btnFollow.isEnabled = false
            lifecycleScope.launch {
                try {
                    val apiService = RetrofitClient.getInstance(requireContext())
                    if (isFollowedByMe) {
                        apiService.unfollowUser(username)
                        isFollowedByMe = false
                    } else {
                        apiService.followUser(username)
                        isFollowedByMe = true
                    }
                    updateFollowButton()
                } catch (_: Exception) {
                    // ignore for now
                } finally {
                    btnFollow.isEnabled = true
                }
            }
        }

        configureUiForMode(ProfileMode.ME)

        lifecycleScope.launch {
            val args = arguments
            val requestedUsername = args?.getString(ARG_USERNAME)
            val forceMe = args?.getBoolean(ARG_FORCE_ME, false) ?: false
            val apiService = RetrofitClient.getInstance(requireContext())

            val me = try {
                apiService.getMe()
            } catch (_: Exception) {
                null
            }

            val myUsername = me?.username
            val resolvedMode = when {
                forceMe -> ProfileMode.ME
                requestedUsername.isNullOrBlank() -> ProfileMode.ME
                !myUsername.isNullOrBlank() && requestedUsername == myUsername -> ProfileMode.ME
                else -> ProfileMode.PUBLIC
            }

            viewingUsername = if (resolvedMode == ProfileMode.ME) myUsername else requestedUsername
            configureUiForMode(resolvedMode)

            val usernameToLoad = viewingUsername
            if (usernameToLoad.isNullOrBlank()) {
                tvDisplayName.text = getString(R.string.error_loading_profile)
                return@launch
            }

            try {
                val profile = apiService.getPublicProfile(usernameToLoad)

                tvDisplayName.text = profile.displayName
                tvUsername.text = getString(R.string.username_format, profile.username)
                ivAvatar.setImageResource(R.drawable.ic_default_avatar)

                updateCounts(profile)

                if (profileMode == ProfileMode.PUBLIC) {
                    isFollowedByMe = profile.isFollowedByMe ?: false
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
            imageResId = null,
        )
    }

    private fun ProjectReadDto.toFeedItemProject(): FeedItem.Project {
        return FeedItem.Project(
            id = id.toLong(),
            author = author.toUserSummary(),
            createdAt = createdAt,
            title = title,
            content = content,
            imageResId = null,
            hookSize = hookSize,
            pattern = pattern,
            yarnType = yarnType,
            yarnAmount = yarnAmount,
            timeToComplete = timeToComplete,
            additionalMaterials = additionalMaterials,
        )
    }
}