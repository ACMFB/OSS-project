package com.example.hangeulstudy
import android.graphics.Color

enum class Difficulty(
    val label: String,
    val displayName: String,
    val color: Int
) {
    EASY("very common", "쉬움", Color.parseColor("#4CAF50")),
    MEDIUM("common", "보통", Color.parseColor("#FF9800")),
    HARD("intermediate", "어려움", Color.parseColor("#F44336")),
    RANDOM("random", "무작위", Color.GRAY);
    companion object {
        fun fromPrompt(value: String): Difficulty {
            return entries.firstOrNull { it.label == value } ?: MEDIUM
        }
    }
}
