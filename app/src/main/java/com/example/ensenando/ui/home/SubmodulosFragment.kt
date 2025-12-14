package com.example.ensenando.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ensenando.databinding.FragmentSubmodulosBinding

/**
 * ✅ OPTIMIZADO: Fragment para mostrar submódulos de un módulo seleccionado
 * Evita RecyclerViews anidados y mejora el rendimiento
 */
class SubmodulosFragment : Fragment() {
    private var _binding: FragmentSubmodulosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SubmodulosViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubmodulosBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val moduloNombre = arguments?.getString("moduloNombre") ?: ""
        
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        // ✅ FIX: Esperar a que la vista esté completamente renderizada
        binding.root.post {
            android.util.Log.d("SubmodulosFragment", "Vista lista, cargando submódulos...")
            viewModel.cargarSubmodulos(moduloNombre)
        }
    }
    
    override fun onResume() {
        super.onResume()
        android.util.Log.d("SubmodulosFragment", "onResume() - Asegurando que los submódulos se muestren")
    }
    
    private fun setupRecyclerView() {
        val adapter = SubmoduloButtonAdapter(
            onSubmoduloClick = { submoduloNombre ->
                val moduloNombre = arguments?.getString("moduloNombre") ?: ""
                findNavController().navigate(
                    com.example.ensenando.R.id.gestosFragment,
                    Bundle().apply {
                        putString("moduloNombre", moduloNombre)
                        putString("submoduloNombre", submoduloNombre)
                    }
                )
            }
        )
        binding.rvSubmodulos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSubmodulos.adapter = adapter
        
        viewModel.submodulos.observe(viewLifecycleOwner) { submodulos ->
            val nombresSubmodulos = submodulos.map { it.nombre }
            adapter.submitList(nombresSubmodulos)
        }
    }
    
    private fun setupObservers() {
        viewModel.moduloNombre.observe(viewLifecycleOwner) { nombre ->
            binding.toolbar.title = nombre
        }
    }
    
    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

