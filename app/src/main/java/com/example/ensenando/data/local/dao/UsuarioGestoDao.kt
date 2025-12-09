package com.example.ensenando.data.local.dao

import androidx.room.*
import com.example.ensenando.data.local.entity.UsuarioGestoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioGestoDao {
    @Query("SELECT * FROM usuario_gestos WHERE id_usuario = :idUsuario")
    fun getProgresoByUsuario(idUsuario: Int): Flow<List<UsuarioGestoEntity>>
    
    @Query("SELECT * FROM usuario_gestos WHERE id_usuario = :idUsuario AND id_gesto = :idGesto")
    suspend fun getProgreso(idUsuario: Int, idGesto: Int): UsuarioGestoEntity?
    
    @Query("SELECT * FROM usuario_gestos WHERE id_usuario = :idUsuario AND estado = 'aprendido'")
    fun getGestosAprendidos(idUsuario: Int): Flow<List<UsuarioGestoEntity>>
    
    @Query("SELECT * FROM usuario_gestos WHERE sync_status = 'pending'")
    suspend fun getPendingProgreso(): List<UsuarioGestoEntity>
    
    @Query("SELECT COUNT(*) FROM usuario_gestos WHERE id_usuario = :idUsuario AND estado = 'aprendido'")
    suspend fun getCountGestosAprendidos(idUsuario: Int): Int
    
    @Query("SELECT AVG(porcentaje) FROM usuario_gestos WHERE id_usuario = :idUsuario")
    suspend fun getPromedioProgreso(idUsuario: Int): Float?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgreso(progreso: UsuarioGestoEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgresos(progresos: List<UsuarioGestoEntity>)
    
    @Update
    suspend fun updateProgreso(progreso: UsuarioGestoEntity)
    
    @Query("UPDATE usuario_gestos SET porcentaje = :porcentaje, estado = :estado, sync_status = 'pending', last_updated = :timestamp WHERE id_usuario = :idUsuario AND id_gesto = :idGesto")
    suspend fun updateProgreso(idUsuario: Int, idGesto: Int, porcentaje: Int, estado: String, timestamp: Long)
    
    @Query("UPDATE usuario_gestos SET sync_status = :status WHERE id_usuario = :idUsuario AND id_gesto = :idGesto")
    suspend fun updateSyncStatus(idUsuario: Int, idGesto: Int, status: String)
    
    @Query("DELETE FROM usuario_gestos WHERE id_usuario = :idUsuario AND id_gesto = :idGesto")
    suspend fun deleteProgreso(idUsuario: Int, idGesto: Int)
}


