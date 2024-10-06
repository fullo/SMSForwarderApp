// app/src/main/java/com/apropos/smsforwarder/SmsReceiver.kt
package com.apropos.smsforwarder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.withContext

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private const val CHANNEL_ID = "SmsForwarderChannel"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            messages.forEach { smsMessage ->
                val sender = smsMessage.displayOriginatingAddress
                val body = smsMessage.messageBody
                val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(smsMessage.timestampMillis))
                Log.d(TAG, "SMS received from $sender: $body")

                context?.let { ctx ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val result = EmailSender.sendEmail(ctx, sender, body, time)
                        val logAdded = SmsLog.addLog(ctx, sender, body, time, result.isSuccess)

                        withContext(Dispatchers.Main) {
                            if (result.isSuccess) {
                                Log.d(TAG, "Email sent successfully for SMS from $sender")
                                showNotification(ctx, "SMS Forwarded", "Message from $sender has been forwarded")
                            } else {
                                Log.e(TAG, "Failed to send email for SMS from $sender: ${result.exceptionOrNull()?.message}")
                                showNotification(ctx, "SMS Forward Failed", "Failed to forward message from $sender")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showNotification(context: Context, title: String, content: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "SMS Forwarder Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
        Log.d(TAG, "Notification shown: $title - $content")
    }
}