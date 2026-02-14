package com.example.palabras.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "levels")
data class LevelEntity(
    @PrimaryKey val id: Int,
    val letters: String,      // Guardamos como string "abcde"
    val targetWords: String,  // Guardamos como "palabra1,palabra2"
    val difficulty: String
)