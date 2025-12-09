package com.example.ensenando.util

import android.content.Context
import android.util.Log
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * ✅ NUEVO: Sistema mejorado de carga de modelos
 * - Precarga modelos en memoria
 * - Cache de modelos cargados
 * - Manejo robusto de errores
 */
object ModelLoader {
    private const val TAG = "ModelLoader"
    private val modelCache = mutableMapOf<String, MappedByteBuffer>()
    
    /**
     * Carga un modelo desde assets
     * @param context Contexto de la aplicación
     * @param modelPath Ruta del modelo en assets (ej: "INFO/pose_landmark.tflite")
     * @return MappedByteBuffer del modelo o null si falla
     */
    fun loadModel(context: Context, modelPath: String): MappedByteBuffer? {
        return try {
            // Verificar cache
            modelCache[modelPath]?.let {
                Log.d(TAG, "Modelo encontrado en cache: $modelPath")
                return it
            }
            
            // Cargar modelo
            val modelBuffer = loadModelFromAssets(context, modelPath)
            
            // Guardar en cache
            if (modelBuffer != null) {
                modelCache[modelPath] = modelBuffer
                Log.d(TAG, "Modelo cargado exitosamente: $modelPath, tamaño: ${modelBuffer.capacity()} bytes")
            } else {
                Log.e(TAG, "Error al cargar modelo: $modelPath")
            }
            
            modelBuffer
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al cargar modelo: $modelPath", e)
            null
        }
    }
    
    /**
     * Carga modelo desde assets
     */
    private fun loadModelFromAssets(context: Context, modelPath: String): MappedByteBuffer? {
        var assetFd: android.content.res.AssetFileDescriptor? = null
        var inputStream: java.io.InputStream? = null
        
        return try {
            assetFd = context.assets.openFd(modelPath)
            inputStream = assetFd.createInputStream()
            val fileChannel = inputStream.channel
            
            val modelBuffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                assetFd.startOffset,
                assetFd.declaredLength
            )
            
            modelBuffer
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar modelo desde assets: $modelPath", e)
            null
        } finally {
            try {
                inputStream?.close()
                assetFd?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error al cerrar recursos del modelo", e)
            }
        }
    }
    
    /**
     * Verifica si un modelo existe en assets
     */
    fun modelExists(context: Context, modelPath: String): Boolean {
        return try {
            context.assets.openFd(modelPath).use { true }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Limpia el cache de modelos
     */
    fun clearCache() {
        modelCache.clear()
        Log.d(TAG, "Cache de modelos limpiado")
    }
    
    /**
     * Precarga modelos críticos
     * NOTA: hand_landmarker.task NO se precarga aquí porque usa setModelAssetPath directamente
     */
    fun preloadCriticalModels(context: Context) {
        val criticalModels = listOf(
            "INFO/pose_landmark.tflite",
            // ✅ hand_landmarker.task NO se precarga aquí - se carga directamente con setModelAssetPath
            "INFO/modelo_lsp.tflite"
        )
        
        criticalModels.forEach { modelPath ->
            try {
                if (modelExists(context, modelPath)) {
                    loadModel(context, modelPath)
                    Log.d(TAG, "Modelo precargado: $modelPath")
                } else {
                    Log.w(TAG, "Modelo no encontrado para precarga: $modelPath")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al precargar modelo: $modelPath", e)
            }
        }
    }
}

