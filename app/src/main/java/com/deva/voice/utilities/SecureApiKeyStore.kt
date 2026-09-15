package com.deva.voice.utilities

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecureApiKeyStore {
    private const val PREFS_FILE = "secure_ai_api_settings"
    private const val KEY_GEMINI = "gemini_api_key"
    private const val KEY_OPENROUTER = "openrouter_api_key"

    private fun preferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveGeminiKey(context: Context, key: String) {
        val cleaned = key.trim()
        preferences(context).edit().putString(KEY_GEMINI, cleaned.ifBlank { "" }).apply()
    }

    fun saveOpenRouterKey(context: Context, key: String) {
        val cleaned = key.trim()
        preferences(context).edit().putString(KEY_OPENROUTER, cleaned.ifBlank { "" }).apply()
    }

    fun getGeminiKey(context: Context): String? {
        val value = preferences(context).getString(KEY_GEMINI, null)?.trim()
        return value?.takeIf { it.isNotEmpty() }
    }

    fun getOpenRouterKey(context: Context): String? {
        val value = preferences(context).getString(KEY_OPENROUTER, null)?.trim()
        return value?.takeIf { it.isNotEmpty() }
    }

    fun clearGeminiKey(context: Context) {
        preferences(context).edit().remove(KEY_GEMINI).apply()
    }

    fun clearOpenRouterKey(context: Context) {
        preferences(context).edit().remove(KEY_OPENROUTER).apply()
    }

    fun isGeminiConfigured(context: Context): Boolean = !getGeminiKey(context).isNullOrBlank()

    fun isOpenRouterConfigured(context: Context): Boolean = !getOpenRouterKey(context).isNullOrBlank()

    fun getGeminiStatus(context: Context): String = if (isGeminiConfigured(context)) "Configured" else "Not configured"

    fun getOpenRouterStatus(context: Context): String = if (isOpenRouterConfigured(context)) "Configured" else "Not configured"
}
