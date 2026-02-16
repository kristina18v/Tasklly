package com.example.tasklly.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREF = "app_settings"
    private const val KEY_DARK = "dark_mode"

    fun isDark(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_DARK, false)
    }

    fun apply(context: Context) {
        val night = if (isDark(context)) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(night)
    }

    fun setDark(context: Context, enabled: Boolean) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_DARK, enabled).apply()
        apply(context)
    }
}
