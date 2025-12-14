package com.example.ensenando.data.local.dao

import androidx.room.*
import com.example.ensenando.data.local.entity.ConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    
    @Query("SELECT valor FROM config WHERE clave = :clave")
    suspend fun getValor(clave: String): String?
    
    @Query("SELECT valor FROM config WHERE clave = :clave")
    fun getValorFlow(clave: String): Flow<String?>
    
    @Query("SELECT * FROM config WHERE clave = :clave")
    suspend fun getConfig(clave: String): ConfigEntity?
    
    @Query("SELECT * FROM config")
    suspend fun getAllConfig(): List<ConfigEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: ConfigEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(configs: List<ConfigEntity>)
    
    @Update
    suspend fun updateConfig(config: ConfigEntity)
    
    @Delete
    suspend fun deleteConfig(config: ConfigEntity)
    
    @Query("DELETE FROM config WHERE clave = :clave")
    suspend fun deleteByClave(clave: String)
    
    // Métodos específicos para tema
    @Query("SELECT valor FROM config WHERE clave = 'tema'")
    suspend fun getTema(): String?
    
    @Query("SELECT valor FROM config WHERE clave = 'tema'")
    fun getTemaFlow(): Flow<String?>
    
    @Query("INSERT OR REPLACE INTO config (clave, valor) VALUES ('tema', :tema)")
    suspend fun guardarTema(tema: String)
    
    // Métodos para notificaciones
    @Query("SELECT valor FROM config WHERE clave = 'notificaciones_logros' LIMIT 1")
    suspend fun getNotificacionesLogros(): String?
    
    @Query("INSERT OR REPLACE INTO config (clave, valor) VALUES ('notificaciones_logros', :habilitado)")
    suspend fun guardarNotificacionesLogros(habilitado: String)
    
    @Query("SELECT valor FROM config WHERE clave = 'notificaciones_solicitudes' LIMIT 1")
    suspend fun getNotificacionesSolicitudes(): String?
    
    @Query("INSERT OR REPLACE INTO config (clave, valor) VALUES ('notificaciones_solicitudes', :habilitado)")
    suspend fun guardarNotificacionesSolicitudes(habilitado: String)
    
    @Query("SELECT valor FROM config WHERE clave = 'recordatorios' LIMIT 1")
    suspend fun getRecordatorios(): String?
    
    @Query("INSERT OR REPLACE INTO config (clave, valor) VALUES ('recordatorios', :habilitado)")
    suspend fun guardarRecordatorios(habilitado: String)
}
