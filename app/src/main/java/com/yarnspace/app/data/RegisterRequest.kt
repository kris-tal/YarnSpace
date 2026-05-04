package com.yarnspace.app.data

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("nick") val nick: String,
    @SerializedName("password") val password: String,
    @SerializedName("accentColor") val accentColor: String = "sage"
)
