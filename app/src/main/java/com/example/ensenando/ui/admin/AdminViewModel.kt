package com.example.ensenando.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.local.entity.DocenteEstudianteEntity
import com.example.ensenando.data.remote.RetrofitClient
import com.example.ensenando.data.remote.model.ProgresoDetalle
import com.example.ensenando.data.remote.model.UsuarioResponse
import com.example.ensenando.data.repository.DocenteEstudianteRepository
import com.example.ensenando.data.repository.ProgresoRepository
import com.example.ensenando.util.SecurityUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val apiService = RetrofitClient.apiService
    private val docenteEstudianteRepository = DocenteEstudianteRepository(application, database, apiService)
    private val progresoRepository = ProgresoRepository(application, database, apiService)
    
    private val _estudiantes = MutableLiveData<List<UsuarioResponse>>()
    val estudiantes: LiveData<List<UsuarioResponse>> = _estudiantes
    
    private val _docentes = MutableLiveData<List<UsuarioResponse>>()
    val docentes: LiveData<List<UsuarioResponse>> = _docentes
    
    private val _relaciones = MutableLiveData<List<DocenteEstudianteEntity>>()
    val relaciones: LiveData<List<DocenteEstudianteEntity>> = _relaciones
    
    private val _reporteGenerado = MutableLiveData<Result<String>?>()
    val reporteGenerado: LiveData<Result<String>?> = _reporteGenerado
    
    private val _relacionEliminada = MutableLiveData<Result<Unit>>()
    val relacionEliminada: LiveData<Result<Unit>> = _relacionEliminada
    
    private val _progresoEstudiante = MutableLiveData<List<ProgresoDetalle>>()
    val progresoEstudiante: LiveData<List<ProgresoDetalle>> = _progresoEstudiante
    
    fun cargarTodosEstudiantes() {
        viewModelScope.launch {
            try {
                val response = apiService.getProgresoUsuarios(idAdmin = SecurityUtils.getUserId(getApplication()))
                if (response.isSuccessful) {
                    val progreso = response.body()?.progreso ?: emptyList()
                    val estudiantes = progreso.map { progresoDetalle ->
                        UsuarioResponse(
                            id_usuario = progresoDetalle.id_usuario,
                            nombre = progresoDetalle.nombre ?: "",
                            correo = progresoDetalle.correo ?: "",
                            rol = "estudiante"
                        )
                    }
                    _estudiantes.value = estudiantes
                }
            } catch (e: Exception) {
                _estudiantes.value = emptyList()
            }
        }
    }
    
    fun cargarDocentes() {
        viewModelScope.launch {
            try {
                val response = apiService.listarDocentes()
                if (response.isSuccessful) {
                    _docentes.value = response.body() ?: emptyList()
                } else {
                    _docentes.value = emptyList()
                }
            } catch (e: Exception) {
                _docentes.value = emptyList()
            }
        }
    }
    
    fun buscarEstudiante(busqueda: String) {
        viewModelScope.launch {
            try {
                val response = apiService.buscarEstudiante(busqueda = busqueda)
                if (response.isSuccessful) {
                    val resultados = response.body() ?: emptyList()
                    val estudiantes = resultados.filter { it.rol == "estudiante" }
                    _estudiantes.value = estudiantes
                }
            } catch (e: Exception) {
                _estudiantes.value = emptyList()
            }
        }
    }
    
    fun buscarDocenteLocal(query: String) {
        val lista = _docentes.value ?: return
        val filtrados = lista.filter { it.nombre.contains(query, ignoreCase = true) || it.correo.contains(query, ignoreCase = true) }
        _docentes.value = filtrados
    }
    
    fun buscarRelacion(query: String) {
        val lista = _relaciones.value ?: return
        val filtrados = lista.filter { relacion ->
            // Buscar por nombre de docente o estudiante
            val docente = _docentes.value?.find { it.id_usuario == relacion.idDocente }
            val estudiante = _estudiantes.value?.find { it.id_usuario == relacion.idEstudiante }
            docente?.nombre?.contains(query, ignoreCase = true) == true ||
            docente?.correo?.contains(query, ignoreCase = true) == true ||
            estudiante?.nombre?.contains(query, ignoreCase = true) == true ||
            estudiante?.correo?.contains(query, ignoreCase = true) == true
        }
        _relaciones.value = filtrados
    }
    
    fun cargarRelaciones() {
        viewModelScope.launch {
            try {
                // Cargar todas las relaciones desde la base de datos local
                // O hacer una llamada especial para admin
                val relaciones = docenteEstudianteRepository.getEstudiantesByDocente(0).first()
                _relaciones.value = relaciones
            } catch (e: Exception) {
                _relaciones.value = emptyList()
            }
        }
    }
    
    fun generarReporte(idUsuario: Int) {
        viewModelScope.launch {
            try {
                // ✅ FIX: Primero intentar obtener datos del servidor usando el endpoint reporte.php
                val reporte = try {
                    generarReporteDesdeServidor(idUsuario)
                } catch (e: Exception) {
                    android.util.Log.w("AdminViewModel", "Error al obtener reporte del servidor, usando datos locales", e)
                    // Fallback: usar datos locales si el servidor falla
                    generarReporteLocal(idUsuario)
                }
                _reporteGenerado.value = Result.success(reporte)
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Error al generar reporte", e)
                _reporteGenerado.value = Result.failure(e)
            }
        }
    }
    
    /**
     * ✅ NUEVO: Genera el reporte usando datos del servidor (reporte.php)
     */
    private suspend fun generarReporteDesdeServidor(idUsuario: Int): String {
        val context = getApplication<android.app.Application>()
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
        val usuario = com.example.ensenando.data.local.entity.UsuarioEntity(
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
            com.example.ensenando.data.local.entity.UsuarioGestoEntity(
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
        val usuario = database.usuarioDao().getUsuarioById(idUsuario)
            ?: com.example.ensenando.data.local.entity.UsuarioEntity(
                idUsuario = idUsuario,
                nombre = _docentes.value?.firstOrNull { it.id_usuario == idUsuario }?.nombre
                    ?: _estudiantes.value?.firstOrNull { it.id_usuario == idUsuario }?.nombre
                    ?: "Usuario $idUsuario",
                correo = _docentes.value?.firstOrNull { it.id_usuario == idUsuario }?.correo
                    ?: _estudiantes.value?.firstOrNull { it.id_usuario == idUsuario }?.correo
                    ?: "correo@desconocido.com",
                contrasena = null,
                rol = _docentes.value?.firstOrNull { it.id_usuario == idUsuario }?.rol
                    ?: _estudiantes.value?.firstOrNull { it.id_usuario == idUsuario }?.rol
                    ?: "desconocido",
                fechaRegistro = "",
                syncStatus = "synced",
                lastUpdated = System.currentTimeMillis()
            )
        
        return generarReportePDF(usuario, progresos)
    }
    
    fun resetActividad(idUsuario: Int, idGesto: Int) {
        viewModelScope.launch {
            try {
                progresoRepository.updateProgreso(idUsuario, idGesto, 0)
                // Toast removido - el ViewModel no debe mostrar UI directamente
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun verProgreso(idUsuario: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.getProgresoUsuarios(
                    idAdmin = SecurityUtils.getUserId(getApplication()),
                    idEstudiante = idUsuario
                )
                if (response.isSuccessful) {
                    val progreso = response.body()?.progreso ?: emptyList()
                    _progresoEstudiante.value = progreso
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun eliminarRelacion(idDocente: Int, idEstudiante: Int) {
        viewModelScope.launch {
            val result = docenteEstudianteRepository.eliminarRelacion(idDocente, idEstudiante)
            _relacionEliminada.value = result
        }
    }
    
    fun mostrarDialogoReset(idUsuario: Int) {
        // TODO: Implementar diálogo para seleccionar gesto
        // Por ahora, resetear todos los gestos
        viewModelScope.launch {
            val progresos = progresoRepository.getProgresoByUsuario(idUsuario).first()
            progresos.forEach { progreso ->
                progresoRepository.updateProgreso(idUsuario, progreso.idGesto, 0)
            }
        }
    }
    
    private suspend fun generarReportePDF(
        usuario: com.example.ensenando.data.local.entity.UsuarioEntity,
        progresos: List<com.example.ensenando.data.local.entity.UsuarioGestoEntity>
    ): String {
        val context = getApplication<android.app.Application>()
        
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
     * ✅ FIX: Limpia el estado del reporte generado para evitar que se muestre automáticamente
     * cuando el usuario vuelve al fragment después de haberlo visto
     */
    fun limpiarReporteGenerado() {
        _reporteGenerado.value = null
    }
}

