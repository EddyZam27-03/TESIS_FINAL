package com.example.ensenando.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ensenando.databinding.ItemSubmoduloButtonBinding

/**
 * ✅ OPTIMIZADO: Adapter simple para mostrar botones de submódulos
 */
class SubmoduloButtonAdapter(
    private val onSubmoduloClick: (String) -> Unit
) : ListAdapter<String, SubmoduloButtonAdapter.SubmoduloButtonViewHolder>(SubmoduloButtonDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubmoduloButtonViewHolder {
        val binding = ItemSubmoduloButtonBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SubmoduloButtonViewHolder(binding, onSubmoduloClick)
    }
    
    override fun onBindViewHolder(holder: SubmoduloButtonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class SubmoduloButtonViewHolder(
        private val binding: ItemSubmoduloButtonBinding,
        private val onSubmoduloClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(submoduloNombre: String) {
            binding.btnSubmodulo.text = submoduloNombre
            binding.btnSubmodulo.setOnClickListener {
                onSubmoduloClick(submoduloNombre)
            }
        }
    }
    
    class SubmoduloButtonDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
        
        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}

