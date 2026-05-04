package com.yarnspace.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.yarnspace.app.data.profile.MockProfileRepository
import com.yarnspace.app.retrofit.RetrofitClient
import com.yarnspace.app.ui.feed.FeedAdapter
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvNick = view.findViewById<TextView>(R.id.profile_nick)
        val tvUsername = view.findViewById<TextView>(R.id.profile_username_text)
        val ivAvatar = view.findViewById<ImageView>(R.id.ivProfileAvatar)

        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getInstance(requireContext())
                val user = apiService.getMe()
                
                tvNick.text = user.nick
                tvUsername.text = "@${user.username}"

                // if avatarUrl null => keep default
                if (user.avatarUrl == null) {
                    ivAvatar.setImageResource(R.drawable.ic_default_avatar)
                } else {
                    // when i make editing possible i'll use glide for this probably
                    ivAvatar.setImageResource(R.drawable.ic_default_avatar)
                }
            } catch (e: Exception) {
                tvNick.text = "Error loading profile"
            }
        }

        val adapter = FeedAdapter(
            onProjectClick = { project ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, ProjectDetailsFragment.newInstance(project))
                    .addToBackStack(null)
                    .commit()
            },
        )

        view.findViewById<RecyclerView>(R.id.rvProfile).adapter = adapter
        adapter.submitList(MockProfileRepository.getMyPosts())
    }
}