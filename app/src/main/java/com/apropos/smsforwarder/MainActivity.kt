package com.apropos.smsforwarder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 100
        private const val PREFS_NAME = "SmsForwarderPrefs"
        private const val SERVICE_RUNNING_KEY = "isServiceRunning"
    }

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var recipientEditText: EditText
    private lateinit var toggleServiceButton: Button
    private lateinit var viewLogsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        recipientEditText = findViewById(R.id.recipientEditText)
        toggleServiceButton = findViewById(R.id.toggleServiceButton)
        viewLogsButton = findViewById(R.id.viewLogsButton)

        loadSettings()
        updateToggleButtonText()

        toggleServiceButton.setOnClickListener {
            if (isServiceRunning()) {
                stopSmsForwarderService()
            } else {
                if (checkPermissions()) {
                    saveSettings()
                    startSmsForwarderService()
                } else {
                    requestPermissions()
                }
            }
            updateToggleButtonText()
        }

        viewLogsButton.setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }
    }

    private fun loadSettings() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        emailEditText.setText(sharedPrefs.getString("email", ""))
        passwordEditText.setText(sharedPrefs.getString("password", ""))
        recipientEditText.setText(sharedPrefs.getString("recipient", ""))
    }

    private fun saveSettings() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("email", emailEditText.text.toString())
            putString("password", passwordEditText.text.toString())
            putString("recipient", recipientEditText.text.toString())
            apply()
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECEIVE_SMS),
            PERMISSIONS_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startSmsForwarderService()
            updateToggleButtonText()
        } else {
            Toast.makeText(this, "SMS permission is required for the app to function", Toast.LENGTH_LONG).show()
        }
    }

    private fun startSmsForwarderService() {
        val serviceIntent = Intent(this, SmsForwarderService::class.java)
        startForegroundService(serviceIntent)
        setServiceRunning(true)
        Toast.makeText(this, "SMS Forwarder Service Started", Toast.LENGTH_SHORT).show()
    }

    private fun stopSmsForwarderService() {
        val serviceIntent = Intent(this, SmsForwarderService::class.java)
        stopService(serviceIntent)
        setServiceRunning(false)
        Toast.makeText(this, "SMS Forwarder Service Stopped", Toast.LENGTH_SHORT).show()
    }

    private fun isServiceRunning(): Boolean {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getBoolean(SERVICE_RUNNING_KEY, false)
    }

    private fun setServiceRunning(isRunning: Boolean) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean(SERVICE_RUNNING_KEY, isRunning).apply()
    }

    private fun updateToggleButtonText() {
        if (isServiceRunning()) {
            toggleServiceButton.text = getString(R.string.stop_service)
        } else {
            toggleServiceButton.text = getString(R.string.start_service)
        }
    }

    override fun onResume() {
        super.onResume()
        updateToggleButtonText()
    }
}