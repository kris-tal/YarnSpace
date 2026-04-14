package com.yarnspace.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.yarnspace.app.domain.feed.FeedItem

class ProjectDetailsFragment : Fragment() {

    companion object {
        private const val ARG_PROJECT_TITLE = "arg_project_title"
        private const val ARG_PROJECT_AUTHOR = "arg_project_author"
        private const val ARG_PROJECT_CONTENT = "arg_project_content"

        private const val ARG_PROJECT_IMAGE_RES_ID = "arg_project_image_res_id"

        private const val ARG_PROJECT_HOOK_SIZE = "arg_project_hook_size"
        private const val ARG_PROJECT_PATTERN = "arg_project_pattern"
        private const val ARG_PROJECT_YARN_TYPE = "arg_project_yarn_type"
        private const val ARG_PROJECT_YARN_AMOUNT = "arg_project_yarn_amount"
        private const val ARG_PROJECT_TIME_TO_COMPLETE = "arg_project_time_to_complete"
        private const val ARG_PROJECT_ADDITIONAL_MATERIALS = "arg_project_additional_materials"

        fun newInstance(project: FeedItem.Project): ProjectDetailsFragment {
            return ProjectDetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PROJECT_TITLE, project.title)
                    putString(ARG_PROJECT_AUTHOR, "@${project.author.username}")
                    putString(ARG_PROJECT_CONTENT, project.content ?: "")

                    putInt(ARG_PROJECT_IMAGE_RES_ID, project.imageResId ?: -1)

                    putString(ARG_PROJECT_HOOK_SIZE, project.hookSize)
                    putString(ARG_PROJECT_PATTERN, project.pattern)
                    putString(ARG_PROJECT_YARN_TYPE, project.yarnType)
                    putString(ARG_PROJECT_YARN_AMOUNT, project.yarnAmount)
                    putString(ARG_PROJECT_TIME_TO_COMPLETE, project.timeToComplete)
                    putString(ARG_PROJECT_ADDITIONAL_MATERIALS, project.additionalMaterials)
                }
            }
        }
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

        view.findViewById<TextView>(R.id.tvProjectDetailsTitle).text = requireArguments().getString(ARG_PROJECT_TITLE)
        view.findViewById<TextView>(R.id.tvProjectDetailsAuthor).text = requireArguments().getString(ARG_PROJECT_AUTHOR)
        view.findViewById<TextView>(R.id.tvProjectDetailsContent).text = requireArguments().getString(ARG_PROJECT_CONTENT)

        val imageResId = requireArguments().getInt(ARG_PROJECT_IMAGE_RES_ID, -1)
        val imageView = view.findViewById<ImageView>(R.id.ivProjectDetailsImage)
        if (imageResId != -1) {
            imageView.visibility = View.VISIBLE
            imageView.setImageResource(imageResId)
        } else {
            imageView.visibility = View.GONE
        }

        view.findViewById<TextView>(R.id.tvSpecHookSizeValue).text = requireArguments().getString(ARG_PROJECT_HOOK_SIZE).orEmpty()
        view.findViewById<TextView>(R.id.tvSpecPatternValue).text = requireArguments().getString(ARG_PROJECT_PATTERN).orEmpty()
        view.findViewById<TextView>(R.id.tvSpecYarnTypeValue).text = requireArguments().getString(ARG_PROJECT_YARN_TYPE).orEmpty()
        view.findViewById<TextView>(R.id.tvSpecYarnAmountValue).text = requireArguments().getString(ARG_PROJECT_YARN_AMOUNT).orEmpty()
        view.findViewById<TextView>(R.id.tvSpecTimeToCompleteValue).text = requireArguments().getString(ARG_PROJECT_TIME_TO_COMPLETE).orEmpty()
        view.findViewById<TextView>(R.id.tvSpecAdditionalMaterialsValue).text = requireArguments().getString(ARG_PROJECT_ADDITIONAL_MATERIALS).orEmpty()
    }
}


