package com.octopus.launcher.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.octopus.launcher.recommendations.NotificationsServiceV4

object PermissionUtils {
    private const val TAG = "PermissionUtils"
    private const val PREFS = "permission_prefs"
    private const val KEY_OVERLAY_PROMPTED = "overlay_prompted"
    private const val KEY_NOTIF_LISTENER_PROMPTED = "notif_listener_prompted"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isNotificationListenerEnabled(context: Context): Boolean {
        // Quick check via NotificationManagerCompat (package-level)
        val enabledPackages = runCatching {
            NotificationManagerCompat.getEnabledListenerPackages(context)
        }.getOrElse {
            Log.e(TAG, "Failed to query enabled listener packages", it)
            emptySet()
        }
        if (enabledPackages.contains(context.packageName)) {
            return true
        }

        return try {
            val component = ComponentName(context, NotificationsServiceV4::class.java)
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            if (!flat.isNullOrBlank()) {
                val names = flat.split(":")
                names.any { s ->
                    val cn2 = ComponentName.unflattenFromString(s)
                    cn2 != null && (cn2.packageName == context.packageName || cn2 == component)
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check notification listener", e)
            false
        }
    }

    fun ensureOverlayPermission(context: Context) {
        // Only prompt if not granted and we haven't prompted before
        if (!Settings.canDrawOverlays(context)) {
            val prompted = prefs(context).getBoolean(KEY_OVERLAY_PROMPTED, false)
            if (!prompted) {
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    prefs(context).edit().putBoolean(KEY_OVERLAY_PROMPTED, true).apply()
                } catch (_: Exception) { /* ignore */ }
            }
        } else {
            // If granted, reset the prompt flag so we can re-prompt in future if user revokes
            prefs(context).edit().putBoolean(KEY_OVERLAY_PROMPTED, false).apply()
        }
    }

    fun ensureNotificationListenerPermission(context: Context) {
        if (!isNotificationListenerEnabled(context)) {
            val prompted = prefs(context).getBoolean(KEY_NOTIF_LISTENER_PROMPTED, false)
            if (!prompted) {
                try {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    prefs(context).edit().putBoolean(KEY_NOTIF_LISTENER_PROMPTED, true).apply()
                } catch (_: Exception) { /* ignore */ }
            }
        } else {
            // If granted, clear the prompted flag
            prefs(context).edit().putBoolean(KEY_NOTIF_LISTENER_PROMPTED, false).apply()
        }
    }

    fun markNotificationListenerGranted(context: Context) {
        prefs(context).edit().putBoolean(KEY_NOTIF_LISTENER_PROMPTED, false).apply()
    }
}


