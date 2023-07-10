package com.faridnia.mystrava.di

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.room.Room
import com.faridnia.mystrava.db.RunDatabase
import com.faridnia.mystrava.other.Constants.DATABASE_NAME
import com.faridnia.mystrava.other.Constants.IS_FIRST_TIME
import com.faridnia.mystrava.other.Constants.KEY_NAME
import com.faridnia.mystrava.other.Constants.KEY_WEIGHT
import com.faridnia.mystrava.other.Constants.SHARED_PREF_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideDatabase(
        @ApplicationContext app: Context
    ): RunDatabase {
        return Room.databaseBuilder(
            app,
            RunDatabase::class.java,
            DATABASE_NAME
        ).build()
    }

    @Singleton
    @Provides
    fun provideRunDao(db: RunDatabase) = db.getRunDao()

    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext context: Context) =
        context.getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)

    @Singleton
    @Provides
    fun provideName(sharedPref: SharedPreferences) =
        sharedPref.getString(KEY_NAME, "") ?: ""


    @Singleton
    @Provides
    fun provideWeight(sharedPref: SharedPreferences) =
        sharedPref.getInt(KEY_WEIGHT, 80) ?: 0

    @Singleton
    @Provides
    fun provideIsFirstTime(sharedPref: SharedPreferences) =
        sharedPref.getBoolean(IS_FIRST_TIME, true)


}