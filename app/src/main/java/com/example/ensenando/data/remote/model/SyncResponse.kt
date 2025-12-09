package com.example.ensenando.data.remote.model

data class SyncResponse(
    val usuario_gestos: List<UsuarioGestoResponse>? = null,
    val docente_estudiante: List<DocenteEstudianteResponse>? = null
)


