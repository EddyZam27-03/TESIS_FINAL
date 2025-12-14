package com.example.ensenando.data.repository

import android.content.Context
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.local.entity.HistorialIntentoEntity
import com.example.ensenando.util.NetworkUtils
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class HistorialIntentoRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    
    private val historialDao = database.historialIntentoDao()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    /**
     * Obtener últimos intentos de un gesto para un usuario
     */
    suspend fun getUltimosIntentos(idUsuario: Int, idGesto: Int, limit: Int = 5): List<HistorialIntentoEntity> {
        return historialDao.getUltimosIntentos(idUsuario, idGesto, limit)
    }
    
    /**
     * Obtener últimos intentos como Flow
     */
    fun getUltimosIntentosFlow(idUsuario: Int, idGesto: Int, limit: Int = 5): Flow<List<HistorialIntentoEntity>> {
        return historialDao.getUltimosIntentosFlow(idUsuario, idGesto, limit)
    }
    
    /**
     * Guardar un nuevo intento
     */
    suspend fun insertIntento(
        idUsuario: Int,
        idGesto: Int,
        porcentajeObtenido: Int
    ): Result<HistorialIntentoEntity> {
        return try {
            val fechaIntento = dateFormat.format(Date())
            val intento = HistorialIntentoEntity(
                id_usuario = idUsuario,
                id_gesto = idGesto,
                porcentaje_obtenido = porcentajeObtenido,
                fecha_intento = fechaIntento,
                sync_status = if (NetworkUtils.isNetworkAvailable(context)) "synced" else "pending"
            )
            historialDao.insertIntento(intento)
            Result.success(intento)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtener intentos pendientes de sincronización
     */
    suspend fun getPendingIntentos(): List<HistorialIntentoEntity> {
        return historialDao.getPendingIntentos()
    }
    
    /**
     * Marcar intentos como sincronizados
     */
    suspend fun markAsSynced(ids: List<Int>) {
        historialDao.markAsSynced(ids)
    }
    
    /**
     * Obtener cantidad de intentos para un gesto
     */
    suspend fun getCantidadIntentos(idUsuario: Int, idGesto: Int): Int {
        return historialDao.getCantidadIntentos(idUsuario, idGesto)
    }
    
    /**
     * Eliminar intentos de un gesto (útil para reset)
     */
    suspend fun deleteIntentosPorGesto(idUsuario: Int, idGesto: Int) {
        historialDao.deleteIntentosPorGesto(idUsuario, idGesto)
    }
}
