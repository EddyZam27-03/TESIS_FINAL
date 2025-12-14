package com.example.ensenando.data.local.dao

import androidx.room.*
import com.example.ensenando.data.local.entity.HistorialIntentoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistorialIntentoDao {
    
    @Query("SELECT * FROM historial_intentos WHERE id_usuario = :idUsuario AND id_gesto = :idGesto ORDER BY fecha_intento DESC LIMIT :limit")
    suspend fun getUltimosIntentos(idUsuario: Int, idGesto: Int, limit: Int = 5): List<HistorialIntentoEntity>
    
    @Query("SELECT * FROM historial_intentos WHERE id_usuario = :idUsuario AND id_gesto = :idGesto ORDER BY fecha_intento DESC LIMIT :limit")
    fun getUltimosIntentosFlow(idUsuario: Int, idGesto: Int, limit: Int = 5): Flow<List<HistorialIntentoEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntento(intento: HistorialIntentoEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(intentos: List<HistorialIntentoEntity>)
    
    @Query("SELECT * FROM historial_intentos WHERE sync_status = 'pending'")
    suspend fun getPendingIntentos(): List<HistorialIntentoEntity>
    
    @Query("UPDATE historial_intentos SET sync_status = 'synced' WHERE id_historial IN (:ids)")
    suspend fun markAsSynced(ids: List<Int>)
    
    @Query("DELETE FROM historial_intentos WHERE id_usuario = :idUsuario AND id_gesto = :idGesto")
    suspend fun deleteIntentosPorGesto(idUsuario: Int, idGesto: Int)
    
    @Query("SELECT COUNT(*) FROM historial_intentos WHERE id_usuario = :idUsuario AND id_gesto = :idGesto")
    suspend fun getCantidadIntentos(idUsuario: Int, idGesto: Int): Int
}
