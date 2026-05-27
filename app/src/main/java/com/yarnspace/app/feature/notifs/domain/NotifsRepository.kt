package com.yarnspace.app.feature.notifs.domain

import com.yarnspace.app.feature.notifs.domain.model.Notif

interface NotifsRepository {
    suspend fun getUnreadCount(): Int

    suspend fun listMyNotifications(limit: Int = 50, offset: Int = 0): List<Notif>

    suspend fun markAllRead(): Result<Unit>
}



