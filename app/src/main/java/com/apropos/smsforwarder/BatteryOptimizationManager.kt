// ===== 1. BatteryOptimizationManager.kt (NUOVO FILE COMPLETO) =====
package com.apropos.smsforwarder

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * Manages manufacturer-specific battery optimizations
 */
object BatteryOptimizationManager {

    private const val TAG = "BatteryOptManager"

    enum class Manufacturer {
        XIAOMI,
        OPPO,
        VIVO,
        HUAWEI,
        SAMSUNG,
        ONEPLUS,
        REALME,
        ASUS,
        NOKIA,
        STANDARD
    }

    data class OptimizationStatus(
        val isOptimized: Boolean,
        val manufacturer: Manufacturer,
        val limitations: List<String>
    )

    /**
     * Detects the device manufacturer
     */
    fun getManufacturer(): Manufacturer {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> Manufacturer.XIAOMI
            manufacturer.contains("oppo") -> Manufacturer.OPPO
            manufacturer.contains("vivo") -> Manufacturer.VIVO
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> Manufacturer.HUAWEI
            manufacturer.contains("samsung") -> Manufacturer.SAMSUNG
            manufacturer.contains("oneplus") -> Manufacturer.ONEPLUS
            manufacturer.contains("realme") -> Manufacturer.REALME
            manufacturer.contains("asus") -> Manufacturer.ASUS
            manufacturer.contains("nokia") || manufacturer.contains("hmd") -> Manufacturer.NOKIA
            else -> Manufacturer.STANDARD
        }
    }

    /**
     * Checks if battery optimizations are disabled
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return pm.isIgnoringBatteryOptimizations(context.packageName)
        }
        return true
    }

    /**
     * Gets the complete optimization status with limitations
     */
    fun getOptimizationStatus(context: Context): OptimizationStatus {
        val isOptimized = !isIgnoringBatteryOptimizations(context)
        val manufacturer = getManufacturer()

        val limitations = mutableListOf<String>()

        if (isOptimized) {
            limitations.add(context.getString(R.string.limitation_sms_delay))
            limitations.add(context.getString(R.string.limitation_forwarding_delay))
            limitations.add(context.getString(R.string.limitation_service_stops))

            when (manufacturer) {
                Manufacturer.XIAOMI, Manufacturer.OPPO, Manufacturer.REALME, Manufacturer.VIVO -> {
                    limitations.add(context.getString(R.string.limitation_device_blocks_app))
                    limitations.add(context.getString(R.string.limitation_manual_config_needed))
                }
                Manufacturer.SAMSUNG, Manufacturer.HUAWEI -> {
                    limitations.add(context.getString(R.string.limitation_additional_restrictions))
                }
                else -> {}
            }
        }

        return OptimizationStatus(isOptimized, manufacturer, limitations)
    }

    /**
     * Shows a non-invasive informational dialog
     */
    fun showOptimizationInfo(activity: AppCompatActivity, status: OptimizationStatus) {
        if (!status.isOptimized) {
            return // All good, show nothing
        }

        val message = buildString {
            appendLine(activity.getString(R.string.optimization_info_dialog_message_limitations))
            appendLine()
            status.limitations.forEach { limitation ->
                appendLine(limitation) // These are already localized strings from getOptimizationStatus
            }
            appendLine()
            appendLine(activity.getString(R.string.optimization_info_dialog_message_disable_optimizations))
        }

        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.optimization_info_dialog_title))
            .setMessage(message.toString())
            .setPositiveButton(activity.getString(R.string.configure_now)) { _, _ ->
                openOptimizationSettings(activity, status.manufacturer)
            }
            .setNegativeButton(activity.getString(R.string.continue_anyway)) { _, _ ->
                saveUserChoice(activity, activity.getString(R.string.battery_optimization_active_warning)) // Use string res
            }
            .setNeutralButton(activity.getString(R.string.dont_ask_again)) { _, _ ->
                saveUserChoice(activity, activity.getString(R.string.dont_ask_again)) // Use string res
            }
            .setCancelable(true)
            .show()
    }

    /**
     * Opens the appropriate settings for the manufacturer
     */
    fun openOptimizationSettings(activity: AppCompatActivity, manufacturer: Manufacturer? = null) {
        val mfg = manufacturer ?: getManufacturer()

        // First try standard Android exclusion
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isIgnoringBatteryOptimizations(activity)) {
                try {
                    val intent = Intent().apply {
                        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivity(intent)
                    return
                } catch (e: Exception) {
                    Log.e(TAG, "Error opening standard battery settings", e)
                }
            }
        }

        // Then try manufacturer-specific
        when (mfg) {
            Manufacturer.XIAOMI -> tryOpenXiaomiSettings(activity)
            Manufacturer.SAMSUNG -> tryOpenSamsungSettings(activity)
            Manufacturer.HUAWEI -> tryOpenHuaweiSettings(activity)
            Manufacturer.OPPO, Manufacturer.REALME -> tryOpenOppoSettings(activity)
            Manufacturer.VIVO -> tryOpenVivoSettings(activity)
            Manufacturer.ONEPLUS -> tryOpenOnePlusSettings(activity)
            else -> openAppDetailsSettings(activity)
        }
    }

    private fun tryOpenXiaomiSettings(activity: AppCompatActivity) {
        val intents = listOf(
            Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            },
            Intent().apply {
                component = ComponentName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                )
            }
        )

        for (intent in intents) {
            if (tryStartActivity(activity, intent)) return
        }
        openAppDetailsSettings(activity)
    }

    private fun tryOpenSamsungSettings(activity: AppCompatActivity) {
        val intent = Intent().apply {
            component = ComponentName(
                "com.samsung.android.lool",
                "com.samsung.android.sm.battery.ui.BatteryActivity"
            )
        }
        if (!tryStartActivity(activity, intent)) {
            openAppDetailsSettings(activity)
        }
    }

    private fun tryOpenHuaweiSettings(activity: AppCompatActivity) {
        val intent = Intent().apply {
            component = ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
        }
        if (!tryStartActivity(activity, intent)) {
            openAppDetailsSettings(activity)
        }
    }

    private fun tryOpenOppoSettings(activity: AppCompatActivity) {
        val intent = Intent().apply {
            component = ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            )
        }
        if (!tryStartActivity(activity, intent)) {
            openAppDetailsSettings(activity)
        }
    }

    private fun tryOpenVivoSettings(activity: AppCompatActivity) {
        val intent = Intent().apply {
            component = ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )
        }
        if (!tryStartActivity(activity, intent)) {
            openAppDetailsSettings(activity)
        }
    }

    private fun tryOpenOnePlusSettings(activity: AppCompatActivity) {
        val intent = Intent().apply {
            component = ComponentName(
                "com.oneplus.security",
                "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
            )
        }
        if (!tryStartActivity(activity, intent)) {
            openAppDetailsSettings(activity)
        }
    }

    private fun tryStartActivity(activity: AppCompatActivity, intent: Intent): Boolean {
        return try {
            if (isIntentAvailable(activity, intent)) {
                activity.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening settings", e)
            false
        }
    }

    private fun openAppDetailsSettings(activity: AppCompatActivity) {
        val intent = Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.parse("package:${activity.packageName}")
        }
        activity.startActivity(intent)
    }

    private fun isIntentAvailable(context: Context, intent: Intent): Boolean {
        return context.packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        ).isNotEmpty()
    }

    private fun saveUserChoice(context: Context, choiceKey: String) { // choiceKey is now a resolved string
        context.getSharedPreferences(context.getString(R.string.sms_forwarder_prefs), Context.MODE_PRIVATE)
            .edit()
            .putBoolean(choiceKey, true)
            .putLong("${choiceKey}_time", System.currentTimeMillis()) // Suffix '_time' to the resolved key
            .apply()
    }

    fun shouldShowOptimizationDialog(context: Context): Boolean {
        val prefs = context.getSharedPreferences(context.getString(R.string.sms_forwarder_prefs), Context.MODE_PRIVATE)

        val dontAskKey = context.getString(R.string.dont_ask_again)
        val partialModeKey = context.getString(R.string.battery_optimization_active_warning)

        // If the user chose "Don't ask again"
        if (prefs.getBoolean(dontAskKey, false)) {
            return false
        }

        // If they accepted partial mode, show only every 7 days
        if (prefs.getBoolean(partialModeKey, false)) {
            val lastTime = prefs.getLong("${partialModeKey}_time", 0) // Suffix '_time'
            val daysSince = (System.currentTimeMillis() - lastTime) / (1000 * 60 * 60 * 24)
            return daysSince >= 7
        }

        return true
    }
}
