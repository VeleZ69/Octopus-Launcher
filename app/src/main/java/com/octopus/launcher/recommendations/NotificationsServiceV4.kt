package com.octopus.launcher.recommendations

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.octopus.launcher.utils.PermissionUtils

class NotificationsServiceV4 : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotificationsServiceV4", "Notification listener connected")
        PermissionUtils.markNotificationListenerGranted(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // TODO: Hook into recommendations if needed
        Log.d("NotificationsServiceV4", "Notification posted from ${sbn.packageName}")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d("NotificationsServiceV4", "Notification removed from ${sbn.packageName}")
    }
}

