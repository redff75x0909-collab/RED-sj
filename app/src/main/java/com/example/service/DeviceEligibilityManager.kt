package com.example.service

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class DeviceSpecs(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val totalRamGb: Double,
    val isVivo: Boolean,
    val is4GbRam: Boolean,
    val isEligible: Boolean,
    val isBypassActive: Boolean
)

class DeviceEligibilityManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("rdc_device_eligibility", Context.MODE_PRIVATE)

    private val _deviceSpecs = MutableStateFlow(checkDeviceSpecs())
    val deviceSpecs: StateFlow<DeviceSpecs> = _deviceSpecs.asStateFlow()

    private fun checkDeviceSpecs(): DeviceSpecs {
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val brand = Build.BRAND.orEmpty()
        val model = Build.MODEL.orEmpty()

        val isVivo = manufacturer.contains("vivo", ignoreCase = true) ||
                brand.contains("vivo", ignoreCase = true)

        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)
        val totalRamBytes = memInfo.totalMem
        val totalRamGb = totalRamBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)

        // 4GB RAM phones generally report between 3.2 GB and 4.8 GB of physical RAM
        val is4GbRam = totalRamGb in 3.2..4.8

        val isBypassActive = prefs.getBoolean("dev_testing_bypass", false)
        val isEligible = (isVivo && is4GbRam) || isBypassActive

        return DeviceSpecs(
            manufacturer = manufacturer,
            brand = brand,
            model = model,
            totalRamGb = totalRamGb,
            isVivo = isVivo,
            is4GbRam = is4GbRam,
            isEligible = isEligible,
            isBypassActive = isBypassActive
        )
    }

    fun refresh() {
        _deviceSpecs.value = checkDeviceSpecs()
    }

    fun toggleTestingBypass(enable: Boolean) {
        prefs.edit().putBoolean("dev_testing_bypass", enable).apply()
        refresh()
    }

    fun isDeviceAllowed(): Boolean {
        return _deviceSpecs.value.isEligible
    }
}
