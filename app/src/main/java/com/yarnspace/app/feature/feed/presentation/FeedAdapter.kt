package com.yarnspace.app.feature.feed.presentation

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.yarnspace.app.R
import com.yarnspace.app.core.model.FeedItem
import com.yarnspace.app.core.model.UserSummary
import com.yarnspace.app.core.util.UrlUtils
import com.yarnspace.app.theme.AccentColor
import com.yarnspace.app.theme.AvatarIcon

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
        private val cvAvatarContainer: MaterialCardView = itemView.findViewById(R.id.cvFeedItemAvatarContainer)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivFeedItemAvatar)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvFeedItemType)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvFeedItemAuthor)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvFeedItemTitle)
        private val tvContent: TextView = itemView.findViewById(R.id.tvFeedItemContent)
        private val ivImage: ImageView = itemView.findViewById(R.id.ivFeedItemImage)

        private val llRebloggedNotice: LinearLayout = itemView.findViewById(R.id.llRebloggedNotice)
        private val ivRebloggedNoticeIcon: ImageView = itemView.findViewById(R.id.ivRebloggedNoticeIcon)
        private val tvRebloggedNoticeText: TextView = itemView.findViewById(R.id.tvRebloggedNoticeText)

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

            val context = itemView.context
            val isNightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            if (displayAuthor != null) {
                val theme = AccentColor.fromBackendName(displayAuthor.accentColor)

                val topBarRes = if (isNightMode) theme.nightColorResId else theme.colorResId
                val authorColor = ContextCompat.getColor(context, topBarRes)

                topBar.backgroundTintList = ColorStateList.valueOf(authorColor)

                val onPrimaryColor = MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorOnPrimary)

                tvAuthor.setTextColor(onPrimaryColor)

                val bgColor = ContextCompat.getColor(context, theme.getLighterBg(isNightMode))
                val iconColor = ContextCompat.getColor(context, theme.getDarkerIcon(isNightMode))

                cvAvatarContainer.setCardBackgroundColor(bgColor)
                ivAvatar.setColorFilter(iconColor)

                val iconEnum = AvatarIcon.fromBackendName(displayAuthor.avatarIcon)
                ivAvatar.setImageResource(iconEnum.resId)

            } else {
                topBar.backgroundTintList = null
                ivAvatar.setImageResource(R.drawable.ic_avatar_default)
                ivAvatar.clearColorFilter()
                cvAvatarContainer.strokeWidth = 0
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
                        llRebloggedNotice.visibility = View.VISIBLE

                        val projectTheme = AccentColor.fromBackendName(item.rebloggedProject.author.accentColor)
                        val activeTint = ContextCompat.getColor(context, if (isNightMode) projectTheme.nightColorResId else projectTheme.colorResId)

                        ivRebloggedNoticeIcon.imageTintList = ColorStateList.valueOf(activeTint)

                        val authorUsername = item.author.username
                        tvRebloggedNoticeText.text = "reblogged by @$authorUsername"

                        tvTitle.visibility = View.VISIBLE
                        tvTitle.text = item.rebloggedProject.title
                        tvContent.text = item.rebloggedProject.content ?: ""

                        bindContentImage(
                            view = ivImage,
                            imageUrl = item.rebloggedProject.imageUrl,
                            imageResId = item.rebloggedProject.imageResId,
                        )

                        llActions.visibility = View.VISIBLE
                        btnReblog.visibility = View.VISIBLE

                        setupReblogButton(item.rebloggedProject, activeTint)
                        setupSaveButton(item.rebloggedProject, activeTint)

                        itemView.setOnClickListener { onProjectClick(item.rebloggedProject) }
                        itemView.isClickable = true
                    } else {
                        llRebloggedNotice.visibility = View.GONE
                        tvTitle.visibility = View.GONE
                        tvContent.text = item.content
                        llActions.visibility = View.GONE

                        bindContentImage(
                            view = ivImage,
                            imageUrl = item.imageUrl,
                            imageResId = item.imageResId,
                        )
                        itemView.setOnClickListener(null)
                        itemView.isClickable = false
                    }
                }

                is FeedItem.Project -> {
                    llRebloggedNotice.visibility = View.GONE
                    tvTitle.visibility = View.VISIBLE
                    tvTitle.text = item.title
                    tvContent.text = item.content ?: ""

                    bindContentImage(
                        view = ivImage,
                        imageUrl = item.imageUrl,
                        imageResId = item.imageResId,
                    )

                    llActions.visibility = View.VISIBLE
                    btnReblog.visibility = View.VISIBLE

                    val projectTheme = AccentColor.fromBackendName(item.author.accentColor)
                    val activeTint = ContextCompat.getColor(context, if (isNightMode) projectTheme.nightColorResId else projectTheme.colorResId)

                    setupReblogButton(item, activeTint)
                    setupSaveButton(item, activeTint)

                    itemView.setOnClickListener { onProjectClick(item) }
                    itemView.isClickable = true
                }

                else -> {
                    llRebloggedNotice.visibility = View.GONE
                    tvTitle.visibility = View.GONE
                    tvContent.text = ""
                    llActions.visibility = View.GONE
                    ivImage.visibility = View.GONE
                    itemView.setOnClickListener(null)
                    itemView.isClickable = false
                }
            }
        }

        private fun bindContentImage(view: ImageView, imageUrl: String?, imageResId: Int?) {
            val url = UrlUtils.resolve(imageUrl)
            when {
                url != null -> {
                    view.visibility = View.VISIBLE
                    view.load(url) {
                        crossfade(true)
                    }
                }

                imageResId != null -> {
                    view.visibility = View.VISIBLE
                    view.setImageResource(imageResId)
                }

                else -> {
                    view.visibility = View.GONE
                }
            }
        }

        private fun setupSaveButton(project: FeedItem.Project, activeTint: Int) {
            val isSaved = project.isSavedByMe
            btnSave.setImageResource(if (isSaved) R.drawable.ic_star_filled else R.drawable.ic_star_outline)

            val tint = if (isSaved) activeTint else MaterialColors.getColor(btnSave, com.google.android.material.R.attr.colorOnSurfaceVariant)
            btnSave.imageTintList = ColorStateList.valueOf(tint)
            btnSave.imageAlpha = if (isSaved) 255 else 170

            btnSave.setOnClickListener { onSaveClick(project) }
        }

        private fun setupReblogButton(project: FeedItem.Project, activeTint: Int) {
            val isReblogged = project.isRebloggedByMe
            btnReblog.setImageResource(if (isReblogged) R.drawable.ic_reblog_filled else R.drawable.ic_reblog_outline)

            val tint = if (isReblogged) activeTint else MaterialColors.getColor(btnReblog, com.google.android.material.R.attr.colorOnSurfaceVariant)
            btnReblog.imageTintList = ColorStateList.valueOf(tint)
            btnReblog.imageAlpha = if (isReblogged) 255 else 170

            btnReblog.setOnClickListener { onReblogClick(project) }
        }
    }
}