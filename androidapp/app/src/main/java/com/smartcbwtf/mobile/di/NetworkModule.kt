package com.smartcbwtf.mobile.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.smartcbwtf.mobile.BuildConfig
import com.smartcbwtf.mobile.network.AuthInterceptor
import com.smartcbwtf.mobile.network.api.AttendanceApi
import com.smartcbwtf.mobile.network.api.AuthApi
import com.smartcbwtf.mobile.network.api.BagEventApi
import com.smartcbwtf.mobile.network.api.HcfApi
import com.smartcbwtf.mobile.network.api.LocationApi
import com.smartcbwtf.mobile.network.api.ProfileApi
import com.smartcbwtf.mobile.network.api.VerificationApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
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
        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)

        // if (!BuildConfig.DEBUG) {
        //     // TODO: Add real certificate pin for production
        //     val certificatePinner = CertificatePinner.Builder()
        //         .add("api.smartcbwtf.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        //         .build()
        //     builder.certificatePinner(certificatePinner)
        // }

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
                redactHeader("Authorization")
            }
            builder.addInterceptor(logging)
        }
        return builder.build()
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

    @Provides
    @Singleton
    fun provideVerificationApi(retrofit: Retrofit): VerificationApi = retrofit.create(VerificationApi::class.java)

    @Provides
    @Singleton
    fun provideAttendanceApi(retrofit: Retrofit): AttendanceApi = retrofit.create(AttendanceApi::class.java)

    @Provides
    @Singleton
    fun provideProfileApi(retrofit: Retrofit): ProfileApi = retrofit.create(ProfileApi::class.java)

    @Provides
    @Singleton
    fun provideLocationApi(retrofit: Retrofit): LocationApi = retrofit.create(LocationApi::class.java)

    @Provides
    @Singleton
    fun provideRouteApi(retrofit: Retrofit): com.smartcbwtf.mobile.network.api.RouteApi = 
        retrofit.create(com.smartcbwtf.mobile.network.api.RouteApi::class.java)
}
