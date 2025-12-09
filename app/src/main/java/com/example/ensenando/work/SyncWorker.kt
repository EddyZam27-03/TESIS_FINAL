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
            
            // Sincronizar gestos primero
            gestoRepository.syncGestos()
            
            // Sincronizar progreso
            progresoRepository.syncProgreso()
            
            // Sincronizar relaciones docente-estudiante
            docenteEstudianteRepository.syncRelaciones()
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}


