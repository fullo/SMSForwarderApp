// app/src/main/java/com/apropos/smsforwarder/ServiceRestartWorker.kt
package com.apropos.smsforwarder

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class ServiceRestartWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val serviceIntent = Intent(applicationContext, SmsForwarderService::class.java)
        ContextCompat.startForegroundService(applicationContext, serviceIntent)
        return Result.success()
    }
}