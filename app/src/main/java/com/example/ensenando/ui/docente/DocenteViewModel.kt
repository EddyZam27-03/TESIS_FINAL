package com.example.ensenando.ui.docente

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.remote.RetrofitClient
import com.example.ensenando.data.repository.DocenteEstudianteRepository
import com.example.ensenando.data.repository.ProgresoRepository
import com.example.ensenando.util.SecurityUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DocenteViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val apiService = RetrofitClient.apiService
    private val docenteEstudianteRepository = DocenteEstudianteRepository(application, database, apiService)
    private val progresoRepository = ProgresoRepository(application, database, apiService)
    
    private val _estudiantesVinculados = MutableLiveData<List<EstudianteInfo>>()
    val estudiantesVinculados: LiveData<List<EstudianteInfo>> = _estudiantesVinculados
    
    private val _estudiantesRezagados = MutableLiveData<List<EstudianteInfo>>()
    val estudiantesRezagados: LiveData<List<EstudianteInfo>> = _estudiantesRezagados
    
    private val _progresoPorCategoria = MutableLiveData<Map<String, Double>>()
    val progresoPorCategoria: LiveData<Map<String, Double>> = _progresoPorCategoria
    
    init {
        loadEstudiantesVinculados()
    }
    
    fun loadEstudiantesVinculados() {
        viewModelScope.launch {
            val idDocente = SecurityUtils.getUserId(getApplication())
            if (idDocente == -1) return@launch
            
            try {
                val relaciones = docenteEstudianteRepository.getEstudiantesByDocente(idDocente).first()
                    .filter { it.estado == "aceptado" }
                
                val estudiantesInfo = relaciones.map { relacion ->
                    val usuario = database.usuarioDao().getUsuarioById(relacion.idEstudiante)
                    val progresos = progresoRepository.getProgresoByUsuario(relacion.idEstudiante).first()
                    val promedio = if (progresos.isNotEmpty()) {
                        progresos.map { it.porcentaje }.average()
                    } else {
                        0.0
                    }
                    
                    EstudianteInfo(
                        idEstudiante = relacion.idEstudiante,
                        nombre = usuario?.nombre ?: "Estudiante",
                        correo = usuario?.correo ?: "",
                        progresoTotal = promedio.toInt(),
                        ultimaActividad = progresos.maxByOrNull { it.lastUpdated }?.lastUpdated
                    )
                }
                
                _estudiantesVinculados.value = estudiantesInfo
                
                // Cargar estudiantes rezagados
                val rezagados = estudiantesInfo.filter { it.progresoTotal < 50 }
                _estudiantesRezagados.value = rezagados
                
                // Cargar progreso por categoría (promedio de todos los estudiantes)
                loadProgresoPorCategoria(estudiantesInfo.map { it.idEstudiante })
            } catch (e: Exception) {
                android.util.Log.e("DocenteViewModel", "Error al cargar estudiantes", e)
            }
        }
    }
    
    private fun loadProgresoPorCategoria(idsEstudiantes: List<Int>) {
        viewModelScope.launch {
            try {
                val progresoMap = mutableMapOf<String, MutableList<Int>>()
                
                idsEstudiantes.forEach { idEstudiante ->
                    val progresos = progresoRepository.getProgresoByUsuario(idEstudiante).first()
                    progresos.forEach { progreso ->
                        try {
                            val gesto = database.gestoDao().getGestoById(progreso.idGesto)
                            val categoria = gesto?.categoria ?: "Sin categoría"
                            val partes = categoria.split(" - ")
                            val categoriaPrincipal = partes.firstOrNull() ?: categoria
                            
                            progresoMap.getOrPut(categoriaPrincipal) { mutableListOf() }
                                .add(progreso.porcentaje)
                        } catch (e: Exception) {
                            // Continuar si no se encuentra el gesto
                        }
                    }
                }
                
                val promedioPorCategoria = progresoMap.mapValues { (_, porcentajes) ->
                    porcentajes.average()
                }
                
                _progresoPorCategoria.value = promedioPorCategoria
            } catch (e: Exception) {
                android.util.Log.e("DocenteViewModel", "Error al cargar progreso por categoría", e)
            }
        }
    }
    
    data class EstudianteInfo(
        val idEstudiante: Int,
        val nombre: String,
        val correo: String,
        val progresoTotal: Int,
        val ultimaActividad: Long?
    )
}
