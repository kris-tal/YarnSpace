package com.yarnspace.app.retrofit

import com.yarnspace.app.data.AuthResponse
import com.yarnspace.app.data.RegisterRequest
import com.yarnspace.app.data.User
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("auth/logout")
    suspend fun logout()

    @GET("users/me")
    suspend fun getMe(): User
}
