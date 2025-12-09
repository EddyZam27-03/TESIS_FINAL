package com.example.ensenando.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ensenando.databinding.FragmentAdminBinding
import com.example.ensenando.ui.profile.ReporteDialogFragment
import kotlinx.coroutines.launch

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
        // NO cargar relaciones al inicio - solo cuando se busque
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
                // Ya no se usa - el reporte se ve con btnVerReporte
            },
            onResetClick = { estudiante ->
                // Resetear actividad del estudiante
                estudiante.id_usuario?.let { idUsuario ->
                    lifecycleScope.launch {
                        viewModel.mostrarDialogoReset(idUsuario)
                    }
                }
            },
            onVerReporte = { estudiante ->
                estudiante.id_usuario?.let { viewModel.generarReporte(it) }
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
            // Ocultar RecyclerView si no hay resultados
            if (relaciones.isEmpty()) {
                binding.rvRelaciones.visibility = View.GONE
            } else {
                binding.rvRelaciones.visibility = View.VISIBLE
            }
        }
        
        // Actualizar nombres cuando se carguen
        viewModel.nombresUsuarios.observe(viewLifecycleOwner) { nombres ->
            relacionesAdapter.updateNombresUsuarios(nombres)
        }
    }
    
    private fun setupObservers() {
        viewModel.reporteGenerado.observe(viewLifecycleOwner) { result ->
            result.onSuccess { filePath ->
                // Mostrar primero el reporte en pantalla y luego permitir descarga desde el diálogo
                val dialog = ReporteDialogFragment.newInstance(filePath)
                dialog.show(parentFragmentManager, "ReporteDialogAdmin")
            }.onFailure { exception ->
                android.widget.Toast.makeText(
                    requireContext(),
                    "Error al generar reporte: ${exception.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        viewModel.relacionEliminada.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                // Recargar relaciones solo si hay una búsqueda activa
                val busqueda = binding.etBuscarRelacion.text.toString()
                if (busqueda.isNotEmpty()) {
                    viewModel.buscarRelacion(busqueda)
                }
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
        binding.etBuscarDocente.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val busqueda = s?.toString() ?: ""
                if (busqueda.isNotEmpty()) {
                    viewModel.buscarDocenteLocal(busqueda)
                } else {
                    viewModel.cargarDocentes()
                }
            }
        })
        
        // Buscar estudiante (filtro en tiempo real)
        binding.etBuscarEstudiante.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val busqueda = s?.toString() ?: ""
                if (busqueda.isNotEmpty()) {
                    viewModel.buscarEstudiante(busqueda)
                } else {
                    viewModel.cargarTodosEstudiantes()
                }
            }
        })
        
        // Buscar relaciones (filtro en tiempo real) - SOLO mostrar cuando se busque
        binding.etBuscarRelacion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val busqueda = s?.toString() ?: ""
                if (busqueda.isNotEmpty()) {
                    // Cargar todas las relaciones primero si no están cargadas
                    viewModel.cargarRelaciones()
                    // Luego buscar
                    viewModel.buscarRelacion(busqueda)
                } else {
                    // Si está vacío, ocultar resultados
                    viewModel.limpiarRelaciones()
                }
            }
        })
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

