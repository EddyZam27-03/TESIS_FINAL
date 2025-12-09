package com.example.ensenando.data.local.dao

import androidx.room.*
import com.example.ensenando.data.local.entity.UsuarioLogroEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioLogroDao {
    @Query("SELECT * FROM usuario_logros WHERE id_usuario = :idUsuario")
    fun getLogrosByUsuario(idUsuario: Int): Flow<List<UsuarioLogroEntity>>
    
    @Query("SELECT * FROM usuario_logros WHERE id_usuario = :idUsuario AND id_logro = :idLogro")
    suspend fun getUsuarioLogro(idUsuario: Int, idLogro: Int): UsuarioLogroEntity?
    
    @Query("SELECT * FROM usuario_logros WHERE sync_status = 'pending'")
    suspend fun getPendingUsuarioLogros(): List<UsuarioLogroEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsuarioLogro(usuarioLogro: UsuarioLogroEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsuarioLogros(usuarioLogros: List<UsuarioLogroEntity>)
    
    @Update
    suspend fun updateUsuarioLogro(usuarioLogro: UsuarioLogroEntity)
    
    @Query("UPDATE usuario_logros SET sync_status = :status WHERE id_usuario = :idUsuario AND id_logro = :idLogro")
    suspend fun updateSyncStatus(idUsuario: Int, idLogro: Int, status: String)
}


