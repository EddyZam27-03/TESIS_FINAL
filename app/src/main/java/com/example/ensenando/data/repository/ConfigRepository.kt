package com.example.ensenando.data.repository

import android.content.Context
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.local.entity.ConfigEntity
import kotlinx.coroutines.flow.Flow

class ConfigRepository(
    private val database: AppDatabase
) {
    
    private val configDao = database.configDao()
    
    /**
     * Obtener valor de configuración
     */
    suspend fun getValor(clave: String): String? {
        return configDao.getValor(clave)
    }
    
    /**
     * Obtener valor de configuración como Flow
     */
    fun getValorFlow(clave: String): Flow<String?> {
        return configDao.getValorFlow(clave)
    }
    
    /**
     * Guardar valor de configuración
     */
    suspend fun guardarValor(clave: String, valor: String) {
        configDao.insertConfig(ConfigEntity(clave, valor))
    }
    
    /**
     * Obtener tema guardado
     */
    suspend fun getTema(): String {
        return configDao.getTema() ?: "auto"
    }
    
    /**
     * Obtener tema como Flow
     */
    fun getTemaFlow(): Flow<String?> {
        return configDao.getTemaFlow()
    }
    
    /**
     * Guardar tema
     */
    suspend fun guardarTema(tema: String) {
        configDao.guardarTema(tema)
    }
    
    /**
     * Obtener estado de notificaciones de logros
     */
    suspend fun getNotificacionesLogros(): Boolean {
        return try {
            val valor = configDao.getNotificacionesLogros()
            valor?.toBoolean() ?: true
        } catch (e: Exception) {
            // Si no existe, crear con valor por defecto
            configDao.guardarNotificacionesLogros("true")
            true
        }
    }
    
    /**
     * Guardar estado de notificaciones de logros
     */
    suspend fun guardarNotificacionesLogros(habilitado: Boolean) {
        configDao.guardarNotificacionesLogros(habilitado.toString())
    }
    
    /**
     * Obtener estado de notificaciones de solicitudes
     */
    suspend fun getNotificacionesSolicitudes(): Boolean {
        return try {
            val valor = configDao.getNotificacionesSolicitudes()
            valor?.toBoolean() ?: true
        } catch (e: Exception) {
            configDao.guardarNotificacionesSolicitudes("true")
            true
        }
    }
    
    /**
     * Guardar estado de notificaciones de solicitudes
     */
    suspend fun guardarNotificacionesSolicitudes(habilitado: Boolean) {
        configDao.guardarNotificacionesSolicitudes(habilitado.toString())
    }
    
    /**
     * Obtener estado de recordatorios
     */
    suspend fun getRecordatorios(): Boolean {
        return try {
            val valor = configDao.getRecordatorios()
            valor?.toBoolean() ?: false
        } catch (e: Exception) {
            configDao.guardarRecordatorios("false")
            false
        }
    }
    
    /**
     * Guardar estado de recordatorios
     */
    suspend fun guardarRecordatorios(habilitado: Boolean) {
        configDao.guardarRecordatorios(habilitado.toString())
    }
    
    /**
     * Obtener todas las configuraciones
     */
    suspend fun getAllConfig(): List<ConfigEntity> {
        return configDao.getAllConfig()
    }
}
