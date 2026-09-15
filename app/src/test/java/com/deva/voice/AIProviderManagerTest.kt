package com.deva.voice

import com.deva.voice.v2.llm.AIProvider
import com.deva.voice.v2.llm.AIProviderManager
import com.deva.voice.v2.llm.GeminiMessage
import com.deva.voice.v2.llm.MessageRole
import com.deva.voice.v2.llm.TextPart
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AIProviderManagerTest {

    @Test
    fun `gemini succeeds and openrouter is not called`() = runBlocking {
        var openRouterCalled = false
        val manager = AIProviderManager(
            geminiProvider = object : AIProvider {
                override val name: String = "Gemini"
                override suspend fun generateJson(messages: List<GeminiMessage>): String? = "{\"ok\":true}"
            },
            openRouterProvider = object : AIProvider {
                override val name: String = "OpenRouter"
                override suspend fun generateJson(messages: List<GeminiMessage>): String? {
                    openRouterCalled = true
                    return "{\"ok\":false}"
                }
            },
            geminiConfigured = true,
            openRouterConfigured = true
        )

        val output = manager.generateJson(listOf(GeminiMessage(MessageRole.USER, listOf(TextPart("hi")))))

        assertEquals("{\"ok\":true}", output)
        assertEquals(false, openRouterCalled)
    }

    @Test
    fun `falls back to openrouter when gemini fails with auth error`() = runBlocking {
        val manager = AIProviderManager(
            geminiProvider = object : AIProvider {
                override val name: String = "Gemini"
                override suspend fun generateJson(messages: List<GeminiMessage>): String? {
                    throw RuntimeException("HTTP 401 unauthorized")
                }
            },
            openRouterProvider = object : AIProvider {
                override val name: String = "OpenRouter"
                override suspend fun generateJson(messages: List<GeminiMessage>): String? = "{\"ok\":true,\"fallback\":true}"
            },
            geminiConfigured = true,
            openRouterConfigured = true
        )

        val output = manager.generateJson(listOf(GeminiMessage(MessageRole.USER, listOf(TextPart("hello")))))

        assertEquals("{\"ok\":true,\"fallback\":true}", output)
    }

    @Test
    fun `returns null without crashing when no providers are configured`() = runBlocking {
        val manager = AIProviderManager(
            geminiProvider = object : AIProvider {
                override val name: String = "Gemini"
                override suspend fun generateJson(messages: List<GeminiMessage>): String? = null
            },
            openRouterProvider = object : AIProvider {
                override val name: String = "OpenRouter"
                override suspend fun generateJson(messages: List<GeminiMessage>): String? = null
            },
            geminiConfigured = false,
            openRouterConfigured = false
        )

        val output = manager.generateJson(listOf(GeminiMessage(MessageRole.USER, listOf(TextPart("hello")))))

        assertNull(output)
    }
}
