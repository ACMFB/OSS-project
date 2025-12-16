package com.example.hangeulstudy

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Word(
    val korean: String,
    val meaning: String,
    val example: String,
    val difficulty: Difficulty,
    var isBookmarked: Boolean = false
) : Parcelable {

    // Override equals and hashCode to base them only on the word's name (korean).
    // This ensures that the Set can find the object even if 'isBookmarked' changes.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Word
        return korean == other.korean
    }

    override fun hashCode(): Int {
        return korean.hashCode()
    }
}
