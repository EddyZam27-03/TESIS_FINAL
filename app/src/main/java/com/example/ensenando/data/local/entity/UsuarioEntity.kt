package com.example.ensenando.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_usuario")
    val idUsuario: Int,
    
    @ColumnInfo(name = "nombre")
    val nombre: String,
    
    @ColumnInfo(name = "correo")
    val correo: String,
    
    @ColumnInfo(name = "contrasena")
    val contrasena: String?,
    
    @ColumnInfo(name = "rol")
    val rol: String, // 'administrador', 'docente', 'estudiante'
    
    @ColumnInfo(name = "fecha_registro")
    val fechaRegistro: String,
    
    // Campos solo en Room (NO en MySQL)
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "synced", // "pending", "synced", "error"
    
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
)


