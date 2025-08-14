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
            val prefs = SecurePreferencesManager.getInstance(context)

            val email = prefs.getString("email")
            val password = prefs.getString("password")
            val recipient = prefs.getString("recipient")
            val subjectFormat = prefs.getString("subjectFormat", "SMS from {sender}")
            val bodyFormat = prefs.getString("bodyFormat", "From: {sender}\nTime: {time}\n\n{body}")

            if (email.isEmpty() || password.isEmpty() || recipient.isEmpty()) {
                throw IllegalStateException("Email, password, and recipient email must be configured") // Consider using a string resource here
            }

            val props = Properties()
            props["mail.smtp.auth"] = "true"
            props["mail.smtp.starttls.enable"] = "true"
            props["mail.smtp.host"] = "smtp.gmail.com" // Consider making these configurable
            props["mail.smtp.port"] = "587" // Consider making these configurable

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