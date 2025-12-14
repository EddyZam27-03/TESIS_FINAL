package com.example.ensenando

import android.app.Application
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.util.SecurityUtils
import com.example.ensenando.util.ModelLoader
import com.example.ensenando.util.ThemeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EnsenandoApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    
    // ✅ NUEVO: Scope para operaciones en background
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        SecurityUtils.init(this)
        
        // ✅ NUEVO: Aplicar tema guardado al iniciar
        applicationScope.launch {
            try {
                ThemeUtils.aplicarTemaGuardado(this@EnsenandoApplication)
                android.util.Log.d("EnsenandoApplication", "Tema aplicado")
            } catch (e: Exception) {
                android.util.Log.e("EnsenandoApplication", "Error al aplicar tema", e)
            }
        }
        
        // ✅ MEJORADO: Precargar modelos críticos en background
        applicationScope.launch {
            try {
                ModelLoader.preloadCriticalModels(this@EnsenandoApplication)
                android.util.Log.d("EnsenandoApplication", "Modelos críticos precargados")
            } catch (e: Exception) {
                android.util.Log.e("EnsenandoApplication", "Error al precargar modelos", e)
            }
        }
        
        // ✅ NUEVO: Crear canal de notificaciones
        com.example.ensenando.util.NotificationManager.createNotificationChannel(this)
    }
}


