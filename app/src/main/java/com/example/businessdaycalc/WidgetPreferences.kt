package com.example.businessdaycalc

import android.content.Context

object WidgetPreferences {
    private const val PREFS_NAME = "widget_prefs"
    private const val KEY_OPACITY_PREFIX = "opacity_"
    private const val KEY_THEME_PREFIX = "theme_"

    fun saveOpacity(context: Context, appWidgetId: Int, opacity: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_OPACITY_PREFIX + appWidgetId, opacity)
            .apply()
    }

    fun getOpacity(context: Context, appWidgetId: Int): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_OPACITY_PREFIX + appWidgetId, 100)
    }

    fun saveTheme(context: Context, appWidgetId: Int, isDark: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_THEME_PREFIX + appWidgetId, isDark)
            .apply()
    }

    fun isDark(context: Context, appWidgetId: Int): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_THEME_PREFIX + appWidgetId, false)
    }
}
