package com.yarnspace.app.feature.notifs.presentation

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

class NotifsAdapter : ListAdapter<Notif, NotifsAdapter.NotifViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notif, parent, false)
        return NotifViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotifViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvNotifMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvNotifTime)

        fun bind(item: Notif) {
            tvMessage.text = item.message

            val time = DateUtils.getRelativeTimeSpanString(
                item.createdAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE,
            )
            tvTime.text = time

            itemView.alpha = if (item.readAt == null) 1f else 0.65f
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Notif>() {
            override fun areItemsTheSame(oldItem: Notif, newItem: Notif): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Notif, newItem: Notif): Boolean = oldItem == newItem
        }
    }
}

