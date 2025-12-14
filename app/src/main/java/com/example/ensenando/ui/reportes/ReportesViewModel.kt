package com.example.ensenando.ui.reportes

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.remote.RetrofitClient
import com.example.ensenando.data.repository.ProgresoRepository
import com.example.ensenando.util.SecurityUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReportesViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val apiService = RetrofitClient.apiService
    private val progresoRepository = ProgresoRepository(application, database, apiService)
    private val usuarioRepository = com.example.ensenando.data.repository.UsuarioRepository(application, database, apiService)
    
    private val _datosReporte = MutableLiveData<List<DatoReporte>>()
    val datosReporte: LiveData<List<DatoReporte>> = _datosReporte
    
    private val _paginaActual = MutableLiveData<Int>(1)
    val paginaActual: LiveData<Int> = _paginaActual
    
    // ✅ FIX: Estado del reporte generado
    private val _reporteGenerado = MutableLiveData<Result<String>>()
    val reporteGenerado: LiveData<Result<String>> = _reporteGenerado
    
    private val _reporteListo = MutableLiveData<Boolean>(false)
    val reporteListo: LiveData<Boolean> = _reporteListo
    
    private var reportePath: String? = null
    
    private val itemsPorPagina = 5
    
    fun cargarDatosIniciales() {
        cargarDatosPagina()
    }
    
    fun paginaAnterior() {
        val pagina = _paginaActual.value ?: 1
        if (pagina > 1) {
            _paginaActual.value = pagina - 1
            cargarDatosPagina()
        }
    }
    
    fun paginaSiguiente() {
        val pagina = _paginaActual.value ?: 1
        _paginaActual.value = pagina + 1
        cargarDatosPagina()
    }
    
    private fun cargarDatosPagina() {
        viewModelScope.launch {
            val idUsuario = SecurityUtils.getUserId(getApplication())
            if (idUsuario != -1) {
                try {
                    val progresos = progresoRepository.getProgresoByUsuario(idUsuario).first()
                    val pagina = _paginaActual.value ?: 1
                    val inicio = (pagina - 1) * itemsPorPagina
                    val fin = inicio + itemsPorPagina
                    
                    val datos = progresos.subList(
                        inicio.coerceAtLeast(0),
                        fin.coerceAtMost(progresos.size)
                    ).map { progreso ->
                        val gesto = database.gestoDao().getGestoById(progreso.idGesto)
                        DatoReporte(
                            titulo = gesto?.nombre ?: "Gesto ${progreso.idGesto}",
                            valor = "${progreso.porcentaje}% - ${progreso.estado}"
                        )
                    }
                    
                    _datosReporte.value = datos
                } catch (e: Exception) {
                    android.util.Log.e("ReportesViewModel", "Error al cargar datos", e)
                }
            }
        }
    }
    
    fun aplicarFiltros() {
        // Reiniciar a página 1 y recargar
        _paginaActual.value = 1
        cargarDatosPagina()
    }
    
    // ✅ FIX: Primero generar y mostrar reporte con vista previa
    fun generarYMostrarReporte(idUsuario: Int) {
        viewModelScope.launch {
            try {
                // Mostrar loading
                _reporteListo.value = false
                
                val progresos = progresoRepository.getProgresoByUsuario(idUsuario).first()
                val usuario = usuarioRepository.getUsuarioByIdSuspend(idUsuario)
                
                if (usuario != null) {
                    // Obtener nombres de gestos
                    val gestosMap = mutableMapOf<Int, String>()
                    val gestosCompletosMap = mutableMapOf<Int, com.example.ensenando.data.local.entity.GestoEntity>()
                    try {
                        val gestosList = database.gestoDao().getAllGestos().first()
                        gestosList.forEach { gesto ->
                            gestosMap[gesto.idGesto] = gesto.nombre
                            gestosCompletosMap[gesto.idGesto] = gesto
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ReportesViewModel", "Error al cargar gestos", e)
                    }
                    
                    // Generar PDF
                    val path = com.example.ensenando.util.PdfGenerator.generarReportePDF(
                        getApplication(),
                        usuario,
                        progresos,
                        gestosMap,
                        gestosCompletosMap
                    )
                    
                    reportePath = path
                    
                    // Actualizar datos del reporte para mostrar vista previa ANTES de mostrar diálogo
                    actualizarDatosReporte(progresos, gestosMap)
                    
                    // Luego mostrar el diálogo con el PDF
                    _reporteGenerado.value = Result.success(path)
                    _reporteListo.value = true
                } else {
                    _reporteGenerado.value = Result.failure(Exception("Usuario no encontrado"))
                }
            } catch (e: Exception) {
                android.util.Log.e("ReportesViewModel", "Error al generar reporte", e)
                _reporteGenerado.value = Result.failure(e)
            }
        }
    }
    
    private fun actualizarDatosReporte(
        progresos: List<com.example.ensenando.data.local.entity.UsuarioGestoEntity>,
        gestosMap: Map<Int, String>
    ) {
        viewModelScope.launch {
            try {
                val datos = progresos.take(20).map { progreso -> // Mostrar primeros 20
                    val nombreGesto = gestosMap[progreso.idGesto] ?: "Gesto ${progreso.idGesto}"
                    DatoReporte(
                        titulo = nombreGesto,
                        valor = "${progreso.porcentaje}% - ${progreso.estado}"
                    )
                }
                _datosReporte.value = datos
            } catch (e: Exception) {
                android.util.Log.e("ReportesViewModel", "Error al actualizar datos", e)
            }
        }
    }
    
    // ✅ FIX: Compartir reporte generado
    fun compartirReporte(context: android.content.Context) {
        reportePath?.let { path ->
            val file = java.io.File(path)
            if (file.exists()) {
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Compartir reporte"))
            } else {
                android.widget.Toast.makeText(
                    context,
                    "Primero debe generar el reporte",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        } ?: run {
            android.widget.Toast.makeText(
                context,
                "Primero debe generar el reporte",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    data class DatoReporte(
        val titulo: String,
        val valor: String
    )
}
