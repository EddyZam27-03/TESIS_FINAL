package com.example.ensenando.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "usuario_gestos",
    primaryKeys = ["id_usuario", "id_gesto"],
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id_usuario"],
            childColumns = ["id_usuario"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GestoEntity::class,
            parentColumns = ["id_gesto"],
            childColumns = ["id_gesto"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["id_gesto"])]
)
data class UsuarioGestoEntity(
    @ColumnInfo(name = "id_usuario")
    val idUsuario: Int,
    
    @ColumnInfo(name = "id_gesto")
    val idGesto: Int,
    
    @ColumnInfo(name = "porcentaje")
    val porcentaje: Int = 0,
    
    @ColumnInfo(name = "estado")
    val estado: String = "pendiente", // 'pendiente', 'aprendido'
    
    // Campos solo en Room (NO en MySQL)
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "synced",
    
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
)

