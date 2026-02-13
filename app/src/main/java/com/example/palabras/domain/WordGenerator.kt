package com.example.palabras.domain

import com.example.palabras.domain.models.Level
import com.example.palabras.domain.models.GridWord

import kotlin.random.Random

/**
 * Generador de niveles: crea un conjunto de letras y selecciona palabras objetivo
 * basadas en el diccionario proporcionado. También aplica filtros de dificultad.
 */
object WordGenerator {

    data class Config(
        val lettersMin: Int = 6,
        val lettersMax: Int = 10,
        val minValidWordsForAccept: Int = 10,
        val desiredTargetWords: Int = 4,
        val wordLenMin: Int = 3,
        val wordLenMax: Int = 10,
        val maxAttempts: Int = 300
    )

    fun generateLevel(levelNumber: Int, dictionary: Set<String>, config: Config = Config()): Level {
        if (dictionary.isEmpty()) {
            // Fallback sencillo si no hay diccionario: letras y palabras fijas
            val fallbackLetters = listOf('p', 'e', 'r', 'r', 'o', 's')
            val fallbackWords = listOf("perro", "pero", "repro", "erró")
            val grid = listOf(
                GridWord(fallbackWords[0], 0, 0, false),
                GridWord(fallbackWords[1], 0, 0, true)
            )
            return Level(levelNumber, fallbackLetters, fallbackWords, grid, difficulty = "medio")
        }

        // Normalize dictionary to lowercase and remove accents assumptions handled elsewhere
        val dictLower = dictionary.map { it.lowercase() }.toSet()

        // Build alphabet frequency (used to bias letter selection)
        val alphabet = buildAlphabetFromDictionary(dictLower)

        // Rango flexible según nivel
        val wordLenMin = if (levelNumber <= 3) levelNumber + 2 else levelNumber
        val wordLenMax = levelNumber + 3

        // Letras ofrecidas: N+2 o N+3
        val lettersCount = Random.nextInt(levelNumber + 2, levelNumber + 4)

        // --- Precompute: group dictionary by length and compute letter-count arrays for each word ---
        // Map length -> list of Pair(word, countsArray)
        val dictByLen = mutableMapOf<Int, MutableList<Pair<String, IntArray>>>()
        for (w in dictLower) {
            if (w.length < 2) continue
            val cnt = wordToCounts(w)
            dictByLen.getOrPut(w.length) { mutableListOf() }.add(w to cnt)
        }

        // Helper to quickly collect candidate words that can be formed from given letters
        fun wordsFromLettersFast(letters: List<Char>, minLen: Int, maxLen: Int): List<String> {
            if (letters.isEmpty()) return emptyList()
            val avail = IntArray(26)
            for (c in letters) {
                if (c in 'a'..'z') avail[c - 'a']++
            }
            val results = mutableListOf<String>()
            for (len in minLen..maxLen) {
                val bucket = dictByLen[len] ?: continue
                for ((word, cnt) in bucket) {
                    var ok = true
                    // check counts
                    for (i in 0 until 26) {
                        if (cnt[i] > avail[i]) { ok = false; break }
                    }
                    if (ok) results.add(word)
                }
            }
            return results
        }

        var bestLetters: List<Char> = emptyList()
        var bestValidWords: List<String> = emptyList()

        var attempts = 0
        val maxAttempts = 200

        // Try to find a good set of letters quickly
        while (attempts < maxAttempts) {
            attempts++
            val letters = generateRandomLetters(alphabet, lettersCount)
            val validWords = wordsFromLettersFast(letters, wordLenMin, wordLenMax)

            if (validWords.size >= 4) {
                bestLetters = letters
                bestValidWords = selectDiverseWords(validWords, config.desiredTargetWords)
                break
            }

            if (validWords.size > bestValidWords.size) {
                bestValidWords = validWords
                bestLetters = letters
            }
        }

        // If not enough words found, expand length window a modo de fallback
        if (bestValidWords.size < config.desiredTargetWords) {
            var fallbackAttempts = 0
            val expandedMin = maxOf(3, wordLenMin - 1)
            val expandedMax = wordLenMax + 1
            while (fallbackAttempts < maxAttempts && bestValidWords.size < config.desiredTargetWords) {
                fallbackAttempts++
                val letters = generateRandomLetters(alphabet, lettersCount)
                val validWords = wordsFromLettersFast(letters, expandedMin, expandedMax)
                if (validWords.size > bestValidWords.size) {
                    bestValidWords = validWords
                    bestLetters = letters
                }
                if (validWords.size >= config.desiredTargetWords) {
                    bestLetters = letters
                    bestValidWords = selectDiverseWords(validWords, config.desiredTargetWords)
                    break
                }
            }
        }

        val finalLetters = if (bestLetters.isEmpty()) generateRandomLetters(alphabet, lettersCount) else bestLetters

        val validWordsFinalAll = if (bestValidWords.isEmpty()) wordsFromLettersFast(finalLetters, maxOf(3, wordLenMin - 1), wordLenMax + 1) else bestValidWords

        // Filter final words to the wanted range if possible, but accept shorter ones if needed
        val validWordsFinal = validWordsFinalAll.filter { it.length >= 3 }.sortedWith(compareByDescending<String> { it.length }.thenBy { it })

        // Determine difficulty
        val difficulty = when {
            validWordsFinal.size >= 15 -> "fácil"
            validWordsFinal.size >= 8 -> "medio"
            else -> "difícil"
        }

        val targetWords = validWordsFinal.take(config.desiredTargetWords).distinct()

        // Build a simple grid
        val grid = mutableListOf<GridWord>()
        targetWords.forEachIndexed { idx, w ->
            val gw = when (idx % 2) {
                0 -> GridWord(w, 0, idx, false)
                else -> GridWord(w, idx, 0, true)
            }
            grid.add(gw)
        }

        return Level(levelNumber, finalLetters.shuffled(), targetWords, grid, difficulty = difficulty)
    }

    private fun wordToCounts(word: String): IntArray {
        val cnt = IntArray(26)
        for (c in word) {
            if (c in 'a'..'z') cnt[c - 'a']++
        }
        return cnt
    }

    private fun selectDiverseWords(words: List<String>, count: Int): List<String> {
        if (words.size <= count) return words.distinct()

        val selected = mutableListOf<String>()
        val remaining = words.toMutableList()
        remaining.sortByDescending { it.length }

        while (selected.size < count && remaining.isNotEmpty()) {
            val word = remaining.removeAt(0)
            val prefix = word.substring(0, minOf(3, word.length))
            val hasSimilarPrefix = selected.any { it.substring(0, minOf(3, it.length)) == prefix }
            if (!hasSimilarPrefix || selected.size < 2) selected.add(word)
        }

        return selected.distinct()
    }

    private fun buildAlphabetFromDictionary(dictionary: Set<String>): List<Char> {
        val freq = mutableMapOf<Char, Int>()
        for (w in dictionary) {
            for (c in w) {
                if (c.isLetter()) freq[c] = freq.getOrDefault(c, 0) + 1
            }
        }
        if (freq.isEmpty()) return ('a'..'z').toList()
        return freq.entries.sortedByDescending { it.value }.map { it.key }
    }

    private fun generateRandomLetters(alphabet: List<Char>, count: Int): List<Char> {
        if (alphabet.isEmpty()) return List(count) { ('a'..'z').random() }
        val result = mutableListOf<Char>()
        for (i in 0 until count) {
            val index = Random.nextInt(alphabet.size)
            result.add(alphabet[index])
        }
        return result
    }

}
