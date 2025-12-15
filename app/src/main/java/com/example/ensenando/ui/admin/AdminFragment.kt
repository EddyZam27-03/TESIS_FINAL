package com.example.ensenando.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ensenando.databinding.FragmentAdminBinding
import com.example.ensenando.ui.profile.ReporteDialogFragment
import java.io.File

class AdminFragment : Fragment() {
    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerViews()
        setupObservers()
        setupClickListeners()
        
        // Cargar datos iniciales
        viewModel.cargarDocentes()
        viewModel.cargarTodosEstudiantes()
        viewModel.cargarRelaciones()
    }
    
    private fun setupRecyclerViews() {
        // RecyclerView de docentes
        val docentesAdapter = DocenteAdminAdapter(
            onVerReporte = { docente ->
                docente.id_usuario?.let { viewModel.generarReporte(it) }
            }
        )
        binding.rvDocentes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDocentes.adapter = docentesAdapter
        
        viewModel.docentes.observe(viewLifecycleOwner) { docentes ->
            docentesAdapter.submitList(docentes)
        }
        
        // RecyclerView de estudiantes
        val estudiantesAdapter = EstudianteAdminAdapter(
            onEstudianteClick = { estudiante ->
                // Click en el item (puede usarse para ver detalles)
                val idUsuario = estudiante.id_usuario ?: estudiante.id
                if (idUsuario != null) {
                    viewModel.verProgreso(idUsuario)
                }
            },
            onVerReporte = { estudiante ->
                // ✅ NUEVO: Botón Ver Reporte
                val idUsuario = estudiante.id_usuario ?: estudiante.id
                if (idUsuario != null) {
                    viewModel.generarReporte(idUsuario)
                }
            }
        )
        binding.rvEstudiantes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEstudiantes.adapter = estudiantesAdapter
        
        viewModel.estudiantes.observe(viewLifecycleOwner) { estudiantes ->
            estudiantesAdapter.submitList(estudiantes)
        }
        
        // RecyclerView de relaciones
        val relacionesAdapter = RelacionAdminAdapter(
            onEliminar = { relacion ->
                viewModel.eliminarRelacion(relacion.idDocente, relacion.idEstudiante)
            }
        )
        binding.rvRelaciones.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRelaciones.adapter = relacionesAdapter
        
        viewModel.relaciones.observe(viewLifecycleOwner) { relaciones ->
            relacionesAdapter.submitList(relaciones)
        }
    }
    
    private fun setupObservers() {
        viewModel.reporteGenerado.observe(viewLifecycleOwner) { result ->
            // ✅ FIX: Solo procesar si el resultado no es null (evita que se muestre automáticamente al volver)
            result?.let {
                it.onSuccess { filePath ->
                    // ✅ UNIFICADO: Usar el mismo diálogo que ProfileFragment
                    mostrarReporteEnPantalla(filePath)
                    // ✅ FIX: Limpiar el valor después de mostrar para evitar que se muestre de nuevo
                    viewModel.limpiarReporteGenerado()
                }.onFailure { exception ->
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Error al generar reporte: ${exception.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    // ✅ FIX: Limpiar el valor después de mostrar el error
                    viewModel.limpiarReporteGenerado()
                }
            }
        }
        
        viewModel.relacionEliminada.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                viewModel.cargarRelaciones()
                android.widget.Toast.makeText(
                    requireContext(),
                    "Relación eliminada exitosamente",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }.onFailure { exception ->
                android.widget.Toast.makeText(
                    requireContext(),
                    "Error al eliminar relación: ${exception.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        
        // Buscar docente (filtro local)
        binding.etBuscarDocente.setOnEditorActionListener { _, _, _ ->
            val busqueda = binding.etBuscarDocente.text.toString()
            if (busqueda.isNotEmpty()) {
                viewModel.buscarDocenteLocal(busqueda)
            } else {
                viewModel.cargarDocentes()
            }
            true
        }
        
        // Buscar estudiante
        binding.etBuscarEstudiante.setOnEditorActionListener { _, _, _ ->
            val busqueda = binding.etBuscarEstudiante.text.toString()
            if (busqueda.isNotEmpty()) {
                viewModel.buscarEstudiante(busqueda)
            } else {
                viewModel.cargarTodosEstudiantes()
            }
            true
        }
        
        // Buscar relación
        binding.etBuscarRelacion.setOnEditorActionListener { _, _, _ ->
            val busqueda = binding.etBuscarRelacion.text.toString()
            if (busqueda.isNotEmpty()) {
                viewModel.buscarRelacion(busqueda)
            } else {
                viewModel.cargarRelaciones()
            }
            true
        }
    }
    
    /**
     * ✅ UNIFICADO: Muestra el reporte usando el mismo diálogo que ProfileFragment
     */
    private fun mostrarReporteEnPantalla(filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "El archivo del reporte no existe",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return
            }
            
            // Usar el mismo ReporteDialogFragment que ProfileFragment
            val dialog = ReporteDialogFragment.newInstance(filePath)
            dialog.show(parentFragmentManager, "ReporteDialogAdmin")
            
        } catch (e: Exception) {
            android.util.Log.e("AdminFragment", "Error al mostrar reporte", e)
            android.widget.Toast.makeText(
                requireContext(),
                "Error al mostrar reporte: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

