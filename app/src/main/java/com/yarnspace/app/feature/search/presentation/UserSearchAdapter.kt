package com.yarnspace.app.feature.search.presentation

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.yarnspace.app.R
import com.yarnspace.app.core.model.UserSummary
import com.yarnspace.app.core.theme.AccentColor
import com.yarnspace.app.core.theme.AvatarIcon

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

        private val cvAvatarContainer: MaterialCardView? = itemView.findViewById(R.id.cvUserAvatarContainer)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivUserAvatar)
        private val tvDisplayName: TextView = itemView.findViewById(R.id.tvDisplayName)
        private val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)

        fun bind(user: UserSummary) {
            tvDisplayName.text = user.displayName
            tvUsername.text = itemView.context.getString(R.string.username_format, user.username)

            val context = itemView.context
            val isNightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            val userTheme = AccentColor.fromBackendName(user.accentColor)
            val bgColor = ContextCompat.getColor(context, userTheme.getLighterShade(isNightMode))
            val iconColor = ContextCompat.getColor(context, userTheme.getDarkerShade(isNightMode))

            cvAvatarContainer?.setCardBackgroundColor(bgColor)
            ivAvatar.setColorFilter(iconColor)

            val iconEnum = AvatarIcon.fromBackendName(user.avatarIcon)
            ivAvatar.setImageResource(iconEnum.resId)

            itemView.setOnClickListener { onUserClick(user) }
        }
    }
}