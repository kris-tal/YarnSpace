package com.yarnspace.app.feature.feed.presentation

import android.content.res.ColorStateList
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.yarnspace.app.R
import com.yarnspace.app.core.model.FeedItem
import com.yarnspace.app.core.model.UserSummary
import com.yarnspace.app.theme.resolveAccentColorInt

class FeedAdapter(
    private val onProjectClick: (FeedItem.Project) -> Unit,
    private val onReblogClick: (FeedItem.Project) -> Unit,
    private val onSaveClick: (FeedItem.Project) -> Unit
) : ListAdapter<FeedItem, FeedAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<FeedItem>() {
        override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
            val oldBase = oldItem as? FeedItem.Base
            val newBase = newItem as? FeedItem.Base
            return oldItem::class == newItem::class && oldBase?.id == newBase?.id
        }

        override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feed_card, parent, false)
        return VH(view, onProjectClick, onReblogClick, onSaveClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        itemView: View,
        private val onProjectClick: (FeedItem.Project) -> Unit,
        private val onReblogClick: (FeedItem.Project) -> Unit,
        private val onSaveClick: (FeedItem.Project) -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {

        private val topBar: View = itemView.findViewById(R.id.llFeedItemTopBar)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivFeedItemAvatar)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvFeedItemType)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvFeedItemAuthor)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvFeedItemTitle)
        private val tvContent: TextView = itemView.findViewById(R.id.tvFeedItemContent)
        private val ivImage: ImageView = itemView.findViewById(R.id.ivFeedItemImage)
        private val tvReblogNotice: TextView = itemView.findViewById(R.id.tvRebloggedNotice)
        private val llActions: View = itemView.findViewById(R.id.llFeedActions)
        private val btnReblog: ImageButton = itemView.findViewById(R.id.btnReblog)
        private val btnSave: ImageButton = itemView.findViewById(R.id.btnSave)

        fun bind(item: FeedItem) {
            val base = item as? FeedItem.Base

            val displayAuthor: UserSummary? = when (item) {
                is FeedItem.Post -> item.rebloggedProject?.author ?: item.author
                is FeedItem.Project -> item.author
                else -> base?.author
            }

            tvAuthor.text = displayAuthor?.let { "@${it.username}" } ?: ""
            val avatarRes = displayAuthor?.avatarResId ?: R.drawable.ic_default_avatar
            ivAvatar.setImageResource(avatarRes)

            if (displayAuthor != null) {
                val accentInt = displayAuthor.resolveAccentColorInt(itemView.context)
                topBar.backgroundTintList = ColorStateList.valueOf(accentInt)
            } else {
                topBar.backgroundTintList = null
            }

            if (base != null) {
                tvTimestamp.text = DateUtils.getRelativeTimeSpanString(
                    base.createdAt,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )
            }

            when (item) {
                is FeedItem.Post -> {
                    if (item.rebloggedProject != null) {
                        tvReblogNotice.visibility = View.VISIBLE
                        tvReblogNotice.text = itemView.context.getString(
                            R.string.feed_reblogged_notice,
                            item.author.displayName
                        )

                        tvTitle.visibility = View.VISIBLE
                        tvTitle.text = item.rebloggedProject.title
                        tvContent.text = item.rebloggedProject.content ?: ""

                        val img = item.rebloggedProject.imageResId
                        if (img != null) {
                            ivImage.visibility = View.VISIBLE
                            ivImage.setImageResource(img)
                        } else {
                            ivImage.visibility = View.GONE
                        }

                        llActions.visibility = View.VISIBLE
                        btnReblog.visibility = View.VISIBLE
                        val activeTint = item.rebloggedProject.author.resolveAccentColorInt(itemView.context)
                        setupReblogButton(item.rebloggedProject, activeTint)
                        setupSaveButton(item.rebloggedProject, activeTint)

                        itemView.setOnClickListener { onProjectClick(item.rebloggedProject) }
                        itemView.isClickable = true
                    } else {
                        tvReblogNotice.visibility = View.GONE
                        tvTitle.visibility = View.GONE
                        tvContent.text = item.content
                        llActions.visibility = View.GONE

                        val img = item.imageResId
                        if (img != null) {
                            ivImage.visibility = View.VISIBLE
                            ivImage.setImageResource(img)
                        } else {
                            ivImage.visibility = View.GONE
                        }
                        itemView.setOnClickListener(null)
                        itemView.isClickable = false
                    }
                }

                is FeedItem.Project -> {
                    tvReblogNotice.visibility = View.GONE
                    tvTitle.visibility = View.VISIBLE
                    tvTitle.text = item.title
                    tvContent.text = item.content ?: ""

                    val img = item.imageResId
                    if (img != null) {
                        ivImage.visibility = View.VISIBLE
                        ivImage.setImageResource(img)
                    } else {
                        ivImage.visibility = View.GONE
                    }

                    llActions.visibility = View.VISIBLE
                    btnReblog.visibility = View.VISIBLE
                    val activeTint = item.author.resolveAccentColorInt(itemView.context)
                    setupReblogButton(item, activeTint)
                    setupSaveButton(item, activeTint)

                    itemView.setOnClickListener { onProjectClick(item) }
                    itemView.isClickable = true
                }

                else -> {
                    tvReblogNotice.visibility = View.GONE
                    tvTitle.visibility = View.GONE
                    tvContent.text = ""
                    llActions.visibility = View.GONE
                    ivImage.visibility = View.GONE
                    itemView.setOnClickListener(null)
                    itemView.isClickable = false
                }
            }
        }

        private fun setupSaveButton(project: FeedItem.Project, activeTint: Int) {
            val isSaved = project.isSavedByMe
            btnSave.setImageResource(if (isSaved) R.drawable.ic_star_filled else R.drawable.ic_star_outline)

            val tint = if (isSaved) activeTint else MaterialColors.getColor(btnSave, com.google.android.material.R.attr.colorOnSurfaceVariant)
            btnSave.imageTintList = ColorStateList.valueOf(tint)

            btnSave.setOnClickListener { onSaveClick(project) }
        }

        private fun setupReblogButton(project: FeedItem.Project, activeTint: Int) {
            val isReblogged = project.isRebloggedByMe
            btnReblog.setImageResource(if (isReblogged) R.drawable.ic_reblog_filled else R.drawable.ic_reblog_outline)

            val tint = if (isReblogged) activeTint else MaterialColors.getColor(btnReblog, com.google.android.material.R.attr.colorOnSurfaceVariant)
            btnReblog.imageTintList = ColorStateList.valueOf(tint)

            btnReblog.setOnClickListener { onReblogClick(project) }
        }
    }
}

