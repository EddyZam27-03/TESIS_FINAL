package com.example.ensenando.ui.docente

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ensenando.databinding.FragmentDocenteDashboardBinding

class DocenteDashboardFragment : Fragment() {
    private var _binding: FragmentDocenteDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DocenteViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDocenteDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerViews()
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupRecyclerViews() {
        // RecyclerView de estudiantes
        binding.rvEstudiantes.layoutManager = LinearLayoutManager(requireContext())
        // TODO: Crear EstudianteDocenteAdapter
        
        // RecyclerView de estudiantes rezagados
        binding.rvEstudiantesRezagados.layoutManager = LinearLayoutManager(requireContext())
        // TODO: Crear EstudianteDocenteAdapter
        
        // RecyclerView de progreso por categoría
        binding.rvProgresoCategoria.layoutManager = LinearLayoutManager(requireContext())
        // TODO: Crear ProgresoCategoriaAdapter
    }
    
    private fun setupObservers() {
        viewModel.estudiantesVinculados.observe(viewLifecycleOwner) { estudiantes ->
            // TODO: Actualizar adapter
            if (estudiantes.isEmpty()) {
                binding.rvEstudiantes.visibility = ViewGroup.GONE
            } else {
                binding.rvEstudiantes.visibility = ViewGroup.VISIBLE
            }
        }
        
        viewModel.estudiantesRezagados.observe(viewLifecycleOwner) { rezagados ->
            if (rezagados.isNotEmpty()) {
                binding.cardAlertas.visibility = ViewGroup.VISIBLE
                binding.tvCantidadRezagados.text = rezagados.size.toString()
                // TODO: Actualizar adapter
            } else {
                binding.cardAlertas.visibility = ViewGroup.GONE
            }
        }
        
        viewModel.progresoPorCategoria.observe(viewLifecycleOwner) { progreso ->
            // TODO: Actualizar adapter
        }
    }
    
    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        
        binding.btnGenerarReporte.setOnClickListener {
            // ✅ FIX: Navegar a ReportesFragment sin argumentos para evitar problemas de navegación
            findNavController().navigate(com.example.ensenando.R.id.reportesFragment)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
