package com.example.ensenando.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "logros")
data class LogroEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_logro")
    val idLogro: Int,
    
    @ColumnInfo(name = "titulo")
    val titulo: String?,
    
    @ColumnInfo(name = "descripcion")
    val descripcion: String?,
    
    // Campos solo en Room (NO en MySQL)
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "synced",
    
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
)


