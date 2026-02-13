package com.example.palabras.data.repository

import com.example.palabras.data.database.UserStats
import com.example.palabras.data.database.UserStatsDao
import kotlinx.coroutines.flow.Flow

class UserStatsRepository(private val userStatsDao: UserStatsDao) {
    fun getUserStats(): Flow<UserStats?> = userStatsDao.getUserStats()

    suspend fun updateUserStats(stats: UserStats) {
        userStatsDao.upsertStats(stats)
    }

    suspend fun insertInitialStats() {
        userStatsDao.insertStats(UserStats())
    }
}
