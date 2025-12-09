package com.example.ensenando.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "usuario_logros",
    primaryKeys = ["id_usuario", "id_logro"],
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id_usuario"],
            childColumns = ["id_usuario"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LogroEntity::class,
            parentColumns = ["id_logro"],
            childColumns = ["id_logro"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["id_logro"])]
)
data class UsuarioLogroEntity(
    @ColumnInfo(name = "id_usuario")
    val idUsuario: Int,
    
    @ColumnInfo(name = "id_logro")
    val idLogro: Int,
    
    @ColumnInfo(name = "fecha_obtenido")
    val fechaObtenido: String,
    
    // Campos solo en Room (NO en MySQL)
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "synced",
    
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
)

