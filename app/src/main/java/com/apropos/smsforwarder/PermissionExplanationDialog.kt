package com.apropos.smsforwarder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages permission explanation and requests, including partial functionality.
 */
object PermissionExplanationDialog {

    data class PermissionInfo(
        val permission: String,
        @param:StringRes val nameResId: Int,
        @param:StringRes val reasonResId: Int,
        @param:StringRes val impactResId: Int, // What doesn't work without this permission
        val critical: Boolean = false
    )

    private val permissionExplanations = listOf(
        PermissionInfo(
            Manifest.permission.RECEIVE_SMS,
            R.string.perm_receive_sms_name,
            R.string.perm_receive_sms_reason,
            R.string.perm_receive_sms_impact,
            critical = true
        ),
        PermissionInfo(
            Manifest.permission.POST_NOTIFICATIONS,
            R.string.perm_post_notifications_name,
            R.string.perm_post_notifications_reason,
            R.string.perm_post_notifications_impact,
            critical = false
        ),
        PermissionInfo(
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            R.string.perm_receive_boot_completed_name,
            R.string.perm_receive_boot_completed_reason,
            R.string.perm_receive_boot_completed_impact,
            critical = false
        ),
        PermissionInfo(
            Manifest.permission.WAKE_LOCK,
            R.string.perm_wake_lock_name,
            R.string.perm_wake_lock_reason,
            R.string.perm_wake_lock_impact,
            critical = false
        ),
        PermissionInfo(
            Manifest.permission.INTERNET,
            R.string.perm_internet_name,
            R.string.perm_internet_reason,
            R.string.perm_internet_impact,
            critical = true
        )
    )

    fun buildPermissionInfoMessages(activity: AppCompatActivity): String {

        val missingPermissions = getMissingPermissions(activity)
        val grantedPermissions = getGrantedPermissions(activity)

        return buildString {
            if (grantedPermissions.isNotEmpty()) {
                appendLine(activity.getString(R.string.dialog_perms_granted_header))
                grantedPermissions.forEach { info ->
                    appendLine(activity.getString(R.string.dialog_perms_granted_item_format, activity.getString(info.nameResId)))
                }
                appendLine()
            }

            if (missingPermissions.isNotEmpty()) {
                appendLine(activity.getString(R.string.dialog_perms_missing_header))
                appendLine()
                missingPermissions.forEach { info ->
                    appendLine(activity.getString(R.string.dialog_perms_missing_item_name_format, activity.getString(info.nameResId)))
                    appendLine(activity.getString(R.string.dialog_perms_missing_item_impact_format, activity.getString(info.impactResId)))
                    appendLine()
                }

                val criticalMissing = missingPermissions.filter { it.critical }
                if (criticalMissing.isNotEmpty()) {
                    appendLine(activity.getString(R.string.dialog_perms_critical_missing_header))
                    criticalMissing.forEach {
                        appendLine(activity.getString(R.string.dialog_perms_critical_missing_item_format, activity.getString(it.nameResId)))
                    }
                }
            } else {
                appendLine(activity.getString(R.string.dialog_perms_all_granted_message))
            }
        }

    }

    /**
     * Shows a dialog with an explanation and impact of partial functionality.
     */
    fun showPermissionStatusDialog(activity: AppCompatActivity) {
        val missingPermissions = getMissingPermissions(activity)
        val grantedPermissions = getGrantedPermissions(activity)

        val message = buildPermissionInfoMessages(activity)

        val dialogBuilder = MaterialAlertDialogBuilder(activity)
            .setTitle(activity.getString(R.string.permission_status_title)) // Reusing existing
            .setMessage(message)

        if (missingPermissions.isNotEmpty()) {
            dialogBuilder.setPositiveButton(activity.getString(R.string.grant_permissions)) { _, _ -> // Reusing existing
                requestMissingPermissions(activity, missingPermissions)
            }
            dialogBuilder.setNegativeButton(activity.getString(R.string.continue_without)) { _, _ -> // Reusing existing
                if (missingPermissions.any { it.critical }) {
                    showCriticalWarning(activity, missingPermissions)
                }
            }
        } else {
            dialogBuilder.setPositiveButton(activity.getString(R.string.ok_button)) { _, _ -> } // Reusing existing
        }

        dialogBuilder.show()
    }

    /**
     * Manual permission check (callable by the user).
     */
    fun performManualPermissionCheck(activity: AppCompatActivity): String {
        val missing = getMissingPermissions(activity)
        val granted = getGrantedPermissions(activity)
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        return buildString {
            appendLine(activity.getString(R.string.manual_check_perms_header))
            appendLine(activity.getString(R.string.manual_check_perms_date_format, sdf.format(Date())))
            appendLine()
            appendLine(activity.getString(R.string.manual_check_perms_granted_count, granted.size, permissionExplanations.size))
            appendLine(activity.getString(R.string.manual_check_perms_missing_count, missing.size, permissionExplanations.size))

            appendLine()
            appendLine(buildPermissionInfoMessages(activity))

            if (missing.isNotEmpty()) {
                appendLine()
                appendLine(activity.getString(R.string.manual_check_perms_limited_functionality_header))
                missing.forEach {
                    appendLine(activity.getString(R.string.manual_check_perms_impact_item_format, activity.getString(it.impactResId)))
                }
            }

            val criticalMissing = missing.filter { it.critical }
            if (criticalMissing.isNotEmpty()) {
                appendLine()
                appendLine(activity.getString(R.string.manual_check_perms_app_not_working_warning))
            } else if (missing.isNotEmpty()) {
                appendLine()
                appendLine(activity.getString(R.string.manual_check_perms_app_working_limited))
            } else {
                appendLine()
                appendLine(activity.getString(R.string.manual_check_perms_app_fully_working))
            }
        }
    }

    private fun getMissingPermissions(context: Context): List<PermissionInfo> {
        val missing = mutableListOf<PermissionInfo>()

        permissionExplanations.forEach { info ->
            when (info.permission) {
                Manifest.permission.POST_NOTIFICATIONS -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (!hasPermission(context, info.permission)) {
                            missing.add(info)
                        }
                    }
                }
                else -> {
                    if (!hasPermission(context, info.permission)) {
                        missing.add(info)
                    }
                }
            }
        }
        return missing
    }

    private fun getGrantedPermissions(context: Context): List<PermissionInfo> {
        return permissionExplanations.filter { info ->
            when (info.permission) {
                Manifest.permission.POST_NOTIFICATIONS -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        hasPermission(context, info.permission)
                    } else {
                        true // On older versions, this permission is implicitly granted or not needed
                    }
                }
                else -> hasPermission(context, info.permission)
            }
        }
    }

    private fun hasPermission(context: Context, permission: String): Boolean {
        return try {
            context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            // Should not happen if manifest is correct, but as a fallback, assume not granted or issue
            false
        }
    }

    private fun requestMissingPermissions(activity: AppCompatActivity, missing: List<PermissionInfo>) {
        val permissions = missing.map { it.permission }.toTypedArray()
        activity.requestPermissions(permissions, 100) // PERMISSION_REQUEST_CODE = 100
    }

    private fun showCriticalWarning(activity: AppCompatActivity, missing: List<PermissionInfo>) {
        val critical = missing.filter { it.critical }

        val message = buildString {
            appendLine(activity.getString(R.string.dialog_critical_warning_message_header))
            appendLine()
            critical.forEach {
                appendLine(activity.getString(R.string.dialog_critical_warning_item_format, activity.getString(it.nameResId), activity.getString(it.reasonResId)))
            }
            appendLine()
            appendLine(activity.getString(R.string.dialog_critical_warning_disabled_functionality_header))
            critical.forEach {
                // Removing "Without: " prefix if present, as it might be redundant here
                val impactText = activity.getString(it.impactResId).removePrefix("Without: ").removePrefix("Senza: ")
                appendLine(activity.getString(R.string.dialog_critical_warning_disabled_item_format, impactText))
            }
        }

        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.app_not_working)) // Reusing existing
            .setMessage(message)
            .setPositiveButton(activity.getString(R.string.understood)) { _, _ -> } // Reusing existing
            .show()
    }
}
