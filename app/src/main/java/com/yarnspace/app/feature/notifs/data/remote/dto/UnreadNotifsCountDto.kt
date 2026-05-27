package com.yarnspace.app.feature.notifs.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UnreadNotifsCountDto(
    @SerializedName("count")
    val count: Int,
)

