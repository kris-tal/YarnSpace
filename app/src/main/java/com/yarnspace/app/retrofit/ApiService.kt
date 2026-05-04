package com.yarnspace.app.retrofit

import com.yarnspace.app.data.AuthResponse
import com.yarnspace.app.data.RegisterRequest
import com.yarnspace.app.data.remote.dto.UserPublicDto
import com.yarnspace.app.data.remote.dto.UserPrivateDto
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

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
    suspend fun getMe(): UserPrivateDto

    @GET("users/search")
    suspend fun searchUsers(@Query("q") query: String): List<UserPublicDto>
}
