package com.deva.voice.v2.llm

import android.util.Log

interface AIProvider {
    val name: String
    val isConfigured: Boolean
        get() = true
    suspend fun generateJson(messages: List<GeminiMessage>): String?
}

class AIProviderManager(
    private val geminiProvider: AIProvider,
    private val openRouterProvider: AIProvider,
    private val geminiConfigured: Boolean = true,
    private val openRouterConfigured: Boolean = true
) {

    suspend fun generateJson(messages: List<GeminiMessage>): String? {
        val failures = mutableListOf<String>()
        if (geminiConfigured && geminiProvider.isConfigured) {
            try {
                val result = geminiProvider.generateJson(messages)
                if (!result.isNullOrBlank()) {
                    return result
                }
                failures += "${geminiProvider.name} returned an empty response"
            } catch (exception: Exception) {
                failures += "${geminiProvider.name}: ${exception.message ?: exception.javaClass.simpleName}"
                Log.w(
                    "AIProviderManager",
                    "Gemini failed; falling back to OpenRouter: ${exception.message ?: exception.javaClass.simpleName}"
                )
            }
        }

        if (openRouterConfigured && openRouterProvider.isConfigured) {
            try {
                val result = openRouterProvider.generateJson(messages)
                if (!result.isNullOrBlank()) {
                    Log.i("AIProviderManager", "${openRouterProvider.name} fallback succeeded.")
                    return result
                }
                failures += "${openRouterProvider.name} returned an empty response"
            } catch (exception: Exception) {
                failures += "${openRouterProvider.name}: ${exception.message ?: exception.javaClass.simpleName}"
                Log.e(
                    "AIProviderManager",
                    "OpenRouter fallback failed: ${exception.message ?: exception.javaClass.simpleName}"
                )
            }
        }

        if (failures.isNotEmpty()) {
            throw IllegalStateException(failures.joinToString("; "))
        }
        return null
    }
}
