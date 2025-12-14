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
            
            // Obtener cambios pendientes
            val pendientes = usuarioGestoDao.getPendingProgreso()
            
            // ✅ MEJORADO: Sincronizar usando sync.php (más eficiente)
            if (pendientes.isNotEmpty()) {
                val syncRequest = com.example.ensenando.data.remote.model.SyncRequest(
                    usuario_gestos = pendientes.map { progreso ->
                        com.example.ensenando.data.remote.model.UsuarioGestoSyncItem(
                            id_usuario = progreso.idUsuario,
                            id_gesto = progreso.idGesto,
                            porcentaje = progreso.porcentaje,
                            estado = progreso.estado
                            // last_updated no se envía (solo para Room local)
                        )
                    },
                    docente_estudiante = null
                )
                
                val response = apiService.sync(syncRequest)
                if (response.isSuccessful) {
                    val syncResponse = response.body()
                    // ✅ NUEVO: Resolver conflictos
                    syncResponse?.usuario_gestos?.forEach { remoteProgreso ->
                        val localProgreso = pendientes.find { 
                            it.idUsuario == remoteProgreso.id_usuario && 
                            it.idGesto == remoteProgreso.id_gesto 
                        }
                        
                        if (localProgreso != null) {
                            // ✅ Resolver conflicto: mantener porcentaje más alto
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
            
            // Método anterior (mantener por compatibilidad)
            pendientes.forEach { progreso ->
                try {
                    val syncRequest = com.example.ensenando.data.remote.model.SyncProgresoRequest(
                        id_usuario = progreso.idUsuario,
                        id_gesto = progreso.idGesto,
                        porcentaje = progreso.porcentaje,
                        estado = progreso.estado
                    )
                    
                    val response = apiService.syncProgreso(syncRequest)
                    if (response.isSuccessful && response.body()?.success == true) {
                        usuarioGestoDao.updateSyncStatus(progreso.idUsuario, progreso.idGesto, "synced")
                    }
                } catch (e: Exception) {
                    // Continuar con el siguiente
                }
            }
            
            // Obtener progreso actualizado del servidor
            val response = apiService.getGestosUsuario(idUsuario = idUsuario)
            if (response.isSuccessful) {
                val progresosResponse = response.body() ?: emptyList()
                val progresos = progresosResponse.map { progresoResponse ->
                    val local = usuarioGestoDao.getProgreso(progresoResponse.id_usuario, progresoResponse.id_gesto)
                    
                    // ✅ MEDIUM FIX: Resolución de conflictos correcta
                    when {
                        local == null -> {
                            // No existe local, usar remoto
                            UsuarioGestoEntity(
                                idUsuario = progresoResponse.id_usuario,
                                idGesto = progresoResponse.id_gesto,
                                porcentaje = progresoResponse.porcentaje,
                                estado = progresoResponse.estado,
                                syncStatus = "synced",
                                lastUpdated = System.currentTimeMillis()
                            )
                        }
                        local.syncStatus == "pending" -> {
                            // Local tiene cambios pendientes, mantener local
                            // (ya se sincronizó arriba con syncProgresoRequest)
                            local
                        }
                        else -> {
                            // Comparar timestamps si el backend los proporciona
                            // Por ahora, si local tiene syncStatus "synced", usar remoto
                            // (asumiendo que el servidor tiene la versión más reciente)
                            UsuarioGestoEntity(
                                idUsuario = progresoResponse.id_usuario,
                                idGesto = progresoResponse.id_gesto,
                                porcentaje = progresoResponse.porcentaje,
                                estado = progresoResponse.estado,
                                syncStatus = "synced",
                                lastUpdated = System.currentTimeMillis()
                            )
                        }
                    }
                }
                usuarioGestoDao.insertProgresos(progresos)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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

