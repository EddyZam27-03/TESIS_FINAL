package com.example.ensenando.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.local.entity.DocenteEstudianteEntity
import com.example.ensenando.data.local.entity.UsuarioEntity
import com.example.ensenando.data.local.entity.UsuarioGestoEntity
import com.example.ensenando.data.repository.DocenteEstudianteRepository
import com.example.ensenando.data.repository.ProgresoRepository
import com.example.ensenando.data.repository.UsuarioRepository
import com.example.ensenando.data.remote.RetrofitClient
import com.example.ensenando.util.SecurityUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val apiService = RetrofitClient.apiService
    private val usuarioRepository = UsuarioRepository(application, database, apiService)
    private val progresoRepository = ProgresoRepository(application, database, apiService)
    private val docenteEstudianteRepository = DocenteEstudianteRepository(application, database, apiService)
    
    private val _usuario = MutableLiveData<UsuarioEntity?>()
    val usuario: LiveData<UsuarioEntity?> = _usuario
    
    private val _solicitudes = MutableLiveData<List<DocenteEstudianteEntity>>()
    val solicitudes: LiveData<List<DocenteEstudianteEntity>> = _solicitudes
    
    // Mapa de nombres de usuarios para mostrar en solicitudes
    private val _nombresUsuarios = MutableLiveData<Map<Int, String>>()
    val nombresUsuarios: LiveData<Map<Int, String>> = _nombresUsuarios
    
    // ✅ NUEVO: Mapa de correos de usuarios
    private val _correosUsuarios = MutableLiveData<Map<Int, String>>()
    val correosUsuarios: LiveData<Map<Int, String>> = _correosUsuarios
    
    private val _reporteGenerado = MutableLiveData<Result<String>?>()
    val reporteGenerado: LiveData<Result<String>?> = _reporteGenerado
    
    init {
        loadUsuario()
        loadSolicitudes()
        loadNombresUsuarios()
        loadProgresoTotal()
    }
    
    /**
     * Carga los nombres de todos los usuarios para mostrar en solicitudes
     */
    private fun loadNombresUsuarios() {
        viewModelScope.launch {
            try {
                val usuarios = usuarioRepository.getAllUsuarios().first()
                val nombresMap = usuarios.associate { it.idUsuario to it.nombre }
                val correosMap = usuarios.associate { it.idUsuario to it.correo }
                _nombresUsuarios.value = nombresMap
                _correosUsuarios.value = correosMap
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error al cargar nombres de usuarios", e)
                _nombresUsuarios.value = emptyMap()
                _correosUsuarios.value = emptyMap()
            }
        }
    }
    
    private fun loadUsuario() {
        viewModelScope.launch {
            val idUsuario = SecurityUtils.getUserId(getApplication())
            if (idUsuario != -1) {
                val usuario = usuarioRepository.getUsuarioByIdSuspend(idUsuario)
                _usuario.value = usuario
            }
        }
    }
    
    private fun loadSolicitudes() {
        viewModelScope.launch {
            val idUsuario = SecurityUtils.getUserId(getApplication())
            val rol = SecurityUtils.getUserRol(getApplication())
            
            if (idUsuario != -1) {
                try {
                    when (rol) {
                        "estudiante" -> {
                            val response = apiService.consultarSolicitudEstudiante(idEstudiante = idUsuario)
                            val body = response.body()
                            if (response.isSuccessful && body?.success == true) {
                                val solicitudesResponse = body.solicitudes ?: emptyList()
                                val relaciones = solicitudesResponse.map { solicitud ->
                                    DocenteEstudianteEntity(
                                        idDocente = solicitud.id_docente,
                                        idEstudiante = solicitud.id_estudiante,
                                        estado = solicitud.estado,
                                        syncStatus = "synced",
                                        lastUpdated = System.currentTimeMillis()
                                    )
                                }
                                _solicitudes.value = relaciones
                            } else {
                                // Fallback: cargar desde local
                                val solicitudesList = docenteEstudianteRepository.getSolicitudesPendientes(idUsuario).first()
                                _solicitudes.value = solicitudesList
                            }
                        }
                        "docente" -> {
                            val response = apiService.listarSolicitudesDocente(idDocente = idUsuario)
                            val body = response.body()
                            if (response.isSuccessful && body?.success == true) {
                                val solicitudesResponse = body.solicitudes ?: emptyList()
                                val relaciones = solicitudesResponse.map { solicitud ->
                                    DocenteEstudianteEntity(
                                        idDocente = solicitud.id_docente,
                                        idEstudiante = solicitud.id_estudiante,
                                        estado = solicitud.estado,
                                        syncStatus = "synced",
                                        lastUpdated = System.currentTimeMillis()
                                    )
                                }
                                _solicitudes.value = relaciones
                            } else {
                                // Fallback: cargar desde local
                                val estudiantesList = docenteEstudianteRepository.getEstudiantesByDocente(idUsuario).first()
                                _solicitudes.value = estudiantesList
                            }
                        }
                        "administrador" -> {
                            // Administrador puede ver todas las relaciones
                            val relacionesList = docenteEstudianteRepository.getEstudiantesByDocente(idUsuario).first()
                            _solicitudes.value = relacionesList
                        }
                    }
                } catch (e: Exception) {
                    // Fallback: cargar desde local
                    when (rol) {
                        "estudiante" -> {
                            val solicitudesList = docenteEstudianteRepository.getSolicitudesPendientes(idUsuario).first()
                            _solicitudes.value = solicitudesList
                        }
                        else -> {
                            val estudiantesList = docenteEstudianteRepository.getEstudiantesByDocente(idUsuario).first()
                            _solicitudes.value = estudiantesList
                        }
                    }
                }
            }
        }
    }
    
    fun aceptarSolicitud(idDocente: Int, idEstudiante: Int) {
        viewModelScope.launch {
            val result = docenteEstudianteRepository.actualizarSolicitud(idDocente, idEstudiante, "aceptado")
            if (result.isSuccess) {
                loadSolicitudes()
            }
        }
    }
    
    fun rechazarSolicitud(idDocente: Int, idEstudiante: Int) {
        viewModelScope.launch {
            val result = docenteEstudianteRepository.actualizarSolicitud(idDocente, idEstudiante, "rechazado")
            if (result.isSuccess) {
                loadSolicitudes()
            }
        }
    }
    
    fun eliminarRelacion(idDocente: Int, idEstudiante: Int) {
        viewModelScope.launch {
            val result = docenteEstudianteRepository.eliminarRelacion(idDocente, idEstudiante)
            if (result.isSuccess) {
                loadSolicitudes()
            }
        }
    }
    
    fun generarReporte(idUsuario: Int, formato: String = "pdf") {
        viewModelScope.launch {
            try {
                // ✅ FIX: Primero intentar obtener datos del servidor usando el endpoint reporte.php
                val reporte = try {
                    generarReporteDesdeServidor(idUsuario)
                } catch (e: Exception) {
                    android.util.Log.w("ProfileViewModel", "Error al obtener reporte del servidor, usando datos locales", e)
                    // Fallback: usar datos locales si el servidor falla
                    generarReporteLocal(idUsuario)
                }
                _reporteGenerado.value = Result.success(reporte)
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error al generar reporte", e)
                _reporteGenerado.value = Result.failure(e)
            }
        }
    }
    
    /**
     * ✅ NUEVO: Genera el reporte usando datos del servidor (reporte.php)
     */
    private suspend fun generarReporteDesdeServidor(idUsuario: Int): String {
        val context = getApplication<Application>()
        val token = SecurityUtils.getToken(context)
        
        if (token.isNullOrEmpty()) {
            throw Exception("No hay token de autenticación")
        }
        
        // Llamar al endpoint del servidor
        val response = apiService.generarReporte(idUsuario = idUsuario, formato = "pdf")
        
        if (!response.isSuccessful || response.body() == null) {
            throw Exception("Error al obtener reporte del servidor: ${response.message()}")
        }
        
        val reporteResponse = response.body()!!
        if (reporteResponse.success != true || reporteResponse.data == null) {
            throw Exception(reporteResponse.message ?: "Error al obtener datos del reporte")
        }
        
        val data = reporteResponse.data!!
        val usuarioReporte = data.usuario ?: throw Exception("Datos de usuario no disponibles")
        val progresosReporte = data.progresos ?: emptyList()
        
        // Convertir datos del servidor a entidades locales para generar el PDF
        val usuario = UsuarioEntity(
            idUsuario = idUsuario,
            nombre = usuarioReporte.nombre,
            correo = usuarioReporte.correo,
            rol = usuarioReporte.rol,
            contrasena = null,
            fechaRegistro = "",
            syncStatus = "synced",
            lastUpdated = System.currentTimeMillis()
        )
        
        val progresos = progresosReporte.map { progresoReporte ->
            UsuarioGestoEntity(
                idUsuario = idUsuario,
                idGesto = progresoReporte.id_gesto,
                porcentaje = progresoReporte.porcentaje,
                estado = progresoReporte.estado,
                syncStatus = "synced",
                lastUpdated = System.currentTimeMillis()
            )
        }
        
        // Obtener nombres de gestos y gestos completos
        val gestosMap = mutableMapOf<Int, String>()
        val gestosCompletosMap = mutableMapOf<Int, com.example.ensenando.data.local.entity.GestoEntity>()
        try {
            val gestosList = database.gestoDao().getAllGestos().first()
            gestosList.forEach { gesto ->
                gestosMap[gesto.idGesto] = gesto.nombre
                gestosCompletosMap[gesto.idGesto] = gesto
            }
            // También usar los nombres que vienen del servidor
            progresosReporte.forEach { progresoReporte ->
                gestosMap[progresoReporte.id_gesto] = progresoReporte.nombre
            }
        } catch (e: Exception) {
            // Si no hay gestos locales, usar solo los del servidor
            progresosReporte.forEach { progresoReporte ->
                gestosMap[progresoReporte.id_gesto] = progresoReporte.nombre
            }
        }
        
        // Generar PDF usando PdfGenerator
        return com.example.ensenando.util.PdfGenerator.generarReportePDF(
            context,
            usuario,
            progresos,
            gestosMap,
            gestosCompletosMap
        )
    }
    
    /**
     * ✅ MANTENER: Genera el reporte usando datos locales (fallback)
     */
    private suspend fun generarReporteLocal(idUsuario: Int): String {
        val progresos = progresoRepository.getProgresoByUsuario(idUsuario).first()
        val usuario = usuarioRepository.getUsuarioByIdSuspend(idUsuario)
            ?: throw Exception("Usuario no encontrado")
        
        return generarReporteLocal(usuario, progresos)
    }
    
    private suspend fun generarReporteLocal(usuario: com.example.ensenando.data.local.entity.UsuarioEntity, progresos: List<com.example.ensenando.data.local.entity.UsuarioGestoEntity>): String {
        val context = getApplication<Application>()
        
        // Obtener nombres de gestos y gestos completos
        val gestosMap = mutableMapOf<Int, String>()
        val gestosCompletosMap = mutableMapOf<Int, com.example.ensenando.data.local.entity.GestoEntity>()
        try {
            val gestosList = database.gestoDao().getAllGestos().first()
            gestosList.forEach { gesto ->
                gestosMap[gesto.idGesto] = gesto.nombre
                gestosCompletosMap[gesto.idGesto] = gesto
            }
        } catch (e: Exception) {
            // Si no hay gestos, continuar sin nombres
        }
        
        // Generar PDF usando PdfGenerator mejorado
        return com.example.ensenando.util.PdfGenerator.generarReportePDF(
            context,
            usuario,
            progresos,
            gestosMap,
            gestosCompletosMap
        )
    }
    
    fun resetActividad(idUsuario: Int, idGesto: Int) {
        viewModelScope.launch {
            try {
                // Resetear localmente
                progresoRepository.updateProgreso(idUsuario, idGesto, 0)
                loadUsuario()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // ✅ FIX: Método para cerrar sesión
    fun logout() {
        SecurityUtils.clearAll(getApplication())
    }
    
    // ✅ NUEVO: Editar perfil
    fun editarPerfil(nuevoNombre: String) {
        viewModelScope.launch {
            val idUsuario = SecurityUtils.getUserId(getApplication())
            if (idUsuario != -1) {
                try {
                    val usuario = usuarioRepository.getUsuarioByIdSuspend(idUsuario)
                    if (usuario != null) {
                        val usuarioActualizado = usuario.copy(
                            nombre = nuevoNombre,
                            syncStatus = "pending"
                        )
                        usuarioRepository.updateUsuario(usuarioActualizado)
                        loadUsuario()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProfileViewModel", "Error al editar perfil", e)
                }
            }
        }
    }
    
    // ✅ NUEVO: Cambiar contraseña
    fun cambiarContraseña(passwordActual: String, nuevaPassword: String) {
        viewModelScope.launch {
            val idUsuario = SecurityUtils.getUserId(getApplication())
            if (idUsuario != -1) {
                try {
                    val usuario = usuarioRepository.getUsuarioByIdSuspend(idUsuario)
                    if (usuario != null) {
                        // TODO: Verificar contraseña actual contra BD o servidor
                        // Por ahora, asumimos que es correcta
                        // Usar bcrypt o el mismo método que usa el servidor
                        val hashNuevaPassword = android.util.Base64.encodeToString(
                            java.security.MessageDigest.getInstance("SHA-256")
                                .digest(nuevaPassword.toByteArray()),
                            android.util.Base64.NO_WRAP
                        )
                        
                        val usuarioActualizado = usuario.copy(
                            contrasena = hashNuevaPassword,
                            syncStatus = "pending",
                            lastUpdated = System.currentTimeMillis()
                        )
                        usuarioRepository.updateUsuario(usuarioActualizado)
                        loadUsuario()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProfileViewModel", "Error al cambiar contraseña", e)
                }
            }
        }
    }
    
    // ✅ NUEVO: Cargar progreso total
    private val _progresoTotal = MutableLiveData<ProgresoTotal>()
    val progresoTotal: LiveData<ProgresoTotal> = _progresoTotal
    
    fun loadProgresoTotal() {
        viewModelScope.launch {
            val idUsuario = SecurityUtils.getUserId(getApplication())
            if (idUsuario != -1) {
                try {
                    val progresos = progresoRepository.getProgresoByUsuario(idUsuario).first()
                    val gestosAprendidos = progresos.count { it.estado == "aprendido" }
                    val totalGestos = database.gestoDao().getAllGestos().first().size
                    
                    val logroRepository = com.example.ensenando.data.repository.LogroRepository(
                        getApplication(), database, apiService
                    )
                    val logros = logroRepository.getLogrosUsuario(idUsuario).getOrNull() ?: emptyList()
                    val logrosObtenidos = logros.count { it.desbloqueado == true }
                    val totalLogros = database.logroDao().getAllLogros().first().size
                    
                    _progresoTotal.value = ProgresoTotal(
                        gestosAprendidos = gestosAprendidos,
                        totalGestos = totalGestos,
                        logrosObtenidos = logrosObtenidos,
                        totalLogros = totalLogros
                    )
                } catch (e: Exception) {
                    android.util.Log.e("ProfileViewModel", "Error al cargar progreso total", e)
                }
            }
        }
    }
    
    /**
     * ✅ FIX: Limpia el estado del reporte generado para evitar que se muestre automáticamente
     * cuando el usuario vuelve al perfil después de haberlo visto
     */
    fun limpiarReporteGenerado() {
        _reporteGenerado.value = null
    }
    
    data class ProgresoTotal(
        val gestosAprendidos: Int,
        val totalGestos: Int,
        val logrosObtenidos: Int,
        val totalLogros: Int
    )
}

