package com.example.ensenando.ui.logros

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ensenando.databinding.FragmentLogrosBinding
import kotlinx.coroutines.launch

class LogrosFragment : Fragment() {
    private var _binding: FragmentLogrosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LogrosViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogrosBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupRecyclerView() {
        val adapter = LogrosAdapter()
        binding.rvLogros.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLogros.adapter = adapter
        
        viewModel.logros.observe(viewLifecycleOwner) { logros ->
            adapter.submitList(logros)
            binding.tvEmptyLogros.visibility = if (logros.isEmpty()) View.VISIBLE else View.GONE
        }
    }
    
    private fun setupObservers() {
        viewModel.totalLogros.observe(viewLifecycleOwner) { total ->
            binding.tvTotalLogros.text = "Total: $total logros desbloqueados"
        }
        
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // Mostrar/ocultar loading indicator si existe
            if (isLoading) {
                binding.fabRefresh.isEnabled = false
            } else {
                binding.fabRefresh.isEnabled = true
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        
        binding.fabRefresh.setOnClickListener {
            viewModel.refreshLogros()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

