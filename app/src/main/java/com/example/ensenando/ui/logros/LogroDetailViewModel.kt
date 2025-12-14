package com.example.ensenando.ui.logros

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.remote.RetrofitClient
import com.example.ensenando.data.remote.model.LogrosResponse
import com.example.ensenando.data.repository.LogroRepository
import com.example.ensenando.util.SecurityUtils
import kotlinx.coroutines.launch

class LogroDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val apiService = RetrofitClient.apiService
    private val logroRepository = LogroRepository(application, database, apiService)
    
    private val _logro = MutableLiveData<LogrosResponse?>()
    val logro: LiveData<LogrosResponse?> = _logro
    
    fun loadLogro(idLogro: Int) {
        viewModelScope.launch {
            try {
                val idUsuario = SecurityUtils.getUserId(getApplication())
                if (idUsuario == -1) return@launch
                
                val logros = logroRepository.getLogrosUsuario(idUsuario)
                logros.onSuccess { listaLogros ->
                    val logroEncontrado = listaLogros.find { 
                        (it.id_logro ?: it.id) == idLogro 
                    }
                    _logro.value = logroEncontrado
                }.onFailure {
                    android.util.Log.e("LogroDetailViewModel", "Error al cargar logro", it)
                }
            } catch (e: Exception) {
                android.util.Log.e("LogroDetailViewModel", "Error al cargar logro", e)
            }
        }
    }
}
