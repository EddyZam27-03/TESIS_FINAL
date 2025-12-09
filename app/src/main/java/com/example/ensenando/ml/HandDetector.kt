package com.example.ensenando.ml

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.io.Closeable

class HandDetector(context: Context) : Closeable {

    private val appContext = context.applicationContext
    private var handLandmarker: HandLandmarker? = null
    private var isInitialized = false

    data class Hand(
        val landmarks: FloatArray,
        val handedness: String
    )

    init {
        initialize()
    }

    private fun initialize() {
        try {
            android.util.Log.d("HandDetector", "Iniciando carga del modelo...")
            // ✅ CORREGIDO: Usar hand_landmark.task (nombre correcto del archivo)
            // Los archivos .task de MediaPipe deben cargarse con setModelAssetPath, NO con setModelAssetBuffer
            val modelPath = "INFO/hand_landmark.task"
            android.util.Log.d("HandDetector", "Cargando modelo desde: $modelPath")
            
            // ✅ VALIDACIÓN: Verificar que el archivo existe antes de intentar cargarlo
            if (!assetFileExists(appContext, modelPath)) {
                throw RuntimeException("❌ El archivo del modelo NO existe: $modelPath. Verifica que el archivo esté en app/src/main/assets/$modelPath")
            }
            
            // ✅ VALIDACIÓN: Verificar tamaño del archivo (no debe ser 0)
            val fileSize = getAssetFileSize(appContext, modelPath)
            android.util.Log.d("HandDetector", "Tamaño del archivo: $fileSize bytes")
            if (fileSize == 0L) {
                throw RuntimeException("❌ El archivo del modelo está vacío (0 bytes): $modelPath")
            }
            if (fileSize < 1024) {
                android.util.Log.w("HandDetector", "⚠️ ADVERTENCIA: El archivo del modelo es muy pequeño ($fileSize bytes). Debería ser varios MB.")
            }

            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(modelPath)
                .build()

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setMinHandDetectionConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setNumHands(2)
                .build()

            android.util.Log.d("HandDetector", "Creando HandLandmarker...")
            handLandmarker = HandLandmarker.createFromOptions(appContext, options)
            isInitialized = true
            android.util.Log.d("HandDetector", "HandLandmarker inicializado correctamente")

        } catch (e: UnsatisfiedLinkError) {
            // ✅ ERROR ESPECÍFICO: Librería nativa no encontrada
            android.util.Log.e("HandDetector", "❌ ERROR CRÍTICO: Librería nativa de MediaPipe no encontrada", e)
            android.util.Log.e("HandDetector", "Mensaje: ${e.message}")
            android.util.Log.e("HandDetector", "Causa: ${e.cause?.message}")
            android.util.Log.e("HandDetector", "SOLUCIÓN: Verifica que:")
            android.util.Log.e("HandDetector", "  1. Estás usando un emulador/dispositivo ARM (NO x86/x86_64)")
            android.util.Log.e("HandDetector", "  2. Las dependencias de MediaPipe están actualizadas (0.10.14+)")
            android.util.Log.e("HandDetector", "  3. android:extractNativeLibs=\"true\" está en AndroidManifest")
            android.util.Log.e("HandDetector", "  4. useLegacyPackaging = true está en build.gradle")
            isInitialized = false
        } catch (e: RuntimeException) {
            // ✅ ERROR ESPECÍFICO: Archivo no encontrado o vacío
            android.util.Log.e("HandDetector", "❌ ERROR: ${e.message}", e)
            android.util.Log.e("HandDetector", "SOLUCIÓN:")
            android.util.Log.e("HandDetector", "  1. Verifica que el archivo existe en: app/src/main/assets/INFO/hand_landmark.task")
            android.util.Log.e("HandDetector", "  2. Verifica que el archivo NO está vacío (debe ser varios MB)")
            android.util.Log.e("HandDetector", "  3. Limpia y reconstruye el proyecto (Build -> Clean Project -> Rebuild)")
            isInitialized = false
        } catch (e: Exception) {
            android.util.Log.e("HandDetector", "❌ ERROR al inicializar HandLandmarker", e)
            android.util.Log.e("HandDetector", "Tipo de error: ${e.javaClass.simpleName}")
            android.util.Log.e("HandDetector", "Mensaje: ${e.message}")
            android.util.Log.e("HandDetector", "Stack trace:", e)
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


    fun detect(bitmap: Bitmap): List<Hand> {
        if (!isInitialized || handLandmarker == null) return emptyList()

        return try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result: HandLandmarkerResult = handLandmarker!!.detect(mpImage)
            extractHands(result)
        } catch (e: Exception) {
            android.util.Log.e("HandDetector", "Error detectando manos", e)
            emptyList()
        }
    }

    private fun extractHands(result: HandLandmarkerResult): List<Hand> {
        val list = mutableListOf<Hand>()
        val landmarksList = result.landmarks()
        val handedList = result.handednesses()

        for (i in landmarksList.indices) {
            val lm = landmarksList[i]
            val arr = FloatArray(63) // 21 puntos * 3

            for (j in lm.indices) {
                val base = j * 3
                arr[base] = lm[j].x()
                arr[base + 1] = lm[j].y()
                arr[base + 2] = lm[j].z()
            }

            val handedness = if (handedList.isNotEmpty() && handedList[i].isNotEmpty()) {
                handedList[i][0].categoryName()
            } else "Unknown"

            list.add(Hand(arr, handedness))
        }

        return list
    }

    override fun close() {
        try {
            handLandmarker?.close()
            handLandmarker = null
            isInitialized = false
        } catch (e: Exception) {
            android.util.Log.e("HandDetector", "Error cerrando", e)
        }
    }
}
