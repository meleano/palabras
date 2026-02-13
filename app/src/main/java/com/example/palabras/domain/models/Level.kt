package com.example.palabras.domain.models

data class Level(
    val id: Int,
    val letters: List<Char>,
    val targetWords: List<String>,
    val grid: List<GridWord>,
    val difficulty: String = "medio"
)

data class GridWord(
    val word: String,
    val startX: Int,
    val startY: Int,
    val isVertical: Boolean
)
