package com.octopus.launcher.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class WallpaperChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("WallpaperChangedReceiver", "WALLPAPER_CHANGED received: ${intent.action}")
        // TODO: Refresh any cached wallpaper-related UI if necessary
    }
}

