package com.example.ensenando.ui.logros

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ensenando.data.remote.model.LogrosResponse
import com.example.ensenando.databinding.ItemLogroBinding
import java.text.SimpleDateFormat
import java.util.*

class LogrosAdapter : ListAdapter<LogrosResponse, LogrosAdapter.LogroViewHolder>(LogroDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogroViewHolder {
        val binding = ItemLogroBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogroViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: LogroViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class LogroViewHolder(
        private val binding: ItemLogroBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(logro: LogrosResponse) {
            // Usar titulo si existe, sino nombre
            val titulo = logro.titulo ?: logro.nombre ?: "Logro"
            binding.tvLogroNombre.text = titulo
            
            // Usar descripcion si existe
            val descripcion = logro.descripcion ?: ""
            binding.tvLogroDescripcion.text = descripcion
            
            // Formatear fecha si existe
            val fecha = logro.fecha_obtenido ?: logro.fechaDesbloqueo
            if (fecha != null) {
                try {
                    val formato = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val fechaObj = formato.parse(fecha)
                    val formatoSalida = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    binding.tvFechaDesbloqueo.text = "Obtenido: ${formatoSalida.format(fechaObj)}"
                    binding.tvFechaDesbloqueo.visibility = ViewGroup.VISIBLE
                } catch (e: Exception) {
                    binding.tvFechaDesbloqueo.text = "Obtenido: $fecha"
                    binding.tvFechaDesbloqueo.visibility = ViewGroup.VISIBLE
                }
            } else {
                binding.tvFechaDesbloqueo.visibility = ViewGroup.GONE
            }
            
            // Mostrar como desbloqueado si tiene fecha
            val desbloqueado = logro.desbloqueado ?: (fecha != null)
            if (desbloqueado) {
                binding.root.alpha = 1.0f
                binding.ivLogroIcon.setImageResource(android.R.drawable.star_big_on)
                binding.ivDesbloqueado.visibility = ViewGroup.VISIBLE
            } else {
                binding.root.alpha = 0.5f
                binding.ivLogroIcon.setImageResource(android.R.drawable.star_big_off)
                binding.ivDesbloqueado.visibility = ViewGroup.GONE
            }
            
            // Mostrar porcentaje si existe
            val porcentaje = logro.porcentajeAvance ?: 0
            binding.progressLogro.progress = porcentaje
            binding.tvPorcentajeLogro.text = "$porcentaje%"
        }
    }
    
    class LogroDiffCallback : DiffUtil.ItemCallback<LogrosResponse>() {
        override fun areItemsTheSame(oldItem: LogrosResponse, newItem: LogrosResponse): Boolean {
            return (oldItem.id_logro ?: oldItem.id) == (newItem.id_logro ?: newItem.id)
        }
        
        override fun areContentsTheSame(oldItem: LogrosResponse, newItem: LogrosResponse): Boolean {
            return oldItem == newItem
        }
    }
}
