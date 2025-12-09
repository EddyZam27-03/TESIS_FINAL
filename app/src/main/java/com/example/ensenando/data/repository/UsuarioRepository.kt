package com.example.ensenando.data.repository

import android.content.Context
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.local.dao.UsuarioDao
import com.example.ensenando.data.local.entity.UsuarioEntity
import com.example.ensenando.data.remote.ApiService
import com.example.ensenando.data.remote.model.*
import com.example.ensenando.util.NetworkUtils
import com.example.ensenando.util.SecurityUtils
import kotlinx.coroutines.flow.Flow

class UsuarioRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val apiService: ApiService
) {
    private val usuarioDao: UsuarioDao = database.usuarioDao()
    
    suspend fun login(correo: String, contrasena: String): Result<UsuarioEntity> {
        return try {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                // ✅ CRITICAL FIX: Eliminar login offline con contraseña en texto plano
                // Se requiere conexión para iniciar sesión por seguridad
                return Result.failure(Exception("Se requiere conexión para iniciar sesión"))
            }
            
            val response = apiService.login(LoginRequest(correo, contrasena))
            val body = response.body()
            if (response.isSuccessful && body?.success == true) {
                val usuarioResponse = body.usuario ?: return Result.failure(Exception("Respuesta sin usuario"))
                
                // ✅ MEDIUM FIX: Validar ID antes de usar
                val idUsuario = usuarioResponse.id ?: usuarioResponse.id_usuario
                if (idUsuario == null || idUsuario <= 0) {
                    return Result.failure(Exception("ID de usuario inválido en respuesta del servidor"))
                }
                
                val usuario = UsuarioEntity(
                    idUsuario = idUsuario, // Ya validado
                    nombre = usuarioResponse.nombre,
                    correo = usuarioResponse.correo,
                    contrasena = null, // No guardar contraseña
                    rol = usuarioResponse.rol,
                    fechaRegistro = "", // No viene en la respuesta
                    syncStatus = "synced",
                    lastUpdated = System.currentTimeMillis()
                )
                
                usuarioDao.insertUsuario(usuario)
                SecurityUtils.saveUserId(context, usuario.idUsuario)
                SecurityUtils.saveUserRol(context, usuario.rol)
                SecurityUtils.saveUserNombre(context, usuario.nombre)
                SecurityUtils.saveUserCorreo(context, usuario.correo)
                body.token?.let { token: String -> SecurityUtils.saveToken(context, token) }
                
                Result.success(usuario)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error en login"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun register(nombre: String, correo: String, contrasena: String, rol: String? = null): Result<UsuarioEntity> {
        return try {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                return Result.failure(Exception("Se requiere conexión para registrar"))
            }
            
            val response = apiService.register(RegisterRequest(nombre, correo, contrasena, rol ?: "estudiante"))
            val body = response.body()
            if (response.isSuccessful && body?.success == true) {
                val usuarioResponse = body.usuario ?: return Result.failure(Exception("Respuesta sin usuario"))
                
                // ✅ MEDIUM FIX: Validar ID antes de usar
                val idUsuario = usuarioResponse.id ?: usuarioResponse.id_usuario
                if (idUsuario == null || idUsuario <= 0) {
                    return Result.failure(Exception("ID de usuario inválido en respuesta del servidor"))
                }
                
                val usuario = UsuarioEntity(
                    idUsuario = idUsuario, // Ya validado
                    nombre = usuarioResponse.nombre,
                    correo = usuarioResponse.correo,
                    contrasena = null, // No guardar contraseña
                    rol = usuarioResponse.rol,
                    fechaRegistro = "", // No viene en la respuesta
                    syncStatus = "synced",
                    lastUpdated = System.currentTimeMillis()
                )
                
                usuarioDao.insertUsuario(usuario)
                SecurityUtils.saveUserId(context, usuario.idUsuario)
                SecurityUtils.saveUserRol(context, usuario.rol)
                SecurityUtils.saveUserNombre(context, usuario.nombre)
                SecurityUtils.saveUserCorreo(context, usuario.correo)
                body.token?.let { token: String -> SecurityUtils.saveToken(context, token) }
                
                Result.success(usuario)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error en registro"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getUsuarioById(id: Int): Flow<UsuarioEntity?> {
        return kotlinx.coroutines.flow.flow {
            emit(usuarioDao.getUsuarioById(id))
        }
    }
    
    suspend fun getUsuarioByIdSuspend(id: Int): UsuarioEntity? {
        return usuarioDao.getUsuarioById(id)
    }
    
    fun getAllUsuarios(): Flow<List<UsuarioEntity>> {
        return usuarioDao.getAllUsuarios()
    }
}

