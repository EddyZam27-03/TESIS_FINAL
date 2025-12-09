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
}


