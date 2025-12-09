package com.example.ensenando.ui.logros

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.remote.RetrofitClient
import com.example.ensenando.data.repository.LogroRepository
import com.example.ensenando.data.remote.model.LogrosResponse
import com.example.ensenando.util.onSuccess
import com.example.ensenando.util.onFailure
import com.example.ensenando.util.SecurityUtils
import kotlinx.coroutines.launch

class LogrosViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val apiService = RetrofitClient.apiService
    private val logroRepository = LogroRepository(application, database, apiService)
    
    private val _logros = MutableLiveData<List<LogrosResponse>>()
    val logros: LiveData<List<LogrosResponse>> = _logros
    
    private val _totalLogros = MutableLiveData<Int>()
    val totalLogros: LiveData<Int> = _totalLogros
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    init {
        loadLogros()
    }
    
    fun loadLogros() {
        viewModelScope.launch {
            _loading.value = true
            val idUsuario = SecurityUtils.getUserId(getApplication())
            if (idUsuario != -1) {
                val result = logroRepository.getLogrosUsuario(idUsuario)
                result.onSuccess { logrosList ->
                    _logros.value = logrosList
                    _totalLogros.value = logrosList.size
                }
            }
            _loading.value = false
        }
    }
    
    fun refreshLogros() {
        loadLogros()
    }
}

