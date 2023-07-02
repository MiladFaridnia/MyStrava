package com.faridnia.mystrava.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faridnia.mystrava.db.Run
import com.faridnia.mystrava.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    val mainRepository: MainRepository
) : ViewModel() {

    val runs = mainRepository.getAllRunsByDate()

    fun insertRun(run: Run) = viewModelScope.launch {
        mainRepository.insertRun(run)
    }
}