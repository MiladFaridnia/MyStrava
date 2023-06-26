package com.faridnia.mystrava.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
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
import com.faridnia.mystrava.other.Constants.ACTION_PAUSE_SERVICE
import com.faridnia.mystrava.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.faridnia.mystrava.other.Constants.ACTION_STOP_SERVICE
import com.faridnia.mystrava.other.Constants.FASTEST_LOCATION_INTERVAL
import com.faridnia.mystrava.other.Constants.LOCATION_UPDATE_INTERVAL
import com.faridnia.mystrava.other.Constants.NOTIFICATION_CHANNEL_ID
import com.faridnia.mystrava.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.faridnia.mystrava.other.Constants.NOTIFICATION_ID
import com.faridnia.mystrava.other.TrackingUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

typealias PolyLine = MutableList<LatLng>
typealias PolyLinesList = MutableList<PolyLine>


@AndroidEntryPoint
class TrackingService : LifecycleService() {

    private var isFirstRun = true
    private var isServiceKilled = false

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    lateinit var currentNotificationBuilder: NotificationCompat.Builder


    private var isTimerEnabled = false
    private var lapTime = 0L
    private var timeRun = 0L
    private var timeStarted = 0L
    private var lastSecondTimestamp = 0L

    private val timeRunInSecondsLiveData = MutableLiveData<Long>()

    companion object {
        val timeRunInMillisLiveData = MutableLiveData<Long>()
        val isTrackingLiveData = MutableLiveData<Boolean>()
        val pathPointsLiveData = MutableLiveData<PolyLinesList>()
    }

    override fun onCreate() {
        super.onCreate()

        currentNotificationBuilder = baseNotificationBuilder

        postInitialValues()

        isTrackingLiveData.observe(this) {
            updateLocationTracking(it)
            updateNotificationState(it)
        }

    }

    private fun postInitialValues() {
        isTrackingLiveData.postValue(false)
        pathPointsLiveData.postValue(mutableListOf())
        timeRunInSecondsLiveData.postValue(0L)
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

    private fun updateNotificationState(isTracking: Boolean) {
        if (isServiceKilled.not()) {
            currentNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
                isAccessible = true
                set(currentNotificationBuilder, ArrayList<NotificationCompat.Action>())
            }

            currentNotificationBuilder = baseNotificationBuilder
                .addAction(
                    R.drawable.ic_pause_black_24dp,
                    getActionTextTitle(isTracking),
                    getNotificationPendingIntent(isTracking)
                )

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.notify(NOTIFICATION_ID, currentNotificationBuilder.build())
        }
    }

    private fun getNotificationPendingIntent(isTracking: Boolean): PendingIntent? {
        return if (isTracking) {
            val pauseIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_PAUSE_SERVICE
            }
            PendingIntent.getService(this, 1, pauseIntent, getFlag())
        } else {
            val resumeIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_START_OR_RESUME_SERVICE
            }
            PendingIntent.getService(this, 1, resumeIntent, getFlag())
        }
    }

    private fun getFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            FLAG_IMMUTABLE
        } else {
            FLAG_UPDATE_CURRENT
        }
    }

    private fun getActionTextTitle(isTracking: Boolean): String {
        return if (isTracking) "Pause" else "Resume"
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

                ACTION_START_OR_RESUME_SERVICE -> {
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

                ACTION_PAUSE_SERVICE -> {
                    pauseService()
                    Timber.d("ACTION_PAUSE_SERVICE")
                }


                ACTION_STOP_SERVICE -> {
                    killService()
                    Timber.d("ACTION_STOP_SERVICE")
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun killService() {
        isServiceKilled = true
        isFirstRun = true
        pauseService()
        postInitialValues()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTimer() {
        addEmptyPolyLine()
        isTrackingLiveData.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled = true
        calculateTimeAndPostSeconds()
    }

    private fun calculateTimeAndPostSeconds() {
        CoroutineScope(Dispatchers.Main).launch {
            while (isTrackingLiveData.value!!) {
                // time difference between now and timeStarted
                lapTime = System.currentTimeMillis() - timeStarted
                // post the new lapTime
                timeRunInMillisLiveData.postValue(timeRun + lapTime)
                if (timeRunInMillisLiveData.value!! >= lastSecondTimestamp + 1000L) {
                    timeRunInSecondsLiveData.postValue(timeRunInSecondsLiveData.value!! + 1)
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


    private fun startForegroundService() {

        isTrackingLiveData.postValue(true)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }


        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())


        timeRunInSecondsLiveData.observe(this) { timeInMillis ->
            if (isServiceKilled.not()) {
                val notification = currentNotificationBuilder.setContentText(
                    TrackingUtils.getFormattedStopWatchTime(timeInMillis * 1000)
                )
                notificationManager.notify(NOTIFICATION_ID, notification.build())
            }
        }

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