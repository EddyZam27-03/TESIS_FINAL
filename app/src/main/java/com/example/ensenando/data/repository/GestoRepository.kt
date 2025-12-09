package com.example.ensenando.data.repository

import android.content.Context
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.local.dao.GestoDao
import com.example.ensenando.data.local.entity.GestoEntity
import com.example.ensenando.data.remote.ApiService
import com.example.ensenando.data.remote.model.GestoResponse
import com.example.ensenando.util.NetworkUtils
import com.example.ensenando.util.SecurityUtils
import kotlinx.coroutines.flow.Flow

class GestoRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val apiService: ApiService
) {
    private val gestoDao: GestoDao = database.gestoDao()
    
    suspend fun syncGestos(): Result<Unit> {
        return try {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                return Result.failure(Exception("Sin conexión"))
            }
            
            val response = apiService.getGestos()
            val body = response.body()
            if (response.isSuccessful && body?.success == true) {
                val gestosResponse = body.gestos ?: emptyList()
                val gestos = gestosResponse.map { gestoResponse ->
                    GestoEntity(
                        idGesto = gestoResponse.id_gesto,
                        nombre = gestoResponse.nombre,
                        dificultad = gestoResponse.dificultad,
                        categoria = gestoResponse.categoria,
                        syncStatus = "synced",
                        lastUpdated = System.currentTimeMillis()
                    )
                }
                gestoDao.insertGestos(gestos)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al sincronizar gestos"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getAllGestos(): Flow<List<GestoEntity>> {
        return gestoDao.getAllGestos()
    }
    
    suspend fun getGestoById(id: Int): GestoEntity? {
        return gestoDao.getGestoById(id)
    }
    
    fun getGestosByCategoria(categoria: String): Flow<List<GestoEntity>> {
        return gestoDao.getGestosByCategoria("%$categoria%")
    }
    
    fun getGestosByModulo(modulo: String): Flow<List<GestoEntity>> {
        return when (modulo.uppercase()) {
            "BASICO" -> gestoDao.getGestosByCategoria("%BASICO%")
            "SOCIAL" -> gestoDao.getGestosByCategoria("%SOCIAL%")
            "ACADEMICO" -> gestoDao.getGestosByCategoria("%ACADEMICO%")
            else -> gestoDao.getAllGestos()
        }
    }
}

