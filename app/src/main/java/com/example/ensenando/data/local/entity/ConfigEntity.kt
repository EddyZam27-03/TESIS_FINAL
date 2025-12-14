package com.example.ensenando.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "config")
data class ConfigEntity(
    @PrimaryKey
    val clave: String,
    val valor: String
)
