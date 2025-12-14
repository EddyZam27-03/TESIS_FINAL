package com.example.ensenando.ui.activity

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ensenando.data.local.entity.HistorialIntentoEntity
import com.example.ensenando.databinding.ItemHistorialIntentoBinding
import java.text.SimpleDateFormat
import java.util.*

class HistorialIntentoAdapter : ListAdapter<HistorialIntentoEntity, HistorialIntentoAdapter.HistorialViewHolder>(
    HistorialDiffCallback()
) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistorialViewHolder {
        val binding = ItemHistorialIntentoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HistorialViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: HistorialViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class HistorialViewHolder(
        private val binding: ItemHistorialIntentoBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        private val displayFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        
        fun bind(intento: HistorialIntentoEntity) {
            // Formatear fecha
            try {
                val fecha = dateFormat.parse(intento.fecha_intento)
                if (fecha != null) {
                    binding.tvFechaIntento.text = displayFormat.format(fecha)
                } else {
                    binding.tvFechaIntento.text = intento.fecha_intento
                }
            } catch (e: Exception) {
                binding.tvFechaIntento.text = intento.fecha_intento
            }
            
            // Mostrar porcentaje
            binding.tvPorcentajeObtenido.text = "${intento.porcentaje_obtenido}%"
            
            // Icono según éxito (≥80% = éxito)
            if (intento.porcentaje_obtenido >= 80) {
                binding.ivEstado.setImageResource(android.R.drawable.checkbox_on_background)
                binding.ivEstado.setColorFilter(
                    binding.root.context.getColor(android.R.color.holo_green_dark)
                )
            } else {
                binding.ivEstado.setImageResource(android.R.drawable.checkbox_off_background)
                binding.ivEstado.setColorFilter(
                    binding.root.context.getColor(android.R.color.darker_gray)
                )
            }
        }
    }
    
    class HistorialDiffCallback : DiffUtil.ItemCallback<HistorialIntentoEntity>() {
        override fun areItemsTheSame(
            oldItem: HistorialIntentoEntity,
            newItem: HistorialIntentoEntity
        ): Boolean {
            return oldItem.id_historial == newItem.id_historial
        }
        
        override fun areContentsTheSame(
            oldItem: HistorialIntentoEntity,
            newItem: HistorialIntentoEntity
        ): Boolean {
            return oldItem == newItem
        }
    }
}
