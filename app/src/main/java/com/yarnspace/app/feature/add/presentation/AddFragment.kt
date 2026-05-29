package com.yarnspace.app.feature.add.presentation

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.yarnspace.app.R
import com.yarnspace.app.core.network.ApiService
import com.yarnspace.app.core.util.ImageUploadUtils
import com.yarnspace.app.core.util.PhotoPickerHelper
import com.yarnspace.app.data.remote.dto.PostCreateDto
import com.yarnspace.app.data.remote.dto.ProjectCreateDto
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@AndroidEntryPoint
class AddFragment : Fragment(R.layout.fragment_add) {

    @Inject
    lateinit var apiService: ApiService

    private var pickedPostImageUri: Uri? = null
    private var pickedProjectImageUri: Uri? = null

    private enum class Tab { POST, PROJECT }

    private var currentTab: Tab = Tab.POST
    private var isInternalTabChange: Boolean = false

    private lateinit var postPhotoHelper: PhotoPickerHelper
    private lateinit var projectPhotoHelper: PhotoPickerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        postPhotoHelper = PhotoPickerHelper(this) { uri ->
            if (uri != null) {
                pickedPostImageUri = uri
                view?.findViewById<TextView>(R.id.tvAddPostImageStatus)?.text = "Image selected"
                val iv = view?.findViewById<ImageView>(R.id.ivAddPostImagePreview)
                iv?.visibility = View.VISIBLE
                iv?.load(uri)
            }
        }

        projectPhotoHelper = PhotoPickerHelper(this) { uri ->
            if (uri != null) {
                pickedProjectImageUri = uri
                view?.findViewById<TextView>(R.id.tvAddProjectImageStatus)?.text = "Image selected"
                val iv = view?.findViewById<ImageView>(R.id.ivAddProjectImagePreview)
                iv?.visibility = View.VISIBLE
                iv?.load(uri)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.addToggleGroup)
        val tvAddMode = view.findViewById<TextView>(R.id.tvAddMode)

        val btnTabPost = view.findViewById<MaterialButton>(R.id.add_tab_post)
        val btnTabProject = view.findViewById<MaterialButton>(R.id.add_tab_project)

        val postGroup = view.findViewById<View>(R.id.groupAddPost)
        val projectGroup = view.findViewById<View>(R.id.groupAddProject)

        val etPostContent = view.findViewById<TextInputEditText>(R.id.etAddPostContent)
        val btnAddPostImage = view.findViewById<View>(R.id.btnAddPostImage)
        val tvPostImageStatus = view.findViewById<TextView>(R.id.tvAddPostImageStatus)
        val ivPostImagePreview = view.findViewById<ImageView>(R.id.ivAddPostImagePreview)
        val btnPublishPost = view.findViewById<View>(R.id.btnPublishPost)

        val etProjectTitle = view.findViewById<TextInputEditText>(R.id.etAddProjectTitle)
        val btnAddProjectImage = view.findViewById<View>(R.id.btnAddProjectImage)
        val tvProjectImageStatus = view.findViewById<TextView>(R.id.tvAddProjectImageStatus)
        val ivProjectImagePreview = view.findViewById<ImageView>(R.id.ivAddProjectImagePreview)
        val etProjectContent = view.findViewById<TextInputEditText>(R.id.etAddProjectContent)
        val etProjectHookSize = view.findViewById<TextInputEditText>(R.id.etAddProjectHookSize)
        val etProjectPattern = view.findViewById<TextInputEditText>(R.id.etAddProjectPattern)
        val etProjectYarnType = view.findViewById<TextInputEditText>(R.id.etAddProjectYarnType)
        val etProjectYarnAmount = view.findViewById<TextInputEditText>(R.id.etAddProjectYarnAmount)
        val etProjectTimeToComplete = view.findViewById<TextInputEditText>(R.id.etAddProjectTimeToComplete)
        val etProjectAdditionalMaterials = view.findViewById<TextInputEditText>(R.id.etAddProjectAdditionalMaterials)
        val btnPublishProject = view.findViewById<View>(R.id.btnPublishProject)

        fun raw(editText: TextInputEditText): String = editText.text?.toString().orEmpty()
        fun trimmed(editText: TextInputEditText): String = raw(editText).trim()
        fun blankToNull(value: String): String? = value.trim().ifBlank { null }

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

        fun isDirty(tab: Tab): Boolean {
            return when (tab) {
                Tab.POST -> trimmed(etPostContent).isNotBlank() || pickedPostImageUri != null
                Tab.PROJECT -> listOf(
                    trimmed(etProjectTitle),
                    trimmed(etProjectContent),
                    trimmed(etProjectHookSize),
                    trimmed(etProjectPattern),
                    trimmed(etProjectYarnType),
                    trimmed(etProjectYarnAmount),
                    trimmed(etProjectTimeToComplete),
                    trimmed(etProjectAdditionalMaterials),
                ).any { it.isNotBlank() } || pickedProjectImageUri != null
            }
        }

        fun clearTab(tab: Tab) {
            when (tab) {
                Tab.POST -> {
                    etPostContent.setText("")
                    pickedPostImageUri = null
                    tvPostImageStatus.text = getString(R.string.add_image_not_selected)
                    ivPostImagePreview.visibility = View.GONE
                }

                Tab.PROJECT -> {
                    etProjectTitle.setText("")
                    etProjectContent.setText("")
                    etProjectHookSize.setText("")
                    etProjectPattern.setText("")
                    etProjectYarnType.setText("")
                    etProjectYarnAmount.setText("")
                    etProjectTimeToComplete.setText("")
                    etProjectAdditionalMaterials.setText("")

                    pickedProjectImageUri = null
                    tvProjectImageStatus.text = getString(R.string.add_image_not_selected)
                    ivProjectImagePreview.visibility = View.GONE
                }
            }
        }

        fun setTab(tab: Tab) {
            currentTab = tab
            postGroup.visibility = if (tab == Tab.POST) View.VISIBLE else View.GONE
            projectGroup.visibility = if (tab == Tab.PROJECT) View.VISIBLE else View.GONE

            tvAddMode.text = getString(if (tab == Tab.POST) R.string.add_mode_post else R.string.add_mode_project)
            styleTabButton(btnTabPost, selected = tab == Tab.POST)
            styleTabButton(btnTabProject, selected = tab == Tab.PROJECT)
        }

        fun buttonIdForTab(tab: Tab): Int {
            return when (tab) {
                Tab.POST -> R.id.add_tab_post
                Tab.PROJECT -> R.id.add_tab_project
            }
        }

        fun showDiscardDialog(onDiscard: () -> Unit) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.add_discard_changes_title))
                .setMessage(getString(R.string.add_discard_changes_message))
                .setNegativeButton(getString(R.string.add_discard_changes_action_cancel), null)
                .setPositiveButton(getString(R.string.add_discard_changes_action_discard)) { _, _ ->
                    onDiscard()
                }
                .show()
        }

        fun showImageSourceDialog(helper: PhotoPickerHelper) {
            val options = arrayOf("Camera", "Gallery")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Choose Image Source")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> helper.openCamera()
                        1 -> helper.openGallery()
                    }
                }
                .show()
        }

        isInternalTabChange = true
        toggleGroup.check(R.id.add_tab_post)
        isInternalTabChange = false
        setTab(Tab.POST)

        btnAddPostImage.setOnClickListener {
            showImageSourceDialog(postPhotoHelper)
        }

        btnAddProjectImage.setOnClickListener {
            showImageSourceDialog(projectPhotoHelper)
        }

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            if (isInternalTabChange) return@addOnButtonCheckedListener

            val targetTab = when (checkedId) {
                R.id.add_tab_project -> Tab.PROJECT
                else -> Tab.POST
            }

            if (targetTab == currentTab) return@addOnButtonCheckedListener

            if (isDirty(currentTab)) {
                isInternalTabChange = true
                toggleGroup.check(buttonIdForTab(currentTab))
                isInternalTabChange = false

                showDiscardDialog {
                    val previousTab = currentTab
                    clearTab(previousTab)

                    isInternalTabChange = true
                    toggleGroup.check(buttonIdForTab(targetTab))
                    isInternalTabChange = false
                    setTab(targetTab)
                }
            } else {
                setTab(targetTab)
            }
        }

        fun navigateToProfile() {
            parentFragmentManager.popBackStack()
        }

        btnPublishPost.setOnClickListener {
            val content = trimmed(etPostContent)

            if (content.isBlank()) {
                Snackbar.make(view, getString(R.string.add_error_post_content_required), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnPublishPost.isEnabled = false
            lifecycleScope.launch {
                try {
                    val imageUrl: String? = pickedPostImageUri?.let { uri ->
                        val part = ImageUploadUtils.createJpegPart(
                            context = requireContext(),
                            uri = uri,
                            formFieldName = "file",
                            fileName = "post.jpg",
                            maxDimensionPx = 1080,
                            quality = 80,
                        )
                        if (part == null) throw IllegalStateException("Unable to read selected image")
                        apiService.uploadImage(part).imageUrl
                    }

                    apiService.createPost(PostCreateDto(content = content, imageUrl = imageUrl))
                    Snackbar.make(view, getString(R.string.add_success_post_published), Snackbar.LENGTH_SHORT).show()
                    clearTab(Tab.POST)
                    navigateToProfile()
                } catch (e: Exception) {
                    val message = when (e) {
                        is HttpException -> "Failed to publish post: ${e.code()}"
                        else -> e.message?.let { "Failed to publish post: $it" } ?: "Failed to publish post"
                    }
                    Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
                } finally {
                    btnPublishPost.isEnabled = true
                }
            }
        }

        btnPublishProject.setOnClickListener {
            val title = trimmed(etProjectTitle)

            if (pickedProjectImageUri == null) {
                Snackbar.make(view, getString(R.string.add_error_project_image_required), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (title.isBlank()) {
                Snackbar.make(view, getString(R.string.add_error_project_title_required), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val payload = ProjectCreateDto(
                title = title,
                // will be replaced after upload
                imageUrl = "",
                content = blankToNull(raw(etProjectContent)),
                hookSize = blankToNull(raw(etProjectHookSize)),
                pattern = blankToNull(raw(etProjectPattern)),
                yarnType = blankToNull(raw(etProjectYarnType)),
                yarnAmount = blankToNull(raw(etProjectYarnAmount)),
                timeToComplete = blankToNull(raw(etProjectTimeToComplete)),
                additionalMaterials = blankToNull(raw(etProjectAdditionalMaterials)),
            )

            btnPublishProject.isEnabled = false
            lifecycleScope.launch {
                try {
                    val part = ImageUploadUtils.createJpegPart(
                        context = requireContext(),
                        uri = pickedProjectImageUri!!,
                        formFieldName = "file",
                        fileName = "project.jpg",
                        maxDimensionPx = 1080,
                        quality = 80,
                    )
                    if (part == null) throw IllegalStateException("Unable to read selected image")
                    val imageUrl = apiService.uploadImage(part).imageUrl

                    apiService.createProject(payload.copy(imageUrl = imageUrl))
                    Snackbar.make(view, getString(R.string.add_success_project_published), Snackbar.LENGTH_SHORT).show()
                    clearTab(Tab.PROJECT)
                    navigateToProfile()
                } catch (e: Exception) {
                    val message = when (e) {
                        is HttpException -> "Failed to publish project: ${e.code()}"
                        else -> e.message?.let { "Failed to publish project: $it" } ?: "Failed to publish project"
                    }
                    Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
                } finally {
                    btnPublishProject.isEnabled = true
                }
            }
        }
    }
}