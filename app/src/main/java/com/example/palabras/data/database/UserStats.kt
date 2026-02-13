package com.example.palabras.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 0,
    // Datos de la partida actual
    val currentScore: Int = 0,
    val currentLevel: Int = 1,
    val currentWordsFound: Int = 0,
    val currentExtraWordsFound: Int = 0,
    // Datos históricos acumulados
    val totalScore: Int = 0,
    val totalLevelsCompleted: Int = 0,
    val totalWordsFound: Int = 0,
    val totalExtraWordsFound: Int = 0
)
