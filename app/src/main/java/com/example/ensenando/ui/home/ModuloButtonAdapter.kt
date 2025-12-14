package com.example.ensenando.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ensenando.databinding.ItemModuloButtonBinding

/**
 * ✅ OPTIMIZADO: Adapter simple para mostrar solo botones de módulos
 * Evita RecyclerViews anidados y mejora el rendimiento
 */
class ModuloButtonAdapter(
    private val onModuloClick: (String) -> Unit
) : ListAdapter<String, ModuloButtonAdapter.ModuloButtonViewHolder>(ModuloButtonDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuloButtonViewHolder {
        val binding = ItemModuloButtonBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ModuloButtonViewHolder(binding, onModuloClick)
    }
    
    override fun onBindViewHolder(holder: ModuloButtonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ModuloButtonViewHolder(
        private val binding: ItemModuloButtonBinding,
        private val onModuloClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(moduloNombre: String) {
            binding.btnModulo.text = moduloNombre
            binding.btnModulo.setOnClickListener {
                onModuloClick(moduloNombre)
            }
        }
    }
    
    class ModuloButtonDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
        
        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}

