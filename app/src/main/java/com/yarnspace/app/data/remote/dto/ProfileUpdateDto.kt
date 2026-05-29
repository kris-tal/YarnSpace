package com.yarnspace.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProfileUpdateDto(
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("accentColor") val accentColor: String? = null,
    @SerializedName("avatarIcon") val avatarIcon: String? = null
)