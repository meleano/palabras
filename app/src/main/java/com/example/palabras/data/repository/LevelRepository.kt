package com.example.palabras.data.repository

import com.example.palabras.data.database.LevelDao
import com.example.palabras.data.database.LevelEntity
import com.example.palabras.domain.models.GridWord
import com.example.palabras.domain.models.Level

class LevelRepository(private val levelDao: LevelDao) {

    suspend fun getLevel(id: Int): Level? {
        val entity = levelDao.getLevel(id) ?: return null
        val words = entity.targetWords.split(",").filter { it.isNotEmpty() }
        val letters = entity.letters.toList()
        
        // Reconstruimos el grid simple (alternando horizontal/vertical)
        val grid = words.mapIndexed { idx, w ->
            if (idx % 2 == 0) GridWord(w, 0, idx, false)
            else GridWord(w, idx, 0, true)
        }

        return Level(
            id = entity.id,
            letters = letters,
            targetWords = words,
            grid = grid,
            difficulty = entity.difficulty
        )
    }

    suspend fun saveLevels(levels: List<Level>) {
        val entities = levels.map { level ->
            LevelEntity(
                id = level.id,
                letters = level.letters.joinToString(""),
                targetWords = level.targetWords.joinToString(","),
                difficulty = level.difficulty
            )
        }
        levelDao.insertLevels(entities)
    }

    suspend fun clearLevels() = levelDao.clearLevels()
}