package com.yarnspace.app.di

import com.yarnspace.app.feature.feed.data.FeedRepository
import com.yarnspace.app.feature.feed.data.RemoteFeedRepository
import com.yarnspace.app.feature.profile.data.ProfileRepository
import com.yarnspace.app.feature.profile.data.RemoteProfileRepository
import com.yarnspace.app.feature.search.data.ProjectSearchRepository
import com.yarnspace.app.feature.search.data.RemoteProjectSearchRepository
import com.yarnspace.app.feature.search.data.RemoteUserRepository
import com.yarnspace.app.feature.search.data.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFeedRepository(impl: RemoteFeedRepository): FeedRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: RemoteUserRepository): UserRepository

    @Binds
    @Singleton
    abstract fun bindProjectSearchRepository(impl: RemoteProjectSearchRepository): ProjectSearchRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: RemoteProfileRepository): ProfileRepository
}


