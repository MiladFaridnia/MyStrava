package com.faridnia.mystrava.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.faridnia.mystrava.R
import com.faridnia.mystrava.databinding.FragmentSetupBinding
import com.faridnia.mystrava.other.Constants.IS_FIRST_TIME
import com.faridnia.mystrava.other.Constants.KEY_NAME
import com.faridnia.mystrava.other.Constants.KEY_WEIGHT
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class SetupFragment : Fragment(R.layout.fragment_setup) {

    private var _binding: FragmentSetupBinding? = null

    private val binding get() = _binding!!

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @set:Inject
    var isFirstOpen = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSetupBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isFirstOpen.not()) {
            val navOption = NavOptions.Builder().setPopUpTo(R.id.setupFragment, true).build()
            findNavController().navigate(
                R.id.actionSetupFragmentToRunFragment,
                savedInstanceState,
                navOption
            )
        }

        binding.tvContinue.setOnClickListener {
            handleSaveDataAndContinue()
        }
    }

    private fun handleSaveDataAndContinue() {
        if (canSaveData()) {
            saveDataInPrefs()
            findNavController().navigate(R.id.actionSetupFragmentToRunFragment)
        } else {
            Snackbar.make(binding.root, "You must enter name and weight", Snackbar.LENGTH_LONG)
                .show()
        }
    }

    private fun canSaveData() =
        binding.etName.text.toString().isNotEmpty() && binding.etWeight.text.toString().isNotEmpty()

    private fun saveDataInPrefs() {
        sharedPreferences.edit().apply {
            putString(KEY_NAME, binding.etName.text.toString())
            putInt(KEY_WEIGHT, binding.etWeight.text.toString().toInt())
            putBoolean(IS_FIRST_TIME, false)
        }.apply()
    }
}