package com.yarnspace.app.feature.profile.presentation

import android.content.res.Configuration
import android.content.res.ColorStateList
import android.net.Uri
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
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.yarnspace.app.R
import com.yarnspace.app.data.settings.ThemeSettingsRepository
import com.yarnspace.app.data.remote.dto.ProfilePublicDto
import com.yarnspace.app.core.model.UserSummary
import com.yarnspace.app.feature.feed.presentation.FeedAdapter
import com.yarnspace.app.feature.feed.presentation.FeedViewModel
import com.yarnspace.app.feature.feed.presentation.ProjectDetailsFragment
import com.yarnspace.app.theme.AccentColor
import com.yarnspace.app.theme.AccentThemeCoordinator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import com.yarnspace.app.core.util.ImageUploadUtils
import com.yarnspace.app.core.util.UrlUtils
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {


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

    private val feedViewModel: FeedViewModel by viewModels()

    @Inject
    lateinit var themeSettingsRepository: ThemeSettingsRepository

    @Inject
    lateinit var accentThemeCoordinator: AccentThemeCoordinator

    private lateinit var adapter: FeedAdapter
    private var profileMode: ProfileViewModel.ProfileMode = ProfileViewModel.ProfileMode.PRIVATE

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
        val accentBlock = view.findViewById<View>(R.id.profile_accent_block)
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

        // ddit panel refs
        val editOverlay = view.findViewById<FrameLayout>(R.id.profileEditOverlay)
        val btnEditClose = view.findViewById<ImageButton>(R.id.btnProfileEditClose)
        val ivEditAvatar = view.findViewById<ImageView>(R.id.ivProfileEditAvatar)
        val btnChangePhoto = view.findViewById<Button>(R.id.btnProfileEditChangePhoto)
        val etEditDisplayName = view.findViewById<TextView>(R.id.etProfileEditDisplayName)
        val accentGroup1 = view.findViewById<MaterialButtonToggleGroup>(R.id.profileEditAccentGroup)
        val accentGroup2 = view.findViewById<MaterialButtonToggleGroup>(R.id.profileEditAccentGroup2)
        val btnSave = view.findViewById<Button>(R.id.btnProfileEditSave)
        val btnCancel = view.findViewById<Button>(R.id.btnProfileEditCancel)

        var initialDisplayName: String = ""
        var initialAccent: String = AccentColor.SAGE.backendName
        var currentAccent: String = initialAccent
        var pickedAvatarUri: Uri? = null

        var pendingAccentToApplyGlobally: String? = null

        var applyTabStylesRef: (() -> Unit)? = null

        fun accentInt(name: String): Int {
            val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val accent = AccentColor.fromBackendName(name)
            val resId = if (isNight) accent.nightColorResId else accent.colorResId
            return ContextCompat.getColor(requireContext(), resId)
        }

        fun applyAccentToProfile(name: String) {
            val primary = accentInt(name)
            accentBlock.backgroundTintList = ColorStateList.valueOf(primary)

            applyTabStylesRef?.invoke()
        }

        fun isEditDirty(): Boolean {
            val dn = etEditDisplayName.text?.toString().orEmpty().trim()
            return dn != initialDisplayName.trim() || currentAccent != initialAccent || pickedAvatarUri != null
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
            pickedAvatarUri = null
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
            fun style(buttonId: Int) {
                val b = view.findViewById<MaterialButton>(buttonId)
                val name = b.tag as? String ?: return
                b.backgroundTintList = ColorStateList.valueOf(accentInt(name))
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
            initialDisplayName = profile.displayName
            initialAccent = profile.accentColor
            etEditDisplayName.text = initialDisplayName
            pickedAvatarUri = null

            ivEditAvatar.load(UrlUtils.resolve(profile.avatarUrl)) {
                placeholder(R.drawable.ic_default_avatar)
                error(R.drawable.ic_default_avatar)
            }
            setAccentSelection(initialAccent)
            setEditPanelVisible(true)
        }

        val pickAvatarLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                pickedAvatarUri = uri
                ivEditAvatar.load(uri)
                ivAvatar.load(uri)
            }
        }

        fun createAvatarPart(uri: Uri): MultipartBody.Part? {
            return ImageUploadUtils.createJpegPart(
                context = requireContext(),
                uri = uri,
                formFieldName = "file",
                fileName = "avatar.jpg",
                maxDimensionPx = 512,
                quality = 75,
            )
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (editOverlay.visibility == View.VISIBLE) {
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

        fun configureUiForMode(mode: ProfileViewModel.ProfileMode, isViewingSelf: Boolean = false) {
            profileMode = mode
            when (mode) {
                ProfileViewModel.ProfileMode.PRIVATE -> {
                    btnBack.visibility = View.GONE
                    btnEdit.visibility = View.VISIBLE
                    btnFollow.visibility = View.GONE
                    btnSaved.visibility = View.VISIBLE
                    savedCountGroup.visibility = View.GONE
                    btnFollow.alpha = 1.0f
                }
                ProfileViewModel.ProfileMode.PUBLIC -> {
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

        fun updateFollowButton(isFollowedByMe: Boolean) {
            if (profileMode == ProfileViewModel.ProfileMode.PUBLIC && btnFollow.visibility == View.VISIBLE) {
                btnFollow.text = if (isFollowedByMe) getString(R.string.profile_unfollow) else getString(R.string.profile_follow)
            }
        }

        fun currentTab(): ProfileViewModel.Tab {
            return when (toggleGroup.checkedButtonId) {
                R.id.profile_filter_projects -> ProfileViewModel.Tab.PROJECTS
                R.id.profile_filter_saved -> ProfileViewModel.Tab.SAVED
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
        btnChangePhoto.setOnClickListener { pickAvatarLauncher.launch("image/*") }

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
            val part = pickedAvatarUri?.let { createAvatarPart(it) }

            pendingAccentToApplyGlobally = currentAccent

            btnSave.isEnabled = false
            btnCancel.isEnabled = false
            btnEditClose.isEnabled = false
            btnChangePhoto.isEnabled = false

            viewModel.saveMyProfile(
                displayName = displayName,
                accentColor = currentAccent,
                avatarFile = part,
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
                R.id.profile_filter_posts -> viewModel.onTabSelected(ProfileViewModel.Tab.POSTS)
                R.id.profile_filter_projects -> viewModel.onTabSelected(ProfileViewModel.Tab.PROJECTS)
                R.id.profile_filter_saved -> if (profileMode == ProfileViewModel.ProfileMode.PRIVATE) viewModel.onTabSelected(ProfileViewModel.Tab.SAVED)
            }
            applyTabStyles()
        }

        btnFollow.setOnClickListener {
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
                            tvDisplayName.text = profile.displayName
                            tvUsername.text = getString(R.string.username_format, profile.username)
                            updateCounts(profile)

                            if (editOverlay.visibility != View.VISIBLE) {
                                applyAccentToProfile(profile.accentColor)
                                ivAvatar.load(UrlUtils.resolve(profile.avatarUrl)) {
                                    placeholder(R.drawable.ic_default_avatar)
                                    error(R.drawable.ic_default_avatar)
                                }
                            }
                        } else if (!state.isLoadingProfile) {
                            tvDisplayName.text = getString(R.string.error_loading_profile)
                        }

                        if (state.mode == ProfileViewModel.ProfileMode.PUBLIC) {
                            updateFollowButton(state.isFollowedByMe)
                            if (toggleGroup.checkedButtonId == R.id.profile_filter_saved) {
                                toggleGroup.check(R.id.profile_filter_posts)
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
                                    btnChangePhoto.isEnabled = true
                                }
                            }

                            ProfileViewModel.ProfileUiEvent.CloseEditPanel -> {
                                btnSave.isEnabled = true
                                btnCancel.isEnabled = true
                                btnEditClose.isEnabled = true
                                btnChangePhoto.isEnabled = true
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
}
