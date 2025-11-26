package com.example.hangeulstudy.data

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)
