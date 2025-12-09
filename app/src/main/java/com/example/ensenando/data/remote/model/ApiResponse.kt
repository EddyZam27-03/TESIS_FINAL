package com.example.ensenando.data.remote.model

// Respuesta genérica para endpoints que usan esta estructura
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val token: String? = null
)

// Respuesta de login (estructura específica)
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val usuario: UsuarioResponse? = null,
    val token: String? = null
)

// Respuesta de registro (estructura específica)
data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val usuario: UsuarioResponse? = null,
    val token: String? = null
)

// Respuesta de gestos
data class GestosResponse(
    val success: Boolean,
    val gestos: List<GestoResponse>? = null,
    val message: String? = null
)

// Respuesta de home
data class HomeDataResponse(
    val success: Boolean,
    val usuario: UsuarioHome? = null,
    val estadisticas: EstadisticasHome? = null,
    val actividades: List<ActividadHome>? = null,
    val categorias: List<CategoriaHome>? = null,
    val message: String? = null
)

data class UsuarioHome(
    val id_usuario: Int,
    val nombre: String,
    val correo: String,
    val rol: String
)

data class EstadisticasHome(
    val tiempo_total_minutos: Int? = 0,
    val promedio_progreso: Int? = null, // Puede ser null si no hay datos
    val actividades_incompletas: Int? = 0,
    val gestos_aprendidos: Int? = 0
)

data class ActividadHome(
    val id_gesto: Int,
    val nombre: String,
    val categoria: String,
    val dificultad: String?,
    val porcentaje: Int,
    val estado: String
)

data class CategoriaHome(
    val categoria: String,
    val total: Int,
    val aprendidos: Int
)

// Respuesta de progreso
data class ProgresoResponse(
    val tiempoTotal: Int? = null,
    val leccionesCompletadas: Int? = null,
    val totalLecciones: Int? = null,
    val precision: Float? = null,
    val rachaDias: Int? = null,
    val progreso: List<ProgresoDetalle>? = null
)

data class ProgresoDetalle(
    val id_usuario: Int? = null,
    val nombre: String? = null,
    val correo: String? = null,
    val id_gesto: Int? = null,
    val nombre_gesto: String? = null,
    val categoria: String? = null,
    val dificultad: String? = null,
    val porcentaje: Int,
    val estado: String,
    val total_gestos: Int? = null,
    val gestos_aprendidos: Int? = null,
    val promedio_progreso: Float? = null
)

// Respuesta de logros
data class LogrosResponse(
    val id: Int? = null,
    val nombre: String? = null,
    val descripcion: String? = null,
    val desbloqueado: Boolean? = null,
    val porcentajeAvance: Int? = null,
    val fechaDesbloqueo: String? = null,
    val id_usuario: Int? = null,
    val id_logro: Int? = null,
    val titulo: String? = null,
    val fecha_obtenido: String? = null,
    val total_logros: Int? = null,
    val logros_obtenidos: String? = null,
    val logros: List<LogroDetalle>? = null
)

data class LogroDetalle(
    val id_usuario: Int? = null,
    val nombre: String? = null,
    val correo: String? = null,
    val id_logro: Int? = null,
    val titulo: String? = null,
    val descripcion: String? = null,
    val fecha_obtenido: String? = null,
    val total_logros: Int? = null,
    val logros_obtenidos: String? = null
)

// Respuesta de solicitudes
data class SolicitudesResponse(
    val success: Boolean,
    val solicitudes: List<SolicitudDetalle>? = null,
    val docente_actual: DocenteInfo? = null,
    val total: Int? = null,
    val message: String? = null
)

data class SolicitudDetalle(
    val id_docente: Int,
    val id_estudiante: Int,
    val estado: String,
    val docente: DocenteInfo? = null,
    val estudiante: EstudianteInfo? = null
)

data class DocenteInfo(
    val id_usuario: Int,
    val nombre: String,
    val correo: String
)

data class EstudianteInfo(
    val id_usuario: Int,
    val nombre: String,
    val correo: String
)

