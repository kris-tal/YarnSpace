package com.yarnspace.app.feature.notifs.data

import com.yarnspace.app.feature.notifs.data.remote.NotifsApi
import com.yarnspace.app.feature.notifs.domain.model.Notif
import com.yarnspace.app.feature.notifs.domain.NotifsRepository
import javax.inject.Inject

class RemoteNotifsRepository @Inject constructor(
    private val notifsApi: NotifsApi,
) : NotifsRepository {

    override suspend fun getUnreadCount(): Int {
        return notifsApi.getUnreadCount().count
    }

    override suspend fun listMyNotifications(limit: Int, offset: Int): List<Notif> {
        return notifsApi.listMyNotifications(limit = limit, offset = offset)
            .map {
                Notif(
                    id = it.id,
                    type = it.type,
                    message = it.message,
                    createdAt = it.createdAt,
                    readAt = it.readAt,
                    actorUsername = it.actor?.username,
                )
            }
    }

    override suspend fun markAllRead(): Result<Unit> {
        return try {
            notifsApi.markAllRead()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


