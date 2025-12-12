package com.example.ensenando.ui.profile

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ensenando.databinding.ItemPdfPageBinding

/**
 * Adapter para mostrar páginas de un PDF usando PdfRenderer
 */
class PdfPageAdapter(
    private val pdfRenderer: PdfRenderer,
    private val onPageClick: ((Bitmap) -> Unit)? = null
) : ListAdapter<Int, PdfPageAdapter.PdfPageViewHolder>(PdfPageDiffCallback()) {

    init {
        // Inicializar la lista con índices de páginas
        val pages = (0 until pdfRenderer.pageCount).toList()
        submitList(pages)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfPageViewHolder {
        val binding = ItemPdfPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PdfPageViewHolder(binding, pdfRenderer, onPageClick)
    }

    override fun onBindViewHolder(holder: PdfPageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PdfPageViewHolder(
        private val binding: ItemPdfPageBinding,
        private val pdfRenderer: PdfRenderer,
        private val onPageClick: ((Bitmap) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pageIndex: Int) {
            try {
                // Abrir la página
                val page = pdfRenderer.openPage(pageIndex)
                
                // Calcular el tamaño del bitmap (escalar para que quepa en la pantalla)
                val width = page.width
                val height = page.height
                val scale = 2.0f // Escala para mejor calidad
                val scaledWidth = (width * scale).toInt()
                val scaledHeight = (height * scale).toInt()
                
                // Crear bitmap
                val bitmap = Bitmap.createBitmap(
                    scaledWidth,
                    scaledHeight,
                    Bitmap.Config.ARGB_8888
                )
                
                // Renderizar la página en el bitmap
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                // Mostrar en ImageView
                binding.ivPdfPage.setImageBitmap(bitmap)
                
                // Mostrar número de página
                binding.tvPageNumber.text = "Página ${pageIndex + 1}"
                
                // Click listener
                binding.root.setOnClickListener {
                    onPageClick?.invoke(bitmap)
                }
                
                // Cerrar la página
                page.close()
                
            } catch (e: Exception) {
                android.util.Log.e("PdfPageAdapter", "Error al renderizar página $pageIndex", e)
                binding.tvPageNumber.text = "Error al cargar página ${pageIndex + 1}"
            }
        }
    }

    class PdfPageDiffCallback : DiffUtil.ItemCallback<Int>() {
        override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
            return oldItem == newItem
        }
    }
}

