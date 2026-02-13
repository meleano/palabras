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
import java.text.Normalizer

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

    fun startNewGame() {
        viewModelScope.launch {
            val dict = dictionaryRepository.loadDictionary()
            val stats = userStats.value
            withContext(Dispatchers.Default) {
                val generated = WordGenerator.generateLevel(stats.levelsCompleted + 1, dict)
                withContext(Dispatchers.Main) {
                    applyNewLevel(generated)
                }
            }
        }
    }

    fun continueGame() {
        // Solo reaplicar el nivel actual si existe, o cargar uno nuevo si no
        currentLevel?.let {
            _uiState.value = GameUiState.Success(it)
        } ?: run { loadGame() }
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

    private fun normalize(s: String): String {
        val lower = s.lowercase()
        val n = Normalizer.normalize(lower, Normalizer.Form.NFD)
        return n.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    fun onWordSubmitted(word: String) {
        val level = currentLevel ?: return
        val normalizedWord = normalize(word)

        // targetWords in Level are normalized by generator
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
         // Coste por letra revelada
         val costPerLetter = 20
         // Buscar la primera palabra no encontrada
         val nextWord = level.targetWords.firstOrNull { !foundWords.value.contains(it) }
         if (nextWord == null) return

         val currentRevealed = _revealedHints.value[nextWord] ?: 0
         if (currentRevealed >= nextWord.length) return // ya revelada completamente

         // Verificar puntos
         if (stats.totalScore < costPerLetter) return

         viewModelScope.launch {
             // descontar puntos
             userStatsRepository.updateUserStats(stats.copy(
                 totalScore = stats.totalScore - costPerLetter
             ))

             // aumentar contador de letras reveladas
             _revealedHints.value = _revealedHints.value + (nextWord to (currentRevealed + 1))
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
