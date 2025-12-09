package com.example.ensenando.ml

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.example.ensenando.util.ModelLoader
import java.io.Closeable
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class PoseDetector(context: Context) : Closeable {

    private val appContext = context.applicationContext
    private var poseLandmarker: PoseLandmarker? = null
    private var isInitialized = false

    init {
        initialize()
    }

    private fun initialize() {
        try {
            android.util.Log.d("PoseDetector", "Iniciando carga del modelo...")
            val modelPath = "INFO/pose_landmark.tflite"
            android.util.Log.d("PoseDetector", "Cargando modelo desde: $modelPath")
            
            // ✅ VALIDACIÓN: Verificar que el archivo existe
            if (!assetFileExists(appContext, modelPath)) {
                throw RuntimeException("❌ El archivo del modelo NO existe: $modelPath")
            }
            
            // ✅ VALIDACIÓN: Verificar tamaño del archivo
            val fileSize = getAssetFileSize(appContext, modelPath)
            android.util.Log.d("PoseDetector", "Tamaño del archivo: $fileSize bytes")
            if (fileSize == 0L) {
                throw RuntimeException("❌ El archivo del modelo está vacío (0 bytes): $modelPath")
            }
            
            // ✅ MEJORADO: Usar ModelLoader
            val modelBuffer = ModelLoader.loadModel(appContext, modelPath)
                ?: throw RuntimeException("No se pudo cargar el modelo pose_landmark.tflite")
            android.util.Log.d("PoseDetector", "Modelo cargado, tamaño: ${modelBuffer.capacity()} bytes")

            val baseOptions = BaseOptions.builder()
                .setModelAssetBuffer(modelBuffer)
                .build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setNumPoses(1)
                .build()

            android.util.Log.d("PoseDetector", "Creando PoseLandmarker...")
            poseLandmarker = PoseLandmarker.createFromOptions(appContext, options)
            isInitialized = true
            android.util.Log.d("PoseDetector", "PoseLandmarker inicializado correctamente")

        } catch (e: UnsatisfiedLinkError) {
            // ✅ ERROR ESPECÍFICO: Librería nativa no encontrada
            android.util.Log.e("PoseDetector", "❌ ERROR CRÍTICO: Librería nativa de MediaPipe no encontrada", e)
            android.util.Log.e("PoseDetector", "Mensaje: ${e.message}")
            android.util.Log.e("PoseDetector", "Causa: ${e.cause?.message}")
            android.util.Log.e("PoseDetector", "SOLUCIÓN: Verifica que:")
            android.util.Log.e("PoseDetector", "  1. Estás usando un emulador/dispositivo ARM (NO x86/x86_64)")
            android.util.Log.e("PoseDetector", "  2. Las dependencias de MediaPipe están actualizadas (0.10.14+)")
            android.util.Log.e("PoseDetector", "  3. android:extractNativeLibs=\"true\" está en AndroidManifest")
            android.util.Log.e("PoseDetector", "  4. useLegacyPackaging = true está en build.gradle")
            isInitialized = false
        } catch (e: RuntimeException) {
            android.util.Log.e("PoseDetector", "❌ ERROR: ${e.message}", e)
            isInitialized = false
        } catch (e: Exception) {
            android.util.Log.e("PoseDetector", "❌ ERROR al inicializar PoseLandmarker", e)
            android.util.Log.e("PoseDetector", "Tipo de error: ${e.javaClass.simpleName}")
            android.util.Log.e("PoseDetector", "Mensaje: ${e.message}")
            isInitialized = false
        }
    }
    
    /**
     * Verifica si un archivo existe en assets
     */
    private fun assetFileExists(context: Context, path: String): Boolean {
        return try {
            context.assets.openFd(path).use { true }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Obtiene el tamaño de un archivo en assets
     */
    private fun getAssetFileSize(context: Context, path: String): Long {
        return try {
            context.assets.openFd(path).use { it.length }
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * @deprecated Usar ModelLoader.loadModel() en su lugar
     */
    @Deprecated("Usar ModelLoader.loadModel()")
    private fun loadModelFile(modelPath: String, context: Context): MappedByteBuffer {
        return ModelLoader.loadModel(context, modelPath)
            ?: throw RuntimeException("No se pudo cargar el modelo: $modelPath")
    }

    fun detect(bitmap: Bitmap): FloatArray {
        if (!isInitialized || poseLandmarker == null) return FloatArray(99)

        return try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result: PoseLandmarkerResult = poseLandmarker!!.detect(mpImage)
            extractLandmarks(result)
        } catch (e: Exception) {
            android.util.Log.e("PoseDetector", "Error detectando pose", e)
            FloatArray(99)
        }
    }

    private fun extractLandmarks(result: PoseLandmarkerResult): FloatArray {
        val data = FloatArray(99)
        if (result.landmarks().isNotEmpty()) {
            val pose = result.landmarks()[0]
            for (i in 0 until 33) {
                val base = i * 3
                data[base] = pose[i].x()
                data[base + 1] = pose[i].y()
                data[base + 2] = pose[i].z()
            }
        }
        return data
    }

    override fun close() {
        try {
            poseLandmarker?.close()
            poseLandmarker = null
            isInitialized = false
        } catch (e: Exception) {
            android.util.Log.e("PoseDetector", "Error cerrando", e)
        }
    }
}
