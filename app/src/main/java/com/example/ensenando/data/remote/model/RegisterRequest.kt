package com.example.ensenando.data.remote.model

data class RegisterRequest(
    val nombre: String,
    val correo: String,
    val contrasena: String,
    val rol: String,
    val telefono: String? = null
)


