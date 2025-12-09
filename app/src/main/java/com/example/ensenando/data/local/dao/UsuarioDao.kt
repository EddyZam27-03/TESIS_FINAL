package com.example.ensenando.data.local.dao

import androidx.room.*
import com.example.ensenando.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios WHERE id_usuario = :id")
    suspend fun getUsuarioById(id: Int): UsuarioEntity?
    
    @Query("SELECT * FROM usuarios WHERE correo = :correo")
    suspend fun getUsuarioByCorreo(correo: String): UsuarioEntity?
    
    @Query("SELECT * FROM usuarios")
    fun getAllUsuarios(): Flow<List<UsuarioEntity>>
    
    @Query("SELECT * FROM usuarios WHERE sync_status = 'pending'")
    suspend fun getPendingUsuarios(): List<UsuarioEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsuario(usuario: UsuarioEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsuarios(usuarios: List<UsuarioEntity>)
    
    @Update
    suspend fun updateUsuario(usuario: UsuarioEntity)
    
    @Delete
    suspend fun deleteUsuario(usuario: UsuarioEntity)
    
    @Query("UPDATE usuarios SET sync_status = :status WHERE id_usuario = :id")
    suspend fun updateSyncStatus(id: Int, status: String)
}


