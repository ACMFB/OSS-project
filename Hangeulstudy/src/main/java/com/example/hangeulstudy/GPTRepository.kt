package com.example.hangeulstudy

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class GPTRepository {

    private val client = OkHttpClient()
    private val apiKey = BuildConfig.OPENAI_API_KEY

    init {
        Log.d("OPENAI_KEY_CHECK", "OPENAI_API_KEY = [$apiKey]")
    }

    suspend fun askGPT(prompt: String): String = withContext(Dispatchers.IO) {
        val json = JSONObject()
        json.put("model", "gpt-4o-mini")
        val messages = JSONObject()
        messages.put("role", "user")
        messages.put("content", prompt)
        json.put("messages", JSONArray().put(messages))

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val responseBody = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBody)
                val choices = responseJson.getJSONArray("choices")
                if (choices.length() > 0) {
                    val firstChoice = choices.getJSONObject(0)
                    val message = firstChoice.getJSONObject("message")
                    message.getString("content").trim()
                } else {
                    "Error: No response from API"
                }
            }
        } catch (e: Exception) {
            // Log the exception or handle it as needed
            e.printStackTrace()
            "Error: ${e.message}"
        }
    }
}