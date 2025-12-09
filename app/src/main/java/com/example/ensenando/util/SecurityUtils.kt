package com.example.ensenando.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Utilidad centralizada para manejar datos sensibles con EncryptedSharedPreferences.
 * Permite almacenar y recuperar información del usuario de manera segura.
 *
 * Todos los getters devuelven valores NO nulos para evitar errores tipo "String?".
 */
object SecurityUtils {

    private const val PREFS_NAME = "encrypted_prefs"

    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_ROL = "user_rol"
    private const val KEY_USER_NOMBRE = "user_nombre"
    private const val KEY_USER_CORREO = "user_correo"
    private const val KEY_USER_TOKEN = "user_token"

    // Para uso en repositorios o interceptores sin necesidad de contexto en pantalla
    @Volatile
    private var appContext: Context? = null

    /** Inicializa el contexto globalmente (desde el Application). */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** Obtiene las preferencias cifradas. */
    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ---------------------------------------------------------
    //  USER ID
    // ---------------------------------------------------------

    /** Guarda el ID del usuario. */
    fun saveUserId(context: Context, userId: Int) {
        getEncryptedPrefs(context).edit().putInt(KEY_USER_ID, userId).apply()
    }

    /** Obtiene el ID del usuario o -1 si no existe. */
    fun getUserId(context: Context): Int {
        return getEncryptedPrefs(context).getInt(KEY_USER_ID, -1)
    }

    // ---------------------------------------------------------
    //  USER ROL (NO NULLABLE)
    // ---------------------------------------------------------

    /** Guarda el rol del usuario. */
    fun saveUserRol(context: Context, rol: String) {
        getEncryptedPrefs(context).edit().putString(KEY_USER_ROL, rol).apply()
    }

    /** Obtiene el rol del usuario (no nulo). */
    fun getUserRol(context: Context): String {
        return getEncryptedPrefs(context).getString(KEY_USER_ROL, "") ?: ""
    }

    // ---------------------------------------------------------
    //  USER NOMBRE (NO NULLABLE)
    // ---------------------------------------------------------

    fun saveUserNombre(context: Context, nombre: String) {
        getEncryptedPrefs(context).edit().putString(KEY_USER_NOMBRE, nombre).apply()
    }

    fun getUserNombre(context: Context): String {
        return getEncryptedPrefs(context).getString(KEY_USER_NOMBRE, "") ?: ""
    }

    // ---------------------------------------------------------
    //  USER CORREO (NO NULLABLE)
    // ---------------------------------------------------------

    fun saveUserCorreo(context: Context, correo: String) {
        getEncryptedPrefs(context).edit().putString(KEY_USER_CORREO, correo).apply()
    }

    fun getUserCorreo(context: Context): String {
        return getEncryptedPrefs(context).getString(KEY_USER_CORREO, "") ?: ""
    }

    // ---------------------------------------------------------
    //  TOKEN (NO NULLABLE)
    // ---------------------------------------------------------

    fun saveToken(context: Context, token: String) {
        getEncryptedPrefs(context).edit().putString(KEY_USER_TOKEN, token).apply()
    }

    fun getToken(context: Context): String {
        return getEncryptedPrefs(context).getString(KEY_USER_TOKEN, "") ?: ""
    }

    /**
     * Versión sin contexto (solo funciona si SecurityUtils.init() fue llamado).
     * Devuelve null SOLO si no hay contexto (ej. antes del Application).
     */
    fun getTokenOrNull(): String? {
        val ctx = appContext ?: return null
        return getToken(ctx)
    }

    // ---------------------------------------------------------
    //  LOGIN STATE
    // ---------------------------------------------------------

    /** Retorna true si existe un userId válido. */
    fun isLoggedIn(context: Context): Boolean {
        return getUserId(context) != -1
    }

    // ---------------------------------------------------------
    //  CLEAR
    // ---------------------------------------------------------

    /** Borra TODA la información del usuario. */
    fun clearAll(context: Context) {
        getEncryptedPrefs(context).edit().clear().apply()
    }
}
