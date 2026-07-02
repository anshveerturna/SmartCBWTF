package com.smartcbwtf.mobile.di

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
    @RealScale
    abstract fun bindRealScaleService(service: RealBluetoothScaleService): ScaleService

    companion object {
        @Provides
        @Singleton
        fun provideScaleService(
            @RealScale real: ScaleService
        ): ScaleService {
            return real
        }
    }
}
