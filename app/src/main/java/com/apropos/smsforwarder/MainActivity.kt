package com.apropos.smsforwarder

import android.Manifest
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
    }

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var recipientEditText: EditText
    private lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        recipientEditText = findViewById(R.id.recipientEditText)
        startButton = findViewById(R.id.startButton)

        loadSettings()

        startButton.setOnClickListener {
            if (checkPermissions()) {
                saveSettings()
                startSmsForwarderService()
            } else {
                requestPermissions()
            }
        }

        // Start the service if permissions are already granted
        if (checkPermissions()) {
            startSmsForwarderService()
        }
    }

    private fun loadSettings() {
        val sharedPrefs = getSharedPreferences("SmsForwarderPrefs", MODE_PRIVATE)
        emailEditText.setText(sharedPrefs.getString("email", ""))
        passwordEditText.setText(sharedPrefs.getString("password", ""))
        recipientEditText.setText(sharedPrefs.getString("recipient", ""))
    }

    private fun saveSettings() {
        val sharedPrefs = getSharedPreferences("SmsForwarderPrefs", MODE_PRIVATE)
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
        } else {
            Toast.makeText(this, "SMS permission is required for the app to function", Toast.LENGTH_LONG).show()
        }
    }

    private fun startSmsForwarderService() {
        val serviceIntent = Intent(this, SmsForwarderService::class.java)
        startForegroundService(serviceIntent)
        Toast.makeText(this, "SMS Forwarder Service Started", Toast.LENGTH_SHORT).show()
    }
}