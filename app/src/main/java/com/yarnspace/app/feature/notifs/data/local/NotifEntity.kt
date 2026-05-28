package com.yarnspace.app.feature.notifs.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifs")
data class NotifEntity(
    @PrimaryKey val id: Long,
    val type: String, // follow | reblog | save
    val createdAt: Long,
    val readAt: Long?,
    val actorUsername: String
)
