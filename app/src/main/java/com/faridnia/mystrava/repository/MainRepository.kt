package com.faridnia.mystrava.repository

import com.faridnia.mystrava.db.Run
import com.faridnia.mystrava.db.RunDAO
import javax.inject.Inject

class MainRepository @Inject constructor(private val dao: RunDAO) {

    suspend fun insertRun(run: Run) = dao.insertRun(run)

    suspend fun deleteRun(run: Run) = dao.deleteRun(run)

    fun getAllRunsByRunDuration() = dao.getAllRunsByRunDuration()

    fun getAllRunsByAvgSpeed() = dao.getAllRunsByAvgSpeed()

    fun getAllRunsByDistance() = dao.getAllRunsByDistance()

    fun getAllRunsByDate() = dao.getAllRunsByDate()

    fun getAllRunsByCaloriesBurned() = dao.getAllRunsByCaloriesBurned()

    fun getTotalDistance() = dao.getTotalDistance()

    fun getTotalRunDuration()= dao.getTotalRunDuration()

    fun getTotalCaloriesBurned()= dao.getTotalCaloriesBurned()

    fun getTotalAvgSpeed()= dao.getTotalAvgSpeed()

}