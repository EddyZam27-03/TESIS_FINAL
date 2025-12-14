package com.example.ensenando.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import com.example.ensenando.util.ModelLoader
import java.nio.ByteOrder
import java.nio.ByteBuffer
import java.io.Closeable

/**
 * Clasificador de gestos usando modelo personalizado (modelo_lsp.tflite)
 * ✅ CORREGIDO: Formato de input según entrenamiento del modelo
 * 
 * Input esperado por el modelo:
 * - Shape: (1, 83, 147) o (83, 147)
 * - MAX_SEQUENCE_LENGTH = 83 frames
 * - NUM_FEATURES = 147 por frame:
 *   - Pose upper-body: 7 puntos × 3 = 21 features
 *   - Right hand: 21 puntos × 3 = 63 features
 *   - Left hand: 21 puntos × 3 = 63 features
 * - Total: 83 × 147 = 12,201 valores
 * 
 * Output: 199 gestos clasificados
 * - Modelo ubicado en: app/src/main/assets/INFO/modelo_lsp.tflite
 */
class GestureClassifier(context: Context) : Closeable {
    
    private val appContext = context.applicationContext
    private var tfliteInterpreter: Interpreter? = null
    private var isInitialized = false
    
    // Dimensiones del modelo según entrenamiento
    private val MAX_SEQUENCE_LENGTH = 83 // Frames de secuencia temporal
    private val NUM_FEATURES = 147 // Features por frame (pose + ambas manos)
    private val numGestos = 199 // 199 gestos en el modelo
    
    // Índices de pose upper-body según MediaPipe (7 puntos)
    // 0: nariz, 11: hombro izq, 12: hombro der, 13: codo izq, 14: codo der, 15: muñeca izq, 16: muñeca der
    private val poseUpperIndices = intArrayOf(0, 11, 12, 13, 14, 15, 16)
    
    init {
        loadModel()
    }
    
    /**
     * Carga el modelo TensorFlow Lite desde assets/INFO/modelo_lsp.tflite
     */
    private fun loadModel() {
        try {
            val modelPath = "INFO/modelo_lsp.tflite"
            val modelBuffer = ModelLoader.loadModel(appContext, modelPath)
                ?: throw RuntimeException("No se pudo cargar el modelo: $modelPath")
            
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                setUseNNAPI(true)
            }
            tfliteInterpreter = Interpreter(modelBuffer, options)
            isInitialized = true
            android.util.Log.d("GestureClassifier", "✅ Modelo cargado exitosamente: $modelPath")
            android.util.Log.d("GestureClassifier", "   Input shape esperado: (1, $MAX_SEQUENCE_LENGTH, $NUM_FEATURES)")
            android.util.Log.d("GestureClassifier", "   Output: $numGestos gestos")
        } catch (e: Exception) {
            android.util.Log.e("GestureClassifier", "❌ Error al cargar modelo", e)
            isInitialized = false
        }
    }
    
    /**
     * Clasifica un gesto a partir de una secuencia de frames
     * @param sequence Array de shape (83, 147) - 83 frames × 147 features
     *                 Cada frame contiene: [pose_upper(21) + right_hand(63) + left_hand(63)]
     * @return Pair<gestoId, confidence> o null si falla
     */
    fun classify(sequence: Array<FloatArray>): Pair<Int, Float>? {
        if (!isInitialized || tfliteInterpreter == null) {
            android.util.Log.w("GestureClassifier", "Modelo no inicializado")
            return null
        }
        
        // Validar dimensiones
        if (sequence.size != MAX_SEQUENCE_LENGTH) {
            android.util.Log.e("GestureClassifier", "❌ Tamaño de secuencia incorrecto: esperado $MAX_SEQUENCE_LENGTH frames, recibido ${sequence.size}")
            return null
        }
        
        if (sequence.isNotEmpty() && sequence[0].size != NUM_FEATURES) {
            android.util.Log.e("GestureClassifier", "❌ Tamaño de features incorrecto: esperado $NUM_FEATURES, recibido ${sequence[0].size}")
            return null
        }
        
        return try {
            // Preparar input: [1, 83, 147] = 12,201 valores
            val inputSize = MAX_SEQUENCE_LENGTH * NUM_FEATURES
            val inputBuffer = ByteBuffer.allocateDirect(inputSize * 4).apply {
                order(ByteOrder.nativeOrder())
            }
            
            // Aplanar secuencia: (83, 147) -> (12,201)
            for (frame in sequence) {
                for (feature in frame) {
                    inputBuffer.putFloat(feature)
                }
            }
            
            // Preparar output: [1, 199]
            val outputBuffer = ByteBuffer.allocateDirect(numGestos * 4).apply {
                order(ByteOrder.nativeOrder())
            }
            
            // Ejecutar modelo
            val inputArray = arrayOf(inputBuffer)
            val outputArray = arrayOf(outputBuffer)
            tfliteInterpreter!!.run(inputArray, outputArray)
            
            // Leer resultados
            outputBuffer.rewind()
            val predictions = FloatArray(numGestos)
            outputBuffer.asFloatBuffer().get(predictions)
            
            // Encontrar el gesto con mayor confianza
            var maxIndex = 0
            var maxConfidence = predictions[0]
            predictions.forEachIndexed { index, confidence ->
                if (confidence > maxConfidence) {
                    maxConfidence = confidence
                    maxIndex = index
                }
            }
            
            android.util.Log.d("GestureClassifier", "✅ Predicción: gestoId=${maxIndex + 1}, confianza=${maxConfidence}")
            
            // Retornar gestoId (índice + 1) y confianza (0.0 - 1.0)
            Pair(maxIndex + 1, maxConfidence)
        } catch (e: Exception) {
            android.util.Log.e("GestureClassifier", "❌ Error al clasificar", e)
            null
        }
    }
    
    override fun close() {
        try {
            tfliteInterpreter?.close()
            tfliteInterpreter = null
            isInitialized = false
        } catch (e: Exception) {
            android.util.Log.e("GestureClassifier", "Error al cerrar", e)
        }
    }
}
