package com.example.ensenando.data.remote.model

data class UsuarioResponse(
    val id: Int? = null,
    val id_usuario: Int? = null,
    val nombre: String,
    val correo: String,
    val contrasena: String? = null,
    val rol: String,
    val fecha_registro: String? = null
) {
    // ✅ LOW FIX: Propiedad computada para obtener el ID normalizado
    val normalizedId: Int
        get() = id ?: id_usuario ?: throw IllegalStateException("Usuario sin ID")
    
    @Deprecated("Usar normalizedId en su lugar", ReplaceWith("normalizedId"))
    fun getIdUsuario(): Int {
        return normalizedId
    }
}

