package com.example.ensenando.data.remote

import com.example.ensenando.data.remote.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // ========== AUTENTICACIÓN ==========
    @POST("login.php")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    @POST("registro.php")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>
    
    // ========== GESTOS ==========
    @GET("listar_gestos.php")
    suspend fun getGestos(): Response<GestosResponse>
    
    @GET("obtener_gestos_usuario.php")
    suspend fun getGestosUsuario(
        @Query("usuario_id") usuarioId: Int? = null,
        @Query("id_usuario") idUsuario: Int? = null
    ): Response<List<UsuarioGestoResponse>>
    
    // ========== HOME Y ESTADÍSTICAS ==========
    @GET("obtener_home_data.php")
    suspend fun getHomeData(
        @Query("usuario_id") usuarioId: Int? = null,
        @Query("id_usuario") idUsuario: Int? = null,
        @Query("categoria") categoria: String? = null
    ): Response<HomeDataResponse>
    
    @GET("obtener_progreso_usuarios.php")
    suspend fun getProgresoUsuarios(
        @Query("usuario_id") usuarioId: Int? = null,
        @Query("id_usuario") idUsuario: Int? = null,
        @Query("id_admin") idAdmin: Int? = null,
        @Query("id_estudiante") idEstudiante: Int? = null
    ): Response<ProgresoResponse>
    
    // ========== PROGRESO ==========
    @POST("actualizar_progreso_gesto.php")
    suspend fun actualizarProgresoGesto(@Body request: ActualizarProgresoRequest): Response<ApiResponse<UsuarioGestoResponse>>
    
    @POST("sync_progreso.php")
    suspend fun syncProgreso(@Body request: SyncProgresoRequest): Response<ApiResponse<Unit>>
    
    // ========== SOLICITUDES DOCENTE-ESTUDIANTE ==========
    @POST("enviar_solicitud_docente.php")
    suspend fun enviarSolicitudDocente(@Body request: EnviarSolicitudRequest): Response<ApiResponse<Unit>>
    
    @POST("responder_solicitud.php")
    suspend fun responderSolicitud(@Body request: ResponderSolicitudRequest): Response<ApiResponse<Unit>>
    
    @GET("consultar_solicitud_estudiante.php")
    suspend fun consultarSolicitudEstudiante(
        @Query("id_estudiante") idEstudiante: Int? = null,
        @Query("usuario_id") usuarioId: Int? = null,
        @Query("id_usuario") idUsuario: Int? = null
    ): Response<SolicitudesResponse>
    
    @GET("listar_solicitudes_docente.php")
    suspend fun listarSolicitudesDocente(
        @Query("id_docente") idDocente: Int? = null,
        @Query("usuario_id") usuarioId: Int? = null,
        @Query("id_usuario") idUsuario: Int? = null,
        @Query("estado") estado: String? = null
    ): Response<SolicitudesResponse>
    
    @POST("eliminar_relacion_docente.php")
    suspend fun eliminarRelacionDocente(@Body request: EliminarRelacionRequest): Response<ApiResponse<Unit>>
    
    // ========== PROGRESO Y LOGROS PARA DOCENTES ==========
    @GET("obtener_progreso_estudiante_docente.php")
    suspend fun getProgresoEstudianteDocente(
        @Query("id_docente") idDocente: Int? = null,
        @Query("usuario_id") usuarioId: Int? = null,
        @Query("id_usuario") idUsuario: Int? = null,
        @Query("id_estudiante") idEstudiante: Int? = null,
        @Query("estudiante_id") estudianteId: Int? = null
    ): Response<ProgresoResponse>
    
    @GET("obtener_logros_estudiante_docente.php")
    suspend fun getLogrosEstudianteDocente(
        @Query("id_docente") idDocente: Int? = null,
        @Query("usuario_id") usuarioId: Int? = null,
        @Query("id_usuario") idUsuario: Int? = null,
        @Query("id_estudiante") idEstudiante: Int? = null,
        @Query("estudiante_id") estudianteId: Int? = null
    ): Response<LogrosResponse>
    
    // ========== LOGROS ==========
    @GET("obtener_logros_usuarios.php")
    suspend fun getLogrosUsuarios(
        @Query("usuario_id") usuarioId: Int? = null,
        @Query("id_usuario") idUsuario: Int? = null,
        @Query("id_admin") idAdmin: Int? = null,
        @Query("id_estudiante") idEstudiante: Int? = null
    ): Response<List<LogrosResponse>>
    
    @POST("desbloquear_logro.php")
    suspend fun desbloquearLogro(@Body request: DesbloquearLogroRequest): Response<ApiResponse<Unit>>
    
    // ========== BÚSQUEDA ==========
    @GET("listar_docentes.php")
    suspend fun listarDocentes(): Response<List<UsuarioResponse>>
    
    @GET("buscar_estudiante.php")
    suspend fun buscarEstudiante(
        @Query("busqueda") busqueda: String? = null,
        @Query("correo") correo: String? = null
    ): Response<List<UsuarioResponse>>
    
    @GET("listar_estudiantes_docente.php")
    suspend fun listarEstudiantesDocente(
        @Query("id_docente") idDocente: Int? = null,
        @Query("usuario_id") usuarioId: Int? = null
    ): Response<List<UsuarioResponse>>
}

