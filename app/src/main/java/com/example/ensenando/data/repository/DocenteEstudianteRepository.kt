package com.example.ensenando.data.repository

import android.content.Context
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.local.dao.DocenteEstudianteDao
import com.example.ensenando.data.local.entity.DocenteEstudianteEntity
import com.example.ensenando.data.remote.ApiService
import com.example.ensenando.data.remote.model.DocenteEstudianteResponse
import com.example.ensenando.util.NetworkUtils
import com.example.ensenando.util.SecurityUtils
import kotlinx.coroutines.flow.Flow

class DocenteEstudianteRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val apiService: ApiService
) {
    private val docenteEstudianteDao: DocenteEstudianteDao = database.docenteEstudianteDao()
    private val usuarioDao = database.usuarioDao()
    
    /**
     * Crea una solicitud verificando que los usuarios existan antes de insertar
     * CORREGIDO: Verifica FOREIGN KEY antes de insertar
     */
    suspend fun crearSolicitud(idDocente: Int, idEstudiante: Int): Result<Unit> {
        return try {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                return Result.failure(Exception("Se requiere conexión para enviar solicitud"))
            }
            
            // ✅ CORRECCIÓN: Verificar que los usuarios existan antes de insertar
            val docente = usuarioDao.getUsuarioById(idDocente)
            val estudiante = usuarioDao.getUsuarioById(idEstudiante)
            
            if (docente == null) {
                return Result.failure(Exception("El docente con ID $idDocente no existe en la base de datos local. Por favor, sincroniza los datos primero."))
            }
            
            if (estudiante == null) {
                return Result.failure(Exception("El estudiante con ID $idEstudiante no existe en la base de datos local. Por favor, sincroniza los datos primero."))
            }
            
            val request = com.example.ensenando.data.remote.model.EnviarSolicitudRequest(
                id_docente = idDocente,
                id_estudiante = idEstudiante
            )
            
            val response = apiService.enviarSolicitudDocente(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val relacion = DocenteEstudianteEntity(
                    idDocente = idDocente,
                    idEstudiante = idEstudiante,
                    estado = "pendiente",
                    syncStatus = "synced",
                    lastUpdated = System.currentTimeMillis()
                )
                docenteEstudianteDao.insertRelacion(relacion)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al enviar solicitud"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun actualizarSolicitud(idDocente: Int, idEstudiante: Int, estado: String): Result<Unit> {
        return try {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                return Result.failure(Exception("Se requiere conexión para responder solicitud"))
            }
            
            val accion = when (estado) {
                "aceptado" -> "aceptar"
                "rechazado" -> "rechazar"
                else -> return Result.failure(Exception("Estado inválido"))
            }
            
            val request = com.example.ensenando.data.remote.model.ResponderSolicitudRequest(
                id_docente = idDocente,
                id_estudiante = idEstudiante,
                accion = accion
            )
            
            val response = apiService.responderSolicitud(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val timestamp = System.currentTimeMillis()
                docenteEstudianteDao.updateEstado(idDocente, idEstudiante, estado, timestamp)
                docenteEstudianteDao.updateSyncStatus(idDocente, idEstudiante, "synced")
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al responder solicitud"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun eliminarRelacion(idDocente: Int, idEstudiante: Int): Result<Unit> {
        return try {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                return Result.failure(Exception("Se requiere conexión para eliminar relación"))
            }
            
            val request = com.example.ensenando.data.remote.model.EliminarRelacionRequest(
                id_docente = idDocente,
                id_estudiante = idEstudiante
            )
            
            val response = apiService.eliminarRelacionDocente(request)
            if (response.isSuccessful && response.body()?.success == true) {
                docenteEstudianteDao.deleteRelacion(idDocente, idEstudiante)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al eliminar relación"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sincroniza relaciones verificando que los usuarios existan antes de insertar
     * CORREGIDO: Verifica FOREIGN KEY antes de insertar relaciones
     */
    suspend fun syncRelaciones(): Result<Unit> {
        return try {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                return Result.failure(Exception("Sin conexión"))
            }
            
            val idUsuario = SecurityUtils.getUserId(context)
            val rol = SecurityUtils.getUserRol(context)
            
            if (idUsuario == -1) {
                return Result.failure(Exception("Usuario no identificado"))
            }
            
            // Obtener relaciones según el rol
            when (rol) {
                "estudiante" -> {
                    val response = apiService.consultarSolicitudEstudiante(idEstudiante = idUsuario)
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true) {
                        val solicitudes = body.solicitudes ?: emptyList()
                        val relaciones = solicitudes.mapNotNull { solicitud ->
                            // ✅ CORRECCIÓN: Verificar que los usuarios existan antes de crear la relación
                            val docente = usuarioDao.getUsuarioById(solicitud.id_docente)
                            val estudiante = usuarioDao.getUsuarioById(solicitud.id_estudiante)
                            
                            if (docente == null || estudiante == null) {
                                android.util.Log.w("DocenteEstudianteRepository", 
                                    "Saltando relación: docente=${solicitud.id_docente} existe=${docente != null}, estudiante=${solicitud.id_estudiante} existe=${estudiante != null}")
                                null
                            } else {
                            DocenteEstudianteEntity(
                                idDocente = solicitud.id_docente,
                                idEstudiante = solicitud.id_estudiante,
                                estado = solicitud.estado,
                                syncStatus = "synced",
                                lastUpdated = System.currentTimeMillis()
                            )
                        }
                        }
                        if (relaciones.isNotEmpty()) {
                        docenteEstudianteDao.insertRelaciones(relaciones)
                        }
                    }
                }
                "docente" -> {
                    val response = apiService.listarSolicitudesDocente(idDocente = idUsuario)
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true) {
                        val solicitudes = body.solicitudes ?: emptyList()
                        val relaciones = solicitudes.mapNotNull { solicitud ->
                            // ✅ CORRECCIÓN: Verificar que los usuarios existan antes de crear la relación
                            val docente = usuarioDao.getUsuarioById(solicitud.id_docente)
                            val estudiante = usuarioDao.getUsuarioById(solicitud.id_estudiante)
                            
                            if (docente == null || estudiante == null) {
                                android.util.Log.w("DocenteEstudianteRepository", 
                                    "Saltando relación: docente=${solicitud.id_docente} existe=${docente != null}, estudiante=${solicitud.id_estudiante} existe=${estudiante != null}")
                                null
                            } else {
                            DocenteEstudianteEntity(
                                idDocente = solicitud.id_docente,
                                idEstudiante = solicitud.id_estudiante,
                                estado = solicitud.estado,
                                syncStatus = "synced",
                                lastUpdated = System.currentTimeMillis()
                            )
                        }
                        }
                        if (relaciones.isNotEmpty()) {
                        docenteEstudianteDao.insertRelaciones(relaciones)
                        }
                    }
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getSolicitudesByEstudiante(idEstudiante: Int): Flow<List<DocenteEstudianteEntity>> {
        return docenteEstudianteDao.getSolicitudesByEstudiante(idEstudiante)
    }
    
    fun getEstudiantesByDocente(idDocente: Int): Flow<List<DocenteEstudianteEntity>> {
        return docenteEstudianteDao.getEstudiantesByDocente(idDocente)
    }
    
    fun getSolicitudesPendientes(idEstudiante: Int): Flow<List<DocenteEstudianteEntity>> {
        return docenteEstudianteDao.getSolicitudesPendientes(idEstudiante)
    }
    
    fun getDocentesAceptados(idEstudiante: Int): Flow<List<DocenteEstudianteEntity>> {
        return docenteEstudianteDao.getDocentesAceptados(idEstudiante)
    }
}

