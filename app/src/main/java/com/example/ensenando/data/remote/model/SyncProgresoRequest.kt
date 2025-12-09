package com.example.ensenando.data.remote.model

data class SyncProgresoRequest(
    val id_usuario: Int,
    val id_gesto: Int,
    val porcentaje: Int,
    val estado: String
)

data class ActualizarProgresoRequest(
    val usuario_id: Int? = null,
    val id_usuario: Int? = null,
    val gesto_id: Int? = null,
    val id_gesto: Int? = null,
    val porcentaje: Int,
    val estado: String
)

data class EnviarSolicitudRequest(
    val id_docente: Int? = null,
    val docente_id: Int? = null,
    val id_estudiante: Int? = null,
    val estudiante_id: Int? = null,
    val usuario_id: Int? = null
)

data class ResponderSolicitudRequest(
    val id_docente: Int? = null,
    val docente_id: Int? = null,
    val id_estudiante: Int? = null,
    val estudiante_id: Int? = null,
    val usuario_id: Int? = null,
    val accion: String // "aceptar" o "rechazar"
)

data class EliminarRelacionRequest(
    val id_docente: Int,
    val id_estudiante: Int
)

data class DesbloquearLogroRequest(
    val id_usuario: Int? = null,
    val usuario_id: Int? = null,
    val id_logro: Int? = null,
    val logro_id: Int? = null,
    val fecha_obtenido: String? = null
)


