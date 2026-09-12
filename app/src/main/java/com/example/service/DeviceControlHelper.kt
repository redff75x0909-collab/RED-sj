package com.example.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

object DeviceControlHelper {

    fun executeDeviceShortcut(context: Context, shortcut: String): Boolean {
        return try {
            val intent = when (shortcut.lowercase()) {
                "wifi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
                "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                "display" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
                "sound", "volume" -> Intent(Settings.ACTION_SOUND_SETTINGS)
                "battery" -> Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                "accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                "apps" -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
                "app_details" -> {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                }
                else -> Intent(Settings.ACTION_SETTINGS)
            }.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open settings: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun launchAppByName(context: Context, appQuery: String): Boolean {
        val cleanName = appQuery.lowercase().trim()
            .replace("open ", "")
            .replace("launch ", "")
            .trim()

        val packageManager = context.packageManager

        // Known package mapping
        val knownPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "calculator" to "com.google.android.calculator",
            "camera" to "android.media.action.IMAGE_CAPTURE",
            "settings" to "com.android.settings",
            "maps" to "com.google.android.apps.maps",
            "chrome" to "com.android.chrome",
            "play store" to "com.android.vending",
            "clock" to "com.google.android.deskclock"
        )

        val targetPkg = knownPackages[cleanName]
        if (targetPkg != null) {
            val launchIntent = packageManager.getLaunchIntentForPackage(targetPkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return true
            }
        }

        // Fuzzy search installed apps
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in installedApps) {
            val label = packageManager.getApplicationLabel(app).toString().lowercase()
            if (label.contains(cleanName)) {
                val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return true
                }
            }
        }

        Toast.makeText(context, "Could not find app \"$appQuery\" installed.", Toast.LENGTH_SHORT).show()
        return false
    }
}
