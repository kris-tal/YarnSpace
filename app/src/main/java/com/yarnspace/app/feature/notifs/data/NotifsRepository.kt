package com.yarnspace.app.feature.notifs.data

import com.yarnspace.app.feature.notifs.domain.model.Notif
import kotlinx.coroutines.flow.Flow

interface NotifsRepository {
    fun getNotifications(): Flow<List<Notif>>
    suspend fun markAllRead(): Result<Unit>
    suspend fun getUnreadCount(): Int
}