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
 * ✅ CORREGIDO: Formato según entrenamiento del modelo
 * 
 * - Usa MediaPipe Tasks 0.10.14 para detectar:
 *   - Pose upper-body (7 puntos: nariz, hombros, codos, muñecas)
 *   - Ambas manos (right + left, 21 puntos cada una)
 * - Usa modelo personalizado (modelo_lsp.tflite) para clasificar gestos
 * - Input del modelo: (83, 147) = 83 frames × 147 features
 * - Solo muestra el gesto correcto que se está practicando
 */
class GestureRecognitionManager(context: Context) : DefaultLifecycleObserver {
    
    private val appContext = context.applicationContext
    
    // Flujos de estado para observación
    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()
    
    private val _currentPrediction = MutableStateFlow<Pair<Int, Float>?>(null)
    val currentPrediction: StateFlow<Pair<Int, Float>?> = _currentPrediction.asStateFlow()
    
    // Detectores
    private var poseDetector: PoseDetector? = null
    private var handDetector: HandDetector? = null
    private var gestureClassifier: GestureClassifier? = null
    
    // Buffer de secuencias (83 frames × 147 features)
    private val MAX_SEQUENCE_LENGTH = 83
    private val NUM_FEATURES = 147 // pose_upper(21) + right_hand(63) + left_hand(63)
    private val sequenceBuffer = mutableListOf<FloatArray>()
    
    // Índices de pose upper-body según MediaPipe
    private val poseUpperIndices = intArrayOf(0, 11, 12, 13, 14, 15, 16) // nariz, hombros, codos, muñecas
    
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
            poseDetector = PoseDetector(appContext)
            handDetector = HandDetector(appContext)
            gestureClassifier = GestureClassifier(appContext)
            android.util.Log.d("GestureRecognitionManager", "✅ Detectores inicializados")
        } catch (e: Exception) {
            android.util.Log.e("GestureRecognitionManager", "❌ Error al inicializar", e)
        }
    }
    
    /**
     * Procesa un frame de video (Bitmap)
     * @param bitmap Frame de video a procesar
     * @param gestoId ID del gesto que se está practicando
     */
    fun processFrame(bitmap: Bitmap, gestoId: Int) {
        // Si cambió el gesto, reiniciar validación y buffer
        if (targetGestoId != gestoId) {
            resetProgress()
            resetSequenceBuffer()
            targetGestoId = gestoId
        }
        
        try {
            // 1. Detectar pose upper-body
            val poseLandmarks = poseDetector?.detect(bitmap) ?: FloatArray(99) // 33 puntos × 3
            
            // 2. Detectar manos
            val hands = handDetector?.detect(bitmap) ?: emptyList()
            
            // 3. Construir features del frame actual (147 features)
            val frameFeatures = buildFrameFeatures(poseLandmarks, hands)
            
            // 4. Agregar al buffer de secuencias
            sequenceBuffer.add(frameFeatures)
            
            // 5. Mantener solo los últimos 83 frames
            if (sequenceBuffer.size > MAX_SEQUENCE_LENGTH) {
                sequenceBuffer.removeAt(0)
            }
            
            // 6. Si tenemos suficientes frames, clasificar
            if (sequenceBuffer.size >= MAX_SEQUENCE_LENGTH) {
                val sequence = prepareSequence()
                val classification = gestureClassifier?.classify(sequence)
                
                if (classification != null) {
                    val (detectedGestoId, confidence) = classification
                    
                    // 7. Validar que sea el gesto correcto y tenga suficiente confianza
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
                } else {
                    synchronized(this) {
                        resetPrediction()
                    }
                }
            } else {
                // Aún no tenemos suficientes frames, reiniciar predicción
                synchronized(this) {
                    resetPrediction()
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("GestureRecognitionManager", "❌ Error al procesar frame", e)
            synchronized(this) {
                resetPrediction()
            }
        }
    }
    
    /**
     * Construye features de un frame: pose_upper(21) + right_hand(63) + left_hand(63) = 147
     */
    private fun buildFrameFeatures(poseLandmarks: FloatArray, hands: List<HandDetector.Hand>): FloatArray {
        val features = FloatArray(NUM_FEATURES)
        var idx = 0
        
        // 1. Pose upper-body: 7 puntos × 3 = 21 features
        for (poseIdx in poseUpperIndices) {
            val base = poseIdx * 3
            if (base + 2 < poseLandmarks.size) {
                features[idx++] = poseLandmarks[base]     // x
                features[idx++] = poseLandmarks[base + 1]  // y
                features[idx++] = poseLandmarks[base + 2]  // z
            } else {
                // Si no hay pose, usar ceros
                features[idx++] = 0.0f
                features[idx++] = 0.0f
                features[idx++] = 0.0f
            }
        }
        
        // 2. Right hand: 21 puntos × 3 = 63 features
        val rightHand = hands.find { it.handedness.contains("Right", ignoreCase = true) }
        if (rightHand != null && rightHand.landmarks.size == 63) {
            for (i in 0 until 63) {
                features[idx++] = rightHand.landmarks[i]
            }
        } else {
            // Si no hay mano derecha, usar ceros
            for (i in 0 until 63) {
                features[idx++] = 0.0f
            }
        }
        
        // 3. Left hand: 21 puntos × 3 = 63 features
        val leftHand = hands.find { it.handedness.contains("Left", ignoreCase = true) }
        if (leftHand != null && leftHand.landmarks.size == 63) {
            for (i in 0 until 63) {
                features[idx++] = leftHand.landmarks[i]
            }
        } else {
            // Si no hay mano izquierda, usar ceros
            for (i in 0 until 63) {
                features[idx++] = 0.0f
            }
        }
        
        return features
    }
    
    /**
     * Prepara la secuencia para el modelo: (83, 147)
     * Si hay menos de 83 frames, hace padding con ceros al inicio
     */
    private fun prepareSequence(): Array<FloatArray> {
        val sequence = Array(MAX_SEQUENCE_LENGTH) { FloatArray(NUM_FEATURES) }
        
        // Si tenemos menos de 83 frames, hacer padding al inicio
        val paddingSize = MAX_SEQUENCE_LENGTH - sequenceBuffer.size
        var bufferIdx = 0
        
        // Padding con ceros
        for (i in 0 until paddingSize) {
            for (j in 0 until NUM_FEATURES) {
                sequence[i][j] = 0.0f
            }
        }
        
        // Copiar frames del buffer
        for (i in paddingSize until MAX_SEQUENCE_LENGTH) {
            if (bufferIdx < sequenceBuffer.size) {
                val frame = sequenceBuffer[bufferIdx]
                for (j in 0 until NUM_FEATURES) {
                    sequence[i][j] = if (j < frame.size) frame[j] else 0.0f
                }
                bufferIdx++
            } else {
                // Si no hay más frames, usar ceros
                for (j in 0 until NUM_FEATURES) {
                    sequence[i][j] = 0.0f
                }
            }
        }
        
        return sequence
    }
    
    /**
     * Reinicia el buffer de secuencias
     */
    private fun resetSequenceBuffer() {
        synchronized(this) {
            sequenceBuffer.clear()
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
            resetSequenceBuffer()
        }
    }
    
    /**
     * Cierra los recursos
     */
    fun close() {
        try {
            poseDetector?.close()
            poseDetector = null
        } catch (e: Exception) {
            android.util.Log.e("GestureRecognitionManager", "Error al cerrar PoseDetector", e)
        }
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
