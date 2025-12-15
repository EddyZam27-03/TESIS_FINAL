package com.example.ensenando.ui.profile

import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ensenando.databinding.DialogReporteBinding
import java.io.File

class ReporteDialogFragment : DialogFragment() {
    private var _binding: DialogReporteBinding? = null
    private val binding get() = _binding!!
    private var pdfPath: String? = null
    private var pdfRenderer: PdfRenderer? = null
    private var pdfPageAdapter: PdfPageAdapter? = null

    companion object {
        private const val ARG_PDF_PATH = "pdf_path"

        fun newInstance(pdfPath: String): ReporteDialogFragment {
            val fragment = ReporteDialogFragment()
            val args = Bundle()
            args.putString(ARG_PDF_PATH, pdfPath)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ✅ FIX: Usar el tema de la app para que respete modo claro/oscuro
        setStyle(STYLE_NORMAL, com.example.ensenando.R.style.Theme_Ensenando)
        pdfPath = arguments?.getString(ARG_PDF_PATH)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogReporteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ NUEVO: Configurar RecyclerView para mostrar páginas del PDF
        binding.rvPdfPages.layoutManager = LinearLayoutManager(requireContext())
        
        // ✅ NUEVO: Cargar y mostrar el PDF visualmente
        cargarYMostrarPDF(pdfPath)

        binding.btnDescargarPdf.setOnClickListener {
            pdfPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(
                            requireContext(),
                            "No se encontró aplicación para abrir PDF",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        binding.btnCompartir.setOnClickListener {
            pdfPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(intent, "Compartir reporte"))
                }
            }
        }

        binding.btnCerrar.setOnClickListener {
            dismiss()
        }
    }

    /**
     * ✅ NUEVO: Carga el PDF y lo muestra visualmente usando PdfRenderer
     */
    private fun cargarYMostrarPDF(pdfPath: String?) {
        pdfPath?.let { path ->
            val file = File(path)
            if (!file.exists()) {
                mostrarError("El archivo del reporte no existe")
                return
            }
            
            try {
                // Cerrar renderer anterior si existe
                pdfRenderer?.close()
                pdfRenderer = null
                
                // Abrir el archivo PDF
                val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(fileDescriptor)
                
                // Crear adapter para mostrar las páginas
                pdfPageAdapter = PdfPageAdapter(
                    pdfRenderer = pdfRenderer!!,
                    onPageClick = null
                )
                
                // Asignar adapter al RecyclerView
                binding.rvPdfPages.adapter = pdfPageAdapter
                
                // Ocultar mensaje de error si estaba visible
                binding.tvReportePath.visibility = View.GONE
                
                android.util.Log.d("ReporteDialogFragment", "PDF cargado exitosamente: ${pdfRenderer!!.pageCount} páginas")
                
            } catch (e: Exception) {
                android.util.Log.e("ReporteDialogFragment", "Error al cargar PDF", e)
                mostrarError("Error al cargar PDF: ${e.message}")
            }
        } ?: run {
            mostrarError("No se proporcionó ruta del reporte")
        }
    }
    
    /**
     * Muestra un mensaje de error en el TextView
     */
    private fun mostrarError(mensaje: String) {
        binding.tvReportePath.text = mensaje
        binding.tvReportePath.visibility = View.VISIBLE
        binding.rvPdfPages.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cerrar el renderer del PDF para liberar recursos
        pdfRenderer?.close()
        pdfRenderer = null
        pdfPageAdapter = null
        _binding = null
    }
}

