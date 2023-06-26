package com.faridnia.mystrava.other

import android.graphics.Color

object Constants {

    const val TIMER_UPDATE_INTERVAL = 50L

    const val POLY_LINE_COLOR = Color.RED
    const val POLY_LINE_WIDTH = 7f
    const val MAP_ZOOM = 14f

    const val DATABASE_NAME = "running_db"
    const val REQUEST_PERMISSION_CODE = 1000
    const val REQUEST_NOTIFICATION_PERMISSION_CODE = 1001

    const val ACTION_START_OR_RESUME_SERVICE = "ACTION_START_OR_RESUME_SERVICE"
    const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
    const val ACTION_PAUSE_SERVICE = "ACTION_PAUSE_SERVICE"

    const val LOCATION_UPDATE_INTERVAL = 5000L
    const val FASTEST_LOCATION_INTERVAL = 2000L

    const val NOTIFICATION_CHANNEL_ID = "my_strava_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Tracking"
    const val NOTIFICATION_ID = 1
    const val ACTION_SHOW_TRACKING_FRAGMENT = "ACTION_SHOW_TRACKING_FRAGMENT"
}