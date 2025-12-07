package com.smartcbwtf.mobile.di

import com.smartcbwtf.mobile.BuildConfig
import com.smartcbwtf.mobile.bluetooth.MockScale
import com.smartcbwtf.mobile.bluetooth.MockScaleService
import com.smartcbwtf.mobile.bluetooth.RealBluetoothScaleService
import com.smartcbwtf.mobile.bluetooth.RealScale
import com.smartcbwtf.mobile.bluetooth.ScaleService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ScaleModule {

    @Binds
    @Singleton
    @MockScale
    abstract fun bindMockScaleService(service: MockScaleService): ScaleService

    @Binds
    @Singleton
    @RealScale
    abstract fun bindRealScaleService(service: RealBluetoothScaleService): ScaleService

    companion object {
        @Provides
        @Singleton
        fun provideScaleService(
            @RealScale real: ScaleService,
            @MockScale mock: ScaleService
        ): ScaleService {
            val useMock = BuildConfig.DEBUG
            return if (useMock) mock else real
        }
    }
}
