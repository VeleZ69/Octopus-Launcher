package com.octopus.launcher.recommendations

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class RecommendationsService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("RecommendationsService", "Started")
        // TODO: Build and push recommendations here
        stopSelf(startId)
        return START_NOT_STICKY
    }
}

