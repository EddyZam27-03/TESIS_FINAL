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
import com.example.ensenando.data.repository.UsuarioRepository
import com.example.ensenando.util.SecurityUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val apiService = RetrofitClient.apiService
    private val docenteEstudianteRepository = DocenteEstudianteRepository(application, database, apiService)
    private val progresoRepository = ProgresoRepository(application, database, apiService)
    private val usuarioRepository = UsuarioRepository(application, database, apiService)
    
    private val _estudiantes = MutableLiveData<List<UsuarioResponse>>()
    val estudiantes: LiveData<List<UsuarioResponse>> = _estudiantes
    
    private val _docentes = MutableLiveData<List<UsuarioResponse>>()
    val docentes: LiveData<List<UsuarioResponse>> = _docentes
    
    private val _relaciones = MutableLiveData<List<DocenteEstudianteEntity>>()
    val relaciones: LiveData<List<DocenteEstudianteEntity>> = _relaciones
    
    private val _reporteGenerado = MutableLiveData<Result<String>>()
    val reporteGenerado: LiveData<Result<String>> = _reporteGenerado
    
    private val _relacionEliminada = MutableLiveData<Result<Unit>>()
    val relacionEliminada: LiveData<Result<Unit>> = _relacionEliminada
    
    private val _progresoEstudiante = MutableLiveData<List<ProgresoDetalle>>()
    val progresoEstudiante: LiveData<List<ProgresoDetalle>> = _progresoEstudiante
    
    private val _nombresUsuarios = MutableLiveData<Map<Int, String>>()
    val nombresUsuarios: LiveData<Map<Int, String>> = _nombresUsuarios
    
    // Lista completa para filtrado
    private var relacionesCompletas: List<DocenteEstudianteEntity> = emptyList()
    
    init {
        loadNombresUsuarios()
    }
    
    /**
     * Carga los nombres de todos los usuarios para mostrar en relaciones
     */
    private fun loadNombresUsuarios() {
        viewModelScope.launch {
            try {
                val usuarios = usuarioRepository.getAllUsuarios().first()
                val nombresMap = usuarios.associate { it.idUsuario to it.nombre }
                _nombresUsuarios.value = nombresMap
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Error al cargar nombres de usuarios", e)
                _nombresUsuarios.value = emptyMap()
            }
        }
    }
    
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
    
    /**
     * Carga todas las relaciones desde la base de datos
     * Solo se llama cuando se necesita buscar
     */
    fun cargarRelaciones() {
        viewModelScope.launch {
            try {
                // Cargar todas las relaciones desde la base de datos local
                val todasLasRelaciones = database.docenteEstudianteDao().getAllRelaciones().first()
                relacionesCompletas = todasLasRelaciones
                // No actualizar _relaciones aquí - solo cuando se busque
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Error al cargar relaciones", e)
                relacionesCompletas = emptyList()
            }
        }
    }
    
    /**
     * Busca relaciones por nombre de docente o estudiante
     * Muestra resultados solo cuando hay una búsqueda activa
     */
    fun buscarRelacion(query: String) {
        val lista = relacionesCompletas
        val nombres = _nombresUsuarios.value ?: emptyMap()
        
        if (query.isBlank()) {
            _relaciones.value = emptyList()
        } else {
            val filtrados = lista.filter { relacion ->
                val nombreDocente = nombres[relacion.idDocente] ?: "Docente ${relacion.idDocente}"
                val nombreEstudiante = nombres[relacion.idEstudiante] ?: "Estudiante ${relacion.idEstudiante}"
                nombreDocente.contains(query, ignoreCase = true) ||
                nombreEstudiante.contains(query, ignoreCase = true) ||
                relacion.estado.contains(query, ignoreCase = true)
            }
            _relaciones.value = filtrados
        }
    }
    
    /**
     * Limpia los resultados de relaciones (oculta la lista)
     */
    fun limpiarRelaciones() {
        _relaciones.value = emptyList()
    }
    
    fun generarReporte(idUsuario: Int) {
        viewModelScope.launch {
            try {
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
                
                val reporte = generarReportePDF(usuario, progresos)
                _reporteGenerado.value = Result.success(reporte)
            } catch (e: Exception) {
                _reporteGenerado.value = Result.failure(e)
            }
        }
    }
    
    fun resetActividad(idUsuario: Int, idGesto: Int) {
        viewModelScope.launch {
            try {
                progresoRepository.updateProgreso(idUsuario, idGesto, 0)
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
        // Resetear todos los gestos del estudiante
        viewModelScope.launch {
            try {
                val progresos = progresoRepository.getProgresoByUsuario(idUsuario).first()
                progresos.forEach { progreso ->
                    progresoRepository.updateProgreso(idUsuario, progreso.idGesto, 0)
                }
                android.widget.Toast.makeText(
                    getApplication(),
                    "Actividad reseteada exitosamente",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Error al resetear actividad", e)
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
}

