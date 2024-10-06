// app/src/main/java/com/apropos/smsforwarder/SettingsActivity.kt
package com.apropos.smsforwarder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var recipientEditText: EditText
    private lateinit var subjectFormatEditText: EditText
    private lateinit var bodyFormatEditText: EditText
    private lateinit var saveSettingsButton: Button
    private lateinit var backButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        recipientEditText = findViewById(R.id.recipientEditText)
        subjectFormatEditText = findViewById(R.id.subjectFormatEditText)
        bodyFormatEditText = findViewById(R.id.bodyFormatEditText)
        saveSettingsButton = findViewById(R.id.saveSettingsButton)
        backButton = findViewById(R.id.backButton)

        loadSettings()

        saveSettingsButton.setOnClickListener {
            if (saveSettings()) {
                setResult(RESULT_OK)
                finish()
            }
        }

        backButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }

    private fun loadSettings() {
        val sharedPrefs = getSharedPreferences("SMSForwarderPrefs", MODE_PRIVATE)
        emailEditText.setText(sharedPrefs.getString("email", ""))
        passwordEditText.setText(sharedPrefs.getString("password", ""))
        recipientEditText.setText(sharedPrefs.getString("recipient", ""))
        subjectFormatEditText.setText(sharedPrefs.getString("subjectFormat", "SMS from {sender}"))
        bodyFormatEditText.setText(sharedPrefs.getString("bodyFormat", "From: {sender}\nTime: {time}\n\n{body}"))
    }

    private fun saveSettings(): Boolean {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val recipient = recipientEditText.text.toString().trim()
        val subjectFormat = subjectFormatEditText.text.toString().trim()
        val bodyFormat = bodyFormatEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || recipient.isEmpty()) {
            Toast.makeText(this, "Email, password, and recipient email must be configured", Toast.LENGTH_LONG).show()
            return false
        }

        val sharedPrefs = getSharedPreferences("SMSForwarderPrefs", MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("email", email)
            putString("password", password)
            putString("recipient", recipient)
            putString("subjectFormat", subjectFormat)
            putString("bodyFormat", bodyFormat)
            apply()
        }

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        return true
    }
}