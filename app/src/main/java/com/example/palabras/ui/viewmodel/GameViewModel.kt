package com.example.palabras.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.palabras.data.database.UserStats
import com.example.palabras.data.repository.DictionaryRepository
import com.example.palabras.data.repository.UserStatsRepository
import com.example.palabras.domain.models.GridWord
import com.example.palabras.domain.models.Level
import com.example.palabras.domain.WordGenerator
import com.example.palabras.domain.WordGenerator.Config
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameViewModel(
    private val dictionaryRepository: DictionaryRepository,
    private val userStatsRepository: UserStatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var currentLevel: Level? = null
    private val foundWords = MutableStateFlow<Set<String>>(emptySet())
    val foundWordsState: StateFlow<Set<String>> = foundWords.asStateFlow()
    
    // Almacena qué palabras han recibido una ayuda (índice de letra revelada)
    private val _revealedHints = MutableStateFlow<Map<String, Int>>(emptyMap())
    val revealedHints: StateFlow<Map<String, Int>> = _revealedHints.asStateFlow()

    // Exponer estadísticas en tiempo real
    val userStats: StateFlow<UserStats> = userStatsRepository.getUserStats()
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserStats()
        )

    init {
        loadGame()
    }

    private fun loadGame() {
        viewModelScope.launch {
            val dict = dictionaryRepository.loadDictionary()
            val stats = userStatsRepository.getUserStats().first() ?: UserStats()
            // Generar nivel utilizando WordGenerator en IO
            withContext(Dispatchers.Default) {
                val generated = WordGenerator.generateLevel(stats.levelsCompleted + 1, dict)
                withContext(Dispatchers.Main) {
                    applyNewLevel(generated)
                }
            }
        }
    }

    private fun applyNewLevel(level: Level) {
        currentLevel = level
        foundWords.value = emptySet()
        _revealedHints.value = emptyMap()
        _uiState.value = GameUiState.Success(level)
        Log.d("GameViewModel", "Nivel generado: ${level.id} con letras: ${level.letters}")
    }

    private fun generateLevel(levelNumber: Int) {
        // Mantener para compatibilidad; la generación real ahora se hace en loadGame
    }

    fun onWordSubmitted(word: String) {
        val level = currentLevel ?: return
        val normalizedWord = word.lowercase()
        
        if (level.targetWords.contains(normalizedWord)) {
            if (!foundWords.value.contains(normalizedWord)) {
                val newFoundWords = foundWords.value + normalizedWord
                foundWords.value = newFoundWords
                
                viewModelScope.launch {
                    val stats = userStats.value
                    userStatsRepository.updateUserStats(stats.copy(
                        wordsFound = stats.wordsFound + 1,
                        totalScore = stats.totalScore + 10
                    ))
                }
                
                if (newFoundWords.size == level.targetWords.size) {
                    onLevelComplete()
                }
            }
        } else if (dictionaryRepository.isWordValid(normalizedWord)) {
            viewModelScope.launch {
                val stats = userStats.value
                userStatsRepository.updateUserStats(stats.copy(
                    extraWordsFound = stats.extraWordsFound + 1,
                    totalScore = stats.totalScore + 5
                ))
            }
        }
    }

    fun getHint() {
        val level = currentLevel ?: return
        val stats = userStats.value
        if (stats.totalScore < 20) return // Coste de la ayuda

        // Buscar la primera palabra no encontrada
        val nextWord = level.targetWords.firstOrNull { !foundWords.value.contains(it) }
        
        if (nextWord != null) {
            viewModelScope.launch {
                userStatsRepository.updateUserStats(stats.copy(
                    totalScore = stats.totalScore - 20
                ))
                // Revelar la primera letra (puedes ampliar esto para revelar más)
                _revealedHints.value = _revealedHints.value + (nextWord to 1)
            }
        }
    }

    private fun onLevelComplete() {
        viewModelScope.launch {
            val stats = userStats.value
            val newStats = stats.copy(
                levelsCompleted = stats.levelsCompleted + 1,
                totalScore = stats.totalScore + 50
            )
            userStatsRepository.updateUserStats(newStats)
            // Generar siguiente nivel usando WordGenerator
            val dict = dictionaryRepository.loadDictionary()
            withContext(Dispatchers.Default) {
                val generated = WordGenerator.generateLevel(newStats.levelsCompleted + 1, dict)
                withContext(Dispatchers.Main) {
                    applyNewLevel(generated)
                }
            }
        }
    }
}

sealed class GameUiState {
    object Loading : GameUiState()
    data class Success(val level: Level) : GameUiState()
    data class Error(val message: String) : GameUiState()
}

class GameViewModelFactory(
    private val dictionaryRepository: DictionaryRepository,
    private val userStatsRepository: UserStatsRepository
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(dictionaryRepository, userStatsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
