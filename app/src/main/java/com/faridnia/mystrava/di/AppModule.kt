package com.faridnia.mystrava.di

import android.content.Context
import androidx.room.Room
import com.faridnia.mystrava.db.RunDatabase
import com.faridnia.mystrava.other.Constants.DATABASE_NAME
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

}