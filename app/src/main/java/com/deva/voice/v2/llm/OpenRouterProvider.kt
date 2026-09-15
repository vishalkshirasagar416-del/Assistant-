package com.deva.voice.v2.llm

import android.content.Context
import android.util.Log
import com.deva.voice.utilities.ApiKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenRouterProvider(
    private val context: Context,
    private val modelName: String = "openai/gpt-4o-mini"
) : AIProvider {

    override val name: String = "OpenRouter"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun generateJson(messages: List<GeminiMessage>): String? = withContext(Dispatchers.IO) {
        val apiKey = ApiKeyManager.getOpenRouterKey(context)
            ?: run {
                Log.w("OpenRouterProvider", "OpenRouter key missing.")
                return@withContext null
            }

        val payloadJson = JSONObject().apply {
            put("model", modelName)
            put("messages", buildMessages(messages))
            put("temperature", 0.5)
        }

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .post(payloadJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://priya.ai")
            .addHeader("X-Title", "Priya")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body.isNullOrBlank()) {
                    val safeBody = body
                        ?.replace(apiKey, "[REDACTED]")
                        ?.take(1000)
                        ?: "<empty body>"

                    Log.e(
                        "OpenRouterProvider",
                        "OpenRouter request failed: HTTP ${response.code} body=$safeBody"
                    )
                    return@use null
                }

                val json = JSONObject(body)
                val firstChoice = json.getJSONArray("choices").optJSONObject(0) ?: return@use null
                val message = firstChoice.optJSONObject("message") ?: return@use null
                val content = message.optString("content", "")
                if (content.isBlank()) return@use null
                Log.i("OpenRouterProvider", "OpenRouter request succeeded.")
                content
            }
        } catch (e: Exception) {
            Log.e("OpenRouterProvider", "OpenRouter call error: ${e.message}", e)
            null
        }
    }

    private fun buildMessages(messages: List<GeminiMessage>): org.json.JSONArray {
        val array = org.json.JSONArray()
        messages.forEach { message ->
            val text = message.parts.filterIsInstance<TextPart>().joinToString(separator = "\n") { it.text }
            if (text.isBlank()) return@forEach
            val objectJson = JSONObject().apply {
                put("role", when (message.role) {
                    MessageRole.USER -> "user"
                    MessageRole.MODEL -> "assistant"
                    MessageRole.TOOL -> "tool"
                })
                put("content", text)
            }
            array.put(objectJson)
        }
        return array
    }
}
