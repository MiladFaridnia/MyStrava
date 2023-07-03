package com.faridnia.mystrava.ui.viewmodels

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faridnia.mystrava.db.Run
import com.faridnia.mystrava.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    private val mainRepository: MainRepository
) : ViewModel() {

    private var runsSortedByDate = mainRepository.getAllRunsByDate()
    private var runsSortedByDistance = mainRepository.getAllRunsByDistance()
    private var runsSortedByRunDuration = mainRepository.getAllRunsByRunDuration()
    private var runsSortedByAvgSpeed =mainRepository.getAllRunsByAvgSpeed()
    private var runsSortedByCaloriesBurned =mainRepository.getAllRunsByCaloriesBurned()

    val runs = MediatorLiveData<List<Run>?>()
    
    init {
        runs.addSource(runsSortedByDate) {
            runs.value = it
        }
        runs.addSource(runsSortedByDistance) {
            runs.value = it
        }
        runs.addSource(runsSortedByRunDuration) {
            runs.value = it
        }
        runs.addSource(runsSortedByAvgSpeed) {
            runs.value = it
        }
        runs.addSource(runsSortedByCaloriesBurned) {
            runs.value = it
        }
    }

    fun insertRun(run: Run) = viewModelScope.launch {
        mainRepository.insertRun(run)
    }

    fun selectSortType(position: Int) {
        when (position) {
            0 -> runs.postValue(runsSortedByDate.value)
            1 -> runs.postValue(runsSortedByRunDuration.value)
            2 -> runs.postValue(runsSortedByDistance.value)
            3 -> runs.postValue(runsSortedByAvgSpeed.value)
            4 -> runs.postValue(runsSortedByCaloriesBurned.value)
        }
    }
}