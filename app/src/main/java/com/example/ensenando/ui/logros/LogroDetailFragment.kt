package com.example.ensenando.ui.logros

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.ensenando.databinding.FragmentLogroDetailBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class LogroDetailFragment : Fragment() {
    private var _binding: FragmentLogroDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LogroDetailViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogroDetailBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val idLogro = arguments?.getInt("idLogro") ?: 0
        if (idLogro == 0) {
            android.widget.Toast.makeText(
                requireContext(),
                "ID de logro inválido",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            requireActivity().onBackPressedDispatcher.onBackPressed()
            return
        }
        
        setupObservers()
        setupClickListeners()
        viewModel.loadLogro(idLogro)
    }
    
    private fun setupObservers() {
        viewModel.logro.observe(viewLifecycleOwner) { logro ->
            logro?.let {
                binding.tvLogroTitulo.text = it.titulo ?: it.nombre ?: "Logro"
                binding.tvLogroDescripcion.text = it.descripcion ?: ""
                
                // Icono
                if (it.desbloqueado == true) {
                    binding.ivLogroIconGrande.setImageResource(android.R.drawable.star_big_on)
                } else {
                    binding.ivLogroIconGrande.setImageResource(android.R.drawable.star_big_off)
                    binding.ivLogroIconGrande.alpha = 0.5f
                }
                
                // Categoría (si existe en el modelo)
                // TODO: Agregar categoría al modelo si está disponible
                
                // Fecha
                val fecha = it.fecha_obtenido ?: it.fechaDesbloqueo
                if (fecha != null) {
                    try {
                        val formato = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val fechaObj = formato.parse(fecha)
                        val formatoSalida = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        binding.tvFechaObtenido.text = "Obtenido: ${formatoSalida.format(fechaObj)}"
                        binding.tvFechaObtenido.visibility = ViewGroup.VISIBLE
                    } catch (e: Exception) {
                        binding.tvFechaObtenido.text = "Obtenido: $fecha"
                        binding.tvFechaObtenido.visibility = ViewGroup.VISIBLE
                    }
                } else {
                    binding.tvFechaObtenido.visibility = ViewGroup.GONE
                }
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        
        binding.btnCompartir.setOnClickListener {
            val logro = viewModel.logro.value
            if (logro != null) {
                val titulo = logro.titulo ?: logro.nombre ?: "Logro"
                val shareText = "¡He obtenido el logro \"$titulo\" en Ensenando! 🎉"
                
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                startActivity(Intent.createChooser(intent, "Compartir logro"))
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
