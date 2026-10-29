package com.neoblogger.app.storage

import android.content.Context
import android.content.SharedPreferences

enum class AdState {
    PENDING,
    COMPLETED_SHOWN,
    COMPLETED_SKIPPED,
}

class AdPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("neoblogger_ad_prefs", Context.MODE_PRIVATE)

    var state: AdState
        get() {
            val name = prefs.getString(KEY_AD_STATE, AdState.PENDING.name) ?: AdState.PENDING.name
            return runCatching { AdState.valueOf(name) }.getOrDefault(AdState.PENDING)
        }
        set(value) {
            prefs.edit().putString(KEY_AD_STATE, value.name).apply()
        }

    fun shouldAttemptLaunchAd(): Boolean {
        return state == AdState.PENDING
    }

    fun markAdShown() {
        state = AdState.COMPLETED_SHOWN
    }

    fun markAdSkipped() {
        state = AdState.COMPLETED_SKIPPED
    }

    companion object {
        private const val KEY_AD_STATE = "launch_ad_state"
    }
}