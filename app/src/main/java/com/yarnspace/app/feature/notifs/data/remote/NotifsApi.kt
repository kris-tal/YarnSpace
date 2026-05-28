package com.yarnspace.app.feature.notifs.data.remote

import com.yarnspace.app.feature.notifs.data.remote.dto.UnreadNotifsCountDto
import com.yarnspace.app.feature.notifs.data.remote.dto.NotifDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.Response
interface NotifsApi {

    @GET("notifs/unread-count")
    suspend fun getUnreadCount(): UnreadNotifsCountDto

    @GET("notifs/")
    suspend fun listMyNotifications(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): List<NotifDto>

    @POST("notifs/mark-all-read")
    suspend fun markAllRead(): Response<Unit>
}


