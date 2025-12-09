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

    init {
        loadHomeData()
    }

    /** ============================================
     *   ✅ MEJORADO: CARGA LOCAL PRIMERO, SYNC EN BACKGROUND
     *  ============================================ */
    private fun loadHomeData() {
        viewModelScope.launch {
            val idUsuario = SecurityUtils.getUserId(getApplication())
            if (idUsuario == -1) return@launch

            // ✅ PASO 1: Cargar datos locales INMEDIATAMENTE (sin esperar red)
            loadGestosLocal()
            loadProgresoLocal()

            // ✅ PASO 2: Intentar sincronizar en background (si hay red)
            try {
                if (com.example.ensenando.util.NetworkUtils.isNetworkAvailable(getApplication())) {
                    // Sincronizar gestos en background
                    gestoRepository.syncGestos()

                    // Intentar obtener datos actualizados del servidor
                    val response = apiService.getHomeData(usuarioId = idUsuario)
                    val homeData = response.body()

                    if (response.isSuccessful && homeData?.success == true) {
                        val stats = homeData.estadisticas

                        // Manejar null de forma segura para promedio_progreso
                        val promedioProgreso = try {
                            when (val prom = stats?.promedio_progreso) {
                                null -> 0f
                                is Number -> prom.toFloat()
                                else -> 0f
                            }
                        } catch (e: Exception) {
                            0f
                        }

                        // Actualizar con datos del servidor
                        _progreso.value = ProgresoResumen(
                            totalGestos = gestos.value?.size ?: 0,
                            gestosAprendidos = stats?.gestos_aprendidos ?: 0,
                            promedioProgreso = promedioProgreso,
                            ultimaActividad = homeData.actividades?.firstOrNull()?.id_gesto
                        )

                        // Los gestos ya se actualizaron con syncGestos(), solo reorganizar
                        gestos.value?.let { organizarModulosPrincipales(it) }
                    }
                }
            } catch (e: Exception) {
                // Si falla la sincronización, los datos locales ya están cargados
                android.util.Log.w("HomeViewModel", "Error al sincronizar (continuando con datos locales): ${e.message}")
            }
        }
    }

    /** =========================
     *     CARGA LOCAL (OFFLINE)
     *  ========================= */
    private fun loadGestosLocal() {
        viewModelScope.launch {
            gestoRepository.getAllGestos().collect { gestosList ->
                _gestos.value = gestosList
                organizarModulosPrincipales(gestosList)
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
}
