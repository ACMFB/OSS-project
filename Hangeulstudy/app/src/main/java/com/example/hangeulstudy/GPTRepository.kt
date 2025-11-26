package com.example.hangeulstudy

import com.example.hangeulstudy.BuildConfig
import com.example.hangeulstudy.data.ChatRequest
import com.example.hangeulstudy.data.Message
import com.example.hangeulstudy.network.OpenAIClient

class GPTRepository {

    suspend fun askGPT(prompt: String): String {

        val request = ChatRequest(
            messages = listOf(Message("user", prompt))
        )

        val response = OpenAIClient.api.createChatCompletion(
            apiKey = "Bearer ${BuildConfig.OPENAI_API_KEY}",
            request = request
        )

        return response.choices.first().message.content
    }
}
