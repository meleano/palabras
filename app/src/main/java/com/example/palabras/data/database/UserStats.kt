package com.example.palabras.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 0,
    val totalScore: Int = 0,
    val levelsCompleted: Int = 0,
    val wordsFound: Int = 0,
    val extraWordsFound: Int = 0
)
