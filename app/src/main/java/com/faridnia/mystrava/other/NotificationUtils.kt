package com.faridnia.mystrava.other

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.fragment.app.Fragment
import pub.devrel.easypermissions.EasyPermissions

object NotificationUtils {

    fun hasNotificationPermissions(context: Context): Boolean {
        return if (isBelowAndroidT()) {
            true
        } else {
            EasyPermissions.hasPermissions(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    fun requestPermission(fragment: Fragment) {
        if (isBelowAndroidT()) {
            return
        } else {
            EasyPermissions.requestPermissions(
                fragment,
                "Please Accept these permissions",
                Constants.REQUEST_NOTIFICATION_PERMISSION_CODE,
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    private fun isBelowAndroidT() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
}