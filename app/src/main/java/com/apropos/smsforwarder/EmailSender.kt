// app/src/main/java/com/apropos/smsforwarder/EmailSender.kt
package com.apropos.smsforwarder

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailSender {
    suspend fun sendEmail(
        context: Context,
        sender: String,
        body: String,
        time: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sharedPrefs = context.getSharedPreferences("SMSForwarderPrefs", Context.MODE_PRIVATE)
            val email = sharedPrefs.getString("email", "") ?: ""
            val password = sharedPrefs.getString("password", "") ?: ""
            val recipient = sharedPrefs.getString("recipient", "") ?: ""
            val subjectFormat = sharedPrefs.getString("subjectFormat", "SMS from {sender}") ?: "SMS from {sender}"
            val bodyFormat = sharedPrefs.getString("bodyFormat", "From: {sender}\nTime: {time}\n\n{body}") ?: "From: {sender}\nTime: {time}\n\n{body}"

            if (email.isEmpty() || password.isEmpty() || recipient.isEmpty()) {
                throw IllegalStateException("Email, password, and recipient email must be configured")
            }

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

            val message = MimeMessage(session)
            message.setFrom(InternetAddress(email))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient))
            message.subject = subjectFormat.replace("{sender}", sender)
            message.setText(bodyFormat
                .replace("{sender}", sender)
                .replace("{time}", time)
                .replace("{body}", body)
            )

            Transport.send(message)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}