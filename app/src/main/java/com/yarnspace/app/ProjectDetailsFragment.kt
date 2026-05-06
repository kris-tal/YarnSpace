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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.color.MaterialColors
import com.yarnspace.app.data.feed.RemoteFeedRepository
import com.yarnspace.app.domain.feed.FeedItem
import com.yarnspace.app.ui.feed.FeedViewModel

class ProjectDetailsFragment : Fragment() {

    companion object {
        private const val ARG_PROJECT_ID = "arg_project_id"
        private const val ARG_PROJECT_TITLE = "arg_project_title"
        private const val ARG_PROJECT_AUTHOR_NAME = "arg_project_author_name"
        private const val ARG_PROJECT_AUTHOR_USERNAME = "arg_project_author_username"
        private const val ARG_PROJECT_CONTENT = "arg_project_content"
        private const val ARG_PROJECT_IMAGE_RES_ID = "arg_project_image_res_id"
        private const val ARG_PROJECT_HOOK_SIZE = "arg_project_hook_size"
        private const val ARG_PROJECT_PATTERN = "arg_project_pattern"
        private const val ARG_PROJECT_YARN_TYPE = "arg_project_yarn_type"
        private const val ARG_PROJECT_YARN_AMOUNT = "arg_project_yarn_amount"
        private const val ARG_PROJECT_TIME_TO_COMPLETE = "arg_project_time_to_complete"
        private const val ARG_PROJECT_ADDITIONAL_MATERIALS = "arg_project_additional_materials"
        private const val ARG_PROJECT_IS_SAVED = "arg_project_is_saved"
        private const val ARG_PROJECT_IS_REBLOGGED = "arg_project_is_reblogged"

        fun newInstance(project: FeedItem.Project): ProjectDetailsFragment {
            return ProjectDetailsFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PROJECT_ID, project.id)
                    putString(ARG_PROJECT_TITLE, project.title)
                    putString(ARG_PROJECT_AUTHOR_NAME, project.author.displayName)
                    putString(ARG_PROJECT_AUTHOR_USERNAME, project.author.username)
                    putString(ARG_PROJECT_CONTENT, project.content ?: "")

                    putInt(ARG_PROJECT_IMAGE_RES_ID, project.imageResId ?: -1)

                    putString(ARG_PROJECT_HOOK_SIZE, project.hookSize)
                    putString(ARG_PROJECT_PATTERN, project.pattern)
                    putString(ARG_PROJECT_YARN_TYPE, project.yarnType)
                    putString(ARG_PROJECT_YARN_AMOUNT, project.yarnAmount)
                    putString(ARG_PROJECT_TIME_TO_COMPLETE, project.timeToComplete)
                    putString(ARG_PROJECT_ADDITIONAL_MATERIALS, project.additionalMaterials)
                    putBoolean(ARG_PROJECT_IS_SAVED, project.isSavedByMe)
                    putBoolean(ARG_PROJECT_IS_REBLOGGED, project.isRebloggedByMe)
                }
            }
        }
    }

    private lateinit var viewModel: FeedViewModel
    private var isSaved: Boolean = false
    private var isReblogged: Boolean = false
    private lateinit var project: FeedItem.Project

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = requireArguments()
        isSaved = args.getBoolean(ARG_PROJECT_IS_SAVED)
        isReblogged = args.getBoolean(ARG_PROJECT_IS_REBLOGGED)
        
        project = FeedItem.Project(
            id = args.getLong(ARG_PROJECT_ID),
            author = com.yarnspace.app.domain.feed.UserSummary(
                id = -1,
                username = args.getString(ARG_PROJECT_AUTHOR_USERNAME).orEmpty(),
                displayName = args.getString(ARG_PROJECT_AUTHOR_NAME).orEmpty()
            ),
            createdAt = 0,
            title = args.getString(ARG_PROJECT_TITLE).orEmpty(),
            content = args.getString(ARG_PROJECT_CONTENT),
            imageResId = if (args.getInt(ARG_PROJECT_IMAGE_RES_ID) != -1) args.getInt(ARG_PROJECT_IMAGE_RES_ID) else null,
            isSavedByMe = isSaved,
            isRebloggedByMe = isReblogged
        )

        val repository = RemoteFeedRepository(requireContext())
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FeedViewModel(repository) as T
            }
        })[FeedViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_project_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btnProjectDetailsBack).setOnClickListener {
            if (!parentFragmentManager.popBackStackImmediate()) {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }

        view.findViewById<TextView>(R.id.tvProjectDetailsTitle).text = project.title
        view.findViewById<TextView>(R.id.tvProjectDetailsAuthor).text = "@${project.author.username}"
        view.findViewById<TextView>(R.id.tvProjectDetailsContent).text = project.content

        val imageResId = project.imageResId
        val imageView = view.findViewById<ImageView>(R.id.ivProjectDetailsImage)
        if (imageResId != null) {
            imageView.visibility = View.VISIBLE
            imageView.setImageResource(imageResId)
        } else {
            imageView.visibility = View.GONE
        }

        val args = requireArguments()
        view.findViewById<TextView>(R.id.tvSpecHookSizeValue).text = args.getString(ARG_PROJECT_HOOK_SIZE).orEmpty()
        view.findViewById<TextView>(R.id.tvSpecPatternValue).text = args.getString(ARG_PROJECT_PATTERN).orEmpty()
        view.findViewById<TextView>(R.id.tvSpecYarnTypeValue).text = args.getString(ARG_PROJECT_YARN_TYPE).orEmpty()
        view.findViewById<TextView>(R.id.tvSpecYarnAmountValue).text = args.getString(ARG_PROJECT_YARN_AMOUNT).orEmpty()
        view.findViewById<TextView>(R.id.tvSpecTimeToCompleteValue).text = args.getString(ARG_PROJECT_TIME_TO_COMPLETE).orEmpty()
        view.findViewById<TextView>(R.id.tvSpecAdditionalMaterialsValue).text = args.getString(ARG_PROJECT_ADDITIONAL_MATERIALS).orEmpty()

        val btnReblog = view.findViewById<ImageButton>(R.id.btnProjectDetailsReblog)
        val btnSave = view.findViewById<ImageButton>(R.id.btnProjectDetailsSave)

        updateReblogButton(btnReblog)
        btnReblog.setOnClickListener {
            if (!isReblogged) {
                viewModel.reblogProject(project)
                isReblogged = true
                project = project.copy(isRebloggedByMe = true)
                updateReblogButton(btnReblog)
                com.google.android.material.snackbar.Snackbar.make(view, "Project reblogged!", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            }
        }

        updateSaveButton(btnSave)
        btnSave.setOnClickListener {
            viewModel.toggleSaveProject(project)
            isSaved = !isSaved
            project = project.copy(isSavedByMe = isSaved)
            updateSaveButton(btnSave)
        }
    }

    private fun updateSaveButton(btnSave: ImageButton) {
        val saveIcon = if (isSaved) R.drawable.ic_star_filled else R.drawable.ic_star_outline
        btnSave.setImageResource(saveIcon)
        
        val tintColor = if (isSaved) {
            MaterialColors.getColor(btnSave, com.google.android.material.R.attr.colorPrimary)
        } else {
            MaterialColors.getColor(btnSave, com.google.android.material.R.attr.colorOnSurfaceVariant)
        }
        btnSave.imageTintList = ColorStateList.valueOf(tintColor)
    }

    private fun updateReblogButton(btnReblog: ImageButton) {
        val reblogIcon = if (isReblogged) R.drawable.ic_reblog_filled else R.drawable.ic_reblog_outline
        btnReblog.setImageResource(reblogIcon)
        
        val tintColor = if (isReblogged) {
            MaterialColors.getColor(btnReblog, com.google.android.material.R.attr.colorPrimary)
        } else {
            MaterialColors.getColor(btnReblog, com.google.android.material.R.attr.colorOnSurfaceVariant)
        }
        btnReblog.imageTintList = ColorStateList.valueOf(tintColor)
    }
}
