package com.example.ensenando.ui.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.local.entity.GestoEntity
import com.example.ensenando.data.local.entity.UsuarioGestoEntity
import com.example.ensenando.data.repository.GestoRepository
import com.example.ensenando.data.repository.HistorialIntentoRepository
import com.example.ensenando.data.repository.ProgresoRepository
import com.example.ensenando.data.remote.RetrofitClient
import com.example.ensenando.ml.GestureRecognitionManager
import com.example.ensenando.util.SecurityUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val apiService = RetrofitClient.apiService
    private val gestoRepository = GestoRepository(application, database, apiService)
    private val progresoRepository = ProgresoRepository(application, database, apiService)
    private val historialRepository = HistorialIntentoRepository(application, database)
    
    private val gestureRecognitionManager = GestureRecognitionManager(application)
    
    private val _gesto = MutableLiveData<GestoEntity?>()
    val gesto: LiveData<GestoEntity?> = _gesto
    
    private val _progresoActual = MutableLiveData<UsuarioGestoEntity?>()
    val progresoActual: LiveData<UsuarioGestoEntity?> = _progresoActual
    
    private val _progress = MutableLiveData<Int>()
    val progress: LiveData<Int> = _progress
    
    private val _prediction = MutableLiveData<Pair<Int, Float>?>()
    val prediction: LiveData<Pair<Int, Float>?> = _prediction
    
    private val _historialIntentos = MutableLiveData<List<com.example.ensenando.data.local.entity.HistorialIntentoEntity>>()
    val historialIntentos: LiveData<List<com.example.ensenando.data.local.entity.HistorialIntentoEntity>> = _historialIntentos
    
    init {
        viewModelScope.launch {
            gestureRecognitionManager.progress.collect { progressValue ->
                _progress.value = progressValue
            }
        }
        
        viewModelScope.launch {
            gestureRecognitionManager.currentPrediction.collect { predictionValue ->
                _prediction.value = predictionValue
            }
        }
    }
    
    fun loadGesto(idGesto: Int) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ActivityViewModel", "Cargando gesto con ID: $idGesto")
            val gesto = gestoRepository.getGestoById(idGesto)
                
                if (gesto == null) {
                    android.util.Log.e("ActivityViewModel", "Gesto con ID $idGesto no encontrado")
                    _gesto.value = null
                    return@launch
                }
                
                android.util.Log.d("ActivityViewModel", "Gesto encontrado: ${gesto.nombre}")
            _gesto.value = gesto
            
            val idUsuario = SecurityUtils.getUserId(getApplication())
            if (idUsuario != -1) {
                val progreso = progresoRepository.getProgreso(idUsuario, idGesto)
                _progresoActual.value = progreso
                _progress.value = progreso?.porcentaje ?: 0
                
                // ✅ NUEVO: Cargar historial de intentos
                cargarHistorialIntentos(idUsuario, idGesto)
                }
            } catch (e: Exception) {
                android.util.Log.e("ActivityViewModel", "Error al cargar gesto", e)
                _gesto.value = null
            }
        }
    }
    
    fun processFrame(bitmap: android.graphics.Bitmap) {
        val idGesto = _gesto.value?.idGesto ?: return
        gestureRecognitionManager.processFrame(bitmap, idGesto)
    }
    
    fun saveProgress() {
        viewModelScope.launch {
            val idUsuario = SecurityUtils.getUserId(getApplication())
            val idGesto = _gesto.value?.idGesto
            
            if (idUsuario != -1 && idGesto != null) {
                val currentProgress = gestureRecognitionManager.getCurrentProgress()
                val progresoActual = _progresoActual.value
                
                // Solo guardar si hay incremento
                if (progresoActual == null || currentProgress > progresoActual.porcentaje) {
                    progresoRepository.updateProgreso(idUsuario, idGesto, currentProgress)
                    
                    // Recargar progreso actualizado
                    val nuevoProgreso = progresoRepository.getProgreso(idUsuario, idGesto)
                    _progresoActual.value = nuevoProgreso
                }
            }
        }
    }
    
    fun resetProgress() {
        gestureRecognitionManager.resetProgress()
        _progress.value = 0
    }
    
    private fun cargarHistorialIntentos(idUsuario: Int, idGesto: Int) {
        viewModelScope.launch {
            try {
                val intentos = historialRepository.getUltimosIntentos(idUsuario, idGesto, 10)
                _historialIntentos.value = intentos
            } catch (e: Exception) {
                android.util.Log.e("ActivityViewModel", "Error al cargar historial de intentos", e)
                _historialIntentos.value = emptyList()
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        gestureRecognitionManager.close()
    }
}


