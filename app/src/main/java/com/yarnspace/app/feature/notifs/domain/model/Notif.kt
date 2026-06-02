package com.yarnspace.app.feature.notifs.domain.model

sealed interface Notif {
    val id: Long
    val createdAt: Long
    val isRead: Boolean

    data class Follow(
        override val id: Long,
        override val createdAt: Long,
        override val isRead: Boolean,
        val followerUsername: String
    ) : Notif

    data class Reblog(
        override val id: Long,
        override val createdAt: Long,
        override val isRead: Boolean,
        val rebloggerUsername: String
    ) : Notif

    data class Save(
        override val id: Long,
        override val createdAt: Long,
        override val isRead: Boolean,
        val saverUsername: String
    ) : Notif

    data class Unknown(
        override val id: Long,
        override val createdAt: Long,
        override val isRead: Boolean
    ) : Notif
}