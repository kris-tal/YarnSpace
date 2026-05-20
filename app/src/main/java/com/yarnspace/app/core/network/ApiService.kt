package com.yarnspace.app.core.network

import com.yarnspace.app.data.AuthResponse
import com.yarnspace.app.data.RegisterRequest
import com.yarnspace.app.data.remote.dto.PostCreateDto
import com.yarnspace.app.data.remote.dto.PostReadDto
import com.yarnspace.app.data.remote.dto.ProfilePublicDto
import com.yarnspace.app.data.remote.dto.ProjectCreateDto
import com.yarnspace.app.data.remote.dto.ProjectReadDto
import com.yarnspace.app.data.remote.dto.UserPublicDto
import com.yarnspace.app.data.remote.dto.UserPrivateDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
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

    @GET("users/{username}")
    suspend fun getPublicProfile(@Path("username") username: String): ProfilePublicDto

    @GET("users/{username}/posts")
    suspend fun listUserPosts(
        @Path("username") username: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): List<PostReadDto>

    @GET("users/{username}/projects")
    suspend fun listUserProjects(
        @Path("username") username: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): List<ProjectReadDto>

    @GET("users/me/saved-projects")
    suspend fun listMySavedProjects(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): List<ProjectReadDto>

    @POST("users/{username}/follow")
    suspend fun followUser(@Path("username") username: String): Map<String, String>

    @DELETE("users/{username}/follow")
    suspend fun unfollowUser(@Path("username") username: String): Map<String, String>

    @POST("posts/")
    suspend fun createPost(@Body payload: PostCreateDto): PostReadDto

    @POST("projects/")
    suspend fun createProject(@Body payload: ProjectCreateDto): ProjectReadDto

    @POST("projects/{id}/save")
    suspend fun saveProject(@Path("id") projectId: Long): Map<String, String>

    @DELETE("projects/{id}/save")
    suspend fun unsaveProject(@Path("id") projectId: Long): Map<String, String>

    @GET("feed/")
    suspend fun getGlobalFeed(@Query("limit") limit: Int = 50): List<com.google.gson.JsonElement>
}

