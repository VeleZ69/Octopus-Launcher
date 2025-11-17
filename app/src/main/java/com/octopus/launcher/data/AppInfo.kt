package com.octopus.launcher.data

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable?,
    val isSystemApp: Boolean
) {
    companion object {
        fun fromApplicationInfo(
            pm: PackageManager,
            appInfo: ApplicationInfo
        ): AppInfo {
            return AppInfo(
                packageName = appInfo.packageName,
                name = appInfo.loadLabel(pm).toString(),
                icon = appInfo.loadIcon(pm),
                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            )
        }
    }
}

