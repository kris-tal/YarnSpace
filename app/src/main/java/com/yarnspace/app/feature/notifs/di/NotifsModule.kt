package com.yarnspace.app.feature.notifs.di

import com.yarnspace.app.feature.notifs.data.RemoteNotifsRepository
import com.yarnspace.app.feature.notifs.data.remote.NotifsApi
import com.yarnspace.app.feature.notifs.domain.NotifsRepository
import com.yarnspace.app.feature.notifs.work.NotifsWorkScheduler
import com.yarnspace.app.feature.notifs.work.WorkManagerNotifsWorkScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotifsNetworkModule {

    @Provides
    @Singleton
    fun provideNotifsApi(retrofit: Retrofit): NotifsApi {
        return retrofit.create(NotifsApi::class.java)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NotifsBindingsModule {

    @Binds
    @Singleton
    abstract fun bindNotifsRepository(impl: RemoteNotifsRepository): NotifsRepository

    @Binds
    @Singleton
    abstract fun bindNotifsWorkScheduler(impl: WorkManagerNotifsWorkScheduler): NotifsWorkScheduler
}

