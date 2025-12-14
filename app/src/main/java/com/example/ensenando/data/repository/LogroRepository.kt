package com.example.ensenando.data.repository

import android.content.Context
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.local.entity.UsuarioLogroEntity
import com.example.ensenando.data.remote.ApiService
import com.example.ensenando.data.remote.model.*
import com.example.ensenando.util.NetworkUtils
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class LogroRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val apiService: ApiService
) {
    private val logroDao = database.logroDao()
    private val usuarioLogroDao = database.usuarioLogroDao()
    private val usuarioGestoDao = database.usuarioGestoDao()
    private val docenteEstudianteDao = database.docenteEstudianteDao()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    suspend fun getLogrosUsuario(idUsuario: Int): Result<List<LogrosResponse>> {
        return try {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                // Cargar desde local
                val logrosObtenidos = usuarioLogroDao.getLogrosByUsuario(idUsuario).first()
                val logros = logrosObtenidos.map { usuarioLogro ->
                    val logro = logroDao.getLogroById(usuarioLogro.idLogro)
                    LogrosResponse(
                        id_logro = usuarioLogro.idLogro,
                        titulo = logro?.titulo,
                        descripcion = logro?.descripcion,
                        fecha_obtenido = usuarioLogro.fechaObtenido,
                        desbloqueado = true
                    )
                }
                return Result.success(logros)
            }

            val response = apiService.getLogrosUsuarios(idUsuario = idUsuario)
            if (response.isSuccessful) {
                val logros = response.body() ?: emptyList()
                // Guardar en local
                logros.forEach { logroResponse ->
                    logroResponse.id_logro?.let { idLogro ->
                        val logroEntity = com.example.ensenando.data.local.entity.LogroEntity(
                            idLogro = idLogro,
                            titulo = logroResponse.titulo,
                            descripcion = logroResponse.descripcion
                        )
                        logroDao.insertLogro(logroEntity)
                        
                        if (logroResponse.desbloqueado == true && logroResponse.fecha_obtenido != null) {
                            val usuarioLogro = UsuarioLogroEntity(
                                idUsuario = idUsuario,
                                idLogro = idLogro,
                                fechaObtenido = logroResponse.fecha_obtenido,
                                syncStatus = "synced"
                            )
                            usuarioLogroDao.insertUsuarioLogro(usuarioLogro)
                        }
                    }
                }
                Result.success(logros)
            } else {
                Result.failure(Exception("Error al obtener logros"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifica y desbloquea logros automáticamente según el progreso del usuario
     */
    suspend fun verificarYDesbloquearLogros(idUsuario: Int): Result<List<LogrosResponse>> {
        return try {
            val logrosDesbloqueados = mutableListOf<LogrosResponse>()
            
            // Obtener todos los logros disponibles
            val todosLosLogros = logroDao.getAllLogros().first()
            
            // Obtener progreso del usuario
            val progresoUsuario = usuarioGestoDao.getProgresoByUsuario(idUsuario).first()
            val gestosAprendidos = progresoUsuario.filter { it.estado == "aprendido" }
            val totalGestos = progresoUsuario.size
            val promedioProgreso = progresoUsuario.map { it.porcentaje }.average().toInt()
            
            // Obtener logros ya desbloqueados
            val logrosYaDesbloqueados = usuarioLogroDao.getLogrosByUsuario(idUsuario).first()
            val idsLogrosDesbloqueados = logrosYaDesbloqueados.map { it.idLogro }.toSet()
            
            // Verificar cada logro
            for (logro in todosLosLogros) {
                // Si ya está desbloqueado, saltar
                if (idsLogrosDesbloqueados.contains(logro.idLogro)) {
                    continue
                }
                
                // Verificar condiciones según el título/descripción del logro
                val cumpleCondicion = when {
                    // Progreso Básico
                    logro.titulo?.contains("Primer gesto", ignoreCase = true) == true -> {
                        gestosAprendidos.isNotEmpty()
                    }
                    logro.titulo?.contains("10 gestos", ignoreCase = true) == true -> {
                        gestosAprendidos.size >= 10
                    }
                    logro.titulo?.contains("25 gestos", ignoreCase = true) == true -> {
                        gestosAprendidos.size >= 25
                    }
                    logro.titulo?.contains("50 gestos", ignoreCase = true) == true -> {
                        gestosAprendidos.size >= 50
                    }
                    logro.titulo?.contains("100 gestos", ignoreCase = true) == true -> {
                        gestosAprendidos.size >= 100
                    }
                    
                    // Rendimiento
                    logro.titulo?.contains("Perfeccionista", ignoreCase = true) == true -> {
                        promedioProgreso >= 90
                    }
                    logro.titulo?.contains("Estudiante dedicado", ignoreCase = true) == true -> {
                        promedioProgreso >= 70
                    }
                    logro.titulo?.contains("100% en un gesto", ignoreCase = true) == true -> {
                        progresoUsuario.any { it.porcentaje == 100 }
                    }
                    logro.titulo?.contains("10 gestos al 80", ignoreCase = true) == true -> {
                        progresoUsuario.count { it.porcentaje >= 80 } >= 10
                    }
                    logro.titulo?.contains("20 gestos al 80", ignoreCase = true) == true -> {
                        progresoUsuario.count { it.porcentaje >= 80 } >= 20
                    }
                    
                    // Participación y Comunidad
                    logro.titulo?.contains("Enviar primera solicitud", ignoreCase = true) == true -> {
                        val solicitudes = docenteEstudianteDao.getSolicitudesByEstudiante(idUsuario).first()
                        solicitudes.isNotEmpty()
                    }
                    logro.titulo?.contains("Vincularse con un docente", ignoreCase = true) == true -> {
                        val relacionesAceptadas = docenteEstudianteDao.getDocentesAceptados(idUsuario).first()
                        relacionesAceptadas.isNotEmpty()
                    }
                    
                    else -> false
                }
                
                if (cumpleCondicion) {
                    // Desbloquear logro
                    val fechaObtenido = dateFormat.format(Date())
                    val usuarioLogro = UsuarioLogroEntity(
                        idUsuario = idUsuario,
                        idLogro = logro.idLogro,
                        fechaObtenido = fechaObtenido,
                        syncStatus = if (NetworkUtils.isNetworkAvailable(context)) "synced" else "pending"
                    )
                    
                    usuarioLogroDao.insertUsuarioLogro(usuarioLogro)
                    
                    // Intentar sincronizar con servidor si hay conexión
                    if (NetworkUtils.isNetworkAvailable(context)) {
                        try {
                            val request = DesbloquearLogroRequest(
                                id_usuario = idUsuario,
                                id_logro = logro.idLogro,
                                fecha_obtenido = fechaObtenido
                            )
                            apiService.desbloquearLogro(request)
                        } catch (e: Exception) {
                            // Si falla, mantener como pending
                            usuarioLogroDao.updateSyncStatus(idUsuario, logro.idLogro, "pending")
                        }
                    }
                    
                    // Agregar a la lista de logros desbloqueados
                    logrosDesbloqueados.add(
                        LogrosResponse(
                            id_logro = logro.idLogro,
                            titulo = logro.titulo,
                            descripcion = logro.descripcion,
                            fecha_obtenido = fechaObtenido,
                            desbloqueado = true
                        )
                    )
                }
            }
            
            Result.success(logrosDesbloqueados)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtener logros recientes (últimos N)
     */
    suspend fun getLogrosRecientes(idUsuario: Int, limit: Int = 3): List<LogrosResponse> {
        return try {
            val logrosObtenidos = usuarioLogroDao.getLogrosByUsuario(idUsuario).first()
                .sortedByDescending { it.fechaObtenido }
                .take(limit)
            
            logrosObtenidos.mapNotNull { usuarioLogro ->
                val logro = logroDao.getLogroById(usuarioLogro.idLogro)
                logro?.let {
                    LogrosResponse(
                        id_logro = it.idLogro,
                        titulo = it.titulo,
                        descripcion = it.descripcion,
                        fecha_obtenido = usuarioLogro.fechaObtenido,
                        desbloqueado = true
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}