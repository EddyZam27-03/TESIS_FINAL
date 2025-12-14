package com.example.ensenando.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.repository.ConfigRepository

object ThemeUtils {
    
    /**
     * Aplicar tema manualmente
     */
    fun aplicarTema(context: Context, modoOscuro: Boolean) {
        val modo = if (modoOscuro) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(modo)
    }
    
    /**
     * Aplicar tema automático (seguir sistema)
     */
    fun aplicarTemaAutomatico() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
    
    /**
     * Obtener tema guardado y aplicarlo
     */
    suspend fun aplicarTemaGuardado(context: Context) {
        val database = AppDatabase.getDatabase(context)
        val configRepository = ConfigRepository(database)
        val tema = configRepository.getTema()
        
        when (tema) {
            "dark" -> aplicarTema(context, true)
            "light" -> aplicarTema(context, false)
            else -> aplicarTemaAutomatico()
        }
    }
    
    /**
     * Guardar preferencia de tema
     */
    suspend fun guardarTema(context: Context, modoOscuro: Boolean) {
        val database = AppDatabase.getDatabase(context)
        val configRepository = ConfigRepository(database)
        val tema = if (modoOscuro) "dark" else "light"
        configRepository.guardarTema(tema)
    }
    
    /**
     * Guardar tema automático
     */
    suspend fun guardarTemaAuto(context: Context) {
        val database = AppDatabase.getDatabase(context)
        val configRepository = ConfigRepository(database)
        configRepository.guardarTema("auto")
    }
    
    /**
     * Obtener tema actual guardado
     */
    suspend fun obtenerTemaGuardado(context: Context): String {
        val database = AppDatabase.getDatabase(context)
        val configRepository = ConfigRepository(database)
        return configRepository.getTema()
    }
}
