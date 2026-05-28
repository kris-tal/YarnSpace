package com.yarnspace.app.di

import android.content.Context
import androidx.room.Room
import com.yarnspace.app.core.data.local.AppDatabase
import com.yarnspace.app.feature.notifs.data.local.NotifDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "yarnspace_db"
        ).build()
    }

    @Provides
    fun provideNotifDao(database: AppDatabase): NotifDao {
        return database.notifDao()
    }
}
