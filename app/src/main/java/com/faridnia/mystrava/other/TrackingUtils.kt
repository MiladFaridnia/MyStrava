package com.faridnia.mystrava.other

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.fragment.app.Fragment
import pub.devrel.easypermissions.EasyPermissions

object TrackingUtils {

    fun hasLocationPermissions(context: Context): Boolean {
        return if (isBelowAndroidQ()) {
            EasyPermissions.hasPermissions(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            EasyPermissions.hasPermissions(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }

    fun requestPermission(fragment: Fragment) {
        if (isBelowAndroidQ()) {
            EasyPermissions.requestPermissions(
                fragment,
                "Please Accept these permissions",
                Constants.REQUEST_PERMISSION_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            EasyPermissions.requestPermissions(
                fragment,
                "Please Accept these permissions",
                Constants.REQUEST_PERMISSION_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }

    private fun isBelowAndroidQ() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
}