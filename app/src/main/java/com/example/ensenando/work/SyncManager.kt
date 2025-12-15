package com.example.ensenando.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.remote.RetrofitClient
import com.example.ensenando.data.repository.DocenteEstudianteRepository
import com.example.ensenando.data.repository.GestoRepository
import com.example.ensenando.data.repository.ProgresoRepository
import com.example.ensenando.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object SyncManager {
    private const val SYNC_WORK_NAME = "sync_work"
    
    fun startPeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWork
        )
    }
    
    fun stopPeriodicSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
    }
    
    // ✅ MEJORADO: Sincronización inmediata bidireccional
    fun sincronizarInmediatamente(context: Context) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val apiService = RetrofitClient.apiService
                val progresoRepository = ProgresoRepository(context, database, apiService)
                val docenteEstudianteRepository = DocenteEstudianteRepository(context, database, apiService)
                val gestoRepository = GestoRepository(context, database, apiService)
                
                // ✅ PASO 1: Sincronizar gestos (catálogo)
                gestoRepository.syncGestos()
                
                // ✅ PASO 2: Sincronizar progreso (bidireccional: envía pendientes Y descarga del servidor)
                progresoRepository.syncProgreso()
                
                // ✅ PASO 3: Sincronizar relaciones (bidireccional)
                docenteEstudianteRepository.syncRelaciones()
                
                android.util.Log.d("SyncManager", "Sincronización inmediata completada")
            } catch (e: Exception) {
                android.util.Log.e("SyncManager", "Error en sincronización inmediata", e)
            }
        }
    }
}

