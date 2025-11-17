package com.octopus.launcher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color

class SettingsRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "octopus_launcher_settings",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_BACKGROUND_COLOR_1 = "background_color_1"
        private const val KEY_BACKGROUND_COLOR_2 = "background_color_2"
        private const val KEY_BACKGROUND_COLOR_3 = "background_color_3"
        private const val KEY_BACKGROUND_IMAGE = "background_image"
        private const val KEY_BACKGROUND_BLUR = "background_blur"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        
        // Default colors (ocean green theme) as ARGB
        private const val DEFAULT_COLOR_1 = 0xFF4A90A4L
        private const val DEFAULT_COLOR_2 = 0xFF5BA68CL
        private const val DEFAULT_COLOR_3 = 0xFF6B9E7AL
    }
    
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }
    
    fun setFirstLaunchComplete() {
        prefs.edit()
            .putBoolean(KEY_FIRST_LAUNCH, false)
            .apply()
    }
    
    fun getBackgroundColor1(): Color {
        val colorValue = prefs.getLong(KEY_BACKGROUND_COLOR_1, DEFAULT_COLOR_1).toInt()
        return Color(
            red = android.graphics.Color.red(colorValue) / 255f,
            green = android.graphics.Color.green(colorValue) / 255f,
            blue = android.graphics.Color.blue(colorValue) / 255f,
            alpha = android.graphics.Color.alpha(colorValue) / 255f
        )
    }
    
    fun getBackgroundColor2(): Color {
        val colorValue = prefs.getLong(KEY_BACKGROUND_COLOR_2, DEFAULT_COLOR_2).toInt()
        return Color(
            red = android.graphics.Color.red(colorValue) / 255f,
            green = android.graphics.Color.green(colorValue) / 255f,
            blue = android.graphics.Color.blue(colorValue) / 255f,
            alpha = android.graphics.Color.alpha(colorValue) / 255f
        )
    }
    
    fun getBackgroundColor3(): Color {
        val colorValue = prefs.getLong(KEY_BACKGROUND_COLOR_3, DEFAULT_COLOR_3).toInt()
        return Color(
            red = android.graphics.Color.red(colorValue) / 255f,
            green = android.graphics.Color.green(colorValue) / 255f,
            blue = android.graphics.Color.blue(colorValue) / 255f,
            alpha = android.graphics.Color.alpha(colorValue) / 255f
        )
    }
    
    fun setBackgroundColor1(color: Color) {
        // Use android.graphics.Color to convert to ARGB
        val argb = android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
        prefs.edit()
            .putLong(KEY_BACKGROUND_COLOR_1, argb.toLong())
            .apply()
    }
    
    fun setBackgroundColor2(color: Color) {
        val argb = android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
        prefs.edit()
            .putLong(KEY_BACKGROUND_COLOR_2, argb.toLong())
            .apply()
    }
    
    fun setBackgroundColor3(color: Color) {
        val argb = android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
        prefs.edit()
            .putLong(KEY_BACKGROUND_COLOR_3, argb.toLong())
            .apply()
    }
    
    fun resetToDefaults() {
        prefs.edit()
            .putLong(KEY_BACKGROUND_COLOR_1, DEFAULT_COLOR_1)
            .putLong(KEY_BACKGROUND_COLOR_2, DEFAULT_COLOR_2)
            .putLong(KEY_BACKGROUND_COLOR_3, DEFAULT_COLOR_3)
            .apply()
    }
    
    // Get background color 1 as Android Color (ARGB int) for window background
    fun getBackgroundColor1AsInt(): Int {
        return prefs.getLong(KEY_BACKGROUND_COLOR_1, DEFAULT_COLOR_1).toInt()
    }
    
    // Background image methods
    fun getBackgroundImagePath(): String? {
        return prefs.getString(KEY_BACKGROUND_IMAGE, null)
    }
    
    fun setBackgroundImagePath(path: String?) {
        prefs.edit()
            .putString(KEY_BACKGROUND_IMAGE, path)
            .apply()
    }
    
    fun clearBackgroundImage() {
        prefs.edit()
            .remove(KEY_BACKGROUND_IMAGE)
            .apply()
    }
    
    // Background blur methods
    fun isBackgroundBlurEnabled(): Boolean {
        return prefs.getBoolean(KEY_BACKGROUND_BLUR, false)
    }
    
    fun setBackgroundBlurEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_BACKGROUND_BLUR, enabled)
            .apply()
    }
}

