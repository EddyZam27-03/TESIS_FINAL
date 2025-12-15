package com.example.ensenando.data.repository

import android.content.Context
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.local.dao.UsuarioGestoDao
import com.example.ensenando.data.local.entity.UsuarioGestoEntity
import com.example.ensenando.data.remote.ApiService
import com.example.ensenando.data.remote.model.*
import com.example.ensenando.util.NetworkUtils
import com.example.ensenando.util.SecurityUtils
import kotlinx.coroutines.flow.Flow

class ProgresoRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val apiService: ApiService
) {
    private val usuarioGestoDao: UsuarioGestoDao = database.usuarioGestoDao()
    
    suspend fun updateProgreso(idUsuario: Int, idGesto: Int, porcentaje: Int): Result<Unit> {
        return try {
            val progresoActual = usuarioGestoDao.getProgreso(idUsuario, idGesto)
            val nuevoPorcentaje = porcentaje.coerceIn(0, 100)
            
            // Solo actualizar si el nuevo porcentaje es mayor
            if (progresoActual != null && nuevoPorcentaje <= progresoActual.porcentaje) {
                return Result.success(Unit) // No hay incremento, no actualizar
            }
            
            val estado = if (nuevoPorcentaje >= 80) "aprendido" else "pendiente"
            val timestamp = System.currentTimeMillis()
            
            val nuevoProgreso = UsuarioGestoEntity(
                idUsuario = idUsuario,
                idGesto = idGesto,
                porcentaje = nuevoPorcentaje,
                estado = estado,
                syncStatus = "pending",
                lastUpdated = timestamp
            )
            
            usuarioGestoDao.insertProgreso(nuevoProgreso)
            
            // ✅ NUEVO: Verificar y desbloquear logros automáticamente
            try {
                val logroRepository = com.example.ensenando.data.repository.LogroRepository(
                    context, database, apiService
                )
                val logrosDesbloqueados = logroRepository.verificarYDesbloquearLogros(idUsuario)
                logrosDesbloqueados.onSuccess { logros ->
                    if (logros.isNotEmpty()) {
                        android.util.Log.d("ProgresoRepository", "Logros desbloqueados: ${logros.size}")
                        
                        // ✅ NUEVO: Mostrar notificación para cada logro desbloqueado
                        logros.forEach { logro ->
                            logro.titulo?.let { titulo ->
                                logro.descripcion?.let { descripcion ->
                                    com.example.ensenando.util.NotificationManager.mostrarToastLogro(
                                        context,
                                        titulo
                                    )
                                    // Opcional: Mostrar notificación push si está habilitado
                                    val configRepository = com.example.ensenando.data.repository.ConfigRepository(database)
                                    kotlinx.coroutines.runBlocking {
                                        if (configRepository.getNotificacionesLogros()) {
                                            com.example.ensenando.util.NotificationManager.mostrarNotificacionLogro(
                                                context,
                                                titulo,
                                                descripcion
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("ProgresoRepository", "Error al verificar logros", e)
            }
            
            // Intentar sincronizar inmediatamente si hay conexión
            if (NetworkUtils.isNetworkAvailable(context)) {
                try {
                    val request = com.example.ensenando.data.remote.model.ActualizarProgresoRequest(
                        id_usuario = idUsuario,
                        id_gesto = idGesto,
                        porcentaje = nuevoPorcentaje,
                        estado = estado
                    )
                    val response = apiService.actualizarProgresoGesto(request)
                    if (response.isSuccessful && response.body()?.success == true) {
                        usuarioGestoDao.updateSyncStatus(idUsuario, idGesto, "synced")
                    }
                } catch (e: Exception) {
                    // Si falla, se sincronizará después
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun syncProgreso(): Result<Unit> {
        return try {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                return Result.failure(Exception("Sin conexión"))
            }
            
            val idUsuario = SecurityUtils.getUserId(context)
            if (idUsuario == -1) {
                return Result.failure(Exception("No autenticado"))
            }
            
            // ✅ PASO 1: Enviar cambios pendientes al servidor (App → Servidor)
            val pendientes = usuarioGestoDao.getPendingProgreso()
            
            if (pendientes.isNotEmpty()) {
                val syncRequest = com.example.ensenando.data.remote.model.SyncRequest(
                    usuario_gestos = pendientes.map { progreso ->
                        com.example.ensenando.data.remote.model.UsuarioGestoSyncItem(
                            id_usuario = progreso.idUsuario,
                            id_gesto = progreso.idGesto,
                            porcentaje = progreso.porcentaje,
                            estado = progreso.estado
                        )
                    },
                    docente_estudiante = null
                )
                
                val response = apiService.sync(syncRequest)
                if (response.isSuccessful) {
                    val syncResponse = response.body()
                    // Resolver conflictos con datos del servidor
                    syncResponse?.usuario_gestos?.forEach { remoteProgreso ->
                        val localProgreso = pendientes.find { 
                            it.idUsuario == remoteProgreso.id_usuario && 
                            it.idGesto == remoteProgreso.id_gesto 
                        }
                        
                        if (localProgreso != null) {
                            // Resolver conflicto: mantener porcentaje más alto
                            val porcentajeFinal = maxOf(localProgreso.porcentaje, remoteProgreso.porcentaje)
                            val estadoFinal = if (porcentajeFinal >= 80) "aprendido" else localProgreso.estado
                            
                            usuarioGestoDao.updateProgreso(
                                localProgreso.idUsuario,
                                localProgreso.idGesto,
                                porcentajeFinal,
                                estadoFinal,
                                System.currentTimeMillis()
                            )
                            usuarioGestoDao.updateSyncStatus(localProgreso.idUsuario, localProgreso.idGesto, "synced")
                        }
                    }
                }
            }
            
            // ✅ PASO 2: Descargar TODOS los datos del servidor (Servidor → App)
            descargarDatosDelServidor(idUsuario)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * ✅ NUEVO: Descarga todos los datos del servidor y actualiza la base de datos local
     * Esto asegura que los datos insertados directamente en el servidor se reflejen en la app
     */
    private suspend fun descargarDatosDelServidor(idUsuario: Int) {
        try {
            // Obtener todos los progresos del servidor usando obtener_progreso_usuarios.php
            val response = apiService.getProgresoUsuarios(idUsuario = idUsuario)
            if (response.isSuccessful) {
                val progresoResponse = response.body()
                val progresosDetalle = progresoResponse?.progreso ?: emptyList()
                
                // Convertir y actualizar en la base de datos local
                progresosDetalle.forEach { progresoDetalle ->
                    // ✅ FIX: Validar que los campos nullable no sean null antes de usarlos
                    val idUsuarioDetalle = progresoDetalle.id_usuario ?: return@forEach
                    val idGestoDetalle = progresoDetalle.id_gesto ?: return@forEach
                    
                    val local = usuarioGestoDao.getProgreso(idUsuarioDetalle, idGestoDetalle)
                    
                    when {
                        local == null -> {
                            // No existe local, crear desde servidor
                            val nuevoProgreso = UsuarioGestoEntity(
                                idUsuario = idUsuarioDetalle,
                                idGesto = idGestoDetalle,
                                porcentaje = progresoDetalle.porcentaje,
                                estado = progresoDetalle.estado,
                                syncStatus = "synced",
                                lastUpdated = System.currentTimeMillis()
                            )
                            usuarioGestoDao.insertProgreso(nuevoProgreso)
                        }
                        local.syncStatus == "pending" -> {
                            // Local tiene cambios pendientes, comparar y resolver conflicto
                            val porcentajeFinal = maxOf(local.porcentaje, progresoDetalle.porcentaje)
                            val estadoFinal = if (porcentajeFinal >= 80) "aprendido" else local.estado
                            
                            // Solo actualizar si el servidor tiene un valor mayor
                            if (progresoDetalle.porcentaje > local.porcentaje) {
                                usuarioGestoDao.updateProgreso(
                                    idUsuarioDetalle,
                                    idGestoDetalle,
                                    porcentajeFinal,
                                    estadoFinal,
                                    System.currentTimeMillis()
                                )
                            }
                        }
                        else -> {
                            // Local está sincronizado, actualizar con datos del servidor
                            if (progresoDetalle.porcentaje != local.porcentaje || progresoDetalle.estado != local.estado) {
                                val porcentajeFinal = maxOf(local.porcentaje, progresoDetalle.porcentaje)
                                val estadoFinal = if (porcentajeFinal >= 80) "aprendido" else progresoDetalle.estado
                                
                                usuarioGestoDao.updateProgreso(
                                    idUsuarioDetalle,
                                    idGestoDetalle,
                                    porcentajeFinal,
                                    estadoFinal,
                                    System.currentTimeMillis()
                                )
                                usuarioGestoDao.updateSyncStatus(idUsuarioDetalle, idGestoDetalle, "synced")
                            }
                        }
                    }
                }
            }
            
            // También obtener usando getGestosUsuario como respaldo
            val responseGestos = apiService.getGestosUsuario(idUsuario = idUsuario)
            if (responseGestos.isSuccessful) {
                val progresosResponse = responseGestos.body() ?: emptyList()
                progresosResponse.forEach { progresoResponse ->
                    val local = usuarioGestoDao.getProgreso(progresoResponse.id_usuario, progresoResponse.id_gesto)
                    
                    if (local == null) {
                        // Crear nuevo progreso desde servidor
                        val nuevoProgreso = UsuarioGestoEntity(
                            idUsuario = progresoResponse.id_usuario,
                            idGesto = progresoResponse.id_gesto,
                            porcentaje = progresoResponse.porcentaje,
                            estado = progresoResponse.estado,
                            syncStatus = "synced",
                            lastUpdated = System.currentTimeMillis()
                        )
                        usuarioGestoDao.insertProgreso(nuevoProgreso)
                    } else if (local.syncStatus != "pending") {
                        // Actualizar si no hay cambios pendientes
                        val porcentajeFinal = maxOf(local.porcentaje, progresoResponse.porcentaje)
                        val estadoFinal = if (porcentajeFinal >= 80) "aprendido" else progresoResponse.estado
                        
                        if (progresoResponse.porcentaje != local.porcentaje || progresoResponse.estado != local.estado) {
                            usuarioGestoDao.updateProgreso(
                                progresoResponse.id_usuario,
                                progresoResponse.id_gesto,
                                porcentajeFinal,
                                estadoFinal,
                                System.currentTimeMillis()
                            )
                            usuarioGestoDao.updateSyncStatus(progresoResponse.id_usuario, progresoResponse.id_gesto, "synced")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ProgresoRepository", "Error al descargar datos del servidor", e)
        }
    }
    
    fun getProgresoByUsuario(idUsuario: Int): Flow<List<UsuarioGestoEntity>> {
        return usuarioGestoDao.getProgresoByUsuario(idUsuario)
    }
    
    suspend fun getProgreso(idUsuario: Int, idGesto: Int): UsuarioGestoEntity? {
        return usuarioGestoDao.getProgreso(idUsuario, idGesto)
    }
    
    fun getGestosAprendidos(idUsuario: Int): Flow<List<UsuarioGestoEntity>> {
        return usuarioGestoDao.getGestosAprendidos(idUsuario)
    }
    
    suspend fun getCountGestosAprendidos(idUsuario: Int): Int {
        return usuarioGestoDao.getCountGestosAprendidos(idUsuario)
    }
    
    suspend fun getPromedioProgreso(idUsuario: Int): Float? {
        return usuarioGestoDao.getPromedioProgreso(idUsuario)
    }
}

