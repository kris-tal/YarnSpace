package com.yarnspace.app.feature.notifs.data.remote.dto

import com.google.gson.annotations.SerializedName

data class NotifDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("type")
    val type: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("createdAt")
    val createdAt: Long,
    @SerializedName("readAt")
    val readAt: Long?,
    @SerializedName("actor")
    val actor: NotifActorDto?,
)

data class NotifActorDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("username")
    val username: String,
    @SerializedName("displayName")
    val displayName: String,
    @SerializedName("avatarUrl")
    val avatarUrl: String?,
    @SerializedName("accentColor")
    val accentColor: String,
)

