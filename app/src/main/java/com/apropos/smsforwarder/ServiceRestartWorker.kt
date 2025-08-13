// ===== 1. ServiceRestartWorker.kt (RENOVATED) =====
package com.apropos.smsforwarder

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Worker that monitors and restarts the service if necessary.
 */
class ServiceRestartWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ServiceRestartWorker"
        private const val WORK_NAME = "sms_forwarder_monitor" // Internal work name

        /**
         * Schedules the worker for periodic monitoring.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false) // Works even with low battery
                .build()

            val workRequest = PeriodicWorkRequestBuilder<ServiceRestartWorker>(
                15, TimeUnit.MINUTES // Minimum allowed by Android
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP, // Does not overwrite if already exists
                    workRequest
                )

            Log.d(TAG, "Worker scheduled for periodic monitoring")
        }

        /**
         * Cancels the worker.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Worker canceled")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Checking service status...")

            val sharedPrefs = applicationContext.getSharedPreferences(
                "SMSForwarderPrefs", // Internal key
                Context.MODE_PRIVATE
            )
            val shouldBeRunning = sharedPrefs.getBoolean("isServiceRunning", false) // Internal key

            if (shouldBeRunning) {
                if (!isServiceRunning()) {
                    Log.w(TAG, "Service should be active but it is not. Restarting...")
                    startService()
                } else {
                    Log.d(TAG, "Service already active")
                }
            } else {
                Log.d(TAG, "Service should not be active")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in worker", e)
            Result.retry()
        }
    }

    private fun isServiceRunning(): Boolean {
        // Check via shared flag + attempt to bind
        val activityManager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE)
                as android.app.ActivityManager

        @Suppress("DEPRECATION") // getRunningServices is deprecated but necessary for this check on older APIs
        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (SmsForwarderService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun startService() {
        val serviceIntent = Intent(applicationContext, SmsForwarderService::class.java)
        ContextCompat.startForegroundService(applicationContext, serviceIntent)
    }
}