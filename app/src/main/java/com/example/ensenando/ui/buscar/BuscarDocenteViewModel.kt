package com.example.ensenando.ui.buscar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.remote.RetrofitClient
import com.example.ensenando.data.remote.model.UsuarioResponse
import com.example.ensenando.data.repository.DocenteEstudianteRepository
import com.example.ensenando.util.SecurityUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BuscarDocenteViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val apiService = RetrofitClient.apiService
    private val docenteEstudianteRepository = DocenteEstudianteRepository(application, database, apiService)
    
    private val _docentes = MutableLiveData<List<UsuarioResponse>>()
    val docentes: LiveData<List<UsuarioResponse>> = _docentes
    
    private val _solicitudEnviada = MutableLiveData<Result<Unit>>()
    val solicitudEnviada: LiveData<Result<Unit>> = _solicitudEnviada
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _reporteGenerado = MutableLiveData<Result<String>?>()
    val reporteGenerado: LiveData<Result<String>?> = _reporteGenerado
    
    fun cargarTodosDocentes() {
        viewModelScope.launch {
            _loading.value = true
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
            _loading.value = false
        }
    }
    
    fun buscarDocente(busqueda: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = apiService.buscarEstudiante(busqueda = busqueda)
                if (response.isSuccessful) {
                    // Filtrar solo docentes
                    val resultados = response.body() ?: emptyList()
                    val docentes = resultados.filter { it.rol == "docente" }
                    _docentes.value = docentes
                } else {
                    _docentes.value = emptyList()
                }
            } catch (e: Exception) {
                _docentes.value = emptyList()
            }
            _loading.value = false
        }
    }
    
    fun enviarSolicitud(idDocente: Int) {
        viewModelScope.launch {
            val idEstudiante = SecurityUtils.getUserId(getApplication())
            if (idEstudiante != -1) {
                val result = docenteEstudianteRepository.crearSolicitud(idDocente, idEstudiante)
                _solicitudEnviada.value = result
            } else {
                _solicitudEnviada.value = Result.failure(Exception("No autenticado"))
            }
        }
    }
    
    /**
     * ✅ NUEVO: Genera reporte de un docente
     */
    fun generarReporte(idUsuario: Int) {
        viewModelScope.launch {
            try {
                // Intentar obtener datos del servidor
                val reporte = try {
                    generarReporteDesdeServidor(idUsuario)
                } catch (e: Exception) {
                    android.util.Log.w("BuscarDocenteViewModel", "Error al obtener reporte del servidor, usando datos locales", e)
                    generarReporteLocal(idUsuario)
                }
                _reporteGenerado.value = Result.success(reporte)
            } catch (e: Exception) {
                android.util.Log.e("BuscarDocenteViewModel", "Error al generar reporte", e)
                _reporteGenerado.value = Result.failure(e)
            }
        }
    }
    
    private suspend fun generarReporteDesdeServidor(idUsuario: Int): String {
        val context = getApplication<android.app.Application>()
        val token = SecurityUtils.getToken(context)
        
        if (token.isNullOrEmpty()) {
            throw Exception("No hay token de autenticación")
        }
        
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
        
        val gestosMap = mutableMapOf<Int, String>()
        val gestosCompletosMap = mutableMapOf<Int, com.example.ensenando.data.local.entity.GestoEntity>()
        try {
            val gestosList = database.gestoDao().getAllGestos().first()
            gestosList.forEach { gesto ->
                gestosMap[gesto.idGesto] = gesto.nombre
                gestosCompletosMap[gesto.idGesto] = gesto
            }
            progresosReporte.forEach { progresoReporte ->
                gestosMap[progresoReporte.id_gesto] = progresoReporte.nombre
            }
        } catch (e: Exception) {
            progresosReporte.forEach { progresoReporte ->
                gestosMap[progresoReporte.id_gesto] = progresoReporte.nombre
            }
        }
        
        return com.example.ensenando.util.PdfGenerator.generarReportePDF(
            context,
            usuario,
            progresos,
            gestosMap,
            gestosCompletosMap
        )
    }
    
    private suspend fun generarReporteLocal(idUsuario: Int): String {
        val context = getApplication<android.app.Application>()
        val progresoRepository = com.example.ensenando.data.repository.ProgresoRepository(context, database, apiService)
        val usuarioRepository = com.example.ensenando.data.repository.UsuarioRepository(context, database, apiService)
        
        val progresos = progresoRepository.getProgresoByUsuario(idUsuario).first()
        val usuario = usuarioRepository.getUsuarioByIdSuspend(idUsuario)
            ?: throw Exception("Usuario no encontrado")
        
        val gestosMap = mutableMapOf<Int, String>()
        val gestosCompletosMap = mutableMapOf<Int, com.example.ensenando.data.local.entity.GestoEntity>()
        try {
            val gestosList = database.gestoDao().getAllGestos().first()
            gestosList.forEach { gesto ->
                gestosMap[gesto.idGesto] = gesto.nombre
                gestosCompletosMap[gesto.idGesto] = gesto
            }
        } catch (e: Exception) {
        }
        
        return com.example.ensenando.util.PdfGenerator.generarReportePDF(
            context,
            usuario,
            progresos,
            gestosMap,
            gestosCompletosMap
        )
    }
    
    fun limpiarReporteGenerado() {
        _reporteGenerado.value = null
    }
}


