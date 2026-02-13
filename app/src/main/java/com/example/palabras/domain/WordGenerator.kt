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
            return Level(levelNumber, fallbackLetters, fallbackWords, grid)
        }

        val dictLower = dictionary.map { it.lowercase() }.toSet()
        val alphabet = buildAlphabetFromDictionary(dictLower)

        var bestLetters: List<Char> = emptyList()
        var bestValidWords: List<String> = emptyList()

        var attempts = 0
        var requiredValid = config.minValidWordsForAccept

        while (attempts < config.maxAttempts) {
            attempts++
            val lettersCount = Random.nextInt(config.lettersMin, config.lettersMax + 1)
            val letters = generateRandomLetters(alphabet, lettersCount)

            val validWords = wordsFromLetters(dictLower, letters)
            if (validWords.size >= requiredValid) {
                bestLetters = letters
                bestValidWords = validWords
                break
            }

            // Keep the best found by size
            if (validWords.size > bestValidWords.size) {
                bestValidWords = validWords
                bestLetters = letters
            }

            // If reached many attempts and found nothing, reduce requirement
            if (attempts % 50 == 0 && requiredValid > 1) {
                requiredValid = maxOf(1, requiredValid / 2)
            }
        }

        // Si no encontramos suficientes palabras, usamos lo mejor que haya
        val finalLetters = if (bestLetters.isEmpty()) {
            // fallback: take some frequent letters
            listOf('a', 'e', 'o', 's', 'r', 'l')
        } else bestLetters

        val validWordsFinal = if (bestValidWords.isEmpty()) wordsFromLetters(dictLower, finalLetters) else bestValidWords

        // Determinar dificultad y elegir targetWords
        val difficulty = when {
            validWordsFinal.size >= 20 -> "fácil"
            validWordsFinal.size >= 10 -> "medio"
            else -> "difícil"
        }

        // Elegir palabras objetivo: preferir palabras más largas (más desafiantes)
        val sortedByLength = validWordsFinal.sortedWith(compareByDescending<String> { it.length }.thenBy { it })
        val targetWords = sortedByLength.take(config.desiredTargetWords).distinct().map { it }

        // Crear un ``grid`` simple (puede mejorarse más adelante)
        val grid = mutableListOf<GridWord>()
        targetWords.forEachIndexed { idx, w ->
            val gw = when (idx % 2) {
                0 -> GridWord(w, 0, idx, false)
                else -> GridWord(w, idx, 0, true)
            }
            grid.add(gw)
        }

        return Level(levelNumber, finalLetters.shuffled(), targetWords, grid)
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
