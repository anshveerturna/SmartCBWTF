package com.smartcbwtf.mobile.di

import android.content.Context
import androidx.room.Room
import com.smartcbwtf.mobile.database.AppDatabase
import com.smartcbwtf.mobile.database.dao.AttendanceDao
import com.smartcbwtf.mobile.database.dao.BagEventDao
import com.smartcbwtf.mobile.database.dao.HcfDao
import com.smartcbwtf.mobile.database.dao.UserProfileDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "smartcbwtf.db")
            .build()
    }

    @Provides
    fun provideBagEventDao(db: AppDatabase): BagEventDao = db.bagEventDao()

    @Provides
    fun provideHcfDao(db: AppDatabase): HcfDao = db.hcfDao()

    @Provides
    fun provideAttendanceDao(db: AppDatabase): AttendanceDao = db.attendanceDao()

    @Provides
    fun provideUserProfileDao(db: AppDatabase): UserProfileDao = db.userProfileDao()
}
