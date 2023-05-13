package com.faridnia.mystrava.db

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "running_table")
data class Run(
    val timeStamp: Long = 0L, //when the run was
    val runDurationInMillis: Long = 0L,
    val distanceInMeters: Int = 0,
    val avgSpeedInKMH: Float = 0F,
    val image: Bitmap? = null,
    val caloriesBurned: Int = 0
) {
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null
}