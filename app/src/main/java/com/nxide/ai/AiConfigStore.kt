package com.nxide.ai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_config")

/**
 * AI 配置持久化存储
 */
class AiConfigStore(private val context: Context) {

    companion object {
        private val KEY_ENDPOINT = stringPreferencesKey("api_endpoint")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_MODEL = stringPreferencesKey("model")
        private val KEY_MAX_TOKENS = stringPreferencesKey("max_tokens")
        private val KEY_TEMPERATURE = stringPreferencesKey("temperature")
    }

    val config: Flow<AiConfig> = context.dataStore.data.map { prefs ->
        AiConfig(
            apiEndpoint = prefs[KEY_ENDPOINT] ?: AiConfig.DEFAULT_ENDPOINT,
            apiKey = prefs[KEY_API_KEY] ?: "",
            model = prefs[KEY_MODEL] ?: AiConfig.DEFAULT_MODEL,
            maxTokens = prefs[KEY_MAX_TOKENS]?.toIntOrNull() ?: 4096,
            temperature = prefs[KEY_TEMPERATURE]?.toDoubleOrNull() ?: 0.7
        )
    }

    suspend fun save(config: AiConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ENDPOINT] = config.apiEndpoint
            prefs[KEY_API_KEY] = config.apiKey
            prefs[KEY_MODEL] = config.model
            prefs[KEY_MAX_TOKENS] = config.maxTokens.toString()
            prefs[KEY_TEMPERATURE] = config.temperature.toString()
        }
    }
}
