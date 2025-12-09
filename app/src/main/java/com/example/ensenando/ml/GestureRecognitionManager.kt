package com.example.ensenando.ml

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager para reconocimiento de gestos
 * - Usa MediaPipe Tasks 0.10.10 para detectar manos y extraer landmarks
 * - Usa modelo personalizado (modelo_lsp.tflite) para clasificar gestos
 * - Solo muestra el gesto correcto que se está practicando
 * 
 * CORREGIDO: 
 * - NO usa PoseDetector (solo manos según CONFIGURACION_FINAL.md)
 * - API correcta de MediaPipe Tasks 0.10.10
 * - Validación estricta de gesto correcto
 */
class GestureRecognitionManager(context: Context) : DefaultLifecycleObserver {
    
    private val appContext = context.applicationContext
    
    // Flujos de estado para observación
    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()
    
    private val _currentPrediction = MutableStateFlow<Pair<Int, Float>?>(null)
    val currentPrediction: StateFlow<Pair<Int, Float>?> = _currentPrediction.asStateFlow()
    
    // Detector de manos (MediaPipe Tasks 0.10.10)
    private var handDetector: HandDetector? = null
    
    // Clasificador de gestos (modelo personalizado)
    private var gestureClassifier: GestureClassifier? = null
    
    // Configuración de validación
    private var targetGestoId: Int = -1
    private val confidenceThreshold = 0.8f // 80% de confianza mínimo
    private val requiredConsecutiveFrames = 5 // 5 frames consecutivos con el gesto correcto
    
    // Estado interno para validación
    private var consecutiveFrames = 0
    private var lastDetectedGestoId = -1
    
    init {
        try {
            // Inicializar detectores
            handDetector = HandDetector(appContext)
            gestureClassifier = GestureClassifier(appContext)
        } catch (e: Exception) {
            android.util.Log.e("GestureRecognitionManager", "Error al inicializar", e)
        }
    }
    
    /**
     * Procesa un frame de video (Bitmap)
     * @param bitmap Frame de video a procesar
     * @param gestoId ID del gesto que se está practicando
     */
    fun processFrame(bitmap: Bitmap, gestoId: Int) {
        // Si cambió el gesto, reiniciar validación
        if (targetGestoId != gestoId) {
            resetProgress()
            targetGestoId = gestoId
        }
        
        try {
            // 1. Detectar manos usando MediaPipe Tasks 0.10.10
            val hands = handDetector?.detect(bitmap) ?: emptyList()
            
            if (hands.isEmpty()) {
                // No se detectó ninguna mano, reiniciar predicción
                synchronized(this) {
                    resetPrediction()
                }
                return
            }
            
            // 2. Usar la primera mano detectada (o la mano dominante)
            val hand = hands.firstOrNull() ?: return
            val landmarks = hand.landmarks // FloatArray(63)
            
            // 3. Clasificar gesto usando modelo personalizado
            val classification = gestureClassifier?.classify(landmarks)
            
            if (classification == null) {
                synchronized(this) {
                    resetPrediction()
                }
                return
            }
            
            val (detectedGestoId, confidence) = classification
            
            // 4. Validar que sea el gesto correcto y tenga suficiente confianza
            synchronized(this) {
                if (detectedGestoId == targetGestoId && confidence >= confidenceThreshold) {
                    // Es el gesto correcto con alta confianza
                    if (lastDetectedGestoId == detectedGestoId) {
                        consecutiveFrames++
                    } else {
                        consecutiveFrames = 1
                        lastDetectedGestoId = detectedGestoId
                    }
                    
                    // Solo mostrar si tenemos suficientes frames consecutivos
                    if (consecutiveFrames >= requiredConsecutiveFrames) {
                        _currentPrediction.value = Pair(detectedGestoId, confidence)
                        
                        // Actualizar progreso (0-100 basado en confianza)
                        val progressValue = ((confidence * 100).toInt()).coerceIn(0, 100)
                        _progress.value = progressValue
                    }
                } else {
                    // No es el gesto correcto o confianza baja, ignorar
                    resetPrediction()
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("GestureRecognitionManager", "Error al procesar frame", e)
            synchronized(this) {
                resetPrediction()
            }
        }
    }
    
    /**
     * Reinicia la predicción actual
     */
    private fun resetPrediction() {
        consecutiveFrames = 0
        lastDetectedGestoId = -1
        _currentPrediction.value = null
    }
    
    /**
     * Obtiene el progreso actual (0-100)
     */
    fun getCurrentProgress(): Int {
        return _progress.value
    }
    
    /**
     * Reinicia el progreso y el estado
     */
    fun resetProgress() {
        synchronized(this) {
            targetGestoId = -1
            consecutiveFrames = 0
            lastDetectedGestoId = -1
            _progress.value = 0
            _currentPrediction.value = null
        }
    }
    
    /**
     * Cierra los recursos
     */
    fun close() {
        try {
            handDetector?.close()
            handDetector = null
        } catch (e: Exception) {
            android.util.Log.e("GestureRecognitionManager", "Error al cerrar HandDetector", e)
        }
        try {
            gestureClassifier?.close()
            gestureClassifier = null
        } catch (e: Exception) {
            android.util.Log.e("GestureRecognitionManager", "Error al cerrar GestureClassifier", e)
        }
        resetProgress()
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        close()
    }
}
