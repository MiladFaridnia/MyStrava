package com.faridnia.mystrava.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters


@Database(entities = [Run::class], version = 1)
@TypeConverters(MyTypeConverters::class)
abstract class RunDatabase : RoomDatabase() {

    abstract fun getRunDao(): RunDAO

}