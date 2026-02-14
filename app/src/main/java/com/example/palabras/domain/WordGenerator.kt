package com.example.palabras.domain

import com.example.palabras.domain.models.Level
import com.example.palabras.domain.models.GridWord
import kotlin.random.Random

/**
 * Generador de niveles: crea un conjunto de letras y selecciona palabras objetivo
 * basadas en el diccionario proporcionado. También aplica filtros de dificultad.
 *
 * Esta versión implementa la lógica portada desde el script de Python `generaNiveles.py`
 * para una generación de niveles más robusta y eficiente.
 */
object WordGenerator {

    // Cache para evitar recalcular el mapa length->(word,counts) cada vez
    private var cachedDictHash: Int? = null
    private var cachedMaxWordLength: Int = 0
    private var cachedDictByLen: Map<Int, List<String>>? = null
    private var cachedWords: List<String>? = null

    // Configuración portada de `generaNiveles.py`
    data class PyConfig(
        val minWords: Int = 3,
        val maxWords: Int = 6,
        val prefixLen: Int = 2,
        val maxLevelLetters: Int = 18,
        val maxWordLength: Int = 15,
        val baseWordMinLength: Int = 4,
        val maxAttempts: Int = 300,
        val levelIndex: Int = 1 // En el script original, el índice del nivel afectaba la dificultad
    )

    /**
     * Genera un lote de niveles secuenciales (ej. del 1 al 14).
     * Útil para pre-cargar la "base de datos" de la partida.
     */
    fun generateLevelsBatch(startLevel: Int, endLevel: Int, dictionary: Set<String>): List<Level> {
        prepareDictionaryCache(dictionary, endLevel + 2) // Pre-calentar caché para el nivel más alto
        val levels = mutableListOf<Level>()
        for (i in startLevel..endLevel) {
            levels.add(generateLevel(i, dictionary))
        }
        return levels
    }

    // Punto de entrada principal para la generación de niveles.
    fun generateLevel(levelNumber: Int, dictionary: Set<String>, config: PyConfig = PyConfig()): Level {
        // Ajustamos la configuración dinámicamente según las reglas del nivel
        val dynamicConfig = calculateConfigForLevel(levelNumber, config)
        return generateLevelFromPyLogic(levelNumber, dictionary, dynamicConfig)
    }

    private fun generateLevelFromPyLogic(levelNumber: Int, dictionary: Set<String>, config: PyConfig): Level {
        if (dictionary.isEmpty()) {
            return createFallbackLevel(levelNumber)
        }

        // Normalizar y cachear el diccionario
        prepareDictionaryCache(dictionary, maxOf(config.maxWordLength, 15)) // Asegurar cache amplio
        val words = cachedWords ?: return createFallbackLevel(levelNumber)
        val dictByLen = cachedDictByLen ?: return createFallbackLevel(levelNumber)

        for (attempt in 0 until config.maxAttempts) {
            // Regla: La palabra base debe tener exactamente el número de letras del nivel
            val baseWord = pickBaseWord(words, config.maxLevelLetters, config.maxLevelLetters)
            if (baseWord.isEmpty()) continue

            val (letters, letterCounts) = buildLetterSetStrict(baseWord, config.maxLevelLetters)

            val candidates = filterWordsForLevelEfficient(
                letters = letterCounts,
                dictByLen = dictByLen,
                n = letters.size,
                levelIndex = config.levelIndex,
                minLen = config.baseWordMinLength,
                maxLen = config.maxLevelLetters
            )
            if (candidates.size < config.minWords) continue

            val finalWords = removePrefixCollisions(candidates, config.prefixLen).toMutableList()
            if (finalWords.size !in config.minWords..config.maxWords) continue

            // Asegurarse de que la palabra base esté en la lista final
            if (!finalWords.contains(baseWord)) {
                 // Si no hay espacio, reemplazar la más corta con la palabra base si la base es más larga
                if (finalWords.size >= config.maxWords) {
                    finalWords.sortBy { it.length }
                    if (baseWord.length > finalWords[0].length) {
                        finalWords[0] = baseWord
                    }
                } else {
                    finalWords.add(baseWord)
                }
            }
             if (finalWords.size !in config.minWords..config.maxWords) continue


            val difficulty = "medio" // La dificultad se puede refinar más tarde

            // Construir el objeto Level final
            val grid = buildGrid(finalWords.sorted())
            return Level(
                id = levelNumber,
                letters = letters.shuffled(),
                targetWords = finalWords.sorted(),
                grid = grid,
                difficulty = difficulty
            )
        }

        // Si todos los intentos fallan, devolver un nivel de fallback
        return createFallbackLevel(levelNumber)
    }

    private fun calculateConfigForLevel(level: Int, baseConfig: PyConfig): PyConfig {
        // Tope máximo de 14 letras. A partir de ahí, solo aumenta dificultad (lógica futura), no longitud.
        val effectiveLevel = minOf(level, 14)
        val numLetters = if (effectiveLevel <= 4) 4 else effectiveLevel
        
        // Reglas de longitud de palabras objetivo:
        // Niveles 1-4: Mínimo 3 letras.
        // Niveles 5+: Idealmente longitud N, aceptamos hasta N-2.
        val minWordLen = if (effectiveLevel <= 4) 3 else maxOf(3, numLetters - 2)
        
        // Ajustamos cantidad de palabras según dificultad progresiva
        val targetWordsCount = minOf(10, 3 + (level / 2)) 

        return baseConfig.copy(
            minWords = 3,
            maxWords = targetWordsCount,
            maxLevelLetters = numLetters, // Esto fuerza que el set de letras sea de este tamaño
            maxWordLength = numLetters,
            baseWordMinLength = minWordLen,
            levelIndex = level,
            maxAttempts = 500 // Más intentos para niveles altos donde es difícil encontrar palabras exactas
        )
    }

    private fun prepareDictionaryCache(dictionary: Set<String>, maxWordLength: Int) {
        val dictHash = dictionary.hashCode()
        // Solo reutilizamos cache si es el mismo diccionario Y ya tenemos palabras suficientemente largas cacheadas
        if (cachedDictHash == dictHash && cachedMaxWordLength >= maxWordLength) {
            return // Cache ya está listo
        }

        // Asumimos que el diccionario ya está normalizado (lowercase) para evitar duplicar Strings
        val dictLower = dictionary.filter { it.length <= maxWordLength }
        cachedWords = dictLower

        // Agrupar solo por longitud, calculamos counts bajo demanda para ahorrar memoria
        cachedDictByLen = dictLower.groupBy { it.length }
        cachedDictHash = dictHash
        cachedMaxWordLength = maxWordLength
    }

    private fun pickBaseWord(words: List<String>, minLen: Int, maxLen: Int): String {
        val candidates = words.filter { it.length in minLen..maxLen }
        return candidates.randomOrNull() ?: ""
    }

    // Versión estricta: No añade letras aleatorias, usa exactamente las letras de la palabra base
    private fun buildLetterSetStrict(base: String, targetSize: Int): Pair<List<Char>, IntArray> {
        val letters = base.toList().shuffled()
        val letterCounts = wordToCounts(base)
        return letters to letterCounts
    }

    private fun buildLetterSet(base: String, maxLevelLetters: Int): Pair<List<Char>, IntArray> {
        val l = base.length
        val extra = Random.nextInt(0, 4)
        val n = minOf(l + extra, maxLevelLetters)

        val freq = "aaaaabbcccddddeeeeeeeeeefffggghhhhiiiiijkllllmmmnnnnnnñoooooooppqrrrrrrsssssstttttuuuvvwwxxyyyzz"
        val extraLetters = (1..(n - l)).map { freq.random() }

        val letters = (base.toList() + extraLetters).shuffled()
        val letterCounts = wordToCounts(letters.joinToString(""))
        return letters to letterCounts
    }

    private fun filterWordsForLevelEfficient(
        letters: IntArray,
        dictByLen: Map<Int, List<String>>,
        n: Int,
        levelIndex: Int,
        minLen: Int,
        maxLen: Int,
    ): List<String> {
        val valid = mutableListOf<String>()
        for (len in minLen..maxLen) {
            val bucket = dictByLen[len] ?: continue
            for (word in bucket) {
                val wordCounts = wordToCounts(word)
                if (isSubmultiset(wordCounts, letters)) {
                    valid.add(word)
                }
            }
        }
        return valid
    }

    private fun removePrefixCollisions(words: List<String>, prefixLen: Int): List<String> {
        val buckets = words.groupBy { it.take(prefixLen) }
        return buckets.map { (_, ws) ->
            if (ws.size == 1) {
                ws[0]
            } else {
                ws.maxByOrNull { it.length }!!
            }
        }
    }

    private fun isSubmultiset(a: IntArray, b: IntArray): Boolean {
        for (i in a.indices) {
            if (a[i] > b[i]) return false
        }
        return true
    }

    private fun wordToCounts(word: String): IntArray {
        val cnt = IntArray(27) // 26 para a-z, 1 para ñ
        for (c in word) {
            when (c) {
                in 'a'..'z' -> cnt[c - 'a']++
                'ñ' -> cnt[26]++
            }
        }
        return cnt
    }
     private fun buildGrid(words: List<String>): List<GridWord> {
        val grid = mutableListOf<GridWord>()
        words.forEachIndexed { idx, w ->
            val gw = when (idx % 2) {
                0 -> GridWord(w, 0, idx, false)
                else -> GridWord(w, idx, 0, true)
            }
            grid.add(gw)
        }
        return grid
    }


    private fun createFallbackLevel(levelNumber: Int): Level {
        val fallbackLetters = listOf('a', 'e', 'i', 'o', 'u', 'r', 's')
        val fallbackWords = listOf("uso", "ria", "sur", "sol")
        val grid = buildGrid(fallbackWords)
        return Level(id = levelNumber, fallbackLetters, fallbackWords, grid, difficulty = "fácil")
    }
}
