package com.example.ensenando.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.repository.UsuarioRepository
import com.example.ensenando.data.remote.RetrofitClient
import com.example.ensenando.util.SecurityUtils
import com.example.ensenando.util.SecurityUtils as Security
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val apiService = RetrofitClient.apiService
    private val usuarioRepository = UsuarioRepository(application, database, apiService)
    
    private val _loginResult = MutableLiveData<Result<Unit>>()
    val loginResult: LiveData<Result<Unit>> = _loginResult
    
    private val _registerResult = MutableLiveData<Result<Unit>>()
    val registerResult: LiveData<Result<Unit>> = _registerResult
    
    fun login(correo: String, contrasena: String) {
        viewModelScope.launch {
            val result = usuarioRepository.login(correo, contrasena)
            _loginResult.value = result.map { Unit }
        }
    }
    
    fun register(nombre: String, correo: String, contrasena: String, rol: String) {
        viewModelScope.launch {
            val result = usuarioRepository.register(nombre, correo, contrasena, rol)
            _registerResult.value = result.map { Unit }
        }
    }
    
    fun isLoggedIn(): Boolean {
        return SecurityUtils.isLoggedIn(getApplication())
    }
    
    fun logout() {
        SecurityUtils.clearAll(getApplication())
    }
}

