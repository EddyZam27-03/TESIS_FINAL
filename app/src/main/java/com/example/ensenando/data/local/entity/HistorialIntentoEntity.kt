package com.example.ensenando.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "historial_intentos",
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
    indices = [
        Index(value = ["id_usuario"]),
        Index(value = ["id_gesto"]),
        Index(value = ["sync_status"])
    ]
)
data class HistorialIntentoEntity(
    @PrimaryKey(autoGenerate = true)
    val id_historial: Int = 0,
    val id_usuario: Int,
    val id_gesto: Int,
    val porcentaje_obtenido: Int, // 0-100
    val fecha_intento: String, // timestamp ISO 8601
    val sync_status: String = "pending" // "pending" o "synced"
)
