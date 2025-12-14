package com.example.ensenando.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import com.example.ensenando.databinding.DialogReporteBinding
import java.io.File

class ReporteDialogFragment : DialogFragment() {
    private var _binding: DialogReporteBinding? = null
    private val binding get() = _binding!!
    private var pdfPath: String? = null

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

        // ✅ MEJORADO: Mostrar contenido del reporte antes de descargar
        mostrarContenidoReporte(pdfPath)

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

    private fun mostrarContenidoReporte(pdfPath: String?) {
        pdfPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                // Intentar leer el contenido del PDF y mostrar resumen
                try {
                    // Mostrar información básica del archivo
                    val fileSize = file.length() / 1024 // KB
                    binding.tvReportePath.text = """
                        Reporte generado exitosamente
                        
                        Archivo: ${file.name}
                        Tamaño: ${fileSize} KB
                        Ubicación: ${file.parent}
                        
                        Puede visualizar el PDF completo haciendo clic en "Abrir PDF" o compartirlo con "Compartir".
                    """.trimIndent()
                } catch (e: Exception) {
                    binding.tvReportePath.text = "Reporte generado:\n$path"
                }
            } else {
                binding.tvReportePath.text = "Error: El archivo del reporte no existe"
            }
        } ?: run {
            binding.tvReportePath.text = "Error: No se proporcionó ruta del reporte"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}