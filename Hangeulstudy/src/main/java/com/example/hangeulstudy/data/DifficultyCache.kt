package com.example.hangeulstudy.data

import android.content.Context
import com.example.hangeulstudy.Difficulty
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object DifficultyCache {

    private const val PREF_NAME = "difficulty_cache_pref"
    private const val KEY_DIFFICULTY_MAP = "difficulty_map"
    private val gson = Gson()

    fun save(context: Context, cache: Map<String, Difficulty>) {
        val json = gson.toJson(cache)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DIFFICULTY_MAP, json)
            .apply()
    }

    fun load(context: Context): MutableMap<String, Difficulty> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_DIFFICULTY_MAP, null) ?: return mutableMapOf()

        val type = object : TypeToken<Map<String, Difficulty>>() {}.type
        return gson.fromJson(json, type)
    }
}
