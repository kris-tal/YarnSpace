package com.yarnspace.app.feature.notifs.domain.model

data class Notif(
    val id: Long,
    val type: String,
    val message: String,
    val createdAt: Long,
    val readAt: Long?,
    val actorUsername: String?,
)

