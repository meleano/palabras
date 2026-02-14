package com.example.palabras.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.palabras.data.database.UserStats
import com.example.palabras.data.repository.DictionaryRepository
import com.example.palabras.data.repository.LevelRepository
import com.example.palabras.data.repository.UserStatsRepository
import com.example.palabras.domain.models.GridWord
import com.example.palabras.domain.models.Level
import com.example.palabras.domain.WordGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.text.Normalizer

class GameViewModel(
    private val dictionaryRepository: DictionaryRepository,
    private val userStatsRepository: UserStatsRepository,
    private val levelRepository: LevelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading(0f))
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
            try {
                val dict = dictionaryRepository.loadDictionary()
                val stats = userStatsRepository.getUserStats().first() ?: UserStats()
                
                // Intentar cargar de BD primero
                var level = levelRepository.getLevel(stats.currentLevel)
                
                if (level == null) {
                    // Si no existe (ej. nivel > 14 o primera vez sin cache), generar
                    level = withContext(Dispatchers.Default) {
                        WordGenerator.generateLevel(stats.currentLevel, dict)
                    }
                    levelRepository.saveLevels(listOf(level))
                }
                withContext(Dispatchers.Main) {
                    applyNewLevel(level!!)
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error loading game", e)
                _uiState.value = GameUiState.Error("Error al cargar el juego: ${e.message}")
            }
        }
    }

    fun startNewGame() {
        viewModelScope.launch {
            try {
                val dict = dictionaryRepository.loadDictionary()
                val currentStats = userStats.value
                // Guardar datos históricos antes de resetear
                val newStats = currentStats.copy(
                    currentScore = 0,
                    currentLevel = 1,
                    currentWordsFound = 0,
                    currentExtraWordsFound = 0
                    // totalScore, totalLevelsCompleted se mantienen del histórico
                )
                userStatsRepository.updateUserStats(newStats)

                // Limpiar niveles anteriores y generar lote inicial (1-14)
                levelRepository.clearLevels()
                
                val levelsToGenerate = 14
                val generatedLevels = mutableListOf<Level>()
                
                withContext(Dispatchers.Default) {
                    for (i in 1..levelsToGenerate) {
                        _uiState.value = GameUiState.Loading(i.toFloat() / levelsToGenerate)
                        delay(50) // Pequeña pausa para permitir que la UI se actualice y se vea el progreso
                        generatedLevels.add(WordGenerator.generateLevel(i, dict))
                    }
                    levelRepository.saveLevels(generatedLevels)
                }

                withContext(Dispatchers.Main) {
                    applyNewLevel(generatedLevels.first())
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error starting new game", e)
                _uiState.value = GameUiState.Error("Error al iniciar nueva partida: ${e.message}")
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
                     val pointsPerWord = 10
                     userStatsRepository.updateUserStats(stats.copy(
                         currentScore = stats.currentScore + pointsPerWord,
                         currentWordsFound = stats.currentWordsFound + 1,
                         totalScore = stats.totalScore + pointsPerWord,
                         totalWordsFound = stats.totalWordsFound + 1
                     ))
                 }

                 if (newFoundWords.size == level.targetWords.size) {
                     onLevelComplete()
                 }
             }
         } else if (dictionaryRepository.isWordValid(normalizedWord)) {
             viewModelScope.launch {
                 val stats = userStats.value
                 val pointsPerBonus = 5
                 userStatsRepository.updateUserStats(stats.copy(
                     currentScore = stats.currentScore + pointsPerBonus,
                     currentExtraWordsFound = stats.currentExtraWordsFound + 1,
                     totalScore = stats.totalScore + pointsPerBonus,
                     totalExtraWordsFound = stats.totalExtraWordsFound + 1
                 ))
             }
         }
     }

     fun getHint() {
         val level = currentLevel ?: return
         val stats = userStats.value
         val costPerLetter = 20
         val nextWord = level.targetWords.firstOrNull { !foundWords.value.contains(it) }
         if (nextWord == null) return

         val currentRevealed = _revealedHints.value[nextWord] ?: 0
         if (currentRevealed >= nextWord.length) return

         if (stats.currentScore < costPerLetter) return

         viewModelScope.launch {
             userStatsRepository.updateUserStats(stats.copy(
                 currentScore = stats.currentScore - costPerLetter,
                 totalScore = stats.totalScore - costPerLetter
             ))

             _revealedHints.value = _revealedHints.value + (nextWord to (currentRevealed + 1))
         }
     }

     private fun onLevelComplete() {
         viewModelScope.launch {
             try {
                 val stats = userStats.value
                 val bonusPerLevel = 50
                 val newStats = stats.copy(
                     currentLevel = stats.currentLevel + 1,
                     currentScore = stats.currentScore + bonusPerLevel,
                     totalLevelsCompleted = stats.totalLevelsCompleted + 1,
                     totalScore = stats.totalScore + bonusPerLevel
                 )
                 userStatsRepository.updateUserStats(newStats)
                 Log.d("GameViewModel", "Nivel completado. Nuevo nivel: ${newStats.currentLevel}")

                 // Cargar siguiente nivel (de BD o generar)
                 var nextLevel = levelRepository.getLevel(newStats.currentLevel)
                 if (nextLevel == null) {
                     val dict = dictionaryRepository.loadDictionary()
                     nextLevel = withContext(Dispatchers.Default) {
                         WordGenerator.generateLevel(newStats.currentLevel, dict)
                     }
                     // Guardar para persistencia futura
                     levelRepository.saveLevels(listOf(nextLevel))
                 }

                 withContext(Dispatchers.Main) {
                     applyNewLevel(nextLevel!!)
                 }
             } catch (e: Exception) {
                 Log.e("GameViewModel", "Error completing level", e)
                 _uiState.value = GameUiState.Error("Error al completar nivel: ${e.message}")
             }
         }
     }
 }

 sealed class GameUiState {
     data class Loading(val progress: Float) : GameUiState()
     data class Success(val level: Level) : GameUiState()
     data class Error(val message: String) : GameUiState()
 }

 class GameViewModelFactory(
     private val dictionaryRepository: DictionaryRepository,
     private val userStatsRepository: UserStatsRepository
 ) : ViewModelProvider.Factory {
     override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
         if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
             // Nota: Asumimos que AppDatabase se pasa o se obtiene aquí. 
             // En una app real usaríamos inyección de dependencias.
             // Aquí instanciamos el repositorio extra necesario.
             // Usamos el contexto del repositorio
             val db = com.example.palabras.data.database.AppDatabase.getDatabase(dictionaryRepository.context)
             val levelRepo = LevelRepository(db.levelDao())
             @Suppress("UNCHECKED_CAST")
             return GameViewModel(dictionaryRepository, userStatsRepository, levelRepo) as T
         }
         throw IllegalArgumentException("Unknown ViewModel class")
     }
 }
