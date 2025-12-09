package com.example.ensenando.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "gestos")
data class GestoEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_gesto")
    val idGesto: Int,
    
    @ColumnInfo(name = "nombre")
    val nombre: String,
    
    @ColumnInfo(name = "dificultad")
    val dificultad: String?, // 'baja', 'media', 'alta'
    
    @ColumnInfo(name = "categoria")
    val categoria: String?,
    
    // Campos solo en Room (NO en MySQL)
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "synced",
    
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
)


