package com.example.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

data class LaunchAppResult(
    val success: Boolean,
    val appLabel: String? = null,
    val message: String
)

object DeviceControlHelper {

    private val BENGALI_APP_MAP = mapOf(
        "ইউটিউব" to listOf("youtube", "com.google.android.youtube"),
        "ফেসবুক" to listOf("facebook", "com.facebook.katana", "com.facebook.lite"),
        "হোয়াটসঅ্যাপ" to listOf("whatsapp", "com.whatsapp", "com.whatsapp.w4b"),
        "হোয়াটসঅ্যাপ" to listOf("whatsapp", "com.whatsapp", "com.whatsapp.w4b"),
        "মেসেঞ্জার" to listOf("messenger", "com.facebook.orca"),
        "ক্যামেরা" to listOf("camera", "android.media.action.IMAGE_CAPTURE"),
        "গ্যালারি" to listOf("gallery", "photos", "com.google.android.apps.photos"),
        "ক্যালকুলেটর" to listOf("calculator", "com.google.android.calculator"),
        "সেটিংস" to listOf("settings", "com.android.settings"),
        "ম্যাপ" to listOf("maps", "com.google.android.apps.maps"),
        "ম্যাপস" to listOf("maps", "com.google.android.apps.maps"),
        "ক্রোম" to listOf("chrome", "com.android.chrome"),
        "টিকটক" to listOf("tiktok", "com.zhiliaoapp.musically"),
        "ইনস্টাগ্রাম" to listOf("instagram", "com.instagram.android"),
        "ইন্সটাগ্রাম" to listOf("instagram", "com.instagram.android"),
        "টেলিগ্রাম" to listOf("telegram", "org.telegram.messenger"),
        "প্লে স্টোর" to listOf("play store", "vending", "com.android.vending"),
        "প্লেস্টোর" to listOf("play store", "vending", "com.android.vending"),
        "ঘড়ি" to listOf("clock", "com.google.android.deskclock"),
        "ক্লক" to listOf("clock", "com.google.android.deskclock"),
        "জিমেইল" to listOf("gmail", "com.google.android.gm"),
        "ইমেইল" to listOf("email", "gmail", "com.google.android.gm"),
        "ফোন" to listOf("phone", "dialer", "com.google.android.dialer"),
        "কল" to listOf("phone", "dialer", "com.google.android.dialer"),
        "মেসেজ" to listOf("messages", "com.google.android.apps.messaging"),
        "ফাইল" to listOf("files", "file manager", "com.google.android.documentsui"),
        "ফাইল ম্যানেজার" to listOf("files", "file manager", "com.google.android.documentsui")
    )

    private val KNOWN_PACKAGES = mapOf(
        "youtube" to "com.google.android.youtube",
        "facebook" to "com.facebook.katana",
        "whatsapp" to "com.whatsapp",
        "messenger" to "com.facebook.orca",
        "calculator" to "com.google.android.calculator",
        "camera" to "android.media.action.IMAGE_CAPTURE",
        "settings" to "com.android.settings",
        "maps" to "com.google.android.apps.maps",
        "chrome" to "com.android.chrome",
        "play store" to "com.android.vending",
        "clock" to "com.google.android.deskclock",
        "gmail" to "com.google.android.gm",
        "telegram" to "org.telegram.messenger",
        "instagram" to "com.instagram.android",
        "tiktok" to "com.zhiliaoapp.musically"
    )

    fun openUrl(context: Context, rawUrl: String): Boolean {
        return try {
            val validUrl = when {
                rawUrl.startsWith("http://", ignoreCase = true) || rawUrl.startsWith("https://", ignoreCase = true) -> rawUrl
                rawUrl.startsWith("www.", ignoreCase = true) -> "https://$rawUrl"
                else -> "https://$rawUrl"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "ওয়েব লিংক ওপেন করা যায়নি: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun extractUrl(text: String): String? {
        val urlRegex = "(https?://[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]+|www\\.[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]+)".toRegex(RegexOption.IGNORE_CASE)
        val match = urlRegex.find(text)
        return match?.value
    }

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

    fun isAppLaunchCommand(command: String): Boolean {
        val lower = command.lowercase().trim()
        val prefixes = listOf("open ", "launch ", "start ", "run ", "খোলো", "খুলো", "ওপেন করো", "ওপেন কর", "চালু করো", "চালু কর", "চালাও", "অ্যাপ ওপেন", "ঢুক", "ঢোকো")
        return prefixes.any { lower.contains(it) } || KNOWN_PACKAGES.keys.any { lower.contains(it) } || BENGALI_APP_MAP.keys.any { lower.contains(it) }
    }

    fun cleanAppNameQuery(appQuery: String): String {
        var clean = appQuery.lowercase().trim()
        val removeWords = listOf(
            "open ", "launch ", "start ", "run ", "app ", "the ",
            "ওপেন করো", "ওপেন কর", "খোলো", "খুলো", "চালু করো", "চালু কর", "চালাও",
            "অ্যাপটি", "অ্যাপটা", "অ্যাপ", "টা", "টি", "দাও", "করুন", "কর"
        )
        for (w in removeWords) {
            clean = clean.replace(w, "").trim()
        }
        return clean
    }

    fun launchAppByName(context: Context, appQuery: String): LaunchAppResult {
        val cleanName = cleanAppNameQuery(appQuery)
        val packageManager = context.packageManager

        // 1. Check Bengali keyword dictionary mapping
        for ((bnKey, targets) in BENGALI_APP_MAP) {
            if (cleanName.contains(bnKey) || appQuery.contains(bnKey)) {
                for (target in targets) {
                    val directIntent = packageManager.getLaunchIntentForPackage(target)
                    if (directIntent != null) {
                        directIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(directIntent)
                        return LaunchAppResult(true, bnKey, "$bnKey অ্যাপটি ওপেন করা হয়েছে।")
                    }
                }
                // Try fuzzy with target aliases
                val installed = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                for (app in installed) {
                    val label = packageManager.getApplicationLabel(app).toString().lowercase()
                    if (targets.any { label.contains(it) || app.packageName.lowercase().contains(it) }) {
                        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launchIntent)
                            return LaunchAppResult(true, label, "$bnKey ($label) অ্যাপটি চালু করা হয়েছে।")
                        }
                    }
                }
            }
        }

        // 2. Check Known English Packages
        val knownPkg = KNOWN_PACKAGES[cleanName]
        if (knownPkg != null) {
            val launchIntent = packageManager.getLaunchIntentForPackage(knownPkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return LaunchAppResult(true, cleanName, "Opening $cleanName")
            }
        }

        // 3. Dynamic search across ALL installed applications on user's device
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in installedApps) {
            val label = packageManager.getApplicationLabel(app).toString().lowercase()
            if (label.contains(cleanName) || cleanName.contains(label)) {
                val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return LaunchAppResult(true, label, "$label অ্যাপটি চালু করা হয়েছে।")
                }
            }
        }

        Toast.makeText(context, "\"$appQuery\" নামের কোনো ইনস্টল করা অ্যাপ পাওয়া যায়নি।", Toast.LENGTH_SHORT).show()
        return LaunchAppResult(false, null, "\"$appQuery\" অ্যাপটি ফোনে পাওয়া যায়নি।")
    }
}

