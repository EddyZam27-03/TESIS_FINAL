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
    private val onGestoClick: (Int) -> Unit
) : ListAdapter<HomeViewModel.ModuloPrincipal, ModuloAdapter.ModuloViewHolder>(
    ModuloDiffCallback()
) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuloViewHolder {
        val binding = ItemModuloBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ModuloViewHolder(binding, onGestoClick)
    }
    
    override fun onBindViewHolder(holder: ModuloViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ModuloViewHolder(
        private val binding: ItemModuloBinding,
        private val onGestoClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(modulo: HomeViewModel.ModuloPrincipal) {
            // Mostrar nombre del módulo
            binding.tvModuloNombre.text = modulo.nombre
            
            // Configurar adapter de SUBMÓDULOS (no gestos directos)
            val submoduloAdapter = SubmoduloAdapter(onGestoClick)
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
    private val onGestoClick: (Int) -> Unit
) : ListAdapter<HomeViewModel.Submodulo, SubmoduloAdapter.SubmoduloViewHolder>(
    SubmoduloDiffCallback()
) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubmoduloViewHolder {
        val binding = com.example.ensenando.databinding.ItemSubmoduloBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SubmoduloViewHolder(binding, onGestoClick)
    }
    
    override fun onBindViewHolder(holder: SubmoduloViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class SubmoduloViewHolder(
        private val binding: com.example.ensenando.databinding.ItemSubmoduloBinding,
        private val onGestoClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(submodulo: HomeViewModel.Submodulo) {
            // Mostrar nombre del submódulo
            binding.tvSubmoduloNombre.text = submodulo.nombre
            
            // Configurar adapter de GESTOS
            val gestoAdapter = GestoAdapter(onGestoClick)
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
 */
class GestoAdapter(
    private val onGestoClick: (Int) -> Unit
) : ListAdapter<com.example.ensenando.data.local.entity.GestoEntity, GestoAdapter.GestoViewHolder>(
    GestoDiffCallback()
) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GestoViewHolder {
        val binding = com.example.ensenando.databinding.ItemGestoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GestoViewHolder(binding, onGestoClick)
    }
    
    override fun onBindViewHolder(holder: GestoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class GestoViewHolder(
        private val binding: com.example.ensenando.databinding.ItemGestoBinding,
        private val onGestoClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(gesto: com.example.ensenando.data.local.entity.GestoEntity) {
            binding.tvGestoNombre.text = gesto.nombre
            
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
