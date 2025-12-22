package com.example.andalib.ui.theme

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

class ThemePreferences(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")

    val isDarkTheme: Flow<Boolean> =
        context.themeDataStore.data.map { prefs ->
            prefs[KEY_DARK_MODE] ?: false
        }

    fun setDarkTheme(isDark: Boolean) {
        scope.launch {
            context.themeDataStore.edit { prefs ->
                prefs[KEY_DARK_MODE] = isDark
            }
        }
    }
}

/**
 * CompositionLocal supaya ThemePreferences bisa diakses dari Composable mana pun.
 */
val LocalThemePreferences = staticCompositionLocalOf<ThemePreferences> {
    error("ThemePreferences belum diprovide. Pastikan CompositionLocalProvider(LocalThemePreferences provides ...) dipasang di MainActivity.")
}

