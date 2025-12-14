package com.example.ensenando.ui.reportes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ensenando.databinding.ItemDatoReporteBinding

class DatoReporteAdapter : ListAdapter<ReportesViewModel.DatoReporte, DatoReporteAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDatoReporteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(
        private val binding: ItemDatoReporteBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(dato: ReportesViewModel.DatoReporte) {
            binding.tvTitulo.text = dato.titulo
            binding.tvValor.text = dato.valor
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<ReportesViewModel.DatoReporte>() {
        override fun areItemsTheSame(
            oldItem: ReportesViewModel.DatoReporte,
            newItem: ReportesViewModel.DatoReporte
        ): Boolean {
            return oldItem.titulo == newItem.titulo
        }
        
        override fun areContentsTheSame(
            oldItem: ReportesViewModel.DatoReporte,
            newItem: ReportesViewModel.DatoReporte
        ): Boolean {
            return oldItem == newItem
        }
    }
}

