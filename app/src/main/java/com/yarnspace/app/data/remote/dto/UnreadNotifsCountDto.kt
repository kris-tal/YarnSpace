package com.yarnspace.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UnreadNotifsCountDto(
    @SerializedName("count")
    val count: Int,
)

