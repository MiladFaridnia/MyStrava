package com.faridnia.mystrava.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.faridnia.mystrava.R
import com.faridnia.mystrava.other.Constants
import com.faridnia.mystrava.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import com.faridnia.mystrava.other.Constants.FASTEST_LOCATION_INTERVAL
import com.faridnia.mystrava.other.Constants.LOCATION_UPDATE_INTERVAL
import com.faridnia.mystrava.other.Constants.NOTIFICATION_CHANNEL_ID
import com.faridnia.mystrava.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.faridnia.mystrava.other.Constants.NOTIFICATION_ID
import com.faridnia.mystrava.other.TrackingUtils
import com.faridnia.mystrava.ui.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

typealias PolyLine = MutableList<LatLng>
typealias PolyLinesList = MutableList<PolyLine>


class TrackingService : LifecycleService() {

    private var isFirstRun = true

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var isTimerEnabled = false
    private var lapTime = 0L
    private var timeRun = 0L
    private var timeStarted = 0L
    private var lastSecondTimestamp = 0L

    private val timeRunInSeconds = MutableLiveData<Long>()

    companion object {
        val timeRunInMillisLiveData = MutableLiveData<Long>()
        val isTrackingLiveData = MutableLiveData<Boolean>()
        val pathPointsLiveData = MutableLiveData<PolyLinesList>()
    }

    override fun onCreate() {
        super.onCreate()

        postInitialValues()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        isTrackingLiveData.observe(this) {
            updateLocationTracking(it)
        }

    }

    private fun postInitialValues() {
        isTrackingLiveData.postValue(false)
        pathPointsLiveData.postValue(mutableListOf())
        timeRunInSeconds.postValue(0L)
        timeRunInMillisLiveData.postValue(0L)
    }

    private fun addPathPoint(location: Location?) {
        location?.let {
            val position = LatLng(it.latitude, it.longitude)
            pathPointsLiveData.value?.apply {
                last().add(position)
                pathPointsLiveData.postValue(this)
            }
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            if (isTrackingLiveData.value == true) {
                for (location in locationResult.locations) {
                    addPathPoint(location)
                    Timber.d("location: ${location.latitude}")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking: Boolean) {
        if (isTracking) {
            if (TrackingUtils.hasLocationPermissions(this)) {
                val locationRequest = LocationRequest.Builder(
                    PRIORITY_HIGH_ACCURACY,
                    LOCATION_UPDATE_INTERVAL
                )
                    .setMinUpdateIntervalMillis(FASTEST_LOCATION_INTERVAL)
                    .build()

                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun addEmptyPolyLine() = pathPointsLiveData.value?.apply {
        add(mutableListOf())
        pathPointsLiveData.postValue(this)
    } ?: pathPointsLiveData.postValue(mutableListOf(mutableListOf()))

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            when (it.action) {

                Constants.ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        Timber.d("ACTION_START_SERVICE")
                        startTimer()
                        startForegroundService()
                        isFirstRun = false
                    } else {
                        startTimer()
                        Timber.d("ACTION_RESUME_SERVICE")
                    }
                }

                Constants.ACTION_PAUSE_SERVICE -> {
                    pauseService()
                    Timber.d("ACTION_PAUSE_SERVICE")
                }


                Constants.ACTION_STOP_SERVICE -> {
                    Timber.d("ACTION_STOP_SERVICE")
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startTimer() {
        addEmptyPolyLine()
        isTrackingLiveData.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled = true
        CoroutineScope(Dispatchers.Main).launch {
            while (isTrackingLiveData.value!!) {
                // time difference between now and timeStarted
                lapTime = System.currentTimeMillis() - timeStarted
                // post the new lapTime
                timeRunInMillisLiveData.postValue(timeRun + lapTime)
                if (timeRunInMillisLiveData.value!! >= lastSecondTimestamp + 1000L) {
                    timeRunInSeconds.postValue(timeRunInSeconds.value!! + 1)
                    lastSecondTimestamp += 1000L
                }
                delay(Constants.TIMER_UPDATE_INTERVAL)
            }
            timeRun += lapTime
        }
    }

    private fun pauseService() {
        isTrackingLiveData.postValue(false)
        isTimerEnabled = false
    }


    private fun getMainActivityPendingIntent(): PendingIntent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                this,
                0,
                Intent(
                    this,
                    MainActivity::class.java
                ).also {
                    it.action = ACTION_SHOW_TRACKING_FRAGMENT
                },
                PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                this,
                0,
                Intent(
                    this,
                    MainActivity::class.java
                ).also {
                    it.action = ACTION_SHOW_TRACKING_FRAGMENT
                },
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    private fun startForegroundService() {

        isTrackingLiveData.postValue(true)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_directions_run_black_24dp)
            .setContentTitle("My Strava")
            .setContentText("00:00:00")
            .setContentIntent(getMainActivityPendingIntent())

        startForeground(NOTIFICATION_ID, notificationBuilder.build())


    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )

        notificationManager.createNotificationChannel(channel)

    }

}