package com.faridnia.mystrava.ui.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.faridnia.mystrava.R
import com.faridnia.mystrava.adapter.RunAdapter
import com.faridnia.mystrava.databinding.FragmentRunBinding
import com.faridnia.mystrava.other.TrackingUtils
import com.faridnia.mystrava.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions


@AndroidEntryPoint
class RunFragment : Fragment(R.layout.fragment_run), EasyPermissions.PermissionCallbacks {

    private lateinit var runAdapter: RunAdapter

    private val mainViewModel: MainViewModel by viewModels()

    private var _binding: FragmentRunBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentRunBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestLocationPermissions()

        setFabClickListener()

        setupRecyclerView()

        observeRuns()

        initSpinner()
    }

    private fun initSpinner() {
        val filterOptions = resources.getStringArray(R.array.filter_options).toList()

        val spinnerAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filterOptions)

        with(binding.spFilter) {
            adapter = spinnerAdapter
            prompt = "Sort by"
            gravity = Gravity.CENTER
            setSelection(0, false)

            onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    mainViewModel.selectSortType(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun observeRuns() {
        mainViewModel.runs.observe(viewLifecycleOwner) {
            it?.let { it1 -> runAdapter.submitList(it1) }
        }
    }

    private fun setupRecyclerView() = binding.rvRuns.apply {
        runAdapter = RunAdapter()

        adapter = runAdapter

        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setFabClickListener() {
        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.actionRunFragmentToTrackingFragment)
        }
    }

    private fun requestLocationPermissions() {
        if (TrackingUtils.hasLocationPermissions(requireContext())) {
            return
        }

        TrackingUtils.requestPermission(this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {}

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestLocationPermissions()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
}