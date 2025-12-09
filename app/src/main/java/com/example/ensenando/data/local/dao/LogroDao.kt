package com.example.ensenando.data.local.dao

import androidx.room.*
import com.example.ensenando.data.local.entity.LogroEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogroDao {
    @Query("SELECT * FROM logros")
    fun getAllLogros(): Flow<List<LogroEntity>>
    
    @Query("SELECT * FROM logros WHERE id_logro = :id")
    suspend fun getLogroById(id: Int): LogroEntity?
    
    @Query("SELECT * FROM logros WHERE sync_status = 'pending'")
    suspend fun getPendingLogros(): List<LogroEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogro(logro: LogroEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogros(logros: List<LogroEntity>)
    
    @Update
    suspend fun updateLogro(logro: LogroEntity)
    
    @Query("UPDATE logros SET sync_status = :status WHERE id_logro = :id")
    suspend fun updateSyncStatus(id: Int, status: String)
}


