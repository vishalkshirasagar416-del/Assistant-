package com.deva.voice.utilities

import android.content.Context
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages Gemini/OpenRouter API keys while keeping runtime configuration in Android secure storage.
 * local.properties remains optional for build-time defaults only.
 */
object ApiKeyManager {

    private val currentIndex = AtomicInteger(0)

    fun saveGeminiKey(context: Context, key: String) {
        SecureApiKeyStore.saveGeminiKey(context, key)
    }

    fun saveOpenRouterKey(context: Context, key: String) {
        SecureApiKeyStore.saveOpenRouterKey(context, key)
    }

    fun clearGeminiKey(context: Context) {
        SecureApiKeyStore.clearGeminiKey(context)
    }

    fun clearOpenRouterKey(context: Context) {
        SecureApiKeyStore.clearOpenRouterKey(context)
    }

    fun getGeminiKey(context: Context): String? {
        return SecureApiKeyStore.getGeminiKey(context)
    }

    fun getOpenRouterKey(context: Context): String? = SecureApiKeyStore.getOpenRouterKey(context)

    fun hasGeminiKey(context: Context): Boolean = !getGeminiKey(context).isNullOrBlank()

    fun hasOpenRouterKey(context: Context): Boolean = !getOpenRouterKey(context).isNullOrBlank()

    fun hasAnyKey(context: Context): Boolean = hasGeminiKey(context) || hasOpenRouterKey(context)

    fun getGeminiStatus(context: Context): String = if (hasGeminiKey(context)) "Configured" else "Not configured"

    fun getOpenRouterStatus(context: Context): String = if (hasOpenRouterKey(context)) "Configured" else "Not configured"

    fun getNextKey(context: Context): String {
        val key = getGeminiKey(context)
            ?: throw IllegalStateException("Gemini API key not configured. Please add your AI API key in Settings.")
        return key
    }

    fun getNextKey(): String = getNextKey(MyApplication.appContext)
}

