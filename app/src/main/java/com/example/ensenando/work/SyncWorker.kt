package com.example.ensenando.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.repository.*
import com.example.ensenando.data.remote.RetrofitClient
import com.example.ensenando.util.NetworkUtils

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            if (!NetworkUtils.isNetworkAvailable(applicationContext)) {
                return Result.retry()
            }
            
            val database = AppDatabase.getDatabase(applicationContext)
            val apiService = RetrofitClient.apiService
            
            val progresoRepository = ProgresoRepository(applicationContext, database, apiService)
            val docenteEstudianteRepository = DocenteEstudianteRepository(applicationContext, database, apiService)
            val gestoRepository = GestoRepository(applicationContext, database, apiService)
            
            // ✅ PASO 1: Sincronizar gestos primero (catálogo)
            gestoRepository.syncGestos()
            
            // ✅ PASO 2: Sincronizar progreso (bidireccional: App → Servidor y Servidor → App)
            // Esto ahora descarga TODOS los datos del servidor, incluyendo los insertados directamente
            progresoRepository.syncProgreso()
            
            // ✅ PASO 3: Sincronizar relaciones docente-estudiante (bidireccional)
            docenteEstudianteRepository.syncRelaciones()
            
            android.util.Log.d("SyncWorker", "Sincronización completada exitosamente")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "Error en sincronización", e)
            Result.retry()
        }
    }
}

