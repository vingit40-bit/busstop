package com.example.myfirstapp.navigation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.myfirstapp.MainActivity
import com.example.myfirstapp.R

class NavigationService : Service() {

    companion object {
        const val CHANNEL_ID = "transitgo_nav_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START_NAV = "ACTION_START_NAV"
        const val ACTION_STOP_NAV = "ACTION_STOP_NAV"
        const val ACTION_UPDATE_NAV = "ACTION_UPDATE_NAV"

        const val EXTRA_INSTRUCTION = "EXTRA_INSTRUCTION"
        const val EXTRA_DIST_REMAINING = "EXTRA_DIST_REMAINING"

        fun startService(context: Context, instruction: String, distanceInfo: String) {
            val intent = Intent(context, NavigationService::class.java).apply {
                action = ACTION_START_NAV
                putExtra(EXTRA_INSTRUCTION, instruction)
                putExtra(EXTRA_DIST_REMAINING, distanceInfo)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateService(context: Context, instruction: String, distanceInfo: String) {
            val intent = Intent(context, NavigationService::class.java).apply {
                action = ACTION_UPDATE_NAV
                putExtra(EXTRA_INSTRUCTION, instruction)
                putExtra(EXTRA_DIST_REMAINING, distanceInfo)
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, NavigationService::class.java).apply {
                action = ACTION_STOP_NAV
            }
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_NAV -> {
                stopForeground(true)
                stopSelf()
            }
            ACTION_START_NAV, ACTION_UPDATE_NAV -> {
                val instruction = intent.getStringExtra(EXTRA_INSTRUCTION) ?: "Navigating route..."
                val distanceInfo = intent.getStringExtra(EXTRA_DIST_REMAINING) ?: "TransitGo Navigation"
                val notification = buildNotification(instruction, distanceInfo)
                startForeground(NOTIFICATION_ID, notification)
            }
        }
        return START_STICKY
    }

    private fun buildNotification(instruction: String, distanceInfo: String): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TransitGo • $instruction")
            .setContentText(distanceInfo)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TransitGo Real-Time Voice Navigation",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows active turn-by-turn voice navigation guidance"
                setShowBadge(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
