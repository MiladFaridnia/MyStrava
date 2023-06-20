package com.faridnia.mystrava.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.faridnia.mystrava.R
import com.faridnia.mystrava.other.Constants
import com.faridnia.mystrava.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import com.faridnia.mystrava.other.Constants.NOTIFICATION_CHANNEL_ID
import com.faridnia.mystrava.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.faridnia.mystrava.other.Constants.NOTIFICATION_ID
import com.faridnia.mystrava.ui.MainActivity
import timber.log.Timber

class TrackingService : LifecycleService() {

    var isFirstRun = true

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            when (it.action) {

                Constants.ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        Timber.d("ACTION_START_SERVICE")
                        startForegroundService()
                        isFirstRun = false
                    } else {
                        Timber.d("ACTION_RESUME_SERVICE")
                    }
                }

                Constants.ACTION_PAUSE_SERVICE -> {
                    Timber.d("ACTION_PAUSE_SERVICE")
                }


                Constants.ACTION_STOP_SERVICE -> {
                    Timber.d("ACTION_STOP_SERVICE")
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun getMainActivityPendingIntent() = PendingIntent.getActivity(
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

    private fun startForegroundService() {
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