package com.yarnspace.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ImageUploadResponseDto(
    @SerializedName("imageUrl") val imageUrl: String,
)

