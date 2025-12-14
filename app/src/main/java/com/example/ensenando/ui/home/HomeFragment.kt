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
        
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupObservers() {
        // ✅ OPTIMIZADO: Adapter simple solo con botones de módulos (sin RecyclerViews anidados)
        val adapter = ModuloButtonAdapter(
            onModuloClick = { moduloNombre ->
                // Navegar a fragment de submódulos
                    findNavController().navigate(
                    com.example.ensenando.R.id.submodulosFragment,
                    Bundle().apply { putString("moduloNombre", moduloNombre) }
                    )
                }
            )
            binding.rvModulos.layoutManager = LinearLayoutManager(requireContext())
            binding.rvModulos.adapter = adapter
        
        viewModel.modulosPrincipales.observe(viewLifecycleOwner) { modulos ->
            // Convertir a lista simple de nombres de módulos
            val nombresModulos = modulos.map { it.nombre }
            adapter.submitList(nombresModulos)
        }
        
        viewModel.progreso.observe(viewLifecycleOwner) { progreso ->
            binding.tvTotalGestos.text = progreso.totalGestos.toString()
            binding.tvGestosAprendidos.text = progreso.gestosAprendidos.toString()
            binding.tvPromedio.text = "${progreso.promedioProgreso.toInt()}%"
        }
        
        // ✅ NUEVO: Logros recientes (opcional - solo si existe en layout)
        // Nota: Estos elementos pueden no existir en el layout actual
        viewModel.logrosRecientes.observe(viewLifecycleOwner) { logros ->
            // TODO: Implementar cuando se agreguen estos elementos al layout
            // val cardLogros = binding.root.findViewById<ViewGroup>(R.id.cardLogrosRecientes)
            // cardLogros?.visibility = if (logros.isNotEmpty()) ViewGroup.VISIBLE else ViewGroup.GONE
        }
        
        // ✅ NUEVO: Notificaciones pendientes (opcional - solo si existe en layout)
        // Nota: Estos elementos pueden no existir en el layout actual
        viewModel.notificacionesPendientes.observe(viewLifecycleOwner) { count ->
            // TODO: Implementar cuando se agreguen estos elementos al layout
            // val cardNotif = binding.root.findViewById<ViewGroup>(R.id.cardNotificaciones)
            // val tvCount = binding.root.findViewById<TextView>(R.id.tvNotificacionesCount)
            // cardNotif?.visibility = if (count > 0) ViewGroup.VISIBLE else ViewGroup.GONE
            // tvCount?.text = count.toString()
        }
        
        // ✅ NUEVO: Estado de conexión (opcional - solo si existe en layout)
        // Nota: Estos elementos pueden no existir en el layout actual
        viewModel.isOnline.observe(viewLifecycleOwner) { isOnline ->
            // TODO: Implementar cuando se agreguen estos elementos al layout
            // val ivStatus = binding.root.findViewById<ImageView>(R.id.ivConnectionStatus)
            // val tvStatus = binding.root.findViewById<TextView>(R.id.tvConnectionStatus)
            // ivStatus?.setImageResource(if (isOnline) android.R.drawable.presence_online else android.R.drawable.presence_offline)
            // tvStatus?.text = if (isOnline) "Online" else "Offline"
        }
    }
    
    private fun setupClickListeners() {
        // ✅ NUEVO: Click listeners para elementos opcionales
        // Nota: Estos elementos pueden no existir en el layout actual
        // TODO: Implementar cuando se agreguen estos elementos al layout
        
        // Click en notificaciones
        // val cardNotif = binding.root.findViewById<ViewGroup>(R.id.cardNotificaciones)
        // cardNotif?.setOnClickListener {
        //     findNavController().navigate(R.id.profileFragment)
        // }
        
        // Botones de acceso rápido
        // val btnGestos = binding.root.findViewById<View>(R.id.btnGestos)
        // btnGestos?.setOnClickListener {
        //     binding.rvModulos.smoothScrollToPosition(0)
        // }
        
        // val btnLogros = binding.root.findViewById<View>(R.id.btnLogros)
        // btnLogros?.setOnClickListener {
        //     findNavController().navigate(R.id.logrosFragment)
        // }
        
        // val btnReportes = binding.root.findViewById<View>(R.id.btnReportes)
        // btnReportes?.setOnClickListener {
        //     android.widget.Toast.makeText(requireContext(), "Reportes próximamente", android.widget.Toast.LENGTH_SHORT).show()
        // }
        
        // val btnPerfil = binding.root.findViewById<View>(R.id.btnPerfil)
        // btnPerfil?.setOnClickListener {
        //     findNavController().navigate(R.id.profileFragment)
        // }
        
        // val btnConfiguracion = binding.root.findViewById<View>(R.id.btnConfiguracion)
        // btnConfiguracion?.setOnClickListener {
        //     android.widget.Toast.makeText(requireContext(), "Configuración próximamente", android.widget.Toast.LENGTH_SHORT).show()
        // }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


