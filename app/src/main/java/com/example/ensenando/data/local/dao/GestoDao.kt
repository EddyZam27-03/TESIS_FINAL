package com.example.ensenando.data.local.dao

import androidx.room.*
import com.example.ensenando.data.local.entity.GestoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GestoDao {
    @Query("SELECT * FROM gestos")
    fun getAllGestos(): Flow<List<GestoEntity>>
    
    @Query("SELECT * FROM gestos WHERE id_gesto = :id")
    suspend fun getGestoById(id: Int): GestoEntity?
    
    @Query("SELECT * FROM gestos WHERE categoria LIKE :categoria")
    fun getGestosByCategoria(categoria: String): Flow<List<GestoEntity>>
    
    @Query("SELECT * FROM gestos WHERE sync_status = 'pending'")
    suspend fun getPendingGestos(): List<GestoEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGesto(gesto: GestoEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGestos(gestos: List<GestoEntity>)
    
    @Update
    suspend fun updateGesto(gesto: GestoEntity)
    
    @Query("UPDATE gestos SET sync_status = :status WHERE id_gesto = :id")
    suspend fun updateSyncStatus(id: Int, status: String)
}


