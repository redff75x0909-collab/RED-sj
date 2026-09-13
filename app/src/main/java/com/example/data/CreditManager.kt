package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class CreditManager(context: Context) {

    companion object {
        const val CREDIT_DURATION_MS = 2 * 60 * 60 * 1000L // ২ ঘন্টা (2 Hours)
        const val COOLDOWN_DURATION_MS = 6 * 60 * 60 * 1000L // ৬ ঘন্টা (6 Hours)

        private const val PREFS_NAME = "rdc_credit_prefs"
        private const val KEY_REMAINING_CREDIT_MS = "remaining_credit_ms"
        private const val KEY_EXHAUSTED_TIMESTAMP = "credit_exhausted_timestamp"
        private const val KEY_LAST_CONSUME_TIMESTAMP = "last_consume_timestamp"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val _remainingCreditMs = MutableStateFlow(CREDIT_DURATION_MS)
    val remainingCreditMs: StateFlow<Long> = _remainingCreditMs.asStateFlow()

    private val _cooldownRemainingMs = MutableStateFlow(0L)
    val cooldownRemainingMs: StateFlow<Long> = _cooldownRemainingMs.asStateFlow()

    private val _isCreditActive = MutableStateFlow(true)
    val isCreditActive: StateFlow<Boolean> = _isCreditActive.asStateFlow()

    private var isSessionRunning = false

    init {
        loadState()
        startTicker()
    }

    private fun loadState() {
        var remaining = prefs.getLong(KEY_REMAINING_CREDIT_MS, CREDIT_DURATION_MS)
        val exhaustedAt = prefs.getLong(KEY_EXHAUSTED_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()

        if (exhaustedAt > 0L) {
            val elapsed = now - exhaustedAt
            if (elapsed >= COOLDOWN_DURATION_MS) {
                // ৬ ঘন্টা শেষ হয়েছে -> নতুন ১ ক্রেডিট (২ ঘন্টা) স্বয়ংক্রিয়ভাবে যুক্ত হলো
                remaining = CREDIT_DURATION_MS
                prefs.edit()
                    .putLong(KEY_REMAINING_CREDIT_MS, remaining)
                    .putLong(KEY_EXHAUSTED_TIMESTAMP, 0L)
                    .apply()
                _cooldownRemainingMs.value = 0L
                _isCreditActive.value = true
            } else {
                // এখনো ৬ ঘণ্টার কুলডাউনে আছে
                remaining = 0L
                _cooldownRemainingMs.value = (COOLDOWN_DURATION_MS - elapsed).coerceAtLeast(0L)
                _isCreditActive.value = false
            }
        } else {
            if (remaining <= 0L) {
                // মার্ক কুলডাউন শুরু
                prefs.edit().putLong(KEY_EXHAUSTED_TIMESTAMP, now).apply()
                _cooldownRemainingMs.value = COOLDOWN_DURATION_MS
                _isCreditActive.value = false
            } else {
                _cooldownRemainingMs.value = 0L
                _isCreditActive.value = true
            }
        }

        _remainingCreditMs.value = remaining
    }

    private fun startTicker() {
        scope.launch {
            while (isActive) {
                delay(1000L)
                tick()
            }
        }
    }

    private fun tick() {
        val now = System.currentTimeMillis()
        val exhaustedAt = prefs.getLong(KEY_EXHAUSTED_TIMESTAMP, 0L)

        if (exhaustedAt > 0L) {
            val elapsed = now - exhaustedAt
            if (elapsed >= COOLDOWN_DURATION_MS) {
                // ৬ ঘন্টা পূর্ণ হলো -> ১ নতুন ক্রেডিট (২ ঘন্টা) স্বয়ংক্রিয়ভাবে যোগ হলো
                val newCredit = CREDIT_DURATION_MS
                prefs.edit()
                    .putLong(KEY_REMAINING_CREDIT_MS, newCredit)
                    .putLong(KEY_EXHAUSTED_TIMESTAMP, 0L)
                    .apply()
                _remainingCreditMs.value = newCredit
                _cooldownRemainingMs.value = 0L
                _isCreditActive.value = true
            } else {
                _cooldownRemainingMs.value = (COOLDOWN_DURATION_MS - elapsed).coerceAtLeast(0L)
                _isCreditActive.value = false
            }
        } else {
            // ক্রেডিট সচল আছে
            if (isSessionRunning && _remainingCreditMs.value > 0L) {
                val updated = (_remainingCreditMs.value - 1000L).coerceAtLeast(0L)
                _remainingCreditMs.value = updated
                prefs.edit().putLong(KEY_REMAINING_CREDIT_MS, updated).apply()

                if (updated <= 0L) {
                    // ক্রেডিট শেষ হলো -> এখন ৬ ঘণ্টার কুলডাউন শুরু
                    prefs.edit()
                        .putLong(KEY_EXHAUSTED_TIMESTAMP, now)
                        .putLong(KEY_REMAINING_CREDIT_MS, 0L)
                        .apply()
                    _cooldownRemainingMs.value = COOLDOWN_DURATION_MS
                    _isCreditActive.value = false
                }
            }
        }
    }

    fun setSessionActive(active: Boolean) {
        isSessionRunning = active
    }

    fun isCreditAvailable(): Boolean {
        loadState()
        return _isCreditActive.value && _remainingCreditMs.value > 0L
    }

    fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatDurationBengali(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        fun toBengaliDigits(number: Long): String {
            val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
            return number.toString().map { if (it in '0'..'9') bengaliDigits[it - '0'] else it }.joinToString("")
        }

        return "${toBengaliDigits(hours)} ঘণ্টা ${toBengaliDigits(minutes)} মিনিট ${toBengaliDigits(seconds)} সেকেন্ড"
    }
}
