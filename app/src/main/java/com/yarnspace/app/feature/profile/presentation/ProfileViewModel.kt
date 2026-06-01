package com.yarnspace.app.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yarnspace.app.core.model.FeedItem
import com.yarnspace.app.core.model.UserSummary
import com.yarnspace.app.data.remote.dto.ProfilePublicDto
import com.yarnspace.app.data.remote.dto.ProfileUpdateDto
import com.yarnspace.app.feature.profile.data.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
) : ViewModel() {

    enum class ProfileMode { PRIVATE, PUBLIC }
    enum class Tab { POSTS, PROJECTS, SAVED }

    data class ProfileUiState(
        val mode: ProfileMode = ProfileMode.PRIVATE,
        val viewingUsername: String? = null,
        val viewingSelf: Boolean = false,
        val profile: ProfilePublicDto? = null,
        val isFollowedByMe: Boolean = false,
        val followInProgress: Boolean = false,

        val selectedTab: Tab = Tab.POSTS,
        val tabItems: Map<Tab, List<FeedItem>> = emptyMap(),
        val loadedTabs: Set<Tab> = emptySet(),

        val isLoadingProfile: Boolean = false,
        val isLoadingTab: Boolean = false,
    ) {
        val currentItems: List<FeedItem>
            get() = tabItems[selectedTab].orEmpty()
    }

    sealed interface ProfileUiEvent {
        data class ShowSnackbar(val message: String) : ProfileUiEvent
        data object CloseEditPanel : ProfileUiEvent
        data class EditSaveFinished(val success: Boolean) : ProfileUiEvent
    }

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ProfileUiEvent> = _events.asSharedFlow()

    private var initialized = false

    fun initialize(
        requestedUsername: String?,
        forceMe: Boolean,
        initialTab: Tab,
    ) {
        if (initialized) {
            onTabSelected(initialTab)
            return
        }
        initialized = true

        _uiState.value = _uiState.value.copy(selectedTab = initialTab)
        loadProfile(requestedUsername = requestedUsername, forceMe = forceMe)
    }

    fun onTabSelected(tab: Tab, force: Boolean = false) {
        loadTab(tab = tab, force = force)
    }

    fun refreshCurrentTab() {
        loadTab(tab = _uiState.value.selectedTab, force = true)
    }

    fun onFollowClicked() {
        val username = _uiState.value.viewingUsername ?: return
        if (_uiState.value.mode != ProfileMode.PUBLIC || _uiState.value.viewingSelf) return
        if (_uiState.value.followInProgress) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(followInProgress = true)
            try {
                if (_uiState.value.isFollowedByMe) {
                    repository.unfollowUser(username)
                    _uiState.value = _uiState.value.copy(isFollowedByMe = false)
                } else {
                    repository.followUser(username)
                    _uiState.value = _uiState.value.copy(isFollowedByMe = true)
                }
            } catch (e: Exception) {
                _events.tryEmit(ProfileUiEvent.ShowSnackbar(e.message ?: "Failed to update follow state"))
            } finally {
                _uiState.value = _uiState.value.copy(followInProgress = false)
            }
        }
    }

    fun saveMyProfile(
        displayName: String,
        accentColor: String,
        avatarIcon: String?,
    ) {
        if (_uiState.value.mode != ProfileMode.PRIVATE) return

        viewModelScope.launch {
            try {
                val updated = repository.updateMe(
                    ProfileUpdateDto(
                        displayName = displayName,
                        accentColor = accentColor,
                        avatarIcon = avatarIcon,
                    )
                )

                val current = _uiState.value.profile
                if (current != null) {
                    _uiState.value = _uiState.value.copy(
                        profile = current.copy(
                            displayName = updated.displayName,
                            accentColor = updated.accentColor,
                            avatarIcon = updated.avatarIcon
                        )
                    )
                }

                val updatedSummary = UserSummary(
                    id = updated.id.toLong(),
                    username = updated.username,
                    displayName = updated.displayName,
                    accentColor = updated.accentColor,
                    avatarIcon = updated.avatarIcon
                )
                updateCachedAuthorAppearance(username = updated.username, updated = updatedSummary)

                _events.tryEmit(ProfileUiEvent.CloseEditPanel)
                _events.tryEmit(ProfileUiEvent.EditSaveFinished(success = true))
                _events.tryEmit(ProfileUiEvent.ShowSnackbar("Profile updated"))
            } catch (e: Exception) {
                _events.tryEmit(ProfileUiEvent.EditSaveFinished(success = false))
                _events.tryEmit(ProfileUiEvent.ShowSnackbar(e.message ?: "Failed to update profile"))
            }
        }
    }

    private fun updateCachedAuthorAppearance(username: String, updated: UserSummary) {
        val newTabItems = _uiState.value.tabItems.mapValues { (_, items) ->
            items.map { item ->
                when (item) {
                    is FeedItem.Project -> {
                        if (item.author.username == username) item.copy(author = updated) else item
                    }

                    is FeedItem.Post -> {
                        val newAuthor = if (item.author.username == username) updated else item.author
                        val newReblog = item.rebloggedProject?.let { rp ->
                            if (rp.author.username == username) rp.copy(author = updated) else rp
                        }
                        item.copy(author = newAuthor, rebloggedProject = newReblog)
                    }

                    else -> item
                }
            }
        }

        _uiState.value = _uiState.value.copy(tabItems = newTabItems)
    }

    fun toggleSaveStateInCache(projectId: Long, isSavedByMe: Boolean) {
        val currentUiState = _uiState.value

        val newTabItems = currentUiState.tabItems.mapValues { (tab, items) ->
            if (tab == Tab.SAVED && !isSavedByMe) {
                items.filter { item ->
                    val id = when (item) {
                        is FeedItem.Project -> item.id
                        is FeedItem.Post -> item.rebloggedProject?.id
                        else -> null
                    }
                    id != projectId
                }
            } else {
                items.map { item ->
                    when (item) {
                        is FeedItem.Project -> {
                            if (item.id == projectId) item.copy(isSavedByMe = isSavedByMe) else item
                        }
                        is FeedItem.Post -> {
                            val updatedReblog = item.rebloggedProject?.let { rp ->
                                if (rp.id == projectId) rp.copy(isSavedByMe = isSavedByMe) else rp
                            }
                            item.copy(rebloggedProject = updatedReblog)
                        }
                        else -> item
                    }
                }
            }
        }

        _uiState.value = currentUiState.copy(tabItems = newTabItems)
    }

    fun addReblogToCache(rebloggedProject: FeedItem.Project) {
        val currentUiState = _uiState.value
        val myProfile = currentUiState.profile ?: return

        val meAsAuthor = UserSummary(
            id = myProfile.id.toLong(),
            username = myProfile.username,
            displayName = myProfile.displayName,
            accentColor = myProfile.accentColor,
            avatarIcon = myProfile.avatarIcon
        )

        val newReblogPost = FeedItem.Post(
            id = System.currentTimeMillis(),
            author = meAsAuthor,
            content = null,
            imageUrl = null,
            createdAt = System.currentTimeMillis(),
            rebloggedProject = rebloggedProject
        )

        val currentPosts = currentUiState.tabItems[Tab.POSTS].orEmpty()
        val updatedPosts = mutableListOf<FeedItem>().apply {
            add(newReblogPost)
            addAll(currentPosts)
        }

        val newTabItems = currentUiState.tabItems.toMutableMap().apply {
            put(Tab.POSTS, updatedPosts)
        }

        _uiState.value = currentUiState.copy(tabItems = newTabItems)
    }

    private fun loadProfile(requestedUsername: String?, forceMe: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingProfile = true)

            val me = repository.getMeOrNull()
            val myUsername = me?.username

            val resolvedMode = when {
                forceMe -> ProfileMode.PRIVATE
                requestedUsername.isNullOrBlank() -> ProfileMode.PRIVATE
                else -> ProfileMode.PUBLIC
            }

            val viewingSelf = !myUsername.isNullOrBlank() &&
                    (requestedUsername == myUsername || resolvedMode == ProfileMode.PRIVATE)
            val finalUsername = if (resolvedMode == ProfileMode.PRIVATE) myUsername else requestedUsername

            if (finalUsername.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(
                    isLoadingProfile = false,
                    mode = resolvedMode,
                    viewingSelf = viewingSelf,
                    viewingUsername = null,
                    profile = null,
                )
                _events.tryEmit(ProfileUiEvent.ShowSnackbar("Unable to resolve username"))
                return@launch
            }

            if (_uiState.value.viewingUsername == finalUsername && _uiState.value.profile != null) {
                _uiState.value = _uiState.value.copy(
                    isLoadingProfile = false,
                    mode = resolvedMode,
                    viewingSelf = viewingSelf,
                )
                loadTab(_uiState.value.selectedTab, force = false)
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                mode = resolvedMode,
                viewingUsername = finalUsername,
                viewingSelf = viewingSelf,
                profile = null,
                isFollowedByMe = false,
                tabItems = emptyMap(),
                loadedTabs = emptySet(),
            )

            try {
                val profile = repository.getProfile(finalUsername)
                _uiState.value = _uiState.value.copy(
                    isLoadingProfile = false,
                    profile = profile,
                    isFollowedByMe = if (resolvedMode == ProfileMode.PUBLIC) (profile.isFollowedByMe ?: false) else false,
                )
                loadTab(_uiState.value.selectedTab, force = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingProfile = false,
                    profile = null,
                )
                _events.tryEmit(ProfileUiEvent.ShowSnackbar(e.message ?: "Error loading profile"))
            }
        }
    }

    private fun loadTab(tab: Tab, force: Boolean) {
        val username = _uiState.value.viewingUsername
        if (username.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(selectedTab = tab)
            return
        }

        _uiState.value = _uiState.value.copy(selectedTab = tab)

        if (!force && _uiState.value.loadedTabs.contains(tab)) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingTab = true)

            val items: List<FeedItem> = try {
                when (tab) {
                    Tab.POSTS -> coroutineScope {
                        val posts = async { repository.listUserPosts(username) }
                        val projects = async { repository.listUserProjects(username) }
                        posts.await() + projects.await()
                    }

                    Tab.PROJECTS -> repository.listUserProjects(username)
                    Tab.SAVED -> repository.listMySavedProjects()
                }
            } catch (e: Exception) {
                _events.tryEmit(ProfileUiEvent.ShowSnackbar(e.message ?: "Error loading tab"))
                emptyList()
            }

            val sorted = items
                .mapNotNull { it as? FeedItem.Base }
                .sortedByDescending { it.createdAt }
                .map { it as FeedItem }

            val newTabItems = _uiState.value.tabItems.toMutableMap().apply { put(tab, sorted) }
            val newLoadedTabs = _uiState.value.loadedTabs.toMutableSet().apply { add(tab) }

            _uiState.value = _uiState.value.copy(
                isLoadingTab = false,
                tabItems = newTabItems,
                loadedTabs = newLoadedTabs,
            )
        }
    }

    fun removeFeedItemFromCache(itemToRemove: FeedItem) {
        val currentUiState = _uiState.value
        val newTabItems = currentUiState.tabItems.mapValues { (_, items) ->
            items.filterNot { it == itemToRemove }
        }
        _uiState.value = currentUiState.copy(tabItems = newTabItems)
    }
}