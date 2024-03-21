package com.wizeline.companionbluetothpoc

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

// service created to test if it can be launched in background by CompanionService
class MyService : Service() {
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "ForegroundServiceChannel")
            .setContentTitle("Service")
            .setContentText("Service running in background.")
            .setSmallIcon(androidx.core.R.drawable.ic_call_answer_low)
            .build()
        startForeground(1, notification)
        return START_STICKY
    }
}