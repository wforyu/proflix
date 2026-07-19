package com.proflix.database.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "proflix_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val LANGUAGE = stringPreferencesKey("language")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val QUALITY_PREFERENCE = stringPreferencesKey("quality_preference")
        val SUBTITLE_ENABLED = booleanPreferencesKey("subtitle_enabled")
        val SUBTITLE_LANGUAGE = stringPreferencesKey("subtitle_language")
        val PI_ENABLED = booleanPreferencesKey("pi_enabled")
        val SELECTED_PROVIDER = stringPreferencesKey("selected_provider")
        val CUSTOM_DOMAIN_ANOBOY = stringPreferencesKey("custom_domain_anoboy")
        val CUSTOM_DOMAIN_SAMEHADAKU = stringPreferencesKey("custom_domain_samehadaku")
        val CUSTOM_DOMAIN_OPLOVERZ = stringPreferencesKey("custom_domain_oploverz")
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IS_DARK_THEME] ?: true
    }

    val language: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.LANGUAGE] ?: "en"
    }

    val playbackSpeed: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.PLAYBACK_SPEED] ?: 1f
    }

    val qualityPreference: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.QUALITY_PREFERENCE] ?: "auto"
    }

    val subtitleEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SUBTITLE_ENABLED] ?: false
    }

    val subtitleLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.SUBTITLE_LANGUAGE] ?: "en"
    }

    val isFavorite: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.PI_ENABLED] ?: false
    }

    val selectedProvider: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.SELECTED_PROVIDER] ?: "ANOBOY"
    }

    val customDomainAnoboy: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.CUSTOM_DOMAIN_ANOBOY] ?: ""
    }

    val customDomainSamehadaku: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.CUSTOM_DOMAIN_SAMEHADAKU] ?: ""
    }

    val customDomainOploverz: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.CUSTOM_DOMAIN_OPLOVERZ] ?: ""
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_DARK_THEME] = enabled
        }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language
        }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PLAYBACK_SPEED] = speed
        }
    }

    suspend fun setQualityPreference(quality: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.QUALITY_PREFERENCE] = quality
        }
    }

    suspend fun setSubtitleEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SUBTITLE_ENABLED] = enabled
        }
    }

    suspend fun setSubtitleLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SUBTITLE_LANGUAGE] = language
        }
    }

    suspend fun setSelectedProvider(provider: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_PROVIDER] = provider
        }
    }

    suspend fun setCustomDomainAnoboy(domain: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_DOMAIN_ANOBOY] = domain
        }
    }

    suspend fun setCustomDomainSamehadaku(domain: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_DOMAIN_SAMEHADAKU] = domain
        }
    }

    suspend fun setCustomDomainOploverz(domain: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_DOMAIN_OPLOVERZ] = domain
        }
    }
}
