package com.example.ensenando.data.remote.model

data class SyncRequest(
    val usuario_gestos: List<UsuarioGestoSyncItem>? = null,
    val docente_estudiante: List<DocenteEstudianteSyncItem>? = null
)

data class UsuarioGestoSyncItem(
    val id_usuario: Int,
    val id_gesto: Int,
    val porcentaje: Int,
    val estado: String,
    val last_updated: Long
)

data class DocenteEstudianteSyncItem(
    val id_docente: Int,
    val id_estudiante: Int,
    val estado: String,
    val last_updated: Long
)


