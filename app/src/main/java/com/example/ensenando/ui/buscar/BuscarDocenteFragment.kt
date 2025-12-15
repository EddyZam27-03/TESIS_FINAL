package com.example.ensenando.ui.buscar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ensenando.databinding.FragmentBuscarDocenteBinding
import kotlinx.coroutines.launch

class BuscarDocenteFragment : Fragment() {
    private var _binding: FragmentBuscarDocenteBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BuscarDocenteViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBuscarDocenteBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        // Cargar docentes al iniciar
        viewModel.cargarTodosDocentes()
    }
    
    private fun setupRecyclerView() {
        val adapter = DocenteAdapter(
            onDocenteClick = { docente ->
                // Enviar solicitud al docente
                val idDocente = docente.id_usuario ?: docente.id
                if (idDocente != null) {
                    viewModel.enviarSolicitud(idDocente)
                }
            },
            onVerReporte = { docente ->
                // ✅ NUEVO: Ver reporte del docente
                val idDocente = docente.id_usuario ?: docente.id
                if (idDocente != null) {
                    viewModel.generarReporte(idDocente)
                }
            }
        )
        binding.rvDocentes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDocentes.adapter = adapter
        
        viewModel.docentes.observe(viewLifecycleOwner) { docentes ->
            adapter.submitList(docentes)
            binding.tvEmpty.visibility = if (docentes.isEmpty()) View.VISIBLE else View.GONE
        }
    }
    
    private fun setupObservers() {
        viewModel.solicitudEnviada.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Solicitud enviada exitosamente",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }.onFailure { exception ->
                android.widget.Toast.makeText(
                    requireContext(),
                    "Error al enviar solicitud: ${exception.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // Mostrar/ocultar loading si existe en el layout
            // Por ahora solo deshabilitar botón
            binding.btnBuscar.isEnabled = !isLoading
        }
        
        // ✅ NUEVO: Observar reporte generado
        viewModel.reporteGenerado.observe(viewLifecycleOwner) { result ->
            result?.let {
                it.onSuccess { filePath ->
                    mostrarReporteEnPantalla(filePath)
                    viewModel.limpiarReporteGenerado()
                }.onFailure { exception ->
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Error al generar reporte: ${exception.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    viewModel.limpiarReporteGenerado()
                }
            }
        }
    }
    
    /**
     * ✅ NUEVO: Muestra el reporte usando el mismo diálogo
     */
    private fun mostrarReporteEnPantalla(filePath: String) {
        try {
            val file = java.io.File(filePath)
            if (!file.exists()) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "El archivo del reporte no existe",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return
            }
            
            val dialog = com.example.ensenando.ui.profile.ReporteDialogFragment.newInstance(filePath)
            dialog.show(parentFragmentManager, "ReporteDialogBuscarDocente")
            
        } catch (e: Exception) {
            android.util.Log.e("BuscarDocenteFragment", "Error al mostrar reporte", e)
            android.widget.Toast.makeText(
                requireContext(),
                "Error al mostrar reporte: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        
        // Buscar docente - usar etBusqueda del layout
        binding.etBusqueda.setOnEditorActionListener { _, _, _ ->
            val busqueda = binding.etBusqueda.text.toString()
            if (busqueda.isNotEmpty()) {
                viewModel.buscarDocente(busqueda)
            } else {
                viewModel.cargarTodosDocentes()
            }
            true
        }
        
        binding.btnBuscar.setOnClickListener {
            val busqueda = binding.etBusqueda.text.toString()
            if (busqueda.isNotEmpty()) {
                viewModel.buscarDocente(busqueda)
            } else {
                viewModel.cargarTodosDocentes()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
