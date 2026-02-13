package com.example.palabras

import android.app.Application
import com.example.palabras.data.database.AppDatabase
import com.example.palabras.data.repository.DictionaryRepository
import com.example.palabras.data.repository.UserStatsRepository

class PalabrasApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val userStatsRepository by lazy { UserStatsRepository(database.userStatsDao()) }
    val dictionaryRepository by lazy { DictionaryRepository(this) }
}
