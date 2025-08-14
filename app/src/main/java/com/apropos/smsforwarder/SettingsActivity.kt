package com.apropos.smsforwarder

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

    private lateinit var prefs: SecurePreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = SecurePreferencesManager.getInstance(this)

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
        emailEditText.setText(prefs.getString("email"))
        passwordEditText.setText(prefs.getString("password"))
        recipientEditText.setText(prefs.getString("recipient"))
        subjectFormatEditText.setText(prefs.getString("subjectFormat", "SMS from {sender}"))
        bodyFormatEditText.setText(prefs.getString("bodyFormat", "From: {sender}\nTime: {time}\n\n{body}"))
    }

    private fun saveSettings(): Boolean {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val recipient = recipientEditText.text.toString().trim()
        val subjectFormat = subjectFormatEditText.text.toString().trim()
        val bodyFormat = bodyFormatEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || recipient.isEmpty()) {
            Toast.makeText(this, getString(R.string.email_password_recipient_required), Toast.LENGTH_LONG).show()
            return false
        }

        prefs.putString("email", email)
        prefs.putString("password", password)
        prefs.putString("recipient", recipient)
        prefs.putString("subjectFormat", subjectFormat)
        prefs.putString("bodyFormat", bodyFormat)

        Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
        return true
    }
}