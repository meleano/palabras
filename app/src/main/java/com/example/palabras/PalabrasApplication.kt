package com.example.palabras

import android.app.Application
import com.example.palabras.data.database.AppDatabase
import com.example.palabras.data.database.UserStats
import com.example.palabras.data.repository.DictionaryRepository
import com.example.palabras.data.repository.UserStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class PalabrasApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val userStatsRepository by lazy { UserStatsRepository(database.userStatsDao()) }
    val dictionaryRepository by lazy { DictionaryRepository(this) }

    override fun onCreate() {
        super.onCreate()
        // Inicializar estadísticas si no existen
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stats = database.userStatsDao().getUserStats().first()
                if (stats == null) {
                    database.userStatsDao().insertStats(UserStats())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
