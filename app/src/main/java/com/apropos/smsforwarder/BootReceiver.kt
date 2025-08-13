package com.apropos.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Restarts the service after device boot.
 */
class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            context?.let { ctx ->
                Log.d(TAG, "Boot completed, checking if service needs to be restarted")

                val sharedPrefs = ctx.getSharedPreferences(
                    "SMSForwarderPrefs", // Internal key, do not translate
                    Context.MODE_PRIVATE
                )
                val wasRunning = sharedPrefs.getBoolean("isServiceRunning", false) // Internal key, do not translate

                if (wasRunning) {
                    Log.d(TAG, "Restarting service after boot")
                    val serviceIntent = Intent(ctx, SmsForwarderService::class.java)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(ctx, serviceIntent)
                    } else {
                        ctx.startService(serviceIntent)
                    }

                    // Also schedule the monitoring worker
                    ServiceRestartWorker.schedule(ctx)
                }
            }
        }
    }
}