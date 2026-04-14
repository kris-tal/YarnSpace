package com.yarnspace.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.yarnspace.app.domain.feed.FeedItem

class ProjectDetailsFragment : Fragment() {

    companion object {
        private const val ARG_PROJECT_TITLE = "arg_project_title"
        private const val ARG_PROJECT_AUTHOR = "arg_project_author"
        private const val ARG_PROJECT_CONTENT = "arg_project_content"

        fun newInstance(project: FeedItem.Project): ProjectDetailsFragment {
            return ProjectDetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PROJECT_TITLE, project.title)
                    putString(ARG_PROJECT_AUTHOR, "@${project.author.username}")
                    putString(ARG_PROJECT_CONTENT, project.content ?: "")
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
            // Go back to the previous screen (Feed/Profile) using the existing back stack.
            // If for some reason there's nothing on the back stack, fall back to Activity back.
            if (!parentFragmentManager.popBackStackImmediate()) {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }

        view.findViewById<TextView>(R.id.tvProjectDetailsTitle).text = requireArguments().getString(ARG_PROJECT_TITLE)
        view.findViewById<TextView>(R.id.tvProjectDetailsAuthor).text = requireArguments().getString(ARG_PROJECT_AUTHOR)
        view.findViewById<TextView>(R.id.tvProjectDetailsContent).text = requireArguments().getString(ARG_PROJECT_CONTENT)
    }
}


