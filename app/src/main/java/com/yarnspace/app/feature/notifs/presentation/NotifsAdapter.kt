package com.yarnspace.app.feature.notifs.presentation

import android.text.Html
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yarnspace.app.R
import com.yarnspace.app.feature.notifs.domain.model.Notif

class NotifsAdapter : ListAdapter<NotifsAdapter.NotifItem, RecyclerView.ViewHolder>(DIFF) {

    sealed class NotifItem {
        data class Data(val notif: Notif) : NotifItem()
        object Divider : NotifItem()
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is NotifItem.Data -> TYPE_DATA
            is NotifItem.Divider -> TYPE_DIVIDER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DATA -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notif, parent, false)
                NotifViewHolder(view)
            }
            TYPE_DIVIDER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notif_divider, parent, false)
                DividerViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is NotifViewHolder && item is NotifItem.Data) {
            holder.bind(item.notif)
        }
    }

    fun submitNotifications(list: List<Notif>) {
        val items = mutableListOf<NotifItem>()
        val unread = list.filter { !it.isRead }
        val read = list.filter { it.isRead }

        if (unread.isNotEmpty()) {
            items.addAll(unread.map { NotifItem.Data(it) })
        }

        if (unread.isNotEmpty() && read.isNotEmpty()) {
            items.add(NotifItem.Divider)
        }

        if (read.isNotEmpty()) {
            items.addAll(read.map { NotifItem.Data(it) })
        }

        submitList(items)
    }

    class NotifViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvNotifMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvNotifTime)

        fun bind(item: Notif) {
            val context = itemView.context

            val messageText = when (item) {
                is Notif.Follow -> context.getString(R.string.notif_follow, item.followerUsername)
                is Notif.Reblog -> context.getString(R.string.notif_reblog, item.rebloggerUsername)
                is Notif.Save -> context.getString(R.string.notif_save, item.saverUsername)
                is Notif.Unknown -> context.getString(R.string.notif_unknown)
            }

            tvMessage.text = Html.fromHtml(messageText, Html.FROM_HTML_MODE_LEGACY)

            tvTime.text = DateUtils.getRelativeTimeSpanString(
                item.createdAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            )

            itemView.alpha = if (!item.isRead) 1f else 0.65f
        }
    }

    class DividerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    companion object {
        private const val TYPE_DATA = 0
        private const val TYPE_DIVIDER = 1

        private val DIFF = object : DiffUtil.ItemCallback<NotifItem>() {
            override fun areItemsTheSame(oldItem: NotifItem, newItem: NotifItem): Boolean {
                return if (oldItem is NotifItem.Data && newItem is NotifItem.Data) {
                    oldItem.notif.id == newItem.notif.id
                } else {
                    oldItem == newItem
                }
            }
            override fun areContentsTheSame(oldItem: NotifItem, newItem: NotifItem): Boolean = oldItem == newItem
        }
    }
}