package com.example.ensenando.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.local.entity.GestoEntity
import com.example.ensenando.data.repository.GestoRepository
import com.example.ensenando.data.repository.ProgresoRepository
import com.example.ensenando.data.remote.RetrofitClient
import com.example.ensenando.util.SecurityUtils
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val apiService = RetrofitClient.apiService
    private val gestoRepository = GestoRepository(application, database, apiService)
    private val progresoRepository = ProgresoRepository(application, database, apiService)

    private val _gestos = MutableLiveData<List<GestoEntity>>()
    val gestos: LiveData<List<GestoEntity>> = _gestos

    private val _progreso = MutableLiveData<ProgresoResumen>()
    val progreso: LiveData<ProgresoResumen> = _progreso

    // ✅ CAMBIADO: Ahora son 3 módulos principales (Básico, Social, Académico)
    private val _modulosPrincipales = MutableLiveData<List<ModuloPrincipal>>()
    val modulosPrincipales: LiveData<List<ModuloPrincipal>> = _modulosPrincipales
    
    // ✅ NUEVO: Mapa de progreso por gesto
    private val _progresoMap = MutableLiveData<Map<Int, GestoAdapter.GestoProgreso>>()
    val progresoMap: LiveData<Map<Int, GestoAdapter.GestoProgreso>> = _progresoMap
    
    // ✅ NUEVO: Logros recientes
    private val _logrosRecientes = MutableLiveData<List<com.example.ensenando.data.remote.model.LogrosResponse>>()
    val logrosRecientes: LiveData<List<com.example.ensenando.data.remote.model.LogrosResponse>> = _logrosRecientes
    
    // ✅ NUEVO: Notificaciones pendientes
    private val _notificacionesPendientes = MutableLiveData<Int>()
    val notificacionesPendientes: LiveData<Int> = _notificacionesPendientes
    
    // ✅ NUEVO: Estado de conexión
    private val _isOnline = MutableLiveData<Boolean>()
    val isOnline: LiveData<Boolean> = _isOnline

    init {
        // ✅ OPTIMIZADO: Cargar solo lo esencial al inicio
        loadModulosNombres()
        loadProgresoLocal()
        // Cargar el resto en background sin bloquear
        viewModelScope.launch {
        loadLogrosRecientes()
        loadNotificacionesPendientes()
        updateConnectionStatus()
            // Sincronizar en background sin bloquear UI
            try {
                if (com.example.ensenando.util.NetworkUtils.isNetworkAvailable(getApplication())) {
                    gestoRepository.syncGestos()
                }
            } catch (e: Exception) {
                android.util.Log.w("HomeViewModel", "Error al sincronizar en background", e)
            }
        }
    }

    /** ============================================
     *   ✅ OPTIMIZADO: CARGAR SOLO NOMBRES DE MÓDULOS
     *   Sin procesar todos los gestos
     *  ============================================ */
    private fun loadModulosNombres() {
        viewModelScope.launch {
            try {
                // Cargar solo una vez para obtener los módulos disponibles
                val gestos = gestoRepository.getAllGestos().first()
                val modulosSet = mutableSetOf<String>()
                
                gestos.forEach { gesto ->
                    val categoria = gesto.categoria ?: return@forEach
                    val partes = categoria.split(" - ")
                    if (partes.isNotEmpty()) {
                        val modulo = partes[0].uppercase()
                        when (modulo) {
                            "BASICO" -> modulosSet.add("Básico")
                            "SOCIAL" -> modulosSet.add("Social")
                            "ACADEMICO" -> modulosSet.add("Académico")
                        }
                    }
                }
                
                // Crear lista simple de módulos (solo nombres, sin gestos)
                val modulosList = modulosSet.map { nombre ->
                    ModuloPrincipal(
                        nombre = nombre,
                        nombreCategoria = when (nombre) {
                            "Básico" -> "BASICO"
                            "Social" -> "SOCIAL"
                            "Académico" -> "ACADEMICO"
                            else -> ""
                        },
                        submodulos = emptyList() // No cargar submódulos aquí
                    )
                }
                
                _modulosPrincipales.value = modulosList
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error al cargar módulos", e)
                // Fallback: mostrar módulos por defecto
                _modulosPrincipales.value = listOf(
                    ModuloPrincipal("Básico", "BASICO", emptyList()),
                    ModuloPrincipal("Social", "SOCIAL", emptyList()),
                    ModuloPrincipal("Académico", "ACADEMICO", emptyList())
                )
            }
        }
    }

    private fun loadProgresoLocal() {
        viewModelScope.launch {
            val idUsuario = SecurityUtils.getUserId(getApplication())
            if (idUsuario == -1) return@launch

            progresoRepository.getProgresoByUsuario(idUsuario).collect { progresos ->
                val totalGestos = gestos.value?.size ?: 0
                val gestosAprendidos = progresos.count { it.estado == "aprendido" }
                // Manejar null de forma segura
                val promedio = try {
                    progresoRepository.getPromedioProgreso(idUsuario) ?: 0f
                } catch (e: Exception) {
                    0f
                }
                val ultimaActividad = progresos.maxByOrNull { it.lastUpdated }?.idGesto

                _progreso.value = ProgresoResumen(
                    totalGestos = totalGestos,
                    gestosAprendidos = gestosAprendidos,
                    promedioProgreso = promedio,
                    ultimaActividad = ultimaActividad
                )
                
                // ✅ FIX: Actualizar progresoMap para que los gestos se muestren con progreso
                val progresoMap = progresos.associate { progreso ->
                    progreso.idGesto to GestoAdapter.GestoProgreso(
                        porcentaje = progreso.porcentaje,
                        estado = progreso.estado
                    )
                }
                _progresoMap.value = progresoMap
            }
        }
    }

    /** ================================
     *  ✅ NUEVO: ORGANIZAR 3 MÓDULOS PRINCIPALES
     *  ================================ */
    private fun organizarModulosPrincipales(gestos: List<GestoEntity>) {
        viewModelScope.launch {
            // Crear los 3 módulos principales
            val modulosMap = mutableMapOf<String, MutableMap<String, MutableList<GestoEntity>>>()

            gestos.forEach { gesto ->
                val categoria = gesto.categoria ?: return@forEach
                val partes = categoria.split(" - ")

                if (partes.size < 2) return@forEach

                val modulo = partes[0].uppercase() // BASICO, SOCIAL, ACADEMICO
                val submodulo = partes[1]

                modulosMap
                    .getOrPut(modulo) { mutableMapOf() }
                    .getOrPut(submodulo) { mutableListOf() }
                    .add(gesto)
            }

            // Crear lista de módulos principales (solo los 3 principales)
            val modulosList = mutableListOf<ModuloPrincipal>()
            
            // BASICO
            val basicoSubmodulos = modulosMap["BASICO"] ?: mutableMapOf()
            if (basicoSubmodulos.isNotEmpty()) {
                modulosList.add(
                    ModuloPrincipal(
                        nombre = "Básico",
                        nombreCategoria = "BASICO",
                        submodulos = basicoSubmodulos.map { (subName, gestosList) ->
                            Submodulo(nombre = subName, gestos = gestosList)
                        }
                    )
                )
            }
            
            // SOCIAL
            val socialSubmodulos = modulosMap["SOCIAL"] ?: mutableMapOf()
            if (socialSubmodulos.isNotEmpty()) {
                modulosList.add(
                    ModuloPrincipal(
                        nombre = "Social",
                        nombreCategoria = "SOCIAL",
                        submodulos = socialSubmodulos.map { (subName, gestosList) ->
                            Submodulo(nombre = subName, gestos = gestosList)
                        }
                    )
                )
            }
            
            // ACADEMICO
            val academicoSubmodulos = modulosMap["ACADEMICO"] ?: mutableMapOf()
            if (academicoSubmodulos.isNotEmpty()) {
                modulosList.add(
                    ModuloPrincipal(
                        nombre = "Académico",
                        nombreCategoria = "ACADEMICO",
                        submodulos = academicoSubmodulos.map { (subName, gestosList) ->
                            Submodulo(nombre = subName, gestos = gestosList)
                        }
                    )
                )
            }

            _modulosPrincipales.value = modulosList
        }
    }

    /** ============================
     *      DATA CLASSES
     *  ============================ */
    data class ModuloPrincipal(
        val nombre: String, // "Básico", "Social", "Académico"
        val nombreCategoria: String, // "BASICO", "SOCIAL", "ACADEMICO"
        val submodulos: List<Submodulo>
    )

    data class Submodulo(
        val nombre: String,
        val gestos: List<GestoEntity>
    )

    data class ProgresoResumen(
        val totalGestos: Int,
        val gestosAprendidos: Int,
        val promedioProgreso: Float,
        val ultimaActividad: Int?
    )
    
    private fun loadLogrosRecientes() {
        viewModelScope.launch {
            try {
                val idUsuario = SecurityUtils.getUserId(getApplication())
                if (idUsuario != -1) {
                    val logroRepository = com.example.ensenando.data.repository.LogroRepository(
                        getApplication(),
                        database,
                        apiService
                    )
                    val logros = logroRepository.getLogrosRecientes(idUsuario, 3)
                    _logrosRecientes.value = logros
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error al cargar logros recientes", e)
                _logrosRecientes.value = emptyList()
            }
        }
    }
    
    private fun loadNotificacionesPendientes() {
        viewModelScope.launch {
            try {
                val idUsuario = SecurityUtils.getUserId(getApplication())
                if (idUsuario != -1) {
                    val docenteEstudianteRepository = com.example.ensenando.data.repository.DocenteEstudianteRepository(
                        getApplication(),
                        database,
                        apiService
                    )
                    docenteEstudianteRepository.getSolicitudesPendientes(idUsuario).collect { solicitudes ->
                        _notificacionesPendientes.value = solicitudes.size
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error al cargar notificaciones pendientes", e)
                _notificacionesPendientes.value = 0
            }
        }
    }
    
    private fun updateConnectionStatus() {
        viewModelScope.launch {
            _isOnline.value = com.example.ensenando.util.NetworkUtils.isNetworkAvailable(getApplication())
        }
    }
}
