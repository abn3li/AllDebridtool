package com.example.alldebrid.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "alldebrid_prefs")

enum class ThemeMode { LIGHT, DARK, SYSTEM }

class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_HIDDEN_LINKS = stringSetPreferencesKey("hidden_links")
    }

    val apiKeyFlow: Flow<String?> = context.dataStore.data.map { it[KEY_API_KEY] }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map {
        when (it[KEY_THEME_MODE]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val hiddenLinksFlow: Flow<Set<String>> = context.dataStore.data.map {
        it[KEY_HIDDEN_LINKS] ?: emptySet()
    }

    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { it[KEY_API_KEY] = apiKey }
    }

    suspend fun clearApiKey() {
        context.dataStore.edit {
            it.remove(KEY_API_KEY)
            it.remove(KEY_HIDDEN_LINKS)
        }
    }

    suspend fun saveThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun addHiddenLink(link: String) {
        context.dataStore.edit {
            val current = it[KEY_HIDDEN_LINKS] ?: emptySet()
            it[KEY_HIDDEN_LINKS] = current + link
        }
    }

    suspend fun clearHiddenLinks() {
        context.dataStore.edit { it.remove(KEY_HIDDEN_LINKS) }
    }
}
