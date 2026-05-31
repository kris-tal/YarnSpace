package com.yarnspace.app.feature.notifs.data

import com.yarnspace.app.R
import com.yarnspace.app.feature.notifs.data.local.NotifDao
import com.yarnspace.app.feature.notifs.data.local.NotifEntity
import com.yarnspace.app.feature.notifs.data.remote.NotifsApi
import com.yarnspace.app.feature.notifs.domain.NotifsRepository
import com.yarnspace.app.feature.notifs.domain.model.Notif
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext

class RemoteNotifsRepository @Inject constructor(
    private val notifsApi: NotifsApi,
    private val notifDao: NotifDao,
    @ApplicationContext private val context: Context
) : NotifsRepository {

    override fun getNotifications(): Flow<List<Notif>> {
        return notifDao.getAllNotifications().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun syncNotifications() {
        withContext(Dispatchers.IO) {
            val lastId = notifDao.getMaxId()

            val remoteNotifs = notifsApi.listMyNotifications(limit = 100, offset = 0)

            val entities = remoteNotifs.map { dto ->
                NotifEntity(
                    id = dto.id,
                    type = dto.type,
                    createdAt = parseDateToMillis(dto.createdAt),
                    readAt = dto.readAt?.let { parseDateToMillis(it) },
                    actorUsername = dto.actor?.username ?: context.getString(R.string.unknown_user)
                )
            }

            notifDao.insertNotifications(entities)
            notifDao.deleteOldReadNotifications()
        }
    }

    private fun parseDateToMillis(dateStr: String): Long {
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

    override suspend fun markAllRead(): Result<Unit> {
        return try {
            notifsApi.markAllRead()
            withContext(Dispatchers.IO) {
                notifDao.markAllAsRead(System.currentTimeMillis())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUnreadCount(): Int {
        return notifsApi.getUnreadCount().count
    }

    private fun NotifEntity.toDomain(): Notif {
        val isRead = readAt != null
        return when (type.lowercase()) {
            "follow" -> Notif.Follow(id, createdAt, isRead, actorUsername)
            "reblog" -> Notif.Reblog(id, createdAt, isRead, actorUsername)
            "save" -> Notif.Save(id, createdAt, isRead, actorUsername)
            else -> Notif.Unknown(id, createdAt, isRead)
        }
    }

    override suspend fun clearLocalData() {
        withContext(Dispatchers.IO) {
            notifDao.deleteAll()
        }
    }
}
