package com.example.ensenando.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.ensenando.databinding.FragmentActivityBinding
import com.example.ensenando.ui.camera.CameraActivity
import com.example.ensenando.util.VideoLoader
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File

/**
 * Fragment para mostrar actividad de gesto
 * - Muestra video del gesto desde assets/INFO/GESTOS/
 * - Muestra progreso del usuario
 * - Permite practicar el gesto con cámara
 */
class ActivityFragment : Fragment() {
    private var _binding: FragmentActivityBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ActivityViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Obtener ID del gesto desde los argumentos
        val idGesto = arguments?.getInt("idGesto") ?: 0
        
        // ✅ CORRECCIÓN: Validar idGesto y mostrar mensaje si es inválido
        if (idGesto == 0) {
            android.util.Log.e("ActivityFragment", "ID de gesto inválido o no proporcionado")
            android.widget.Toast.makeText(
                requireContext(),
                "Error: No se proporcionó un ID de gesto válido",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            binding.tvGestoNombre.text = "Gesto no encontrado"
            return
        }
        
        android.util.Log.d("ActivityFragment", "Cargando gesto con ID: $idGesto")
        
        setupObservers()
        setupClickListeners()
        
        // ✅ FIX: Esperar a que la vista esté completamente renderizada antes de cargar
        binding.root.post {
            android.util.Log.d("ActivityFragment", "Vista lista, cargando gesto...")
            viewModel.loadGesto(idGesto)
        }
    }
    
    private fun setupObservers() {
        // Observar gesto cargado
        viewModel.gesto.observe(viewLifecycleOwner) { gesto ->
            if (gesto == null) {
                android.util.Log.w("ActivityFragment", "Gesto es null, no se puede cargar")
                binding.tvGestoNombre.text = "Gesto no encontrado"
                showVideoError()
                return@observe
            }
            
            try {
                android.util.Log.d("ActivityFragment", "Gesto cargado: ${gesto.nombre} (ID: ${gesto.idGesto})")
                binding.tvGestoNombre.text = gesto.nombre
                
                // ✅ NUEVO: Mostrar categoría
                if (gesto.categoria != null) {
                    val partes = gesto.categoria.split(" - ")
                    if (partes.size >= 2) {
                        binding.chipCategoria.text = "${partes[0]} > ${partes[1]}"
                        binding.chipCategoria.visibility = ViewGroup.VISIBLE
                    } else {
                        binding.chipCategoria.text = gesto.categoria
                        binding.chipCategoria.visibility = ViewGroup.VISIBLE
                    }
                } else {
                    binding.chipCategoria.visibility = ViewGroup.GONE
                }
                
                // ✅ NUEVO: Mostrar dificultad
                if (gesto.dificultad != null) {
                    binding.chipDificultad.text = gesto.dificultad
                    binding.chipDificultad.visibility = ViewGroup.VISIBLE
                    // Colores según dificultad
                    when (gesto.dificultad.uppercase()) {
                        "FÁCIL", "FACIL" -> {
                            binding.chipDificultad.setChipBackgroundColorResource(android.R.color.holo_green_light)
                        }
                        "MEDIO", "MEDIA" -> {
                            binding.chipDificultad.setChipBackgroundColorResource(android.R.color.holo_orange_light)
                        }
                        "DIFÍCIL", "DIFICIL" -> {
                            binding.chipDificultad.setChipBackgroundColorResource(android.R.color.holo_red_light)
                        }
                    }
                } else {
                    binding.chipDificultad.visibility = ViewGroup.GONE
                }
                
                // ✅ FIX: Asegurar que el VideoView esté visible
                binding.videoView.visibility = View.VISIBLE
                
                // ✅ MEJORADO: Cargar video directamente sin múltiples post
                lifecycleScope.launch {
                    loadVideoImproved(gesto.nombre)
                }
            } catch (e: Exception) {
                android.util.Log.e("ActivityFragment", "Error al procesar gesto cargado", e)
                binding.tvGestoNombre.text = "Error al cargar gesto"
                showVideoError()
            }
        }
        
        // ✅ NUEVO: Observar historial de intentos
        viewModel.historialIntentos.observe(viewLifecycleOwner) { intentos ->
            if (intentos.isNotEmpty()) {
                binding.rvHistorialIntentos.visibility = ViewGroup.VISIBLE
                binding.tvHistorialVacio.visibility = ViewGroup.GONE
                val adapter = HistorialIntentoAdapter()
                binding.rvHistorialIntentos.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
                binding.rvHistorialIntentos.adapter = adapter
                adapter.submitList(intentos)
            } else {
                binding.rvHistorialIntentos.visibility = ViewGroup.GONE
                binding.tvHistorialVacio.visibility = ViewGroup.VISIBLE
            }
        }
        
        // Observar progreso actual
        viewModel.progresoActual.observe(viewLifecycleOwner) { progreso ->
            progreso?.let { progresoEntity: com.example.ensenando.data.local.entity.UsuarioGestoEntity ->
                binding.progressBar.progress = progresoEntity.porcentaje
                binding.tvPorcentaje.text = "${progresoEntity.porcentaje}%"
            } ?: run {
                binding.progressBar.progress = 0
                binding.tvPorcentaje.text = "0%"
            }
        }
        
        // Observar progreso en tiempo real
        viewModel.progress.observe(viewLifecycleOwner) { progress ->
            binding.progressBar.progress = progress
            binding.tvPorcentaje.text = "$progress%"
        }
        
        // Observar predicción actual
        viewModel.prediction.observe(viewLifecycleOwner) { prediction ->
            prediction?.let { (gestoId: Int, confidence: Float) ->
                // Si la predicción coincide con el gesto actual y tiene alta confianza
                if (gestoId == viewModel.gesto.value?.idGesto && confidence > 0.7f) {
                    // Actualizar progreso visualmente
                    lifecycleScope.launch {
                        viewModel.saveProgress()
                    }
                }
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnPracticar.setOnClickListener {
            val idGesto = viewModel.gesto.value?.idGesto ?: return@setOnClickListener
            val intent = Intent(requireContext(), CameraActivity::class.java).apply {
                putExtra("idGesto", idGesto)
            }
            startActivity(intent)
        }
        
        // Configurar toolbar
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
    
    /**
     * ✅ MEJORADO: Usa el nuevo sistema VideoLoader
     * ✅ FIX: Carga en hilo IO y configura en Main cuando la vista esté lista
     */
    private suspend fun loadVideoImproved(gestoNombre: String) {
        try {
            android.util.Log.d("ActivityFragment", "Buscando video para: $gestoNombre")
            val videoUri = VideoLoader.loadVideoUri(requireContext(), gestoNombre)
            
            withContext(Dispatchers.Main) {
                if (videoUri != null) {
                    android.util.Log.d("ActivityFragment", "Video encontrado, configurando VideoView... URI: $videoUri")
                    // ✅ FIX: Usar post solo una vez para asegurar que la vista esté lista
                    binding.videoView.post {
                        setupVideoView(videoUri, gestoNombre)
                    }
                } else {
                    android.util.Log.w("ActivityFragment", "Video no encontrado: $gestoNombre")
                    showVideoError()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ActivityFragment", "Error al cargar video", e)
            withContext(Dispatchers.Main) {
                showVideoError()
            }
        }
    }
    
    /**
     * Configura el VideoView con el URI del video
     * ✅ FIX: Simplificado para evitar problemas de timing
     */
    private fun setupVideoView(videoUri: android.net.Uri, videoName: String) {
        try {
            android.util.Log.d("ActivityFragment", "Configurando VideoView con URI: $videoUri")
            
            // Asegurar que el VideoView esté visible y tenga tamaño
            binding.videoView.visibility = View.VISIBLE
            binding.videoView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            binding.videoView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            
            // Configurar listeners ANTES de establecer el URI
            binding.videoView.setOnPreparedListener { mediaPlayer ->
                android.util.Log.d("ActivityFragment", "✅ Video preparado: $videoName, duración: ${mediaPlayer.duration}ms, ancho: ${mediaPlayer.videoWidth}, alto: ${mediaPlayer.videoHeight}")
                videoCargado = true
                mediaPlayer.isLooping = true
                
                // ✅ FIX: Asegurar que el VideoView esté visible antes de iniciar
                binding.videoView.visibility = View.VISIBLE
                binding.videoView.start()
                android.util.Log.d("ActivityFragment", "✅ Video iniciado")
            }
            
            binding.videoView.setOnErrorListener { _, what, extra ->
                android.util.Log.e("ActivityFragment", "❌ Error al reproducir video: what=$what, extra=$extra")
                showVideoError()
                videoCargado = false
                true
            }
            
            binding.videoView.setOnCompletionListener {
                android.util.Log.d("ActivityFragment", "Video completado: $videoName")
            }
            
            binding.videoView.setOnInfoListener { _, what, extra ->
                when (what) {
                    android.media.MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                        android.util.Log.d("ActivityFragment", "Video buffering...")
                    }
                    android.media.MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                        android.util.Log.d("ActivityFragment", "Video buffering completado")
                    }
                    android.media.MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                        android.util.Log.d("ActivityFragment", "✅ Video renderizando")
                    }
                }
                false
            }
            
            // Configurar MediaController
            val mediaController = MediaController(requireContext())
            mediaController.setAnchorView(binding.videoView)
            binding.videoView.setMediaController(mediaController)
            
            // ✅ FIX: Establecer URI después de configurar listeners
            binding.videoView.setVideoURI(videoUri)
            binding.videoView.requestFocus()
            
            android.util.Log.d("ActivityFragment", "URI establecido, esperando preparación...")
            
        } catch (e: Exception) {
            android.util.Log.e("ActivityFragment", "Error al configurar VideoView", e)
            showVideoError()
            videoCargado = false
        }
    }
    
    /**
     * @deprecated Usar loadVideoImproved() en su lugar
     */
    @Deprecated("Usar loadVideoImproved()")
    private suspend fun loadVideo(gestoNombre: String) {
        try {
            android.util.Log.d("ActivityFragment", "Buscando video: $gestoNombre en assets/INFO/GESTOS")
            
            val formatos = listOf("mp4", "3gp", "webm")
            val categorias = listOf("ACADEMICO", "BASICO", "SOCIAL")
            
            // Generar múltiples variantes del nombre para búsqueda
            val variantesNombre = generateNameVariants(gestoNombre)
            android.util.Log.d("ActivityFragment", "Variantes a buscar: $variantesNombre")
            
            // 1. Búsqueda recursiva en todas las subcarpetas
            for (categoria in categorias) {
                for (nombreVariante in variantesNombre) {
                    for (formato in formatos) {
                        // Buscar directamente en categoría
                        val videoPath1 = "INFO/GESTOS/$categoria/$nombreVariante.$formato"
                        if (tryLoadVideo(videoPath1, nombreVariante)) {
                            return
                        }
                        
                        // Buscar en subcarpetas conocidas
                        val subcarpetas = listOf(
                            "ABECEDARIO", "Frases esenciales", "MESES", "Necesidades basicas", 
                            "Numero", "Acciones academicas", "Asignaturas", "Conceptos academicos",
                            "Material escolar", "Actividades cotidianas", "Colores", 
                            "Comunicacion basica", "Emociones y sentimientos", "Familia y relaciones",
                            "Lugares y direcciones", "Saludos y despedidas", "Tiempo"
                        )
                        
                        for (subcarpeta in subcarpetas) {
                            val videoPath2 = "INFO/GESTOS/$categoria/$subcarpeta/$nombreVariante.$formato"
                            if (tryLoadVideo(videoPath2, nombreVariante)) {
                                android.util.Log.d("ActivityFragment", "Video encontrado en subcarpeta: $videoPath2")
                                return
                            }
                        }
                    }
                }
            }
            
            // 2. Buscar directamente en GESTOS (sin categoría)
            for (nombreVariante in variantesNombre) {
                for (formato in formatos) {
                    val videoPath = "INFO/GESTOS/$nombreVariante.$formato"
                    if (tryLoadVideo(videoPath, nombreVariante)) {
                        return
                    }
                }
            }
            
            // 3. Búsqueda recursiva usando listDir (más lento pero más completo)
            android.util.Log.d("ActivityFragment", "Intentando búsqueda recursiva completa...")
            val videoEncontrado = buscarVideoRecursivo("INFO/GESTOS", variantesNombre, formatos)
            if (videoEncontrado != null) {
                loadVideoFromAssets(videoEncontrado.first, videoEncontrado.second)
                return
            }
            
            android.util.Log.w("ActivityFragment", "Video no encontrado después de buscar todas las variantes: $variantesNombre")
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                showVideoError()
            }
        } catch (e: Exception) {
            android.util.Log.e("ActivityFragment", "Error al cargar video", e)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                showVideoError()
            }
        }
    }
    
    /**
     * Genera múltiples variantes del nombre del gesto para búsqueda
     */
    private fun generateNameVariants(gestoNombre: String): List<String> {
        val variantes = mutableListOf<String>()
        
        // 1. Nombre original en minúsculas
        variantes.add(gestoNombre.lowercase())
        
        // 2. Nombre normalizado (sin acentos, sin caracteres especiales)
        val normalizado = gestoNombre
            .lowercase()
            .replace(" ", "_")
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u").replace("ñ", "n")
            .replace("[^a-z0-9_]".toRegex(), "")
        if (normalizado != gestoNombre.lowercase()) {
            variantes.add(normalizado)
        }
        
        // 3. Nombre con solo espacios reemplazados por guiones bajos
        val conGuiones = gestoNombre.lowercase().replace(" ", "_")
        if (conGuiones != gestoNombre.lowercase() && conGuiones != normalizado) {
            variantes.add(conGuiones)
        }
        
        // 4. Nombre exacto (para casos como "A", "B", "1", "2")
        variantes.add(gestoNombre)
        
        // 5. Nombre en mayúsculas (para casos como "A", "B")
        if (gestoNombre.length <= 3) {
            variantes.add(gestoNombre.uppercase())
        }
        
        return variantes.distinct()
    }
    
    /**
     * Busca video recursivamente en todas las subcarpetas
     */
    private suspend fun buscarVideoRecursivo(
        basePath: String,
        nombres: List<String>,
        formatos: List<String>
    ): Pair<String, String>? {
        return try {
            val assets = requireContext().assets
            val lista = assets.list(basePath) ?: return null
            
            // Buscar archivos de video directamente en esta carpeta
            for (item in lista) {
                for (nombre in nombres) {
                    for (formato in formatos) {
                        if (item.equals("$nombre.$formato", ignoreCase = true)) {
                            val videoPath = "$basePath/$item"
                            android.util.Log.d("ActivityFragment", "Video encontrado recursivamente: $videoPath")
                            return Pair(videoPath, nombre)
                        }
                    }
                }
            }
            
            // Buscar recursivamente en subcarpetas
            for (item in lista) {
                val subPath = "$basePath/$item"
                try {
                    // Intentar listar (si es carpeta, funcionará; si es archivo, fallará)
                    val subLista = assets.list(subPath)
                    if (subLista != null && subLista.isNotEmpty()) {
                        // Es una carpeta, buscar recursivamente
                        val resultado = buscarVideoRecursivo(subPath, nombres, formatos)
                        if (resultado != null) {
                            return resultado
                        }
                    }
                } catch (e: Exception) {
                    // No es una carpeta o no se puede acceder, continuar
                }
            }
            
            null
        } catch (e: Exception) {
            android.util.Log.w("ActivityFragment", "Error en búsqueda recursiva: ${e.message}")
            null
        }
    }
    
    /**
     * Intenta cargar un video desde la ruta especificada
     * @return true si el video se encontró, false si no se encontró
     */
    private suspend fun tryLoadVideo(videoPath: String, videoName: String): Boolean {
        return try {
            // Verificar que existe
            requireContext().assets.openFd(videoPath).use {
                // Video encontrado, cargarlo
                android.util.Log.d("ActivityFragment", "Video encontrado: $videoPath")
                loadVideoFromAssets(videoPath, videoName)
                true
            }
        } catch (e: Exception) {
            // Video no encontrado en esta ruta, continuar buscando
            false
        }
    }
    
    /**
     * Carga video desde assets copiándolo a filesDir y usando FileProvider
     * CORREGIDO: Usa filesDir en lugar de cacheDir para mejor compatibilidad con FileProvider
     */
    private suspend fun loadVideoFromAssets(videoPath: String, videoName: String) {
        return try {
            android.util.Log.d("ActivityFragment", "Abriendo asset: $videoPath")
            
            // ✅ CORRECCIÓN: Asegurar que estamos en el hilo principal para UI
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                binding.videoView.visibility = View.VISIBLE
            }
            
            // Copiar desde assets a archivo temporal en filesDir (no cacheDir) - en hilo de I/O
            val assetFd = requireContext().assets.openFd(videoPath)
            val tempFile = File(requireContext().filesDir, "${videoName}_temp.mp4")
            android.util.Log.d("ActivityFragment", "Copiando video a: ${tempFile.absolutePath}")
            
            assetFd.createInputStream().use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            assetFd.close()
            
            // Verificar que el archivo se copió correctamente
            if (!tempFile.exists() || tempFile.length() == 0L) {
                throw Exception("El archivo no se copió correctamente o está vacío")
            }
            
            android.util.Log.d("ActivityFragment", "Video copiado exitosamente: ${tempFile.absolutePath}, tamaño: ${tempFile.length()} bytes")
            
            // ✅ CORRECCIÓN: Ejecutar configuración del video en el hilo principal
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    try {
                        // Usar FileProvider para Android 10+
                        val videoUri = FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            tempFile
                        )
                        
                        android.util.Log.d("ActivityFragment", "URI del video creado: $videoUri")
                        
                        // Configurar VideoView
                        val mediaController = MediaController(requireContext())
                        mediaController.setAnchorView(binding.videoView)
                        binding.videoView.setMediaController(mediaController)
                        binding.videoView.setVideoURI(videoUri)
                        binding.videoView.requestFocus()
                        
                        // ✅ CORRECCIÓN: Asegurar que el VideoView esté visible
                        binding.videoView.visibility = View.VISIBLE
                        
                        binding.videoView.setOnPreparedListener { mediaPlayer ->
                            val duracion = mediaPlayer.duration
                            android.util.Log.d("ActivityFragment", "Video preparado exitosamente: $videoName, duración: ${duracion}ms")
                            binding.videoView.start()
                        }
                        
                        binding.videoView.setOnErrorListener { _, what, extra ->
                            android.util.Log.e("ActivityFragment", "Error al reproducir video: what=$what, extra=$extra")
                            showVideoError()
                            true
                        }
                        
                        binding.videoView.setOnCompletionListener {
                            android.util.Log.d("ActivityFragment", "Video completado: $videoName")
                        }
                        
                    } catch (e: Exception) {
                        android.util.Log.e("ActivityFragment", "Error al configurar VideoView: ${e.message}", e)
                        showVideoError()
                    }
                }
                
        } catch (e: Exception) {
            android.util.Log.e("ActivityFragment", "Error al cargar video desde assets: ${e.message}", e)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                showVideoError()
            }
        }
    }
    
    private fun showVideoError() {
        // ✅ CORRECCIÓN: No ocultar el VideoView, mostrar mensaje de error
        android.util.Log.w("ActivityFragment", "No se pudo cargar el video")
        // Mantener el VideoView visible pero con mensaje (opcional)
        // binding.videoView.visibility = View.VISIBLE
        // Podrías agregar un TextView de error aquí si lo deseas
    }
    
    private var videoCargado = false
    
    override fun onResume() {
        super.onResume()
        android.util.Log.d("ActivityFragment", "onResume() - Reintentando cargar video si es necesario")
        
        // ✅ FIX: Cargar video en onResume() si no se ha cargado aún
        val gesto = viewModel.gesto.value
        if (gesto != null) {
            // Verificar si el video ya está cargado usando flag
            if (!videoCargado) {
                android.util.Log.d("ActivityFragment", "Video no cargado aún, cargando en onResume()...")
                binding.root.post {
                    binding.videoView.visibility = View.VISIBLE
                    lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        loadVideoImproved(gesto.nombre)
                    }
                }
            } else {
                // Video ya cargado, solo reanudar si está pausado
                if (binding.videoView.isPlaying.not() && binding.videoView.currentPosition > 0) {
                    binding.videoView.start()
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

