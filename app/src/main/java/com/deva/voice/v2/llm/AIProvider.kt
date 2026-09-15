package com.deva.voice.v2.llm

import android.util.Log

interface AIProvider {
    val name: String
    suspend fun generateJson(messages: List<GeminiMessage>): String?
}

class AIProviderManager(
    private val geminiProvider: AIProvider,
    private val openRouterProvider: AIProvider,
    private val geminiConfigured: Boolean = true,
    private val openRouterConfigured: Boolean = true
) {

    suspend fun generateJson(messages: List<GeminiMessage>): String? {
        if (geminiConfigured) {
            try {
                val result = geminiProvider.generateJson(messages)
                if (!result.isNullOrBlank()) {
                    return result
                }
            } catch (exception: Exception) {
                Log.w(
                    "AIProviderManager",
                    "Gemini failed; falling back to OpenRouter: ${exception.message ?: exception.javaClass.simpleName}"
                )
            }
        }

        if (openRouterConfigured) {
            try {
                val result = openRouterProvider.generateJson(messages)
                if (!result.isNullOrBlank()) {
                    Log.i("AIProviderManager", "OpenRouter fallback succeeded.")
                    return result
                }
            } catch (exception: Exception) {
                Log.e(
                    "AIProviderManager",
                    "OpenRouter fallback failed: ${exception.message ?: exception.javaClass.simpleName}"
                )
            }
        }

        return null
    }
}
