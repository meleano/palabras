package com.example.palabras.data.repository

import android.content.Context
import android.util.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.text.Normalizer

class DictionaryRepository(val context: Context) {
    private var dictionary: Set<String> = emptySet()

    suspend fun loadDictionary(): Set<String> = withContext(Dispatchers.IO) {
        if (dictionary.isNotEmpty()) return@withContext dictionary

        try {
            val words = mutableSetOf<String>()
            context.assets.open("dictionary.json").use { inputStream ->
                JsonReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    reader.beginObject()
                    while (reader.hasNext()) {
                        if (reader.nextName() == "palabras") {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                val raw = reader.nextString()
                                words.add(normalize(raw))
                            }
                            reader.endArray()
                        } else {
                            reader.skipValue()
                        }
                    }
                    reader.endObject()
                }
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
