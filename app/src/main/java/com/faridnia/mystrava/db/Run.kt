package com.faridnia.mystrava.db

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "running_table")
data class Run(
    var timestamp: Long = 0L, //when the run was
    var runDurationInMillis: Long = 0L,
    var distanceInMeters: Int = 0,
    var avgSpeedInKMH: Float = 0F,
    var image: Bitmap? = null,
    var caloriesBurned: Int = 0
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}