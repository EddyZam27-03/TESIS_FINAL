package com.example.ensenando.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.repository.ConfigRepository
import com.example.ensenando.util.NetworkUtils
import com.example.ensenando.util.ThemeUtils
import com.example.ensenando.work.SyncManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val configRepository = ConfigRepository(database)
    
    private val _tema = MutableLiveData<String>()
    val tema: LiveData<String> = _tema
    
    private val _syncStatus = MutableLiveData<String>()
    val syncStatus: LiveData<String> = _syncStatus
    
    private val _lastSync = MutableLiveData<String>()
    val lastSync: LiveData<String> = _lastSync
    
    private val _isOnline = MutableLiveData<Boolean>()
    val isOnline: LiveData<Boolean> = _isOnline
    
    private val _notificacionesLogros = MutableLiveData<Boolean>()
    val notificacionesLogros: LiveData<Boolean> = _notificacionesLogros
    
    private val _notificacionesSolicitudes = MutableLiveData<Boolean>()
    val notificacionesSolicitudes: LiveData<Boolean> = _notificacionesSolicitudes
    
    private val _recordatorios = MutableLiveData<Boolean>()
    val recordatorios: LiveData<Boolean> = _recordatorios
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    
    init {
        cargarConfiguracion()
    }
    
    fun cargarConfiguracion() {
        viewModelScope.launch {
            _tema.value = configRepository.getTema()
            _notificacionesLogros.value = configRepository.getNotificacionesLogros()
            _notificacionesSolicitudes.value = configRepository.getNotificacionesSolicitudes()
            _recordatorios.value = configRepository.getRecordatorios()
            
            // Cargar última sincronización
            val lastSyncTimestamp = configRepository.getValor("last_sync_timestamp")
            if (lastSyncTimestamp != null && lastSyncTimestamp.isNotEmpty()) {
                try {
                    val timestamp = lastSyncTimestamp.toLong()
                    _lastSync.value = dateFormat.format(Date(timestamp))
                } catch (e: Exception) {
                    _lastSync.value = ""
                }
            } else {
                _lastSync.value = ""
            }
            
            // Actualizar estado de sincronización
            actualizarEstadoSincronizacion()
        }
    }
    
    fun cambiarTema(modoOscuro: Boolean) {
        viewModelScope.launch {
            val tema = if (modoOscuro) "dark" else "light"
            configRepository.guardarTema(tema)
            _tema.value = tema
            ThemeUtils.guardarTema(getApplication(), modoOscuro)
        }
    }
    
    fun sincronizarAhora() {
        viewModelScope.launch {
            _syncStatus.value = "Sincronizando..."
            try {
                SyncManager.sincronizarInmediatamente(getApplication())
                _syncStatus.value = "Sincronizado"
                _lastSync.value = dateFormat.format(Date())
                
                // Guardar timestamp
                configRepository.guardarValor("last_sync_timestamp", System.currentTimeMillis().toString())
            } catch (e: Exception) {
                _syncStatus.value = "Error de sincronización"
            }
            actualizarEstadoSincronizacion()
        }
    }
    
    fun actualizarEstadoConexion() {
        viewModelScope.launch {
            _isOnline.value = NetworkUtils.isNetworkAvailable(getApplication())
        }
    }
    
    private fun actualizarEstadoSincronizacion() {
        viewModelScope.launch {
            // Contar elementos pendientes
            val pendingProgreso = database.usuarioGestoDao().getPendingProgreso().size
            val pendingRelaciones = database.docenteEstudianteDao().getPendingRelaciones().size
            val total = pendingProgreso + pendingRelaciones
            
            if (total > 0) {
                _syncStatus.value = "Pendiente: $total elementos"
            } else {
                _syncStatus.value = "Sincronizado"
            }
        }
    }
    
    fun guardarNotificacionesLogros(habilitado: Boolean) {
        viewModelScope.launch {
            configRepository.guardarNotificacionesLogros(habilitado)
            _notificacionesLogros.value = habilitado
        }
    }
    
    fun guardarNotificacionesSolicitudes(habilitado: Boolean) {
        viewModelScope.launch {
            configRepository.guardarNotificacionesSolicitudes(habilitado)
            _notificacionesSolicitudes.value = habilitado
        }
    }
    
    fun guardarRecordatorios(habilitado: Boolean) {
        viewModelScope.launch {
            configRepository.guardarRecordatorios(habilitado)
            _recordatorios.value = habilitado
        }
    }
}
