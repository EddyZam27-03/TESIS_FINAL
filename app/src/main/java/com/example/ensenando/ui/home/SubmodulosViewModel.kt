package com.example.ensenando.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.repository.GestoRepository
import com.example.ensenando.data.remote.RetrofitClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SubmodulosViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val apiService = RetrofitClient.apiService
    private val gestoRepository = GestoRepository(application, database, apiService)
    
    private val _submodulos = MutableLiveData<List<HomeViewModel.Submodulo>>()
    val submodulos: LiveData<List<HomeViewModel.Submodulo>> = _submodulos
    
    private val _moduloNombre = MutableLiveData<String>()
    val moduloNombre: LiveData<String> = _moduloNombre
    
    fun cargarSubmodulos(moduloNombre: String) {
        _moduloNombre.value = moduloNombre
        viewModelScope.launch {
            try {
                val gestos = gestoRepository.getAllGestos().first()
                val moduloCategoria = when (moduloNombre) {
                    "Básico" -> "BASICO"
                    "Social" -> "SOCIAL"
                    "Académico" -> "ACADEMICO"
                    else -> ""
                }
                
                val submodulosMap = mutableMapOf<String, MutableList<com.example.ensenando.data.local.entity.GestoEntity>>()
                
                gestos.forEach { gesto ->
                    val categoria = gesto.categoria ?: return@forEach
                    val partes = categoria.split(" - ")
                    if (partes.size >= 2 && partes[0].uppercase() == moduloCategoria) {
                        val submodulo = partes[1]
                        submodulosMap.getOrPut(submodulo) { mutableListOf() }.add(gesto)
                    }
                }
                
                val submodulosList = submodulosMap.map { (nombre, gestosList) ->
                    HomeViewModel.Submodulo(nombre = nombre, gestos = gestosList)
                }
                
                _submodulos.value = submodulosList
            } catch (e: Exception) {
                android.util.Log.e("SubmodulosViewModel", "Error al cargar submódulos", e)
                _submodulos.value = emptyList()
            }
        }
    }
}

