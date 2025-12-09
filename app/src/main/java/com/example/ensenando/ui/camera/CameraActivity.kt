package com.example.ensenando.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.ensenando.databinding.ActivityCameraBinding
import com.example.ensenando.ui.activity.ActivityViewModel
import com.example.ensenando.util.ImageUtils.toBitmap
import com.example.ensenando.util.ImageUtils.resize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewModel: ActivityViewModel
    private var idGesto: Int = 0
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            // ✅ MEDIUM FIX: Verificar si fue denegado permanentemente
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                // Usuario denegó pero puede cambiar de opinión
                showPermissionRationaleDialog()
            } else {
                // Denegado permanentemente, redirigir a configuración
                showPermissionDeniedDialog()
            }
        }
    }
    
    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permiso de cámara requerido")
            .setMessage("La aplicación necesita acceso a la cámara para reconocer gestos. Por favor, otorga el permiso.")
            .setPositiveButton("Reintentar") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("Cancelar") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permiso de cámara requerido")
            .setMessage("La aplicación necesita acceso a la cámara para reconocer gestos. Por favor, habilítalo en Configuración.")
            .setPositiveButton("Abrir configuración") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        idGesto = intent.getIntExtra("idGesto", 0)
        if (idGesto == 0) {
            Toast.makeText(this, "ID de gesto no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // ✅ HIGH FIX: Inicializar ViewModel correctamente
        viewModel = ViewModelProvider(this)[ActivityViewModel::class.java]
        viewModel.loadGesto(idGesto)
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        
        viewModel.progress.observe(this) { progress ->
            binding.progressBar.progress = progress
            binding.tvPorcentaje.text = "$progress%"
        }
        
        binding.fabClose.setOnClickListener {
            viewModel.saveProgress()
            finish()
        }
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }
            
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun processImageProxy(imageProxy: ImageProxy) {
        try {
            // Convertir ImageProxy a Bitmap
            val bitmap = imageProxy.toBitmap()
            
            // Redimensionar para optimizar (opcional, pero recomendado)
            val resizedBitmap = bitmap.resize(640, 480)
            
            // ✅ MEDIUM FIX: Procesar en coroutine con dispatcher adecuado
            CoroutineScope(Dispatchers.Default).launch {
                viewModel.processFrame(resizedBitmap)
            }
            
            // Si el bitmap fue redimensionado, reciclar el original
            if (bitmap != resizedBitmap) {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // ✅ HIGH FIX: Pausar procesamiento para ahorrar recursos
        viewModel.resetProgress()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        // ✅ HIGH FIX: Cerrar GestureRecognitionManager explícitamente
        // (ViewModel.onCleared() ya lo hace, pero por seguridad)
        viewModel.resetProgress()
    }
}


