package com.example.hangeulstudy.data

import android.content.Context
import com.example.hangeulstudy.Word
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object FavoritesStorage {

    private const val PREF_NAME = "favorites_pref"
    private const val KEY_FAVORITES = "favorite_words"
    private val gson = Gson()

    fun save(context: Context, words: Set<Word>) {
        val json = gson.toJson(words)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FAVORITES, json)
            .apply()
    }

    fun load(context: Context): MutableSet<Word> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_FAVORITES, null) ?: return mutableSetOf()

        val type = object : TypeToken<Set<Word>>() {}.type
        return gson.fromJson(json, type)
    }
}
