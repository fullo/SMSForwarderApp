package com.apropos.smsforwarder

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat

class SmsForwarderService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "SmsForwarderChannel"
        private const val SERVICE_RUNNING_KEY = "isServiceRunning"
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            checkConfiguration()
            createNotificationChannel()
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
            setServiceRunning(true)
            return START_STICKY
        } catch (e: IllegalStateException) {
            stopSelf()
            showErrorNotification(e.message ?: "Configuration error")
            return START_NOT_STICKY
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SMSForwarder::WakeLock" // Internal WakeLock tag
            ).apply {
                acquire(10 * 60 * 1000L) // 10 minutes max
            }
        } catch (e: Exception) {
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun checkConfiguration() {
        val prefs = SecurePreferencesManager.getInstance(this)

        if (!prefs.isEmailConfigured()) {
            throw IllegalStateException("Email, password, and recipient email must be configured")
        }
    }

    private fun showErrorNotification(errorMessage: String) {
        val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
        val sharedPrefs = getSharedPreferences(getString(R.string.sms_forwarder_prefs),
            MODE_PRIVATE
        )
        if (!sharedPrefs.getBoolean(getString(R.string.pref_key_is_service_running), false)) {
            ServiceRestartWorker.cancel(this)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        // If the service should remain active, schedule a restart
        val sharedPrefs = getSharedPreferences(getString(R.string.sms_forwarder_prefs),
            MODE_PRIVATE
        )
        if (sharedPrefs.getBoolean(getString(R.string.pref_key_is_service_running), false)) {

            // Use AlarmManager for an immediate restart attempt
            val restartServiceIntent = Intent(applicationContext, SmsForwarderService::class.java)
            restartServiceIntent.setPackage(packageName)

            val restartServicePendingIntent = PendingIntent.getService(
                applicationContext,
                1, // Unique request code
                restartServiceIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmService = applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager
            alarmService.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000, // 1 second delay
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
        val prefs = SecurePreferencesManager.getInstance(this)
        prefs.putBoolean(SERVICE_RUNNING_KEY, isRunning)
    }
}
