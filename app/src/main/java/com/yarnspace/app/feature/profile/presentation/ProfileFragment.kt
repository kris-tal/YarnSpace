package com.yarnspace.app.feature.profile.presentation

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.yarnspace.app.AuthActivity
import com.yarnspace.app.R
import com.yarnspace.app.core.audio.UiSoundManager
import com.yarnspace.app.core.auth.TokenManager
import com.yarnspace.app.core.model.UserSummary
import com.yarnspace.app.core.network.ApiService
import com.yarnspace.app.data.remote.dto.ProfilePublicDto
import com.yarnspace.app.core.theme.settings.ThemeSettingsRepository
import com.yarnspace.app.feature.feed.presentation.FeedAdapter
import com.yarnspace.app.feature.feed.presentation.FeedViewModel
import com.yarnspace.app.feature.feed.presentation.ProjectDetailsFragment
import com.yarnspace.app.feature.notifs.data.NotifsRepository
import com.yarnspace.app.core.theme.AccentColor
import com.yarnspace.app.core.theme.AccentThemeCoordinator
import com.yarnspace.app.core.theme.AvatarIcon
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    companion object {
        private const val ARG_USERNAME = "arg_username"
        private const val ARG_FORCE_ME = "arg_force_me"
        private const val ARG_PREFILL_ID = "arg_prefill_id"
        private const val ARG_PREFILL_USERNAME = "arg_prefill_username"
        private const val ARG_PREFILL_DISPLAY_NAME = "arg_prefill_display_name"
        private const val ARG_PREFILL_AVATAR_ICON = "arg_prefill_avatar_icon"
        private const val ARG_PREFILL_ACCENT_COLOR = "arg_prefill_accent_color"

        private const val STATE_SETTINGS_OPEN = "state_settings_open"

        fun newPublicInstance(user: UserSummary): ProfileFragment {
            return ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USERNAME, user.username)
                    putBoolean(ARG_FORCE_ME, false)

                    putLong(ARG_PREFILL_ID, user.id)
                    putString(ARG_PREFILL_USERNAME, user.username)
                    putString(ARG_PREFILL_DISPLAY_NAME, user.displayName)
                    putString(ARG_PREFILL_AVATAR_ICON, user.avatarIcon)
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
                        putString(ARG_PREFILL_AVATAR_ICON, prefill.avatarIcon)
                        putString(ARG_PREFILL_ACCENT_COLOR, prefill.accentColor)
                    }
                }
            }
        }
    }

    private val viewModel: ProfileViewModel by viewModels()
    private val feedViewModel: FeedViewModel by viewModels()

    @Inject
    lateinit var themeSettingsRepository: ThemeSettingsRepository
    @Inject
    lateinit var accentThemeCoordinator: AccentThemeCoordinator
    @Inject
    lateinit var apiService: ApiService
    @Inject
    lateinit var notifsRepository: NotifsRepository
    @Inject
    lateinit var soundManager: UiSoundManager
    @Inject
    lateinit var tokenManager: TokenManager

    private lateinit var adapter: FeedAdapter
    private var profileMode: ProfileViewModel.ProfileMode = ProfileViewModel.ProfileMode.PRIVATE

    private lateinit var settingsController: SettingsPanelController
    private lateinit var editController: EditPanelController
    private var pendingAccentToApplyGlobally: String? = null

    private var currentProfileColor: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvDisplayName = view.findViewById<TextView>(R.id.profileDisplayName)
        val tvUsername = view.findViewById<TextView>(R.id.profileUsernameText)
        val accentBlock = view.findViewById<View>(R.id.profileAccentBlock)
        val btnBack = view.findViewById<ImageButton>(R.id.btnProfileBack)
        val btnSettings = view.findViewById<ImageButton>(R.id.btnProfileSettings)
        val btnFollow = view.findViewById<MaterialButton>(R.id.btnProfileFollow)
        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.profileToggleGroup)
        val btnProjects = view.findViewById<MaterialButton>(R.id.btnProfileFilterProjects)
        val btnPosts = view.findViewById<MaterialButton>(R.id.btnProfileFilterPosts)
        val btnSaved = view.findViewById<MaterialButton>(R.id.btnProfileFilterSaved)
        val tvFollowersCount = view.findViewById<TextView>(R.id.tvFollowersCount)
        val tvFollowingCount = view.findViewById<TextView>(R.id.tvFollowingCount)
        val tvPostsCount = view.findViewById<TextView>(R.id.tvPostsCount)
        val tvProjectsCount = view.findViewById<TextView>(R.id.tvProjectsCount)

        // avatar main
        val cvAvatarContainer = view.findViewById<MaterialCardView>(R.id.cvAvatarContainer)
        val ivAvatar = view.findViewById<ImageView>(R.id.ivProfileAvatar)

        // settings panel
        val isSettingsOpen = savedInstanceState?.getBoolean(STATE_SETTINGS_OPEN, false) ?: false
        val settingsRefs = SettingsPanelRefs(
            settingsPanel = view.findViewById(R.id.settingsPanel),
            settingsToggleButton = btnSettings,
            customThemeSwitch = view.findViewById(R.id.switchCustomTheme),
            darkModeSwitch = view.findViewById(R.id.switchDarkMode),
            darkModeRow = view.findViewById(R.id.darkModeRow),
            logoutButton = view.findViewById(R.id.btnLogout)
        )

        settingsController = SettingsPanelController(
            context = requireContext(),
            refs = settingsRefs,
            themeSettingsRepository = themeSettingsRepository,
            tokenManager = tokenManager,
            apiService = apiService,
            resources = resources,
            onLogout = {
                viewLifecycleOwner.lifecycleScope.launch {
                    val intent = Intent(requireContext(), AuthActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            }
        )
        settingsController.bind(isInitiallyOpen = isSettingsOpen)

        val btnSettingsClose = view.findViewById<ImageButton>(R.id.btnSettingsClose)
        btnSettingsClose?.setOnClickListener {
            settingsController.closePanelIfOpen()
        }

        var applyTabStylesRef: (() -> Unit)? = null

        fun loadAvatarIcon(imageView: ImageView, iconName: String?) {
            val iconEnum = AvatarIcon.fromBackendName(iconName)
            imageView.setImageResource(iconEnum.resId)
        }

        fun applyAccentToProfile(name: String) {
            val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val theme = AccentColor.fromBackendName(name)
            val ctx = requireContext()

            val primaryColor = ContextCompat.getColor(ctx, if (isNight) theme.nightColorResId else theme.colorResId)
            currentProfileColor = primaryColor
            accentBlock.backgroundTintList = ColorStateList.valueOf(primaryColor)

            val bgColor = ContextCompat.getColor(ctx, theme.getLighterShade(isNight))
            val iconColor = ContextCompat.getColor(ctx, theme.getDarkerShade(isNight))

            cvAvatarContainer?.setCardBackgroundColor(bgColor)
            ivAvatar?.setColorFilter(iconColor)

            applyTabStylesRef?.invoke()
        }

        // edit panel
        editController = EditPanelController(
            context = requireContext(),
            view = view,
            viewModel = viewModel,
            onAccentPreview = { accentName -> applyAccentToProfile(accentName) },
            onSavePendingGlobalAccent = { accentName -> pendingAccentToApplyGlobally = accentName }
        )

        val btnEdit = view.findViewById<ImageButton>(R.id.btnProfileEdit)
        btnEdit.setOnClickListener {
            val profile = viewModel.uiState.value.profile
            if (profile != null) {
                editController.open(profile)
            } else {
                Snackbar.make(view, getString(R.string.error_loading_profile), Snackbar.LENGTH_SHORT).show()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (settingsController.isPanelOpen()) {
                        settingsController.closePanelIfOpen()
                    } else if (editController.isPanelOpen()) {
                        editController.closePanel(force = false)
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        )

        fun styleTabButton(button: MaterialButton, selected: Boolean) {
            val primary = currentProfileColor ?: MaterialColors.getColor(button, com.google.android.material.R.attr.colorPrimary)
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
            }
        }

        fun applyTabStyles() {
            val checkedId = toggleGroup.checkedButtonId
            styleTabButton(btnPosts, selected = checkedId == R.id.btnProfileFilterPosts)
            styleTabButton(btnProjects, selected = checkedId == R.id.btnProfileFilterProjects)
            styleTabButton(btnSaved, selected = checkedId == R.id.btnProfileFilterSaved)
        }

        applyTabStylesRef = ::applyTabStyles

        fun configureUiForMode(mode: ProfileViewModel.ProfileMode, isViewingSelf: Boolean = false) {
            profileMode = mode

            val iconTint = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnBackground)
            btnSettings.imageTintList = ColorStateList.valueOf(iconTint)
            btnEdit.imageTintList = ColorStateList.valueOf(iconTint)
            btnBack.imageTintList = ColorStateList.valueOf(iconTint)

            when (mode) {
                ProfileViewModel.ProfileMode.PRIVATE -> {
                    btnSettings.visibility = View.VISIBLE
                    btnBack.visibility = View.GONE
                    btnEdit.visibility = View.VISIBLE
                    btnFollow.visibility = View.GONE
                    btnSaved.visibility = View.VISIBLE
                    btnFollow.alpha = 1.0f
                }
                ProfileViewModel.ProfileMode.PUBLIC -> {
                    btnSettings.visibility = View.GONE
                    btnBack.visibility = View.VISIBLE
                    btnEdit.visibility = View.GONE
                    btnFollow.visibility = View.VISIBLE
                    btnFollow.isEnabled = !isViewingSelf

                    btnSaved.visibility = View.GONE
                    if (toggleGroup.checkedButtonId == R.id.btnProfileFilterSaved) {
                        toggleGroup.check(R.id.btnProfileFilterPosts)
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
        }

        fun updateFollowButton(isFollowedByMe: Boolean?) {
            if (profileMode == ProfileViewModel.ProfileMode.PUBLIC && btnFollow.visibility == View.VISIBLE) {
                val profileColor = currentProfileColor ?: MaterialColors.getColor(btnFollow, com.google.android.material.R.attr.colorPrimary)
                val surface = MaterialColors.getColor(btnFollow, com.google.android.material.R.attr.colorSurface)
                val onSurface = MaterialColors.getColor(btnFollow, com.google.android.material.R.attr.colorOnSurface)

                if (isFollowedByMe == true) {
                    btnFollow.text = getString(R.string.profile_unfollow)
                    btnFollow.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), android.R.color.transparent))
                    btnFollow.backgroundTintList = ColorStateList.valueOf(surface)
                    btnFollow.setTextColor(onSurface)
                } else {
                    btnFollow.text = getString(R.string.profile_follow)
                    btnFollow.backgroundTintList = ColorStateList.valueOf(profileColor)
                    btnFollow.strokeWidth = 0
                    val onPrimaryColor = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnPrimary)
                    btnFollow.setTextColor(onPrimaryColor)
                }
            }
        }

        fun currentTab(): ProfileViewModel.Tab {
            return when (toggleGroup.checkedButtonId) {
                R.id.btnProfileFilterProjects -> ProfileViewModel.Tab.PROJECTS
                R.id.btnProfileFilterSaved -> ProfileViewModel.Tab.SAVED
                else -> ProfileViewModel.Tab.POSTS
            }
        }

        val args = arguments
        val requestedUsername = args?.getString(ARG_USERNAME)
        val forceMe = args?.getBoolean(ARG_FORCE_ME, false) ?: false
        val initialMode = if (forceMe || requestedUsername == null) ProfileViewModel.ProfileMode.PRIVATE else ProfileViewModel.ProfileMode.PUBLIC
        configureUiForMode(initialMode)

        arguments?.let { a ->
            val prefillDisplayName = a.getString(ARG_PREFILL_DISPLAY_NAME)
            val prefillUsername = a.getString(ARG_PREFILL_USERNAME)
            if (!prefillDisplayName.isNullOrBlank()) tvDisplayName.text = prefillDisplayName
            if (!prefillUsername.isNullOrBlank()) tvUsername.text = getString(R.string.username_format, prefillUsername)
            loadAvatarIcon(ivAvatar, a.getString(ARG_PREFILL_AVATAR_ICON))
        }

        val myUsername = tokenManager.getUsername()

        adapter = FeedAdapter(
            currentUsername = myUsername,
            onProjectClick = { project ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, ProjectDetailsFragment.newInstance(project))
                    .addToBackStack(null)
                    .commit()
            },
            onReblogClick = { project ->
                feedViewModel.reblogProject(project)
                soundManager.play(soundManager.soundCreate)
                viewModel.addReblogToCache(rebloggedProject = project)
            },
            onSaveClick = { project ->
                feedViewModel.toggleSaveProject(project)
                val newSaveState = !project.isSavedByMe

                if (project.isSavedByMe) {
                    soundManager.play(soundManager.soundUnsave)
                } else {
                    soundManager.play(soundManager.soundSave)
                }
                viewModel.toggleSaveStateInCache(projectId = project.id, isSavedByMe = newSaveState)
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
                        soundManager.play(soundManager.soundDelete)
                        feedViewModel.deleteItem(itemToDelete)

                        viewModel.removeFeedItemFromCache(itemToDelete)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        view.findViewById<RecyclerView>(R.id.rvProfile).adapter = adapter

        btnBack.setOnClickListener {
            if (!parentFragmentManager.popBackStackImmediate()) {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btnProfileFilterPosts -> viewModel.onTabSelected(ProfileViewModel.Tab.POSTS)
                R.id.btnProfileFilterProjects -> viewModel.onTabSelected(ProfileViewModel.Tab.PROJECTS)
                R.id.btnProfileFilterSaved -> if (profileMode == ProfileViewModel.ProfileMode.PRIVATE) viewModel.onTabSelected(ProfileViewModel.Tab.SAVED)
            }
            applyTabStyles()
        }

        btnFollow.setOnClickListener {
            if (viewModel.uiState.value.followInProgress) return@setOnClickListener

            val isCurrentlyFollowing = viewModel.uiState.value.isFollowedByMe

            if (isCurrentlyFollowing) {
                soundManager.play(soundManager.soundUnfollow)
            } else {
                soundManager.play(soundManager.soundFollow)
            }
            viewModel.onFollowClicked()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        configureUiForMode(state.mode, state.viewingSelf)

                        btnFollow.isEnabled = state.mode == ProfileViewModel.ProfileMode.PUBLIC && !state.viewingSelf && !state.followInProgress
                        btnFollow.alpha = if (state.mode == ProfileViewModel.ProfileMode.PUBLIC && state.viewingSelf) 0.5f else 1.0f

                        val profile = state.profile
                        if (profile != null) {
                            tvDisplayName.text = profile.displayName.takeIf { !it.isNullOrBlank() } ?: profile.username
                            tvUsername.text = getString(R.string.username_format, profile.username)
                            updateCounts(profile)

                            if (!editController.isPanelOpen()) {
                                applyAccentToProfile(profile.accentColor)
                                loadAvatarIcon(ivAvatar, profile.avatarIcon)
                            }
                        } else if (!state.isLoadingProfile) {
                            tvDisplayName.text = getString(R.string.error_loading_profile)
                        }

                        if (state.mode == ProfileViewModel.ProfileMode.PUBLIC) {
                            updateFollowButton(state.isFollowedByMe)
                            if (toggleGroup.checkedButtonId == R.id.btnProfileFilterSaved) {
                                toggleGroup.check(R.id.btnProfileFilterPosts)
                            }
                        }

                        adapter.submitList(state.currentItems)
                        applyTabStyles()
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is ProfileViewModel.ProfileUiEvent.ShowSnackbar -> {
                                Snackbar.make(view, event.message, Snackbar.LENGTH_SHORT).show()
                            }

                            is ProfileViewModel.ProfileUiEvent.EditSaveFinished -> {
                                if (!event.success) {
                                    editController.setControlsEnabled(true)
                                }
                            }

                            ProfileViewModel.ProfileUiEvent.CloseEditPanel -> {
                                editController.setControlsEnabled(true)
                                editController.closePanel(force = true)

                                val pending = pendingAccentToApplyGlobally
                                pendingAccentToApplyGlobally = null
                                if (!pending.isNullOrBlank()) {
                                    val old = themeSettingsRepository.getAccentColorName()
                                    if (old != pending) {
                                        themeSettingsRepository.setAccentColorName(pending)
                                        (activity as? AppCompatActivity)?.let { act ->
                                            accentThemeCoordinator.applyAccent(act, pending)
                                            act.recreate()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        viewModel.initialize(
            requestedUsername = requestedUsername,
            forceMe = forceMe,
            initialTab = currentTab(),
        )
    }

    override fun onResume() {
        super.onResume()
        if (::settingsController.isInitialized) {
            settingsController.onResume()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::settingsController.isInitialized) {
            outState.putBoolean(STATE_SETTINGS_OPEN, settingsController.isPanelOpen())
        }
    }
}