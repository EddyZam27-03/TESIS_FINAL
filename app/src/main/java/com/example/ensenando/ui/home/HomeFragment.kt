package com.example.ensenando.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ensenando.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ✅ CORREGIDO: Usar adapter jerárquico (Módulo → Submódulos → Gestos)
        val adapter = ModuloAdapter { idGesto ->
            findNavController().navigate(
                com.example.ensenando.R.id.activityFragment,
                Bundle().apply { putInt("idGesto", idGesto) }
            )
        }
        
        binding.rvModulos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvModulos.adapter = adapter
        
        viewModel.modulosPrincipales.observe(viewLifecycleOwner) { modulos ->
            adapter.submitList(modulos)
        }
        
        viewModel.progreso.observe(viewLifecycleOwner) { progreso ->
            binding.tvTotalGestos.text = progreso.totalGestos.toString()
            binding.tvGestosAprendidos.text = progreso.gestosAprendidos.toString()
            binding.tvPromedio.text = "${progreso.promedioProgreso.toInt()}%"
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


