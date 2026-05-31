package com.yarnspace.app.feature.profile.presentation

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
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
import com.yarnspace.app.MainActivity
import com.yarnspace.app.R
import com.yarnspace.app.core.auth.SessionRepository
import com.yarnspace.app.data.settings.ThemeSettingsRepository
import com.yarnspace.app.data.remote.dto.ProfilePublicDto
import com.yarnspace.app.core.model.UserSummary
import com.yarnspace.app.core.network.ApiService
import com.yarnspace.app.feature.feed.presentation.FeedAdapter
import com.yarnspace.app.feature.feed.presentation.FeedViewModel
import com.yarnspace.app.feature.feed.presentation.ProjectDetailsFragment
import com.yarnspace.app.main.SettingsPanelController
import com.yarnspace.app.main.SettingsPanelRefs
import com.yarnspace.app.theme.AccentColor
import com.yarnspace.app.theme.AccentThemeCoordinator
import com.yarnspace.app.theme.AvatarIcon
import com.yarnspace.app.AuthActivity
import com.yarnspace.app.core.audio.UiSoundManager
import com.yarnspace.app.feature.notifs.domain.NotifsRepository
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
    lateinit var sessionRepository: SessionRepository
    @Inject
    lateinit var apiService: ApiService
    @Inject
    lateinit var notifsRepository: NotifsRepository
    @Inject
    lateinit var soundManager: UiSoundManager

    private lateinit var adapter: FeedAdapter
    private var profileMode: ProfileViewModel.ProfileMode = ProfileViewModel.ProfileMode.PRIVATE

    private lateinit var settingsController: SettingsPanelController

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
        val btnEdit = view.findViewById<ImageButton>(R.id.btnProfileEdit)
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
            sessionRepository = sessionRepository,
            apiService = apiService,
            resources = resources,
            onLogout = {
                viewLifecycleOwner.lifecycleScope.launch {

                    notifsRepository.clearLocalData()

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

        // edit panel
        val editOverlay = view.findViewById<FrameLayout>(R.id.profileEditOverlay)
        val btnEditClose = view.findViewById<ImageButton>(R.id.btnProfileEditClose)

        // avatar edit
        val cvEditAvatarContainer = view.findViewById<MaterialCardView>(R.id.cvProfileEditAvatarContainer)
        val ivEditAvatar = view.findViewById<ImageView>(R.id.ivProfileEditAvatar)

        val llAvatarPickerContainer = view.findViewById<LinearLayout>(R.id.llAvatarPickerContainer)

        val etEditDisplayName = view.findViewById<TextView>(R.id.etProfileEditDisplayName)
        val accentGroup1 = view.findViewById<MaterialButtonToggleGroup>(R.id.profileEditAccentGroup)
        val accentGroup2 = view.findViewById<MaterialButtonToggleGroup>(R.id.profileEditAccentGroup2)
        val btnSave = view.findViewById<Button>(R.id.btnProfileEditSave)
        val btnCancel = view.findViewById<Button>(R.id.btnProfileEditCancel)

        var initialDisplayName = ""
        var initialAccent: String = AccentColor.SAGE.backendName
        var currentAccent: String = initialAccent
        var pickedAvatarIcon: String? = null

        var pendingAccentToApplyGlobally: String? = null
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

            val bgColor = ContextCompat.getColor(ctx, theme.getLighterBg(isNight))
            val iconColor = ContextCompat.getColor(ctx, theme.getDarkerIcon(isNight))

            cvAvatarContainer?.setCardBackgroundColor(bgColor)
            ivAvatar?.setColorFilter(iconColor)

            cvEditAvatarContainer?.setCardBackgroundColor(bgColor)
            ivEditAvatar?.setColorFilter(iconColor)

            applyTabStylesRef?.invoke()
        }

        fun isEditDirty(): Boolean {
            val dn = etEditDisplayName.text?.toString().orEmpty().trim()
            return dn != initialDisplayName.trim() || currentAccent != initialAccent || pickedAvatarIcon != null
        }

        fun setEditPanelVisible(visible: Boolean) {
            editOverlay.visibility = if (visible) View.VISIBLE else View.GONE
        }

        fun confirmDiscard(onDiscard: () -> Unit) {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.profile_edit_discard_title))
                .setMessage(getString(R.string.profile_edit_discard_message))
                .setPositiveButton(getString(R.string.profile_edit_discard_action_discard)) { _, _ -> onDiscard() }
                .setNegativeButton(getString(R.string.profile_edit_discard_action_keep), null)
                .show()
        }

        fun closeEditPanel(force: Boolean) {
            if (!force && isEditDirty()) {
                confirmDiscard { closeEditPanel(force = true) }
                return
            }
            setEditPanelVisible(false)
            pickedAvatarIcon = null
            viewModel.uiState.value.profile?.let { applyAccentToProfile(it.accentColor) }
        }

        fun setAccentSelection(name: String) {
            currentAccent = name
            applyAccentToProfile(name)

            val allButtons = listOf(
                view.findViewById<MaterialButton>(R.id.btnAccentSage),
                view.findViewById<MaterialButton>(R.id.btnAccentPeach),
                view.findViewById<MaterialButton>(R.id.btnAccentLavender),
                view.findViewById<MaterialButton>(R.id.btnAccentYellow),
                view.findViewById<MaterialButton>(R.id.btnAccentPink),
                view.findViewById<MaterialButton>(R.id.btnAccentBlue),
            )
            val target = allButtons.firstOrNull { it.tag == name }
            if (target != null) {
                if (target.parent == accentGroup1) {
                    accentGroup2.clearChecked()
                    accentGroup1.check(target.id)
                } else {
                    accentGroup1.clearChecked()
                    accentGroup2.check(target.id)
                }
            }
        }

        fun styleAccentButtons() {
            val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            fun style(buttonId: Int) {
                val b = view.findViewById<MaterialButton>(buttonId)
                val name = b.tag as? String ?: return
                val theme = AccentColor.fromBackendName(name)

                val resId = if (isNight) theme.nightColorResId else theme.colorResId
                b.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), resId))
                b.setTextColor(MaterialColors.getColor(b, com.google.android.material.R.attr.colorOnPrimary))
            }

            style(R.id.btnAccentSage)
            style(R.id.btnAccentPeach)
            style(R.id.btnAccentLavender)
            style(R.id.btnAccentYellow)
            style(R.id.btnAccentPink)
            style(R.id.btnAccentBlue)
        }

        fun openEditPanel(profile: ProfilePublicDto) {
            styleAccentButtons()

            initialDisplayName = profile.displayName.takeIf { !it.isNullOrBlank() } ?: profile.username

            initialAccent = AccentColor.fromBackendName(profile.accentColor).backendName

            etEditDisplayName.text = initialDisplayName

            pickedAvatarIcon = profile.avatarIcon
            val currentIconEnum = AvatarIcon.fromBackendName(pickedAvatarIcon)
            ivEditAvatar.setImageResource(currentIconEnum.resId)

            setAccentSelection(initialAccent)

            llAvatarPickerContainer.removeAllViews()

            val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val currentTheme = AccentColor.fromBackendName(currentAccent)
            val iconTint = ContextCompat.getColor(requireContext(), currentTheme.getDarkerIcon(isNight))
            val bgTint = ContextCompat.getColor(requireContext(), currentTheme.getLighterBg(isNight))

            AvatarIcon.entries.forEach { iconEnum ->
                val cardView = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        (64 * resources.displayMetrics.density).toInt(),
                        (64 * resources.displayMetrics.density).toInt()
                    ).apply {
                        marginEnd = (12 * resources.displayMetrics.density).toInt()
                    }
                    radius = (32 * resources.displayMetrics.density)
                    cardElevation = 0f
                    strokeWidth = 0
                    setCardBackgroundColor(bgTint)

                    setOnClickListener {
                        pickedAvatarIcon = iconEnum.backendName
                        ivEditAvatar.setImageResource(iconEnum.resId)
                    }
                }

                val imageView = ImageView(requireContext()).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        (32 * resources.displayMetrics.density).toInt(),
                        (32 * resources.displayMetrics.density).toInt()
                    ).apply {
                        gravity = android.view.Gravity.CENTER
                    }
                    setImageResource(iconEnum.resId)
                    setColorFilter(iconTint)
                }

                cardView.addView(imageView)
                llAvatarPickerContainer.addView(cardView)
            }

            setEditPanelVisible(true)
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (settingsController.isPanelOpen()) {
                        settingsController.closePanelIfOpen()
                    } else if (editOverlay.visibility == View.VISIBLE) {
                        closeEditPanel(force = false)
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
                    //btnFollow.alpha = if (isViewingSelf) 0.5f else 1.0f

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

        adapter = FeedAdapter(
            onProjectClick = { project ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, ProjectDetailsFragment.newInstance(project))
                    .addToBackStack(null)
                    .commit()
            },
            onReblogClick = { project ->
                feedViewModel.reblogProject(project)
                if (currentTab() == ProfileViewModel.Tab.POSTS) viewModel.onTabSelected(ProfileViewModel.Tab.POSTS, force = true)
            },
            onSaveClick = { project ->
                feedViewModel.toggleSaveProject(project)
                if (currentTab() == ProfileViewModel.Tab.SAVED) viewModel.onTabSelected(ProfileViewModel.Tab.SAVED, force = true)
            }
        )
        view.findViewById<RecyclerView>(R.id.rvProfile).adapter = adapter

        btnEdit.setOnClickListener {
            val profile = viewModel.uiState.value.profile
            if (profile != null) {
                openEditPanel(profile)
            } else {
                Snackbar.make(view, getString(R.string.error_loading_profile), Snackbar.LENGTH_SHORT).show()
            }
        }

        btnEditClose.setOnClickListener { closeEditPanel(force = false) }
        btnCancel.setOnClickListener { closeEditPanel(force = false) }

        accentGroup1.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (!isChecked || checkedId == View.NO_ID) return@addOnButtonCheckedListener
            accentGroup2.clearChecked()
            val b = group.findViewById<MaterialButton>(checkedId)
            val name = b.tag as? String ?: return@addOnButtonCheckedListener
            currentAccent = name
            applyAccentToProfile(name)
        }

        accentGroup2.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (!isChecked || checkedId == View.NO_ID) return@addOnButtonCheckedListener
            accentGroup1.clearChecked()
            val b = group.findViewById<MaterialButton>(checkedId)
            val name = b.tag as? String ?: return@addOnButtonCheckedListener
            currentAccent = name
            applyAccentToProfile(name)
        }

        etEditDisplayName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = Unit
        })

        btnSave.setOnClickListener {
            val displayName = etEditDisplayName.text?.toString().orEmpty().trim()
            if (displayName.isBlank()) {
                Snackbar.make(view, getString(R.string.error_fill_all_fields), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            pendingAccentToApplyGlobally = currentAccent

            btnSave.isEnabled = false
            btnCancel.isEnabled = false
            btnEditClose.isEnabled = false

            viewModel.saveMyProfile(
                displayName = displayName,
                accentColor = currentAccent,
                avatarIcon = pickedAvatarIcon,
            )
        }

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

                            if (editOverlay.visibility != View.VISIBLE) {
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
                                    btnSave.isEnabled = true
                                    btnCancel.isEnabled = true
                                    btnEditClose.isEnabled = true
                                }
                            }

                            ProfileViewModel.ProfileUiEvent.CloseEditPanel -> {
                                btnSave.isEnabled = true
                                btnCancel.isEnabled = true
                                btnEditClose.isEnabled = true
                                closeEditPanel(force = true)

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