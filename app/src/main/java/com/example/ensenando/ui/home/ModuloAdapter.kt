package com.example.ensenando.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ensenando.databinding.ItemModuloBinding

/**
 * Adapter para mostrar MÓDULOS (BASICO, ACADEMICO, SOCIAL)
 * Cada módulo muestra sus SUBMÓDULOS
 */
class ModuloAdapter(
    private val onGestoClick: (Int) -> Unit,
    private val progresoMap: Map<Int, GestoAdapter.GestoProgreso> = emptyMap(),
    private val onPracticarClick: ((Int) -> Unit)? = null
) : ListAdapter<HomeViewModel.ModuloPrincipal, ModuloAdapter.ModuloViewHolder>(
    ModuloDiffCallback()
) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuloViewHolder {
        val binding = ItemModuloBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ModuloViewHolder(binding, onGestoClick, progresoMap, onPracticarClick)
    }
    
    override fun onBindViewHolder(holder: ModuloViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ModuloViewHolder(
        private val binding: ItemModuloBinding,
        private val onGestoClick: (Int) -> Unit,
        private val progresoMap: Map<Int, GestoAdapter.GestoProgreso> = emptyMap(),
        private val onPracticarClick: ((Int) -> Unit)? = null
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(modulo: HomeViewModel.ModuloPrincipal) {
            // Mostrar nombre del módulo
            binding.tvModuloNombre.text = modulo.nombre
            
            // Configurar adapter de SUBMÓDULOS (no gestos directos)
            val submoduloAdapter = SubmoduloAdapter(onGestoClick, progresoMap, onPracticarClick)
            binding.rvSubmodulos.adapter = submoduloAdapter
            binding.rvSubmodulos.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                binding.root.context
            )
            
            // Pasar la lista de submódulos al adapter
            submoduloAdapter.submitList(modulo.submodulos)
        }
    }
    
    class ModuloDiffCallback : DiffUtil.ItemCallback<HomeViewModel.ModuloPrincipal>() {
        override fun areItemsTheSame(
            oldItem: HomeViewModel.ModuloPrincipal,
            newItem: HomeViewModel.ModuloPrincipal
        ): Boolean {
            return oldItem.nombreCategoria == newItem.nombreCategoria
        }
        
        override fun areContentsTheSame(
            oldItem: HomeViewModel.ModuloPrincipal,
            newItem: HomeViewModel.ModuloPrincipal
        ): Boolean {
            return oldItem == newItem
        }
    }
}

/**
 * Adapter para mostrar SUBMÓDULOS dentro de un módulo
 * Cada submódulo muestra sus GESTOS
 */
class SubmoduloAdapter(
    private val onGestoClick: (Int) -> Unit,
    private val progresoMap: Map<Int, GestoAdapter.GestoProgreso> = emptyMap(),
    private val onPracticarClick: ((Int) -> Unit)? = null
) : ListAdapter<HomeViewModel.Submodulo, SubmoduloAdapter.SubmoduloViewHolder>(
    SubmoduloDiffCallback()
) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubmoduloViewHolder {
        val binding = com.example.ensenando.databinding.ItemSubmoduloBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SubmoduloViewHolder(binding, onGestoClick, progresoMap, onPracticarClick)
    }
    
    override fun onBindViewHolder(holder: SubmoduloViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class SubmoduloViewHolder(
        private val binding: com.example.ensenando.databinding.ItemSubmoduloBinding,
        private val onGestoClick: (Int) -> Unit,
        private val progresoMap: Map<Int, GestoAdapter.GestoProgreso> = emptyMap(),
        private val onPracticarClick: ((Int) -> Unit)? = null
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(submodulo: HomeViewModel.Submodulo) {
            // Mostrar nombre del submódulo
            binding.tvSubmoduloNombre.text = submodulo.nombre
            
            // Configurar adapter de GESTOS con progreso
            val gestoAdapter = GestoAdapter(onGestoClick, onPracticarClick, progresoMap)
            binding.rvGestos.adapter = gestoAdapter
            binding.rvGestos.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                binding.root.context
            )
            
            // Pasar la lista de gestos al adapter
            gestoAdapter.submitList(submodulo.gestos)
        }
    }
    
    class SubmoduloDiffCallback : DiffUtil.ItemCallback<HomeViewModel.Submodulo>() {
        override fun areItemsTheSame(
            oldItem: HomeViewModel.Submodulo,
            newItem: HomeViewModel.Submodulo
        ): Boolean {
            return oldItem.nombre == newItem.nombre
        }
        
        override fun areContentsTheSame(
            oldItem: HomeViewModel.Submodulo,
            newItem: HomeViewModel.Submodulo
        ): Boolean {
            return oldItem == newItem
        }
    }
}

/**
 * Adapter para mostrar GESTOS dentro de un submódulo
 * ✅ MEJORADO: Muestra dificultad, porcentaje, estado, categoría
 */
class GestoAdapter(
    private val onGestoClick: (Int) -> Unit,
    private val onPracticarClick: ((Int) -> Unit)? = null,
    private var progresoMap: Map<Int, GestoProgreso> = emptyMap()
) : ListAdapter<com.example.ensenando.data.local.entity.GestoEntity, GestoAdapter.GestoViewHolder>(
    GestoDiffCallback()
) {
    
    data class GestoProgreso(
        val porcentaje: Int,
        val estado: String
    )
    
    /**
     * ✅ FIX: Actualizar progresoMap sin recrear el adapter
     */
    fun updateProgresoMap(newProgresoMap: Map<Int, GestoProgreso>) {
        progresoMap = newProgresoMap
        // Notificar cambios para que se actualicen las vistas visibles
        notifyItemRangeChanged(0, itemCount)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GestoViewHolder {
        val binding = com.example.ensenando.databinding.ItemGestoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GestoViewHolder(binding, onGestoClick, onPracticarClick)
    }
    
    override fun onBindViewHolder(holder: GestoViewHolder, position: Int) {
        val gesto = getItem(position)
        val progreso = progresoMap[gesto.idGesto]
        holder.bind(gesto, progreso)
    }
    
    class GestoViewHolder(
        private val binding: com.example.ensenando.databinding.ItemGestoBinding,
        private val onGestoClick: (Int) -> Unit,
        private val onPracticarClick: ((Int) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(gesto: com.example.ensenando.data.local.entity.GestoEntity, progreso: GestoProgreso?) {
            // Nombre
            binding.tvGestoNombre.text = gesto.nombre
            
            // Categoría
            if (gesto.categoria != null) {
                val partes = gesto.categoria.split(" - ")
                if (partes.size >= 2) {
                    binding.tvCategoria.text = "${partes[0]} > ${partes[1]}"
                    binding.tvCategoria.visibility = ViewGroup.VISIBLE
                } else {
                    binding.tvCategoria.text = gesto.categoria
                    binding.tvCategoria.visibility = ViewGroup.VISIBLE
                }
            } else {
                binding.tvCategoria.visibility = ViewGroup.GONE
            }
            
            // Dificultad
            if (gesto.dificultad != null) {
                binding.chipDificultad.text = gesto.dificultad
                binding.chipDificultad.visibility = ViewGroup.VISIBLE
                // Colores según dificultad
                when (gesto.dificultad.uppercase()) {
                    "FÁCIL", "FACIL" -> {
                        binding.chipDificultad.setChipBackgroundColorResource(android.R.color.holo_green_light)
                    }
                    "MEDIO", "MEDIA" -> {
                        binding.chipDificultad.setChipBackgroundColorResource(android.R.color.holo_orange_light)
                    }
                    "DIFÍCIL", "DIFICIL" -> {
                        binding.chipDificultad.setChipBackgroundColorResource(android.R.color.holo_red_light)
                    }
                }
            } else {
                binding.chipDificultad.visibility = ViewGroup.GONE
            }
            
            // Progreso y Estado
            if (progreso != null) {
                binding.progressGesto.progress = progreso.porcentaje
                binding.tvPorcentaje.text = "${progreso.porcentaje}%"
                binding.tvPorcentaje.visibility = ViewGroup.VISIBLE
                binding.progressGesto.visibility = ViewGroup.VISIBLE
                
                // Estado
                binding.chipEstado.text = progreso.estado.capitalize()
                binding.chipEstado.visibility = ViewGroup.VISIBLE
                if (progreso.estado == "aprendido") {
                    binding.chipEstado.setChipBackgroundColorResource(android.R.color.holo_green_light)
                } else {
                    binding.chipEstado.setChipBackgroundColorResource(android.R.color.darker_gray)
                }
            } else {
                binding.progressGesto.progress = 0
                binding.tvPorcentaje.text = "0%"
                binding.tvPorcentaje.visibility = ViewGroup.VISIBLE
                binding.progressGesto.visibility = ViewGroup.VISIBLE
                binding.chipEstado.text = "Pendiente"
                binding.chipEstado.visibility = ViewGroup.VISIBLE
                binding.chipEstado.setChipBackgroundColorResource(android.R.color.darker_gray)
            }
            
            // Botón Practicar
            if (onPracticarClick != null) {
                binding.btnPracticar.visibility = ViewGroup.VISIBLE
                binding.btnPracticar.setOnClickListener {
                    onPracticarClick?.invoke(gesto.idGesto)
                }
            } else {
                binding.btnPracticar.visibility = ViewGroup.GONE
            }
            
            // Click en card
            binding.root.setOnClickListener {
                onGestoClick(gesto.idGesto)
            }
        }
    }
    
    class GestoDiffCallback : DiffUtil.ItemCallback<com.example.ensenando.data.local.entity.GestoEntity>() {
        override fun areItemsTheSame(
            oldItem: com.example.ensenando.data.local.entity.GestoEntity,
            newItem: com.example.ensenando.data.local.entity.GestoEntity
        ): Boolean {
            return oldItem.idGesto == newItem.idGesto
        }
        
        override fun areContentsTheSame(
            oldItem: com.example.ensenando.data.local.entity.GestoEntity,
            newItem: com.example.ensenando.data.local.entity.GestoEntity
        ): Boolean {
            return oldItem == newItem
        }
    }
}
