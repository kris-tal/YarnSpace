package com.yarnspace.app.feature.search.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yarnspace.app.R
import com.yarnspace.app.core.model.UserSummary

class UserSearchAdapter(private val onUserClick: (UserSummary) -> Unit) :
    ListAdapter<UserSummary, UserSearchAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<UserSummary>() {
        override fun areItemsTheSame(oldItem: UserSummary, newItem: UserSummary): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: UserSummary, newItem: UserSummary): Boolean =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return VH(view, onUserClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        itemView: View,
        private val onUserClick: (UserSummary) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivUserAvatar)
        private val tvDisplayName: TextView = itemView.findViewById(R.id.tvDisplayName)
        private val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)

        fun bind(user: UserSummary) {
            tvDisplayName.text = user.displayName
            tvUsername.text = itemView.context.getString(R.string.username_format, user.username)

            val avatarRes = user.avatarResId ?: R.drawable.ic_default_avatar
            ivAvatar.setImageResource(avatarRes)

            itemView.setOnClickListener { onUserClick(user) }
        }
    }
}
