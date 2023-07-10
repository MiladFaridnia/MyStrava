package com.faridnia.mystrava.ui.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.faridnia.mystrava.R
import com.faridnia.mystrava.databinding.FragmentTrackingBinding
import com.faridnia.mystrava.db.Run
import com.faridnia.mystrava.other.Constants.ACTION_PAUSE_SERVICE
import com.faridnia.mystrava.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.faridnia.mystrava.other.Constants.ACTION_STOP_SERVICE
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
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject


@AndroidEntryPoint
class TrackingFragment : Fragment(R.layout.fragment_tracking),
    EasyPermissions.PermissionCallbacks,
    MenuProvider {

    private var isTracking: Boolean = false
    private var curTimeInMillis = 0L

    @set:Inject
    var weight = 80

    private val viewModel: MainViewModel by viewModels()

    private var _binding: FragmentTrackingBinding? = null
    private val binding get() = _binding!!

    var map: GoogleMap? = null

    private var menu: Menu? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        _binding = FragmentTrackingBinding.inflate(layoutInflater, container, false)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.mapView.onCreate(savedInstanceState)

        getMap()

        requestNotificationPermission()

        setToggleButtonClickListener()

        observeTrackingServiceData()

    }

    private fun setFinishRunClickListener() {
        binding.btnFinishRun.setOnClickListener {
            handleZoomToEntireRun()
            finishRun()
        }
    }

    private fun handleZoomToEntireRun() {
        val pathPoints = TrackingService.pathPointsLiveData.value

        if (pathPoints != null && pathPoints.size > 1) {
            val bounds = boundBuilder(pathPoints)

            map?.moveCamera(
                CameraUpdateFactory.newLatLngBounds(
                    bounds,
                    binding.mapView.width,
                    binding.mapView.height,
                    (binding.mapView.height * 0.04f).toInt()
                )
            )
        }
    }

    private fun finishRun() {
        var bitmap: Bitmap? = null
        map?.snapshot { bitmapResult ->
            bitmap = bitmapResult
        }
        var distanceInMeters = 0

        val pathPoints = TrackingService.pathPointsLiveData.value
        pathPoints?.let {
            for (point in pathPoints) {
                distanceInMeters += TrackingUtils.calculateDistance(point)
            }
            val averageSpeed = (distanceInMeters / 1000f) / (curTimeInMillis / 1000f / 60 / 60)
            val dateTimestamp = Calendar.getInstance().timeInMillis
            val caloriesBurned = ((distanceInMeters / 1000f) * weight).toInt()


            val run = Run(
                timestamp = dateTimestamp,
                runDurationInMillis = curTimeInMillis,
                distanceInMeters = distanceInMeters,
                avgSpeedInKMH = averageSpeed,
                image = bitmap,
                caloriesBurned = caloriesBurned
            )

            viewModel.insertRun(run)

            stopRun()

            Toast.makeText(requireContext(), "Run Saved", Toast.LENGTH_LONG).show()

        }
    }

    private fun boundBuilder(pathPoints: PolyLinesList): LatLngBounds {
        val bounds = LatLngBounds.builder()
        for (point in pathPoints) {
            for (pos in point) {
                bounds.include(pos)
            }
        }
        return bounds.build()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.tracking_fragment_menu, menu)
        this.menu = menu

        if (curTimeInMillis > 0L) {
            this.menu?.getItem(0)?.isVisible = true
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.cancel_run -> {
                showCancelRunDialog()
            }
        }
        return false
    }

    private fun showCancelRunDialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Cancel Run?")
            .setMessage("Are you sure to cancel the run and delete all its data?")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("Yes") { _, _ ->
                stopRun()
            }.setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
            }.create().show()
    }

    private fun stopRun() {
        sendCommandToService(ACTION_STOP_SERVICE)
        findNavController().navigate(R.id.actionTrackingFragmentToRunFragment)
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

        TrackingService.timeRunInMillisLiveData.observe(viewLifecycleOwner) {
            curTimeInMillis = it
            val formattedTime = TrackingUtils.getFormattedStopWatchTime(curTimeInMillis, true)
            binding.tvTimer.text = formattedTime
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
            menu?.getItem(0)?.isVisible = true
            binding.btnToggleRun.text = getString(R.string.stop)
            binding.btnFinishRun.visibility = View.VISIBLE
        } else {
            binding.btnToggleRun.text = getString(R.string.start)
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
            menu?.getItem(0)?.isVisible = true
            sendCommandToService(ACTION_PAUSE_SERVICE)
        } else {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
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
            Timber.d("map loaded")
            map = it
            if (TrackingUtils.hasLocationPermissions(requireContext())) {
                map?.isMyLocationEnabled = true

            }
            map?.uiSettings?.isMyLocationButtonEnabled = true

            addAllPolyLines()

            setFinishRunClickListener()

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

    private fun sendCommandToService(command: String) {
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