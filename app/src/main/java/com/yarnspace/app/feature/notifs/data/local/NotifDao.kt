package com.yarnspace.app.feature.notifs.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotifDao {

    @Query("SELECT * FROM notifs ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<NotifEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNotifications(notifications: List<NotifEntity>): List<Long>

    @Query("UPDATE notifs SET readAt = :timestamp WHERE readAt IS NULL")
    fun markAllAsRead(timestamp: Long): Int

    @Query("SELECT COALESCE(MAX(id), 0) FROM notifs")
    fun getMaxId(): Long

    @Query("DELETE FROM notifs WHERE readAt IS NOT NULL AND id NOT IN (SELECT id FROM notifs WHERE readAt IS NOT NULL ORDER BY createdAt DESC LIMIT 50)")
    fun deleteOldReadNotifications(): Int

    @Query("DELETE FROM notifs")
    fun deleteAll(): Int
}