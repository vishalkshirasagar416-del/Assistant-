package com.deva.voice.v2.llm

import android.content.Context
import android.net.Uri
import android.util.Log
import com.deva.voice.utilities.ApiKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiProvider(
    private val modelName: String,
    private val context: Context,
    private val apiKeyManager: ApiKeyManager
) : AIProvider {

    override val name: String = "Gemini"
    override val isConfigured: Boolean
        get() = apiKeyManager.hasGeminiKey(context)

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun generateJson(messages: List<GeminiMessage>): String? = withContext(Dispatchers.IO) {
        val apiKey = apiKeyManager.getGeminiKey(context)
            ?: throw IllegalStateException("Gemini API key not configured.")

        val payload = JSONObject().apply {
            put("contents", buildContents(messages))
            put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
        }
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/${Uri.encode(modelName)}:generateContent")
            .addHeader("x-goog-api-key", apiKey)
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        Log.i("GeminiProvider", "Gemini request started.")
        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            if (!response.isSuccessful || body.isNullOrBlank()) {
                val safeBody = body
                    ?.replace(apiKey, "[REDACTED]")
                    ?.take(1000)
                    ?: "<empty body>"

                Log.e(
                    "GeminiProvider",
                    "Gemini request failed: HTTP ${response.code} body=$safeBody"
                )

                throw IllegalStateException(
                    "Gemini HTTP ${response.code}: ${safeBody.take(300)}"
                )
            }
            val responseJson = JSONObject(body)
            val candidate = responseJson.optJSONArray("candidates")?.optJSONObject(0)
            val text = candidate
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
                ?.takeIf { it.isNotBlank() }
                ?: run {
                    val reason = responseJson.optJSONObject("promptFeedback")
                        ?.optString("blockReason")
                        ?.takeIf { it.isNotBlank() }
                    throw IllegalStateException(
                        "Gemini returned no text${reason?.let { " (blocked: $it)" } ?: ""}."
                    )
                }
            Log.i("GeminiProvider", "Gemini request succeeded.")
            text
        }
    }

    private fun buildContents(messages: List<GeminiMessage>): JSONArray {
        val contents = JSONArray()
        messages.forEach { message ->
            val parts = JSONArray()
            message.parts.forEach { part ->
                when (part) {
                    is TextPart -> parts.put(JSONObject().put("text", part.text))
                    is InlineImagePart -> parts.put(JSONObject().apply {
                        put("inline_data", JSONObject().put("mime_type", part.mimeType).put("data", part.base64Data))
                    })
                }
            }
            if (parts.length() == 0) return@forEach
            contents.put(JSONObject().apply {
                put("role", if (message.role == MessageRole.MODEL) "model" else "user")
                put("parts", parts)
            })
        }
        return contents
    }
}
