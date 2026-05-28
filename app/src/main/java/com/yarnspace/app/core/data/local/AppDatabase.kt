package com.yarnspace.app.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yarnspace.app.feature.notifs.data.local.NotifDao
import com.yarnspace.app.feature.notifs.data.local.NotifEntity

@Database(entities = [NotifEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notifDao(): NotifDao
}
