// app/src/main/java/com/apropos/smsforwarder/MainActivity.kt
package com.apropos.smsforwarder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit // KTX for SharedPreferences
import com.google.android.material.snackbar.Snackbar
import android.view.View

class MainActivity : AppCompatActivity() {
    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 100
    }

    private lateinit var smsPermissionStatus: TextView
    private lateinit var notificationPermissionStatus: TextView
    private lateinit var batteryOptimizationStatus: TextView
    private lateinit var securityStatus: TextView
    private lateinit var settingsButton: Button // This is now "Email Settings"
    private lateinit var toggleServiceButton: Button
    private lateinit var viewLogsButton: Button
    private lateinit var checkStatusButton: Button
    private lateinit var limitationsStatusTextView: TextView
    private lateinit var diagnosticInfoTextView: TextView // For displaying diagnostic info

    // New Gear ImageButtons
    private lateinit var smsPermissionGearButton: ImageButton
    private lateinit var notificationPermissionGearButton: ImageButton
    private lateinit var batteryOptimizationGearButton: ImageButton

    // SecurePreferencesManager
    private lateinit var prefs: SecurePreferencesManager

    private val settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        updateUIVisibilityAndStatus()
        displayDiagnosticInfo() // Refresh diagnostic info after returning from settings
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = SecurePreferencesManager.getInstance(this)

        smsPermissionStatus = findViewById(R.id.smsPermissionStatus)
        notificationPermissionStatus = findViewById(R.id.notificationPermissionStatus)
        batteryOptimizationStatus = findViewById(R.id.batteryOptimizationStatus)
        securityStatus = findViewById(R.id.securityStatus)
        settingsButton = findViewById(R.id.settingsButton) // Text changed in XML to "Email Settings"
        toggleServiceButton = findViewById(R.id.toggleServiceButton)
        viewLogsButton = findViewById(R.id.viewLogsButton)
        limitationsStatusTextView = findViewById(R.id.limitationsStatusTextView)
        checkStatusButton = findViewById(R.id.checkStatusButton)
        diagnosticInfoTextView = findViewById(R.id.diagnosticInfoTextView) // Initialization

        // Initialize new gear buttons
        smsPermissionGearButton = findViewById(R.id.smsPermissionGearButton)
        notificationPermissionGearButton = findViewById(R.id.notificationPermissionGearButton)
        batteryOptimizationGearButton = findViewById(R.id.batteryOptimizationGearButton)

        updateUIVisibilityAndStatus()
        updateSecurityStatus()

        // Controlla sicurezza solo se non abbiamo già mostrato l'avviso
        if (!prefs.getBoolean("security_warning_shown", false)) {
            checkSecurityAndShowWarning()
        }

        settingsButton.setOnClickListener { // This button now opens Email Settings
            val intent = Intent(this, SettingsActivity::class.java)
            settingsLauncher.launch(intent)
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

        checkStatusButton.setOnClickListener {
            if (checkPermissions()) {
                requestPermissions()
            }
            displayDiagnosticInfo() // Display diagnostic info in the TextView
        }

        // Setup listeners for gear buttons
        smsPermissionGearButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.toast_sms_permission_already_granted), Toast.LENGTH_SHORT).show()
            } else {
                openAppSettings()
            }
        }

        notificationPermissionGearButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, getString(R.string.toast_notification_permission_already_granted), Toast.LENGTH_SHORT).show()
                } else {
                    openAppSettings()
                }
            } else {
                // For versions below Tiramisu, POST_NOTIFICATIONS permission doesn't exist/isn't runtime.
                // Clicking the gear icon will take them to app settings, which is reasonable.
                openAppSettings()
            }
        }

        batteryOptimizationGearButton.setOnClickListener {
            val status = BatteryOptimizationManager.getOptimizationStatus(this)
            if (!status.isOptimized) { // Correctly access .isOptimized
                // App is NOT optimized (whitelisted) - this is the desired state
                Toast.makeText(this, getString(R.string.toast_battery_optimization_ok), Toast.LENGTH_LONG).show()
            } else {
                // App IS optimized - guide user to settings
                BatteryOptimizationManager.openOptimizationSettings(this)
            }
        }

        checkBatteryOptimizationOnFirstLaunch()
        // checkOptimizationsIfNeeded() // Consider if this is still needed with direct gear icon
        displayDiagnosticInfo() // Initial display of diagnostic info
    }

    override fun onResume() {
        super.onResume()
        updateUIVisibilityAndStatus()
        updateSecurityStatus()
        displayDiagnosticInfo() // Refresh diagnostic info
    }

    private fun updateUIVisibilityAndStatus() {
        updatePermissionStatusText()
        updateToggleButtonText()
        checkPreferencesAndUpdateButton() // This also calls updateToggleButtonText()
        showLimitationsIndicator()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts(getString(R.string.package_uri_prefix), packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun updatePermissionStatusText() {
        val smsPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        smsPermissionStatus.text = getString(R.string.sms_permission_status, if (smsPermissionGranted) getString(R.string.granted) else getString(R.string.not_granted))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            notificationPermissionStatus.text = getString(R.string.notification_permission_status, if (notificationPermissionGranted) getString(R.string.granted) else getString(R.string.not_granted))
        } else {
            notificationPermissionStatus.text = getString(R.string.notification_permission_status, getString(R.string.not_required))
        }
    }

    private fun updateSecurityStatus() {
        securityStatus.text = prefs.getSecurityStatusMessage()

        // Colora il testo in base al livello di sicurezza
        if (prefs.isUsingSecureStorage()) {
            securityStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            securityStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
        }
    }

    private fun checkSecurityAndShowWarning() {
        if (prefs.shouldShowSecurityWarning()) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.security_warning_title))
                .setMessage(getString(R.string.security_warning_message))
                .setPositiveButton(getString(R.string.security_warning_understood)) { dialog, _ ->
                    dialog.dismiss()
                    // Salva che l'utente ha visto l'avviso
                    prefs.putBoolean("security_warning_shown", true)
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun checkPreferencesAndUpdateButton(): Boolean {
        // AGGIORNATO: Usa SecurePreferencesManager invece di SharedPreferences dirette
        val preferencesConfigured = prefs.isEmailConfigured()
        val permissionsGranted = checkPermissions()

        val isEnabled = preferencesConfigured && permissionsGranted
        toggleServiceButton.isEnabled = isEnabled

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
            updatePermissionStatusText() // Update text part
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                if (checkPreferencesAndUpdateButton()) {
                    startSmsForwarderService()
                }
            } else {
                Toast.makeText(this, getString(R.string.permissions_required), Toast.LENGTH_LONG).show()
            }
            showLimitationsIndicator() // Update limitations part
            checkPreferencesAndUpdateButton() // Ensure UI is up-to-date
            displayDiagnosticInfo() // Refresh diagnostic info after permission change
            Toast.makeText(this, getString(R.string.check_status), Toast.LENGTH_SHORT).show()
        }
    }

    private fun startSmsForwarderService() {
        val serviceIntent = Intent(this, SmsForwarderService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        setServiceRunning(true)
        Toast.makeText(this, getString(R.string.sms_forwarder_service_started), Toast.LENGTH_SHORT).show()
    }

    private fun stopSmsForwarderService() {
        val serviceIntent = Intent(this, SmsForwarderService::class.java)
        stopService(serviceIntent)
        setServiceRunning(false)
        Toast.makeText(this, getString(R.string.sms_forwarder_service_stopped), Toast.LENGTH_SHORT).show()
    }

    private fun isServiceRunning(): Boolean {
        // AGGIORNATO: Usa SecurePreferencesManager invece di SharedPreferences dirette
        return prefs.getBoolean("isServiceRunning", false)
    }

    private fun setServiceRunning(isRunning: Boolean) {
        // AGGIORNATO: Usa SecurePreferencesManager invece di SharedPreferences dirette
        prefs.putBoolean("isServiceRunning", isRunning)
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

    private fun checkBatteryOptimizationOnFirstLaunch() {
        // Questa flag non è sensibile, quindi continua a usare SharedPreferences normali
        val prefs = getSharedPreferences(getString(R.string.sms_forwarder_prefs), MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean(getString(R.string.is_first_launch_pref_key), true)

        if (isFirstLaunch) {
            prefs.edit {
                putBoolean(getString(R.string.is_first_launch_pref_key), false)
            }
            PermissionExplanationDialog.showPermissionStatusDialog(this)
        } else {
            if (!BatteryOptimizationManager.isIgnoringBatteryOptimizations(this)) { // Check if app is optimized when it shouldn't be
                showBatteryOptimizationReminder()
            }
        }
    }

    private fun showBatteryOptimizationReminder() {
        val optimizationStatus = BatteryOptimizationManager.getOptimizationStatus(this)
        if (optimizationStatus.isOptimized && // only show if it IS optimized
            optimizationStatus.manufacturer != BatteryOptimizationManager.Manufacturer.STANDARD &&
            BatteryOptimizationManager.shouldShowOptimizationDialog(this)) {

            Snackbar.make(
                findViewById(android.R.id.content),
                getString(R.string.battery_optimization_active_warning),
                Snackbar.LENGTH_LONG
            ).setAction(getString(R.string.configure)) { // Changed from configure_button
                BatteryOptimizationManager.openOptimizationSettings(this)
            }.show()
        }
    }

    private fun showLimitationsIndicator() {
        val missingPermissions = getMissingCriticalPermissions()
        val batteryStatus = BatteryOptimizationManager.getOptimizationStatus(this)
        val limitations = mutableListOf<String>()

        if (missingPermissions.isNotEmpty()) {
            limitations.add(getString(R.string.missing_critical_permissions_summary, missingPermissions.size))
        }
        if (batteryStatus.isOptimized) { // Use .isOptimized here
            limitations.add(getString(R.string.battery_optimization_active))
        }

        batteryOptimizationStatus.text = getString(R.string.battery_permission_status,
            if (batteryStatus.isOptimized) getString(R.string.battery_optimization_active) else getString(R.string.battery_optimization_not_restricted)
        )

        if (limitations.isNotEmpty()) {
            limitationsStatusTextView.text = getString(R.string.limitations_summary_formatted, getString(R.string.limitations_summary_prefix), limitations.joinToString(", "))
            limitationsStatusTextView.visibility = View.VISIBLE
        } else {
            limitationsStatusTextView.visibility = View.GONE
        }
    }

    private fun getMissingCriticalPermissions(): List<String> {
        val missing = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            missing.add(getString(R.string.sms_permission_name)) // Using R.string.sms_permission_name from strings.xml
        }
        // INTERNET permission is usually implicitly granted if declared in Manifest, and not typically checked at runtime unless there's a specific reason.
        // For simplicity, let's assume it's correctly declared and doesn't need runtime check here unless it becomes an issue.
        // if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
        //     missing.add(getString(R.string.internet_permission_name)) // Using R.string.internet_permission_name
        // }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                missing.add(getString(R.string.perm_post_notifications_name)) // Using the string from strings.xml
            }
        }
        return missing
    }

    private fun displayDiagnosticInfo() {
        val diagnosticText = PermissionExplanationDialog.performManualPermissionCheck(this)
        diagnosticInfoTextView.text = diagnosticText
        diagnosticInfoTextView.visibility = if (diagnosticText.isNotBlank()) View.VISIBLE else View.GONE
    }
}