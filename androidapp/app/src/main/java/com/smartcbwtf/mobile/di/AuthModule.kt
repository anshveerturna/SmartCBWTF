package com.smartcbwtf.mobile.di

import com.smartcbwtf.mobile.storage.AuthTokenStore
import com.smartcbwtf.mobile.storage.DefaultAuthTokenStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthTokenStore(store: DefaultAuthTokenStore): AuthTokenStore
}
