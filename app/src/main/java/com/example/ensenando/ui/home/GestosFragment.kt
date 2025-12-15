package com.example.ensenando.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ensenando.databinding.FragmentGestosBinding

/**
 * ✅ OPTIMIZADO: Fragment para mostrar gestos de un submódulo seleccionado
 * Solo carga los gestos del submódulo específico, mejorando el rendimiento
 */
class GestosFragment : Fragment() {
    private var _binding: FragmentGestosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GestosViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGestosBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val moduloNombre = arguments?.getString("moduloNombre") ?: ""
        val submoduloNombre = arguments?.getString("submoduloNombre") ?: ""
        
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        // ✅ FIX: Esperar a que la vista esté completamente renderizada
        binding.root.post {
            android.util.Log.d("GestosFragment", "Vista lista, cargando gestos...")
            viewModel.cargarGestos(moduloNombre, submoduloNombre)
        }
    }
    
    override fun onResume() {
        super.onResume()
        android.util.Log.d("GestosFragment", "onResume() - Asegurando que los gestos se muestren")
        
        // ✅ FIX: Forzar actualización del RecyclerView si hay datos
        adapter?.let {
            val gestos = viewModel.gestos.value
            val progresoMap = viewModel.progresoMap.value
            
            if (gestos != null && gestos.isNotEmpty()) {
                android.util.Log.d("GestosFragment", "onResume() - Re-enviando ${gestos.size} gestos al adapter")
                binding.rvGestos.post {
                    it.submitList(gestos)
                    if (progresoMap != null) {
                        it.updateProgresoMap(progresoMap)
                    }
                }
            }
        }
    }
    
    private var adapter: GestoAdapter? = null
    
    private fun setupRecyclerView() {
        // ✅ FIX: Crear adapter una sola vez, no recrearlo cada vez
        adapter = GestoAdapter(
            onGestoClick = { idGesto ->
                findNavController().navigate(
                    com.example.ensenando.R.id.activityFragment,
                    Bundle().apply { putInt("idGesto", idGesto) }
                )
            },
            onPracticarClick = { idGesto ->
                // ✅ FIX: Ir primero a la pantalla de referencia del video (ActivityFragment)
                findNavController().navigate(
                    com.example.ensenando.R.id.activityFragment,
                    Bundle().apply { putInt("idGesto", idGesto) }
                )
            },
            progresoMap = emptyMap() // Se actualizará después
        )
        binding.rvGestos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGestos.adapter = adapter
        
        // ✅ FIX: Observar gestos PRIMERO y luego progresoMap
        viewModel.gestos.observe(viewLifecycleOwner) { gestos ->
            android.util.Log.d("GestosFragment", "Gestos recibidos: ${gestos.size}")
            if (gestos.isNotEmpty()) {
                // ✅ FIX: Usar post para asegurar que la vista esté lista
                binding.rvGestos.post {
                    adapter?.submitList(gestos)
                    android.util.Log.d("GestosFragment", "Lista enviada al adapter: ${gestos.size} gestos")
                    
                    // También actualizar progresoMap si ya está disponible
                    val progresoMap = viewModel.progresoMap.value
                    if (progresoMap != null && progresoMap.isNotEmpty()) {
                        adapter?.updateProgresoMap(progresoMap)
                    }
                }
            } else {
                android.util.Log.w("GestosFragment", "Lista de gestos vacía")
            }
        }
        
        // ✅ FIX: Observar progresoMap y actualizar el adapter existente
        viewModel.progresoMap.observe(viewLifecycleOwner) { progresoMap ->
            android.util.Log.d("GestosFragment", "ProgresoMap actualizado: ${progresoMap.size} items")
            // Solo actualizar si ya hay gestos cargados
            val gestos = viewModel.gestos.value
            if (gestos != null && gestos.isNotEmpty()) {
                binding.rvGestos.post {
                    adapter?.updateProgresoMap(progresoMap)
                }
            }
        }
    }
    
    private fun setupObservers() {
        viewModel.titulo.observe(viewLifecycleOwner) { titulo ->
            binding.toolbar.title = titulo
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

