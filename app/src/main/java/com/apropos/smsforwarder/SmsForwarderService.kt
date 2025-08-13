package com.apropos.smsforwarder

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat

class SmsForwarderService : Service() {
    companion object {
        private const val TAG = "SmsForwarderService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "SmsForwarderChannel" // Internal ID, not for display
        private const val PREFS_NAME = "SMSForwarderPrefs" // Internal key
        private const val SERVICE_RUNNING_KEY = "isServiceRunning" // Internal key
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")

        try {
            checkConfiguration()
            createNotificationChannel()
            val notification = createNotification()

            // Start as foreground with appropriate type for Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    } else {
                        0
                    }
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            setServiceRunning(true)

            // Schedule the monitoring worker
            ServiceRestartWorker.schedule(this)

            Log.d(TAG, "Service started successfully")
            return START_STICKY // Restart if killed

        } catch (e: Exception) {
            Log.e(TAG, "Error starting service", e)
            stopSelf()
            showErrorNotification(e.message ?: getString(R.string.notification_default_config_error_text))
            return START_NOT_STICKY
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SMSForwarder::WakeLock" // Internal WakeLock tag
            ).apply {
                acquire(10 * 60 * 1000L) // 10 minutes max
            }
            Log.d(TAG, "WakeLock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "WakeLock released")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock", e)
        }
    }

    private fun checkConfiguration() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val email = sharedPrefs.getString("email", "")
        val password = sharedPrefs.getString("password", "")
        val recipient = sharedPrefs.getString("recipient", "")

        if (email.isNullOrEmpty() || password.isNullOrEmpty() || recipient.isNullOrEmpty()) {
            throw IllegalStateException(getString(R.string.error_email_password_recipient_unconfigured))
        }
    }

    private fun showErrorNotification(errorMessage: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title_error))
            .setContentText(errorMessage)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification) // Use a different ID for error notifications
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()

        releaseWakeLock()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        setServiceRunning(false)

        // Cancel the worker if the service was stopped intentionally
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!sharedPrefs.getBoolean(SERVICE_RUNNING_KEY, false)) {
            ServiceRestartWorker.cancel(this)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.w(TAG, "onTaskRemoved - App removed from recents")
        super.onTaskRemoved(rootIntent)

        // If the service should remain active, schedule a restart
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (sharedPrefs.getBoolean(SERVICE_RUNNING_KEY, false)) {
            Log.d(TAG, "Scheduling service restart")

            // Use AlarmManager for an immediate restart attempt
            val restartServiceIntent = Intent(applicationContext, SmsForwarderService::class.java)
            restartServiceIntent.setPackage(packageName)

            val restartServicePendingIntent = PendingIntent.getService(
                applicationContext,
                1, // Unique request code
                restartServiceIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmService.set(
                AlarmManager.ELAPSED_REALTIME,
                android.os.SystemClock.elapsedRealtime() + 1000, // 1 second delay
                restartServicePendingIntent
            )
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0, // Request code
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name)) // Using app_name for title
            .setContentText(getString(R.string.notification_service_active_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun setServiceRunning(isRunning: Boolean) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean(SERVICE_RUNNING_KEY, isRunning).apply()
        Log.d(TAG, "Service status saved: $isRunning") // Log message with dynamic content
    }
}
