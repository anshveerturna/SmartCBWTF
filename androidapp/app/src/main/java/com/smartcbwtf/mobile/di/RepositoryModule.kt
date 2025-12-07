package com.smartcbwtf.mobile.di

import com.smartcbwtf.mobile.repository.AuthRepository
import com.smartcbwtf.mobile.repository.BagEventRepository
import com.smartcbwtf.mobile.repository.DefaultAuthRepository
import com.smartcbwtf.mobile.repository.DefaultBagEventRepository
import com.smartcbwtf.mobile.repository.DefaultHcfRepository
import com.smartcbwtf.mobile.repository.HcfRepository
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
    abstract fun bindAuthRepository(repo: DefaultAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindHcfRepository(repo: DefaultHcfRepository): HcfRepository

    @Binds
    @Singleton
    abstract fun bindBagEventRepository(repo: DefaultBagEventRepository): BagEventRepository
}
