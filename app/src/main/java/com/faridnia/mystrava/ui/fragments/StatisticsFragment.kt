package com.faridnia.mystrava.ui.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.faridnia.mystrava.R
import com.faridnia.mystrava.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private val mainViewModel: MainViewModel by viewModels()

}