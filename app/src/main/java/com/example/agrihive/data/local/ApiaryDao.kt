package com.example.agrihive.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiaryDao {
    @Query("SELECT * FROM apiaries WHERE ownerId = :ownerId")
    fun getApiariesByOwner(ownerId: String): Flow<List<ApiaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiaries(apiaries: List<ApiaryEntity>)

    @Query("DELETE FROM apiaries WHERE ownerId = :ownerId")
    suspend fun deleteApiariesByOwner(ownerId: String)
}
