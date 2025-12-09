package com.example.ensenando.ui.buscar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ensenando.data.remote.model.UsuarioResponse
import com.example.ensenando.databinding.ItemDocenteBinding

class DocenteAdapter(
    private val onDocenteClick: (UsuarioResponse) -> Unit
) : ListAdapter<UsuarioResponse, DocenteAdapter.DocenteViewHolder>(DocenteDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocenteViewHolder {
        val binding = ItemDocenteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DocenteViewHolder(binding, onDocenteClick)
    }
    
    override fun onBindViewHolder(holder: DocenteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class DocenteViewHolder(
        private val binding: ItemDocenteBinding,
        private val onDocenteClick: (UsuarioResponse) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(docente: UsuarioResponse) {
            binding.tvDocenteNombre.text = docente.nombre
            binding.tvDocenteCorreo.text = docente.correo
            
            binding.btnEnviarSolicitud.setOnClickListener {
                onDocenteClick(docente)
            }
        }
    }
    
    class DocenteDiffCallback : DiffUtil.ItemCallback<UsuarioResponse>() {
        override fun areItemsTheSame(oldItem: UsuarioResponse, newItem: UsuarioResponse): Boolean {
            return oldItem.id_usuario == newItem.id_usuario
        }
        
        override fun areContentsTheSame(oldItem: UsuarioResponse, newItem: UsuarioResponse): Boolean {
            return oldItem == newItem
        }
    }
}

