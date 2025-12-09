package com.example.ensenando.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ensenando.data.local.entity.DocenteEstudianteEntity
import com.example.ensenando.databinding.ItemRelacionAdminBinding

class RelacionAdminAdapter(
    private val onEliminar: (DocenteEstudianteEntity) -> Unit
) : ListAdapter<DocenteEstudianteEntity, RelacionAdminAdapter.RelacionViewHolder>(RelacionDiffCallback()) {
    
    private var nombresUsuarios: Map<Int, String> = emptyMap()
    
    fun updateNombresUsuarios(nombres: Map<Int, String>) {
        nombresUsuarios = nombres
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelacionViewHolder {
        val binding = ItemRelacionAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RelacionViewHolder(binding, onEliminar, nombresUsuarios)
    }
    
    override fun onBindViewHolder(holder: RelacionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class RelacionViewHolder(
        private val binding: ItemRelacionAdminBinding,
        private val onEliminar: (DocenteEstudianteEntity) -> Unit,
        private var nombresUsuarios: Map<Int, String>
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(relacion: DocenteEstudianteEntity) {
            // Obtener nombres reales de docente y estudiante
            val nombreDocente = nombresUsuarios[relacion.idDocente] 
                ?: "Docente ID: ${relacion.idDocente}"
            val nombreEstudiante = nombresUsuarios[relacion.idEstudiante] 
                ?: "Estudiante ID: ${relacion.idEstudiante}"
            
            binding.tvDocenteNombre.text = "Docente: $nombreDocente"
            binding.tvEstudianteNombre.text = "Estudiante: $nombreEstudiante"
            
            // Mostrar estado con color
            when (relacion.estado.lowercase()) {
                "pendiente" -> {
                    binding.tvEstado.text = "Estado: PENDIENTE"
                    binding.tvEstado.setTextColor(android.graphics.Color.parseColor("#FF9800")) // Naranja
                }
                "aceptado" -> {
                    binding.tvEstado.text = "Estado: ACEPTADO"
                    binding.tvEstado.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // Verde
                }
                "rechazado" -> {
                    binding.tvEstado.text = "Estado: RECHAZADO"
                    binding.tvEstado.setTextColor(android.graphics.Color.parseColor("#F44336")) // Rojo
                }
                else -> {
                    binding.tvEstado.text = "Estado: ${relacion.estado}"
                }
            }
            
            binding.btnEliminar.setOnClickListener {
                onEliminar(relacion)
            }
        }
    }
    
    class RelacionDiffCallback : DiffUtil.ItemCallback<DocenteEstudianteEntity>() {
        override fun areItemsTheSame(oldItem: DocenteEstudianteEntity, newItem: DocenteEstudianteEntity): Boolean {
            return oldItem.idDocente == newItem.idDocente && oldItem.idEstudiante == newItem.idEstudiante
        }
        
        override fun areContentsTheSame(oldItem: DocenteEstudianteEntity, newItem: DocenteEstudianteEntity): Boolean {
            return oldItem == newItem
        }
    }
}

