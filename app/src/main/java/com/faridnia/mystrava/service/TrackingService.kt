package com.faridnia.mystrava.service

import android.content.Intent
import androidx.lifecycle.LifecycleService
import com.faridnia.mystrava.other.Constants
import timber.log.Timber

class TrackingService : LifecycleService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            when (it.action) {

                Constants.ACTION_START_OR_RESUME_SERVICE -> {
                    Timber.d("ACTION_START_OR_RESUME_SERVICE")
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

}