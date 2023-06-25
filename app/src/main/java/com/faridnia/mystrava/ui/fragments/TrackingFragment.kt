package com.faridnia.mystrava.ui.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.faridnia.mystrava.R
import com.faridnia.mystrava.databinding.FragmentTrackingBinding
import com.faridnia.mystrava.other.Constants.ACTION_PAUSE_SERVICE
import com.faridnia.mystrava.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.faridnia.mystrava.other.Constants.MAP_ZOOM
import com.faridnia.mystrava.other.Constants.POLY_LINE_COLOR
import com.faridnia.mystrava.other.Constants.POLY_LINE_WIDTH
import com.faridnia.mystrava.other.NotificationUtils
import com.faridnia.mystrava.other.TrackingUtils
import com.faridnia.mystrava.service.PolyLinesList
import com.faridnia.mystrava.service.TrackingService
import com.faridnia.mystrava.ui.viewmodels.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.PolylineOptions
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

@AndroidEntryPoint
class TrackingFragment : Fragment(R.layout.fragment_tracking), EasyPermissions.PermissionCallbacks {

    //private var pathPoints: PolyLinesList? = null
    private var isTracking: Boolean = false

    private val viewModel: MainViewModel by viewModels()

    private var _binding: FragmentTrackingBinding? = null
    private val binding get() = _binding!!

    var map: GoogleMap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        _binding = FragmentTrackingBinding.inflate(layoutInflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.mapView.onCreate(savedInstanceState)

        requestNotificationPermission()

        setToggleButtonClickListener()

        observeTrackingServiceData()

        getMap()
    }

    private fun observeTrackingServiceData() {
        TrackingService.isTrackingLiveData.observe(viewLifecycleOwner) {
            updateTracking(it)
        }

        TrackingService.pathPointsLiveData.observe(viewLifecycleOwner) {
            // pathPoints = it
            updateLastPolyLine(it)
            moveCameraToUser(it)
        }
    }

    private fun moveCameraToUser(mutableLists: PolyLinesList) {
        if (mutableLists.isNotEmpty() && mutableLists.last().isNotEmpty())
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(mutableLists.last().last(), MAP_ZOOM)
            )
    }

    private fun updateLastPolyLine(lists: PolyLinesList) {
        if (lists.isNotEmpty() && lists.last().size > 1) {
            val polylineOptions = PolylineOptions()
                .color(POLY_LINE_COLOR)
                .width(POLY_LINE_WIDTH)
                .add(lists.last()[lists.last().size - 2])
                .add(lists.last().last())

            map?.addPolyline(polylineOptions)
        }
    }

    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking

        if (isTracking) {
            binding.btnToggleRun.text = "Stop"
            binding.btnFinishRun.visibility = View.VISIBLE
        } else {
            binding.btnToggleRun.text = "Start"
            binding.btnFinishRun.visibility = View.GONE
        }
    }

    private fun setToggleButtonClickListener() {
        binding.btnToggleRun.setOnClickListener {
            toggleRun()
        }
    }

    private fun toggleRun() {
        if (isTracking) {
            sentCommandToService(ACTION_PAUSE_SERVICE)
        } else {
            sentCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    private fun requestNotificationPermission() {
        if (NotificationUtils.hasNotificationPermissions(requireContext())) {
            return
        }

        NotificationUtils.requestPermission(this)
    }

    @SuppressLint("MissingPermission")
    private fun getMap() {
        binding.mapView.getMapAsync {
            map = it
            if (TrackingUtils.hasLocationPermissions(requireContext())) {
                map?.isMyLocationEnabled = true

            }
            map?.uiSettings?.isMyLocationButtonEnabled = true
            addAllPolyLines()
        }
    }

    private fun addAllPolyLines() {
        val allPoints = TrackingService.pathPointsLiveData.value

        if (allPoints?.isNotEmpty() == true) {
            for (pathPoint in allPoints) {
                val polylineOptions = PolylineOptions()
                    .color(POLY_LINE_COLOR)
                    .width(POLY_LINE_WIDTH)
                    .addAll(pathPoint)

                map?.addPolyline(polylineOptions)
            }
        }
    }

    private fun sentCommandToService(command: String) {
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = command
            requireContext().startService(it)
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {}

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestNotificationPermission()
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


    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

}