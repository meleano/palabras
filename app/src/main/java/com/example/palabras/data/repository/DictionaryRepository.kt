package com.example.palabras.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class DictionaryRepository(private val context: Context) {
    private var dictionary: Set<String> = emptySet()

    suspend fun loadDictionary(): Set<String> = withContext(Dispatchers.IO) {
        if (dictionary.isNotEmpty()) return@withContext dictionary

        try {
            val jsonString = context.assets.open("dictionary.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val jsonArray = jsonObject.getJSONArray("palabras")
            val words = mutableSetOf<String>()
            for (i in 0 until jsonArray.length()) {
                words.add(jsonArray.getString(i))
            }
            dictionary = words
            dictionary
        } catch (e: Exception) {
            e.printStackTrace()
            emptySet()
        }
    }

    fun isWordValid(word: String): Boolean {
        return dictionary.contains(word.lowercase())
    }
}
