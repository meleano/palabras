package com.example.palabras.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.Normalizer

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
                val raw = jsonArray.getString(i)
                val norm = normalize(raw)
                words.add(norm)
            }
            dictionary = words
            dictionary
        } catch (e: Exception) {
            e.printStackTrace()
            emptySet()
        }
    }

    fun isWordValid(word: String): Boolean {
        return dictionary.contains(normalize(word))
    }

    private fun normalize(s: String): String {
        val lower = s.lowercase()
        val n = Normalizer.normalize(lower, Normalizer.Form.NFD)
        return n.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }
}
