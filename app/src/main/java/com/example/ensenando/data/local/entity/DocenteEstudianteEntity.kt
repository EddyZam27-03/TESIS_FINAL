package com.example.ensenando.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "docenteestudiante",
    primaryKeys = ["id_docente", "id_estudiante"],
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id_usuario"],
            childColumns = ["id_docente"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id_usuario"],
            childColumns = ["id_estudiante"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["id_estudiante"])]
)
data class DocenteEstudianteEntity(
    @ColumnInfo(name = "id_docente")
    val idDocente: Int,
    
    @ColumnInfo(name = "id_estudiante")
    val idEstudiante: Int,
    
    @ColumnInfo(name = "estado")
    val estado: String = "pendiente", // 'pendiente', 'aceptado', 'rechazado'
    
    // Campos solo en Room (NO en MySQL)
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "synced",
    
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
)

