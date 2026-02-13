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

        val dictLower = dictionary.map { it.lowercase() }.toSet()
        val alphabet = buildAlphabetFromDictionary(dictLower)

        // Calcular rango de palabras según nivel
        // Nivel N: palabras de N+2 a N+3 letras
        val wordLenMin = levelNumber + 2
        val wordLenMax = levelNumber + 3

        // Letras ofrecidas: N+2 o N+3
        val lettersCount = Random.nextInt(levelNumber + 2, levelNumber + 4)

        var bestLetters: List<Char> = emptyList()
        var bestValidWords: List<String> = emptyList()

        var attempts = 0
        val maxAttempts = 200

        while (attempts < maxAttempts) {
            attempts++
            val letters = generateRandomLetters(alphabet, lettersCount)
            val validWordsAll = wordsFromLetters(dictLower, letters)

            // Filtrar por longitud específica del nivel
            val validWords = validWordsAll.filter { it.length in wordLenMin..wordLenMax }

            if (validWords.size >= 4) {
                bestLetters = letters
                bestValidWords = validWords
                break
            }

            if (validWords.size > bestValidWords.size) {
                bestValidWords = validWords
                bestLetters = letters
            }
        }

        val finalLetters = if (bestLetters.isEmpty()) {
            generateRandomLetters(alphabet, lettersCount)
        } else bestLetters

        val validWordsFinalAll = if (bestValidWords.isEmpty()) {
            wordsFromLetters(dictLower, finalLetters)
        } else bestValidWords

        val validWordsFinal = validWordsFinalAll.filter { it.length in wordLenMin..wordLenMax }

        // Determinar dificultad
        val difficulty = when {
            validWordsFinal.size >= 15 -> "fácil"
            validWordsFinal.size >= 8 -> "medio"
            else -> "difícil"
        }

        // Elegir palabras objetivo: 4 palabras
        // Priorizar: 1-2 largas (N+3), 2-3 cortas (N+2 o incluso N+1)
        val longWords = validWordsFinal.filter { it.length == wordLenMax }.sortedByDescending { it }
        val shortWords = validWordsFinal.filter { it.length < wordLenMax }.sortedBy { it }

        val targetWords = mutableListOf<String>()
        // Añadir 1-2 palabras largas
        targetWords.addAll(longWords.take(2))
        // Rellenar con palabras cortas hasta 4
        targetWords.addAll(shortWords.take(4 - targetWords.size))

        // Si no hay suficientes, devolver lo que tenemos
        val finalTargetWords = targetWords.distinct().take(4)

        // Crear grid simple
        val grid = mutableListOf<GridWord>()
        finalTargetWords.forEachIndexed { idx, w ->
            val gw = when (idx % 2) {
                0 -> GridWord(w, 0, idx, false)
                else -> GridWord(w, idx, 0, true)
            }
            grid.add(gw)
        }

        return Level(levelNumber, finalLetters.shuffled(), finalTargetWords, grid, difficulty = difficulty)
    }

    private fun buildAlphabetFromDictionary(dictionary: Set<String>): List<Char> {
        val freq = mutableMapOf<Char, Int>()
        for (w in dictionary) {
            for (c in w) {
                if (c.isLetter()) freq[c] = freq.getOrDefault(c, 0) + 1
            }
        }
        // Si el diccionario está vacío de letras (improbable), devolver alfabeto básico
        if (freq.isEmpty()) return ('a'..'z').toList()
        return freq.entries.sortedByDescending { it.value }.map { it.key }
    }

    private fun generateRandomLetters(alphabet: List<Char>, count: Int): List<Char> {
        if (alphabet.isEmpty()) return List(count) { ('a'..'z').random() }
        val result = mutableListOf<Char>()
        for (i in 0 until count) {
            // elegir por frecuencia: earlier letters in alphabet are more frequent
            val index = Random.nextInt(alphabet.size)
            result.add(alphabet[index])
        }
        return result
    }

    private fun wordsFromLetters(dictionary: Set<String>, letters: List<Char>): List<String> {
        if (letters.isEmpty()) return emptyList()
        val available = letters.groupingBy { it }.eachCount().mapValues { it.value }
        val results = mutableListOf<String>()
        for (w in dictionary) {
            if (canFormWord(w, available)) results.add(w)
        }
        return results
    }

    private fun canFormWord(word: String, availableCounts: Map<Char, Int>): Boolean {
        val need = mutableMapOf<Char, Int>()
        for (c in word) {
            if (!c.isLetter()) return false
            val lc = c.lowercaseChar()
            need[lc] = need.getOrDefault(lc, 0) + 1
            if (need[lc]!! > (availableCounts[lc] ?: 0)) return false
        }
        return true
    }
}
