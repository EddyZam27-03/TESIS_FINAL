package com.example.ensenando.ui.reportes

import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ensenando.databinding.FragmentReportesBinding
import com.example.ensenando.util.SecurityUtils
import java.io.File

class ReportesFragment : Fragment() {
    private var _binding: FragmentReportesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReportesViewModel by viewModels()
    private var pdfRenderer: PdfRenderer? = null
    private var pdfPageAdapter: com.example.ensenando.ui.profile.PdfPageAdapter? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ✅ FIX: Limpiar argumentos después de leerlos para evitar que se reutilicen
        val filtro = arguments?.getString("filtro") ?: ""
        arguments?.clear()
        
        val rol = SecurityUtils.getUserRol(requireContext())
        
        // Mostrar filtros solo para docente y administrador
        if (rol == "docente" || rol == "administrador") {
            binding.cardFiltros.visibility = ViewGroup.VISIBLE
        } else {
            binding.cardFiltros.visibility = ViewGroup.GONE
        }
        
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        // Cargar datos iniciales
        viewModel.cargarDatosIniciales()
    }
    
    private fun setupRecyclerView() {
        // ✅ FIX: Configurar RecyclerView para mostrar páginas del PDF
        binding.rvPdfPages.layoutManager = LinearLayoutManager(requireContext())
    }
    
    private fun setupObservers() {
        // ✅ FIX: Observar reporte generado y mostrarlo visualmente en la pantalla
        viewModel.reporteGenerado.observe(viewLifecycleOwner) { result ->
            result.onSuccess { filePath ->
                // Cargar y mostrar el PDF visualmente
                cargarYMostrarPDF(filePath)
                // Mostrar vista previa del reporte en la pantalla
                binding.cardVistaPreviaReporte.visibility = ViewGroup.VISIBLE
                // Habilitar botón compartir después de generar
                binding.btnCompartir.isEnabled = true
                binding.btnGenerarPDF.isEnabled = true
                binding.btnGenerarPDF.text = "Generar PDF"
                // Scroll hasta la vista previa
                binding.root.post {
                    binding.cardVistaPreviaReporte.requestFocus()
                }
            }.onFailure { exception ->
                android.widget.Toast.makeText(
                    requireContext(),
                    "Error al generar reporte: ${exception.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                binding.btnGenerarPDF.isEnabled = true
                binding.btnGenerarPDF.text = "Generar PDF"
            }
        }
        
        viewModel.reporteListo.observe(viewLifecycleOwner) { listo ->
            binding.btnCompartir.isEnabled = listo
        }
    }
    
    /**
     * ✅ FIX: Carga el PDF y lo muestra visualmente usando PdfRenderer
     */
    private fun cargarYMostrarPDF(filePath: String) {
        try {
            // Cerrar renderer anterior si existe
            pdfRenderer?.close()
            pdfRenderer = null
            
            val file = File(filePath)
            if (!file.exists()) {
                android.util.Log.e("ReportesFragment", "El archivo PDF no existe: $filePath")
                return
            }
            
            // Abrir el archivo PDF
            val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor)
            
            // Crear adapter para mostrar las páginas
            pdfPageAdapter = com.example.ensenando.ui.profile.PdfPageAdapter(
                pdfRenderer = pdfRenderer!!,
                onPageClick = null
            )
            
            // Asignar adapter al RecyclerView
            binding.rvPdfPages.adapter = pdfPageAdapter
            
            android.util.Log.d("ReportesFragment", "PDF cargado exitosamente: ${pdfRenderer!!.pageCount} páginas")
            
        } catch (e: Exception) {
            android.util.Log.e("ReportesFragment", "Error al cargar PDF", e)
            android.widget.Toast.makeText(
                requireContext(),
                "Error al cargar PDF: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        
        binding.btnAplicarFiltros.setOnClickListener {
            viewModel.aplicarFiltros()
        }
        
        // ✅ FIX: Primero generar y mostrar reporte, luego permitir descargar/compartir
        binding.btnGenerarPDF.setOnClickListener {
            val idUsuario = SecurityUtils.getUserId(requireContext())
            if (idUsuario != -1) {
                // Deshabilitar botón mientras genera
                binding.btnGenerarPDF.isEnabled = false
                binding.btnGenerarPDF.text = "Generando..."
                viewModel.generarYMostrarReporte(idUsuario)
        }
        }
        
        // ✅ FIX: El botón compartir solo se habilita después de generar el reporte
        binding.btnCompartir.isEnabled = false
        binding.btnCompartir.setOnClickListener {
            viewModel.compartirReporte(requireContext())
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Cerrar el renderer del PDF
        pdfRenderer?.close()
        pdfRenderer = null
        pdfPageAdapter = null
        _binding = null
    }
}
