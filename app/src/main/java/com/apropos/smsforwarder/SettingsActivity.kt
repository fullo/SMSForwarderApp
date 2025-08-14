// app/src/main/java/com/apropos/smsforwarder/SettingsActivity.kt
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
        val prefsFile = getString(R.string.sms_forwarder_prefs)
        val sharedPrefs = getSharedPreferences(prefsFile, MODE_PRIVATE)

        emailEditText.setText(sharedPrefs.getString(getString(R.string.pref_key_email_address), ""))
        passwordEditText.setText(sharedPrefs.getString(getString(R.string.pref_key_email_password), ""))
        recipientEditText.setText(sharedPrefs.getString(getString(R.string.pref_key_recipient_email_address), ""))
        // Consider defining default values in strings.xml as well for consistency
        subjectFormatEditText.setText(sharedPrefs.getString(getString(R.string.pref_key_subject_format), "SMS from {sender}"))
        bodyFormatEditText.setText(sharedPrefs.getString(getString(R.string.pref_key_body_format), "From: {sender}\nTime: {time}\n\n{body}"))
    }

    private fun saveSettings(): Boolean {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val recipient = recipientEditText.text.toString().trim()
        val subjectFormat = subjectFormatEditText.text.toString().trim()
        val bodyFormat = bodyFormatEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || recipient.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_email_password_recipient_required), Toast.LENGTH_LONG).show()
            return false
        }

        val prefsFile = getString(R.string.sms_forwarder_prefs)
        val sharedPrefs = getSharedPreferences(prefsFile, MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString(getString(R.string.pref_key_email_address), email)
            putString(getString(R.string.pref_key_email_password), password)
            putString(getString(R.string.pref_key_recipient_email_address), recipient)
            putString(getString(R.string.pref_key_subject_format), subjectFormat)
            putString(getString(R.string.pref_key_body_format), bodyFormat)
            apply()
        }

        Toast.makeText(this, getString(R.string.toast_settings_saved), Toast.LENGTH_SHORT).show()
        return true
    }
}