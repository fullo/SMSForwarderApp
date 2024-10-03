// app/src/main/java/com/apropos/smsforwarder/SmsReceiver.kt
package com.apropos.smsforwarder

import android.app.NotificationChannel
import android.app.NotificationManager
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
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            messages.forEach { smsMessage ->
                val sender = smsMessage.displayOriginatingAddress
                val body = smsMessage.messageBody
                val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(smsMessage.timestampMillis))
                Log.d("SmsReceiver", "SMS received from $sender: $body")

                context?.let { ctx ->
                    CoroutineScope(Dispatchers.IO).launch {
                        forwardSmsToEmail(ctx, sender, body, time)
                    }
                }
            }
        }
    }

    private fun forwardSmsToEmail(context: Context, sender: String, body: String, time: String) {
        val sharedPrefs = context.getSharedPreferences("SmsForwarderPrefs", Context.MODE_PRIVATE)
        val email = sharedPrefs.getString("email", "") ?: ""
        val password = sharedPrefs.getString("password", "") ?: ""
        val recipient = sharedPrefs.getString("recipient", "") ?: ""
        val subjectFormat = sharedPrefs.getString("subjectFormat", "SMS from {sender}") ?: "SMS from {sender}"
        val bodyFormat = sharedPrefs.getString("bodyFormat", "From: {sender}\nTime: {time}\n\n{body}") ?: "From: {sender}\nTime: {time}\n\n{body}"

        val props = Properties()
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.host"] = "smtp.gmail.com"
        props["mail.smtp.port"] = "587"

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(email, password)
            }
        })

        try {
            val message = MimeMessage(session)
            message.setFrom(InternetAddress(email))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient))
            message.subject = subjectFormat.replace("{sender}", sender)
            message.setText(bodyFormat.replace("{sender}", sender).replace("{time}", time).replace("{body}", body))

            Transport.send(message)
            Log.d("SmsReceiver", "Email sent successfully")

            // Log the forwarded SMS
            SmsLog.addLog(context, sender, body, time)

            // Show notification
            showNotification(context, "SMS Forwarded", "Message from $sender has been forwarded")
        } catch (e: MessagingException) {
            Log.e("SmsReceiver", "Error sending email", e)
            showNotification(context, "SMS Forward Failed", "Failed to forward message from $sender")
        }
    }

    private fun showNotification(context: Context, title: String, content: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "SmsForwarderChannel"
        val channelName = "SMS Forwarder Notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}