package com.example.palabras.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LevelDao {
    @Query("SELECT * FROM levels WHERE id = :id")
    suspend fun getLevel(id: Int): LevelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLevels(levels: List<LevelEntity>)

    @Query("DELETE FROM levels")
    suspend fun clearLevels()
}