package com.yarnspace.app.feature.notifs.domain

import com.yarnspace.app.feature.notifs.domain.model.Notif
import kotlinx.coroutines.flow.Flow

interface NotifsRepository {
    fun getNotifications(): Flow<List<Notif>>
    suspend fun syncNotifications()
    suspend fun markAllRead(): Result<Unit>
    suspend fun getUnreadCount(): Int
    suspend fun clearLocalData()
}
