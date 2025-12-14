package com.example.ensenando.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.repository.GestoRepository
import com.example.ensenando.data.repository.ProgresoRepository
import com.example.ensenando.data.remote.RetrofitClient
import com.example.ensenando.util.SecurityUtils
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GestosViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val apiService = RetrofitClient.apiService
    private val gestoRepository = GestoRepository(application, database, apiService)
    private val progresoRepository = ProgresoRepository(application, database, apiService)
    
    private val _gestos = MutableLiveData<List<com.example.ensenando.data.local.entity.GestoEntity>>()
    val gestos: LiveData<List<com.example.ensenando.data.local.entity.GestoEntity>> = _gestos
    
    private val _progresoMap = MutableLiveData<Map<Int, GestoAdapter.GestoProgreso>>()
    val progresoMap: LiveData<Map<Int, GestoAdapter.GestoProgreso>> = _progresoMap
    
    private val _titulo = MutableLiveData<String>()
    val titulo: LiveData<String> = _titulo
    
    fun cargarGestos(moduloNombre: String, submoduloNombre: String) {
        _titulo.value = "$moduloNombre > $submoduloNombre"
        
        viewModelScope.launch {
            try {
                val gestos = gestoRepository.getAllGestos().first()
                val moduloCategoria = when (moduloNombre) {
                    "Básico" -> "BASICO"
                    "Social" -> "SOCIAL"
                    "Académico" -> "ACADEMICO"
                    else -> ""
                }
                
                val gestosFiltrados = gestos.filter { gesto ->
                    val categoria = gesto.categoria ?: return@filter false
                    val partes = categoria.split(" - ")
                    partes.size >= 2 && 
                    partes[0].uppercase() == moduloCategoria && 
                    partes[1] == submoduloNombre
                }
                
                _gestos.value = gestosFiltrados
                
                // Cargar progreso solo para estos gestos
                val idUsuario = SecurityUtils.getUserId(getApplication())
                if (idUsuario != -1) {
                    // ✅ FIX: Usar first() en lugar de collect() para evitar actualizaciones continuas
                    val progresos = progresoRepository.getProgresoByUsuario(idUsuario).first()
                    val progresoMap = progresos
                        .filter { progreso -> gestosFiltrados.any { it.idGesto == progreso.idGesto } }
                        .associate { progreso ->
                            progreso.idGesto to GestoAdapter.GestoProgreso(
                                porcentaje = progreso.porcentaje,
                                estado = progreso.estado
                            )
                        }
                    _progresoMap.value = progresoMap
                    android.util.Log.d("GestosViewModel", "ProgresoMap cargado: ${progresoMap.size} items")
                } else {
                    _progresoMap.value = emptyMap()
                }
            } catch (e: Exception) {
                android.util.Log.e("GestosViewModel", "Error al cargar gestos", e)
                _gestos.value = emptyList()
            }
        }
    }
}

