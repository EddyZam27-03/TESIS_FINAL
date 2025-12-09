package com.example.ensenando.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import com.example.ensenando.util.ModelLoader
import java.nio.MappedByteBuffer
import java.nio.ByteOrder
import java.nio.ByteBuffer
import java.io.Closeable

/**
 * Clasificador de gestos usando modelo personalizado (modelo_lsp.tflite)
 * - Input: 63 valores (21 landmarks × 3 coordenadas)
 * - Output: 199 gestos clasificados
 * - Modelo ubicado en: app/src/main/assets/INFO/modelo_lsp.tflite
 * 
 * CORREGIDO: Usa context.assets.openFd() correctamente, NO FileInputStream
 */
class GestureClassifier(context: Context) : Closeable {
    
    private val appContext = context.applicationContext
    private var tfliteInterpreter: Interpreter? = null
    private var isInitialized = false
    
    // Dimensiones del modelo
    private val inputSize = 63 // 21 landmarks × 3 (x, y, z)
    private val numGestos = 199 // 199 gestos en el modelo
    
    init {
        loadModel()
    }
    
    /**
     * Carga el modelo TensorFlow Lite desde assets/INFO/modelo_lsp.tflite
     * CORREGIDO: Usa openFd() correctamente sin FileInputStream
     */
    private fun loadModel() {
        try {
            val modelPath = "INFO/modelo_lsp.tflite"
            // ✅ MEJORADO: Usar ModelLoader
            val modelBuffer = ModelLoader.loadModel(appContext, modelPath)
                ?: throw RuntimeException("No se pudo cargar el modelo: $modelPath")
            
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                setUseNNAPI(true)
            }
            tfliteInterpreter = Interpreter(modelBuffer, options)
            isInitialized = true
            android.util.Log.d("GestureClassifier", "Modelo cargado exitosamente: $modelPath")
        } catch (e: Exception) {
            android.util.Log.e("GestureClassifier", "Error al cargar modelo", e)
            isInitialized = false
        }
    }
    
    /**
     * Clasifica un gesto a partir de landmarks de mano
     * @param landmarks FloatArray(63) - 21 landmarks × 3 (x, y, z)
     * @return Pair<gestoId, confidence> o null si falla
     */
    fun classify(landmarks: FloatArray): Pair<Int, Float>? {
        if (!isInitialized || tfliteInterpreter == null) {
            return null
        }
        
        if (landmarks.size != inputSize) {
            android.util.Log.e("GestureClassifier", "Tamaño incorrecto: esperado $inputSize, recibido ${landmarks.size}")
            return null
        }
        
        return try {
            // Preparar input: [1, 63]
            val inputBuffer = ByteBuffer.allocateDirect(inputSize * 4).apply {
                order(ByteOrder.nativeOrder())
            }
            
            landmarks.forEach { value ->
                inputBuffer.putFloat(value)
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
            
            // Retornar gestoId (índice + 1) y confianza (0.0 - 1.0)
            Pair(maxIndex + 1, maxConfidence)
        } catch (e: Exception) {
            android.util.Log.e("GestureClassifier", "Error al clasificar", e)
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
