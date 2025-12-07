package com.smartcbwtf.mobile.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.smartcbwtf.mobile.BuildConfig
import com.smartcbwtf.mobile.network.AuthInterceptor
import com.smartcbwtf.mobile.network.api.AuthApi
import com.smartcbwtf.mobile.network.api.BagEventApi
import com.smartcbwtf.mobile.network.api.HcfApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideOkHttp(authInterceptor: AuthInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideHcfApi(retrofit: Retrofit): HcfApi = retrofit.create(HcfApi::class.java)

    @Provides
    @Singleton
    fun provideBagEventApi(retrofit: Retrofit): BagEventApi = retrofit.create(BagEventApi::class.java)
}
