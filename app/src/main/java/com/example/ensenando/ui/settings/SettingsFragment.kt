package com.example.ensenando.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.ensenando.databinding.FragmentSettingsBinding
import com.example.ensenando.util.NetworkUtils
import com.example.ensenando.util.ThemeUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupObservers()
        setupClickListeners()
        loadSettings()
    }
    
    private fun setupObservers() {
        // Observar tema
        viewModel.tema.observe(viewLifecycleOwner) { tema ->
            binding.switchDarkMode.isChecked = tema == "dark"
        }
        
        // Observar estado de sincronización
        viewModel.syncStatus.observe(viewLifecycleOwner) { status ->
            binding.tvSyncStatus.text = status
        }
        
        viewModel.lastSync.observe(viewLifecycleOwner) { lastSync ->
            if (lastSync.isNotEmpty()) {
                binding.tvLastSync.text = "Última sincronización: $lastSync"
            } else {
                binding.tvLastSync.text = "Nunca sincronizado"
            }
        }
        
        // Observar estado de conexión
        viewModel.isOnline.observe(viewLifecycleOwner) { isOnline ->
            if (isOnline) {
                binding.tvConnectionStatus.text = "Online"
                binding.ivConnectionStatus.setImageResource(android.R.drawable.presence_online)
            } else {
                binding.tvConnectionStatus.text = "Offline"
                binding.ivConnectionStatus.setImageResource(android.R.drawable.presence_offline)
            }
        }
        
        // Observar notificaciones
        viewModel.notificacionesLogros.observe(viewLifecycleOwner) { habilitado ->
            binding.switchNotificacionesLogros.isChecked = habilitado
        }
        
        viewModel.notificacionesSolicitudes.observe(viewLifecycleOwner) { habilitado ->
            binding.switchNotificacionesSolicitudes.isChecked = habilitado
        }
        
        viewModel.recordatorios.observe(viewLifecycleOwner) { habilitado ->
            binding.switchRecordatorios.isChecked = habilitado
        }
    }
    
    private fun setupClickListeners() {
        // Toggle tema oscuro
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                viewModel.cambiarTema(isChecked)
                ThemeUtils.aplicarTema(requireContext(), isChecked)
            }
        }
        
        // Sincronizar ahora
        binding.btnSyncNow.setOnClickListener {
            lifecycleScope.launch {
                viewModel.sincronizarAhora()
            }
        }
        
        // Notificaciones de logros
        binding.switchNotificacionesLogros.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                viewModel.guardarNotificacionesLogros(isChecked)
            }
        }
        
        // Notificaciones de solicitudes
        binding.switchNotificacionesSolicitudes.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                viewModel.guardarNotificacionesSolicitudes(isChecked)
            }
        }
        
        // Recordatorios
        binding.switchRecordatorios.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                viewModel.guardarRecordatorios(isChecked)
            }
        }
    }
    
    private fun loadSettings() {
        lifecycleScope.launch {
            viewModel.cargarConfiguracion()
            viewModel.actualizarEstadoConexion()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
