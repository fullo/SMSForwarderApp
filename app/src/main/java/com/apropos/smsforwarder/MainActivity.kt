// app/src/main/java/com/apropos/smsforwarder/MainActivity.kt
package com.apropos.smsforwarder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 100
        private const val SETTINGS_REQUEST_CODE = 101
    }

    private lateinit var smsPermissionStatus: TextView
    private lateinit var notificationPermissionStatus: TextView
    private lateinit var settingsButton: Button
    private lateinit var toggleServiceButton: Button
    private lateinit var viewLogsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        smsPermissionStatus = findViewById(R.id.smsPermissionStatus)
        notificationPermissionStatus = findViewById(R.id.notificationPermissionStatus)
        settingsButton = findViewById(R.id.settingsButton)
        toggleServiceButton = findViewById(R.id.toggleServiceButton)
        viewLogsButton = findViewById(R.id.viewLogsButton)

        updatePermissionStatus()
        updateToggleButtonText()
        checkPreferencesAndUpdateButton()

        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivityForResult(intent, SETTINGS_REQUEST_CODE)
        }

        toggleServiceButton.setOnClickListener {
            if (isServiceRunning()) {
                stopSmsForwarderService()
            } else {
                if (checkPermissions()) {
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

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        checkPreferencesAndUpdateButton()
    }

    private fun updatePermissionStatus() {
        val smsPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        smsPermissionStatus.text = "SMS Permission: ${if (smsPermissionGranted) "Granted" else "Not Granted"}"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            notificationPermissionStatus.text = "Notification Permission: ${if (notificationPermissionGranted) "Granted" else "Not Granted"}"
        } else {
            notificationPermissionStatus.text = "Notification Permission: Not Required"
        }
    }

    private fun checkPreferencesAndUpdateButton(): Boolean {
        val sharedPrefs = getSharedPreferences("SMSForwarderPrefs", MODE_PRIVATE)
        val email = sharedPrefs.getString("email", "")
        val password = sharedPrefs.getString("password", "")
        val recipient = sharedPrefs.getString("recipient", "")

        val preferencesConfigured = !email.isNullOrEmpty() && !password.isNullOrEmpty() && !recipient.isNullOrEmpty()
        val permissionsGranted = checkPermissions()

        val isEnabled = preferencesConfigured && permissionsGranted
        toggleServiceButton.isEnabled = isEnabled

        // Aggiorniamo il testo del pulsante in base allo stato del servizio
        if (!checkPermissions()) {
            requestPermissions()
        }
        updateToggleButtonText()

        return isEnabled
    }

    private fun checkPermissions(): Boolean {
        val smsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return smsPermission && notificationPermission
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECEIVE_SMS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            updatePermissionStatus()
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                if (checkPreferencesAndUpdateButton()) {
                    startSmsForwarderService()
                    updateToggleButtonText()
                }
            } else {
                Toast.makeText(this, "Permissions are required for the app to function", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SETTINGS_REQUEST_CODE) {
            // Ricalcoliamo lo stato indipendentemente dal resultCode
            updatePermissionStatus()
            checkPreferencesAndUpdateButton()
            updateToggleButtonText()
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
        val sharedPrefs = getSharedPreferences("SMSForwarderPrefs", MODE_PRIVATE)
        return sharedPrefs.getBoolean("isServiceRunning", false)
    }

    private fun setServiceRunning(isRunning: Boolean) {
        val sharedPrefs = getSharedPreferences("SMSForwarderPrefs", MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("isServiceRunning", isRunning).apply()
    }

    private fun updateToggleButtonText() {
        if (!toggleServiceButton.isEnabled) {
            toggleServiceButton.text = getString(R.string.start_service)
        } else if (isServiceRunning()) {
            toggleServiceButton.text = getString(R.string.stop_service)
        } else {
            toggleServiceButton.text = getString(R.string.start_service)
        }
    }
}