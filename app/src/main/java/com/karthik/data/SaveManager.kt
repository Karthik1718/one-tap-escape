package com.karthik.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*

class SaveManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("one_tap_escape_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_COINS = "total_coins"
        private const val KEY_HIGH_SCORE = "infinite_high_score"
        private const val KEY_PURCHASED_COLORS = "purchased_colors"
        private const val KEY_SELECTED_COLOR = "selected_color"
        private const val KEY_PURCHASED_THEMES = "purchased_themes"
        private const val KEY_SELECTED_THEME = "selected_theme"
        private const val KEY_PURCHASED_MODELS = "purchased_models"
        private const val KEY_SELECTED_MODEL = "selected_model"

        val ALL_COLORS = setOf("#00E5FF", "#FF4081", "#7C4DFF", "#FFEA00", "#00C853", "#FF3D00", "#FFFFFF")
        val ALL_THEMES = setOf("VIBRANT_CITY", "CYBERPUNK_NIGHT", "DESERT_OUTRUN", "MIDNIGHT_FOREST")
        val ALL_MODELS = setOf("SPEED_RACER", "CYBER_TRUCK", "POLICE_CRUISER", "SUPER_BOLT")

        // Global shared state for reactivity across instances
        var totalCoins by mutableStateOf(0)
            private set
        
        var highScore by mutableStateOf(0f)
            private set
            
        private var initialized = false
    }

    val totalCoins get() = Companion.totalCoins
    val highScore get() = Companion.highScore

    init {
        if (!initialized) {
            Companion.totalCoins = prefs.getInt(KEY_COINS, 0)
            Companion.highScore = prefs.getFloat(KEY_HIGH_SCORE, 0f)
            initialized = true
        }

        // If test mode was previously applied on this device, revert back to clean state
        if (prefs.getBoolean("testing_all_unlocked_v1", false)) {
            prefs.edit()
                .putStringSet(KEY_PURCHASED_COLORS, setOf("#00E5FF"))
                .putStringSet(KEY_PURCHASED_THEMES, setOf("VIBRANT_CITY"))
                .putStringSet(KEY_PURCHASED_MODELS, setOf("SPEED_RACER"))
                .putString(KEY_SELECTED_COLOR, "#00E5FF")
                .putString(KEY_SELECTED_THEME, "VIBRANT_CITY")
                .putString(KEY_SELECTED_MODEL, "SPEED_RACER")
                .putInt(KEY_COINS, 0)
                .remove("testing_all_unlocked_v1")
                .apply()
            Companion.totalCoins = 0
        }
    }

    fun addCoins(amount: Int) {
        Companion.totalCoins += amount
        prefs.edit().putInt(KEY_COINS, Companion.totalCoins).apply()
    }

    fun saveHighScore(score: Float) {
        if (score > highScore) {
            Companion.highScore = score
            prefs.edit().putFloat(KEY_HIGH_SCORE, Companion.highScore).apply()
        }
    }

    fun getPurchasedColors(): Set<String> {
        val defaultColors = setOf("#00E5FF")
        return prefs.getStringSet(KEY_PURCHASED_COLORS, defaultColors) ?: defaultColors
    }

    fun savePurchasedColor(colorHex: String) {
        val colors = getPurchasedColors().toMutableSet()
        colors.add(colorHex)
        prefs.edit().putStringSet(KEY_PURCHASED_COLORS, colors).apply()
    }

    fun getSelectedColor(): String = prefs.getString(KEY_SELECTED_COLOR, "#00E5FF") ?: "#00E5FF"

    fun saveSelectedColor(colorHex: String) {
        prefs.edit().putString(KEY_SELECTED_COLOR, colorHex).apply()
    }

    fun getPurchasedThemes(): Set<String> {
        val defaultThemes = setOf("VIBRANT_CITY")
        return prefs.getStringSet(KEY_PURCHASED_THEMES, defaultThemes) ?: defaultThemes
    }

    fun savePurchasedTheme(themeId: String) {
        val themes = getPurchasedThemes().toMutableSet()
        themes.add(themeId)
        prefs.edit().putStringSet(KEY_PURCHASED_THEMES, themes).apply()
    }

    fun getSelectedTheme(): String = prefs.getString(KEY_SELECTED_THEME, "VIBRANT_CITY") ?: "VIBRANT_CITY"

    fun saveSelectedTheme(themeId: String) {
        prefs.edit().putString(KEY_SELECTED_THEME, themeId).apply()
    }

    fun getPurchasedModels(): Set<String> {
        val defaultModels = setOf("SPEED_RACER")
        return prefs.getStringSet(KEY_PURCHASED_MODELS, defaultModels) ?: defaultModels
    }

    fun savePurchasedModel(modelId: String) {
        val models = getPurchasedModels().toMutableSet()
        models.add(modelId)
        prefs.edit().putStringSet(KEY_PURCHASED_MODELS, models).apply()
    }

    fun getSelectedModel(): String = prefs.getString(KEY_SELECTED_MODEL, "SPEED_RACER") ?: "SPEED_RACER"

    fun saveSelectedModel(modelId: String) {
        prefs.edit().putString(KEY_SELECTED_MODEL, modelId).apply()
    }
}
