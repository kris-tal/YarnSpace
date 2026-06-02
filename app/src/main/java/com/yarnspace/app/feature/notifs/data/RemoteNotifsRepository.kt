package com.yarnspace.app.feature.notifs.data

import android.content.Context
import com.yarnspace.app.R
import com.yarnspace.app.core.network.ApiService
import com.yarnspace.app.data.remote.dto.NotifDto
import com.yarnspace.app.feature.notifs.domain.model.Notif
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class RemoteNotifsRepository @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) : NotifsRepository {

    override fun getNotifications(): Flow<List<Notif>> = flow {
        val remoteNotifs = apiService.listMyNotifications(limit = 100, offset = 0)
        val domainNotifs = remoteNotifs.map { it.toDomain() }
        emit(domainNotifs)
    }.flowOn(Dispatchers.IO)

    override suspend fun markAllRead(): Result<Unit> {
        return try {
            apiService.markAllRead()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUnreadCount(): Int {
        return try {
            apiService.getUnreadCount().count
        } catch (e: Exception) {
            0
        }
    }

    private fun NotifDto.toDomain(): Notif {
        val isRead = readAt != null
        val actorUsername = actor?.username ?: context.getString(R.string.unknown_user)

        return when (type?.lowercase()) {
            "follow" -> Notif.Follow(id, parseDateToMillis(createdAt ?: ""), isRead, actorUsername)
            "reblog" -> Notif.Reblog(id, parseDateToMillis(createdAt ?: ""), isRead, actorUsername)
            "save" -> Notif.Save(id, parseDateToMillis(createdAt ?: ""), isRead, actorUsername)
            else -> Notif.Unknown(id, parseDateToMillis(createdAt ?: ""), isRead)
        }
    }

    private fun parseDateToMillis(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        return try {
            dateStr.toLong()
        } catch (e: NumberFormatException) {
            try {
                java.time.OffsetDateTime.parse(dateStr).toInstant().toEpochMilli()
            } catch (ex: Exception) {
                try {
                    java.time.LocalDateTime.parse(dateStr)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                } catch (innerEx: Exception) {
                    System.currentTimeMillis()
                }
            }
        }
    }
}