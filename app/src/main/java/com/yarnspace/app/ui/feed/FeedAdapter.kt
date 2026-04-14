package com.yarnspace.app.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yarnspace.app.R
import com.yarnspace.app.domain.feed.FeedItem

class FeedAdapter(private val onProjectClick: (FeedItem.Project) -> Unit) : ListAdapter<FeedItem, FeedAdapter.VH>(Diff) {

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
        return VH(view, onProjectClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        itemView: View,
        private val onProjectClick: (FeedItem.Project) -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvType: TextView = itemView.findViewById(R.id.tvFeedItemType)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvFeedItemAuthor)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvFeedItemTitle)
        private val tvContent: TextView = itemView.findViewById(R.id.tvFeedItemContent)
        private val ivImage: ImageView = itemView.findViewById(R.id.ivFeedItemImage)

        fun bind(item: FeedItem) {
            val base = item as? FeedItem.Base
            tvAuthor.text = if (base != null) "@${base.author.username}" else ""

            when (item) {
                is FeedItem.Post -> {
                    tvType.text = itemView.context.getString(R.string.feed_item_type_post)
                    tvTitle.visibility = View.GONE
                    tvContent.text = item.content
                    itemView.isClickable = false
                    itemView.setOnClickListener(null)
                }

                is FeedItem.Project -> {
                    tvType.text = itemView.context.getString(R.string.feed_item_type_project)
                    tvTitle.visibility = View.VISIBLE
                    tvTitle.text = item.title
                    tvContent.text = item.content ?: ""
                    itemView.isClickable = true
                    itemView.setOnClickListener { onProjectClick(item) }
                }

                else -> {
                    tvType.text = ""
                    tvTitle.visibility = View.GONE
                    tvContent.text = ""
                    itemView.isClickable = false
                    itemView.setOnClickListener(null)
                }
            }

            val img = base?.imageResId
            if (img is Int) {
                ivImage.visibility = View.VISIBLE
                ivImage.setImageResource(img)
            } else {
                ivImage.visibility = View.GONE
            }
        }
    }
}


