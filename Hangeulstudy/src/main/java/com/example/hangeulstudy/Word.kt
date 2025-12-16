package com.example.hangeulstudy

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Word(
    val korean: String,
    val meaning: String,
    val example: String,
    val difficulty: Difficulty = Difficulty.MEDIUM
) : Parcelable
