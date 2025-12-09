package com.example.ensenando.util

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * ✅ NUEVO: Sistema mejorado de carga de videos
 * - Carga directa desde assets sin copiar archivos innecesariamente
 * - Cache inteligente de videos
 * - Búsqueda optimizada
 */
object VideoLoader {
    private const val TAG = "VideoLoader"
    private val videoCache = mutableMapOf<String, Uri>()
    
    /**
     * Carga un video desde assets y retorna su URI
     * @param context Contexto de la aplicación
     * @param gestoNombre Nombre del gesto a buscar
     * @return URI del video o null si no se encuentra
     */
    suspend fun loadVideoUri(context: Context, gestoNombre: String): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Verificar cache primero
                val cacheKey = gestoNombre.lowercase().trim()
                videoCache[cacheKey]?.let {
                    Log.d(TAG, "Video encontrado en cache: $gestoNombre")
                    return@withContext it
                }
                
                // 2. Buscar video en assets
                val videoPath = findVideoInAssets(context, gestoNombre)
                if (videoPath == null) {
                    Log.w(TAG, "Video no encontrado: $gestoNombre")
                    return@withContext null
                }
                
                // 3. Cargar video y crear URI
                val uri = loadVideoFromAssets(context, videoPath, gestoNombre)
                
                // 4. Guardar en cache
                if (uri != null) {
                    videoCache[cacheKey] = uri
                }
                
                uri
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar video: $gestoNombre", e)
                null
            }
        }
    }
    
    /**
     * Busca un video en assets de forma optimizada
     */
    private fun findVideoInAssets(context: Context, gestoNombre: String): String? {
        val assetManager = context.assets
        val formatos = listOf("mp4", "3gp", "webm")
        
        // Generar variantes del nombre
        val variantes = generateNameVariants(gestoNombre)
        
        // Buscar en estructura conocida: INFO/GESTOS/CATEGORIA/SUBCATEGORIA/nombre.mp4
        val categorias = listOf("ACADEMICO", "BASICO", "SOCIAL")
        val subcategorias = listOf(
            "ABECEDARIO", "Frases esenciales", "MESES", "Necesidades basicas", "Numero",
            "Acciones academicas", "Asignaturas", "Conceptos academicos", "Material escolar",
            "Actividades cotidianas", "Colores", "Comunicacion basica", 
            "Emociones y sentimientos", "Familia y relaciones",
            "Lugares y direcciones", "Saludos y despedidas", "Tiempo"
        )
        
        // 1. Buscar en subcategorías conocidas
        for (categoria in categorias) {
            for (subcategoria in subcategorias) {
                for (variante in variantes) {
                    for (formato in formatos) {
                        val path = "INFO/GESTOS/$categoria/$subcategoria/$variante.$formato"
                        if (assetExists(assetManager, path)) {
                            Log.d(TAG, "Video encontrado: $path")
                            return path
                        }
                    }
                }
            }
        }
        
        // 2. Buscar directamente en categorías
        for (categoria in categorias) {
            for (variante in variantes) {
                for (formato in formatos) {
                    val path = "INFO/GESTOS/$categoria/$variante.$formato"
                    if (assetExists(assetManager, path)) {
                        Log.d(TAG, "Video encontrado: $path")
                        return path
                    }
                }
            }
        }
        
        // 3. Búsqueda recursiva como último recurso
        return findVideoRecursive(assetManager, "INFO/GESTOS", variantes, formatos)
    }
    
    /**
     * Verifica si un asset existe
     */
    private fun assetExists(assetManager: android.content.res.AssetManager, path: String): Boolean {
        return try {
            assetManager.openFd(path).use { true }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Búsqueda recursiva en assets
     */
    private fun findVideoRecursive(
        assetManager: android.content.res.AssetManager,
        basePath: String,
        nombres: List<String>,
        formatos: List<String>
    ): String? {
        return try {
            val lista = assetManager.list(basePath) ?: return null
            
            // Buscar archivos directamente
            for (item in lista) {
                for (nombre in nombres) {
                    for (formato in formatos) {
                        if (item.equals("$nombre.$formato", ignoreCase = true)) {
                            val path = "$basePath/$item"
                            Log.d(TAG, "Video encontrado recursivamente: $path")
                            return path
                        }
                    }
                }
            }
            
            // Buscar en subcarpetas
            for (item in lista) {
                val subPath = "$basePath/$item"
                try {
                    val subLista = assetManager.list(subPath)
                    if (subLista != null && subLista.isNotEmpty()) {
                        val resultado = findVideoRecursive(assetManager, subPath, nombres, formatos)
                        if (resultado != null) return resultado
                    }
                } catch (e: Exception) {
                    // Continuar
                }
            }
            
            null
        } catch (e: Exception) {
            Log.w(TAG, "Error en búsqueda recursiva: ${e.message}")
            null
        }
    }
    
    /**
     * Genera variantes del nombre para búsqueda
     */
    private fun generateNameVariants(gestoNombre: String): List<String> {
        val variantes = mutableListOf<String>()
        
        // 1. Nombre original
        variantes.add(gestoNombre)
        
        // 2. Minúsculas
        variantes.add(gestoNombre.lowercase())
        
        // 3. Normalizado (sin acentos, sin caracteres especiales)
        val normalizado = gestoNombre
            .lowercase()
            .replace(" ", "_")
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u").replace("ñ", "n")
            .replace("[^a-z0-9_]".toRegex(), "")
        if (normalizado != gestoNombre.lowercase()) {
            variantes.add(normalizado)
        }
        
        // 4. Con guiones bajos
        val conGuiones = gestoNombre.lowercase().replace(" ", "_")
        if (conGuiones != gestoNombre.lowercase() && conGuiones != normalizado) {
            variantes.add(conGuiones)
        }
        
        // 5. Mayúsculas para nombres cortos (A, B, 1, 2, etc.)
        if (gestoNombre.length <= 3) {
            variantes.add(gestoNombre.uppercase())
        }
        
        return variantes.distinct()
    }
    
    /**
     * Carga video desde assets y crea URI usando FileProvider
     */
    private fun loadVideoFromAssets(context: Context, videoPath: String, videoName: String): Uri? {
        var assetFd: AssetFileDescriptor? = null
        var inputStream: java.io.InputStream? = null
        var outputStream: FileOutputStream? = null
        
        return try {
            assetFd = context.assets.openFd(videoPath)
            val tempFile = File(context.filesDir, "${videoName}_temp.mp4")
            
            // Solo copiar si el archivo no existe o es más antiguo
            if (!tempFile.exists() || tempFile.length() != assetFd.length) {
                inputStream = assetFd.createInputStream()
                outputStream = FileOutputStream(tempFile)
                inputStream.copyTo(outputStream)
                Log.d(TAG, "Video copiado: ${tempFile.absolutePath}, tamaño: ${tempFile.length()} bytes")
            } else {
                Log.d(TAG, "Video ya existe en cache: ${tempFile.absolutePath}")
            }
            
            // Crear URI usando FileProvider
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar video desde assets", e)
            null
        } finally {
            try {
                inputStream?.close()
                outputStream?.close()
                assetFd?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error al cerrar recursos", e)
            }
        }
    }
    
    /**
     * Limpia el cache de videos
     */
    fun clearCache() {
        videoCache.clear()
    }
}

